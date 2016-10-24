package org.zstack.kvm;

import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.springframework.transaction.annotation.Transactional;
import org.zstack.core.asyncbatch.AsyncBatchRunner;
import org.zstack.core.asyncbatch.LoopAsyncBatch;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.CloudBusCallBack;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.SimpleQuery;
import org.zstack.core.db.SimpleQuery.Op;
import org.zstack.core.errorcode.ErrorFacade;
import org.zstack.header.core.NoErrorCompletion;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.errorcode.OperationFailureException;
import org.zstack.header.errorcode.SysErrors;
import org.zstack.header.exception.CloudRuntimeException;
import org.zstack.header.message.MessageReply;
import org.zstack.header.rest.RESTFacade;
import org.zstack.header.storage.primary.PrimaryStorageConstant;
import org.zstack.header.storage.primary.PrimaryStorageStatus;
import org.zstack.header.storage.primary.PrimaryStorageVO;
import org.zstack.header.volume.VolumeStatus;
import org.zstack.header.volume.VolumeVO;
import org.zstack.header.volume.VolumeVO_;
import org.zstack.kvm.APIKvmFixVolumeSnapshotChainEvent.FixResult;

import javax.persistence.TypedQuery;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.stream.Collectors;

import static java.util.Arrays.asList;

/**
 * dirty code to fix diverged snapshot chain on NFS, LocalStorage
 */
@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class APIKvmFixVolumeSnapshotChainMsgHandler {
    @Autowired
    private DatabaseFacade dbf;
    @Autowired
    private CloudBus bus;
    @Autowired
    private RESTFacade restf;
    @Autowired
    private ErrorFacade errf;

    public static class Qcow2FileInfo {
        private String path;
        private String backingFile;
        private long size;

        public String getPath() {
            return path;
        }

        public void setPath(String path) {
            this.path = path;
        }

        public String getBackingFile() {
            return backingFile;
        }

        public void setBackingFile(String backingFile) {
            this.backingFile = backingFile;
        }

        public long getSize() {
            return size;
        }

        public void setSize(long size) {
            this.size = size;
        }
    }

    private Map<String, List<Qcow2FileInfo>> qcow2InfosByPsUuid = new HashMap<>();

    public void handle(APIKvmFixVolumeSnapshotChainMsg msg) {
        APIKvmFixVolumeSnapshotChainEvent evt = new APIKvmFixVolumeSnapshotChainEvent(msg.getId());
        List<VolumeVO> volumes = new ArrayList<>();

        if (msg.getPrimaryStorageUuid() != null) {
            if (!new Callable<Boolean>() {
                @Override
                @Transactional(readOnly = true)
                public Boolean call() {
                    String sql = "select count(*) from PrimaryStorageClusterRefVO ref, ClusterVO c where" +
                            " c.uuid = ref.clusterUuid and ref.primaryStorageUuid = :psUuid and" +
                            " cluster.hypervisorType = :hvType";
                    TypedQuery<Long> q = dbf.getEntityManager().createQuery(sql, Long.class);
                    q.setParameter("psUuid", msg.getPrimaryStorageUuid());
                    q.setParameter("hvType", KVMConstant.KVM_HYPERVISOR_TYPE);
                    long count = q.getSingleResult();
                    return count < 1;
                }
            }.call()) {
                throw new OperationFailureException(errf.stringToOperationError(
                        String.format("the primary storage[uuid:%s] is not attached to any KVM cluster", msg.getPrimaryStorageUuid())
                ));
            }

            SimpleQuery<VolumeVO> q = dbf.createQuery(VolumeVO.class);
            q.add(VolumeVO_.primaryStorageUuid, Op.EQ, msg.getPrimaryStorageUuid());
            q.add(VolumeVO_.status, Op.EQ, VolumeStatus.Ready);
            List<VolumeVO> vos = q.list();
            volumes.addAll(vos);
        } else {
            volumes.addAll(new Callable<List<VolumeVO>>() {
                @Override
                @Transactional(readOnly = true)
                public List<VolumeVO> call() {
                    String sql = "select v from PrimaryStorageClusterRefVO ref, ClusterVO c, VolumeVO v," +
                            " PrimaryStorageVO ps where" +
                            " c.uuid = ref.clusterUuid and ref.primaryStorageUuid = ps.uuid and" +
                            " cluster.hypervisorType = :hvType and v.primaryStorageUuid = ps.uuid" +
                            " and v.status = :status and ps.type in (:psType)";
                    TypedQuery<VolumeVO> q = dbf.getEntityManager().createQuery(sql, VolumeVO.class);
                    q.setParameter("hvType", KVMConstant.KVM_HYPERVISOR_TYPE);
                    q.setParameter("status", VolumeStatus.Ready);
                    q.setParameter("psType", asList("NFS", "LocalStorage"));
                    return q.getResultList();
                }
            }.call());
        }

        Map<String, List<VolumeVO>> volumeByPsUuid = new HashMap<>();
        for (VolumeVO volume : volumes) {
            List<VolumeVO> vols = volumeByPsUuid.get(volume.getPrimaryStorageUuid());
            if (vols == null) {
                vols = new ArrayList<>();
                volumeByPsUuid.put(volume.getPrimaryStorageUuid(), vols);
            }
            vols.add(volume);
        }

        for (Map.Entry<String, List<VolumeVO>> e : volumeByPsUuid.entrySet()) {
            String psUuid = e.getKey();
            KvmGetQcow2FileInfoPrimaryStorageMsg gmsg = new KvmGetQcow2FileInfoPrimaryStorageMsg();
            gmsg.setPrimaryStorageUuid(psUuid);
            gmsg.setVolumeUuids(e.getValue().stream().map(VolumeVO::getUuid).collect(Collectors.toList()));
            bus.makeTargetServiceIdByResourceUuid(gmsg, PrimaryStorageConstant.SERVICE_ID, psUuid);
            MessageReply reply = bus.call(gmsg);
            if (reply.isSuccess()) {
                KvmGetQcow2FileInfoPrimaryStorageReply kr = reply.castReply();
                qcow2InfosByPsUuid.put(psUuid, kr.getInfos());
            }
        }

        List<FixResult> results = new ArrayList<>();

        new LoopAsyncBatch<VolumeVO>() {
            Map<String, List<Qcow2FileInfo>> nfsQcow2Info = new HashMap<>();
            Map<String, List<Qcow2FileInfo>> localStorageQcow2Info = new HashMap<>();

            @Override
            protected Collection<VolumeVO> collect() {
                return volumes;
            }

            @Override
            protected AsyncBatchRunner forEach(VolumeVO volume) {
                return new AsyncBatchRunner() {
                    @Override
                    public void run(NoErrorCompletion completion) {
                        PrimaryStorageVO ps = dbf.findByUuid(volume.getPrimaryStorageUuid(), PrimaryStorageVO.class);
                        List<Qcow2FileInfo> qcow2FileInfos;
                        if (ps.getType().equals("NFS")) {
                            qcow2FileInfos = nfsQcow2Info.get(ps.getUuid());
                            if (qcow2FileInfos == null) {
                                KvmGetQcow2FileInfoPrimaryStorageMsg gmsg = new KvmGetQcow2FileInfoPrimaryStorageMsg();
                                gmsg.setPrimaryStorageUuid(ps.getUuid());
                                bus.makeTargetServiceIdByResourceUuid(gmsg, PrimaryStorageConstant.SERVICE_ID, ps.getUuid());
                                MessageReply reply = bus.call(gmsg);
                                if (!reply.isSuccess()) {
                                    FixResult res = new FixResult();
                                    res.setVolumeUuid(volume.getUuid());
                                    res.setVolumeName(volume.getName());
                                    res.setSuccess(false);
                                    res.setError(reply.getError());
                                    results.add(res);
                                } else {
                                    KvmGetQcow2FileInfoPrimaryStorageReply gr = reply.castReply();
                                    qcow2FileInfos = gr.getInfos();
                                    nfsQcow2Info.put(ps.getUuid(), qcow2FileInfos);
                                }
                            }
                        } else if (ps.getType().equals("LocalStorage")) {
                            qcow2FileInfos = nfsQcow2Info.get(ps.getUuid());
                        } else {
                            throw new CloudRuntimeException(String.format("unsupported primary storage type[%s]", ps.getType()));
                        }
                    }
                };
            }

            @Override
            protected void done() {

            }
        }.start();


        new LoopAsyncBatch<String>() {
            @Override
            protected Collection<String> collect() {
                return primaryStorageUuids;
            }

            @Override
            protected AsyncBatchRunner forEach(String psUuid) {
                return new AsyncBatchRunner() {
                    @Override
                    public void run(NoErrorCompletion completion) {
                        PrimaryStorageVO ps = dbf.findByUuid(psUuid, PrimaryStorageVO.class);
                        // only NFS and LocalStorage need to support this API
                        if (!ps.getType().equals("NFS") && !ps.getType().equals("LocalStorage")) {
                            completion.done();
                            return;
                        }

                        if (ps.getStatus() != PrimaryStorageStatus.Connected) {
                            fail(psUuid, errf.stringToOperationError(String.format("the primary storage[uuid:%s, name:%s] is not" +
                                    " connected, current status is %s", ps.getUuid(), ps.getName(), ps.getStatus())));
                            completion.done();
                            return;
                        }

                        GetVolumeBackingFileRelationshipOnPrimaryStorageMsg gmsg = new GetVolumeBackingFileRelationshipOnPrimaryStorageMsg();
                        gmsg.setPrimaryStorageUuid(psUuid);
                        bus.makeTargetServiceIdByResourceUuid(gmsg, PrimaryStorageConstant.SERVICE_ID, psUuid);
                        bus.send(gmsg, new CloudBusCallBack(completion) {
                            @Override
                            public void run(MessageReply reply) {
                                if (!reply.isSuccess()) {
                                    fail(psUuid, errf.instantiateErrorCode(SysErrors.OPERATION_ERROR, String.format("unable to get" +
                                                    " volume backing file relationship on the primary storage[uuid:%s, name:%s]",
                                            ps.getUuid(), ps.getName()), reply.getError()));
                                } else {
                                    fix(psUuid, ((GetVolumeBackingFileRelationshipOnPrimaryStorageReply) reply).getQcow2FileInfo());
                                }

                                completion.done();
                            }
                        });
                    }

                    private void fail(String psUuid, ErrorCode errorCode) {
                        List<VolumeVO> vols = volumes.stream().filter(vol -> vol.getPrimaryStorageUuid().equals(psUuid))
                                .collect(Collectors.toList());

                        synchronized (results) {
                            results.addAll(vols.stream().map(vol -> {
                                FixResult res = new FixResult();
                                res.setVolumeUuid(vol.getUuid());
                                res.setVolumeName(vol.getName());
                                res.setSuccess(false);
                                res.setError(errorCode);
                                return res;
                            }).collect(Collectors.toList()));
                        }
                    }
                };
            }

            private void fix(String psUuid, List<Qcow2FileInfo> qcow2FileInfo) {
                List<VolumeVO> vols = volumes.stream().filter(vol -> vol.getPrimaryStorageUuid().equals(psUuid))
                        .collect(Collectors.toList());
                SnapshotChainFixer fixer = new SnapshotChainFixer(vols, qcow2FileInfo);
                results.addAll(fixer.fix());
            }

            @Override
            protected void done() {
                if (!errors.isEmpty()) {
                    evt.setErrorCode(errf.instantiateErrorCode(SysErrors.INTERNAL, "internal errors happens", errors));
                } else {
                    evt.setResults(results);
                }

                bus.publish(evt);
            }
        }.start();
    }
}
