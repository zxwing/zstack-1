package org.zstack.kvm;

import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.springframework.transaction.annotation.Transactional;
import org.zstack.core.asyncbatch.AsyncBatchRunner;
import org.zstack.core.asyncbatch.LoopAsyncBatch;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.SimpleQuery;
import org.zstack.core.db.SimpleQuery.Op;
import org.zstack.core.errorcode.ErrorFacade;
import org.zstack.header.core.NoErrorCompletion;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.errorcode.OperationFailureException;
import org.zstack.header.errorcode.SysErrors;
import org.zstack.header.message.MessageReply;
import org.zstack.header.rest.RESTFacade;
import org.zstack.header.storage.primary.PrimaryStorageConstant;
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
        private long lastModificationTime;

        public long getLastModificationTime() {
            return lastModificationTime;
        }

        public void setLastModificationTime(long lastModificationTime) {
            this.lastModificationTime = lastModificationTime;
        }

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

    private Map<String, Object> qcow2InfosByPsUuid = new HashMap<>();

    public void handle(APIKvmFixVolumeSnapshotChainMsg msg) {
        APIKvmFixVolumeSnapshotChainEvent evt = new APIKvmFixVolumeSnapshotChainEvent(msg.getId());
        List<VolumeVO> volumes = new ArrayList<>();

        if (msg.getPrimaryStorageUuid() != null) {
            PrimaryStorageVO ps = dbf.findByUuid(msg.getPrimaryStorageUuid(), PrimaryStorageVO.class);
            if (!ps.getType().equals("NFS") && !ps.getType().equals("LocalStorage")) {
                throw new OperationFailureException(errf.stringToInvalidArgumentError(
                        String.format("the primary storage[uuid:%s, name:%s] is of type %s that doesn't support the API",
                                ps.getUuid(), ps.getName(), ps.getType())
                ));
            }

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
                            " c.hypervisorType = :hvType and v.primaryStorageUuid = ps.uuid" +
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
            } else {
                qcow2InfosByPsUuid.put(psUuid, reply.getError());
            }
        }

        List<FixResult> results = new ArrayList<>();

        new LoopAsyncBatch<VolumeVO>() {

            @Override
            protected Collection<VolumeVO> collect() {
                return volumes;
            }

            @Override
            protected AsyncBatchRunner forEach(VolumeVO volume) {
                return new AsyncBatchRunner() {
                    @Override
                    public void run(NoErrorCompletion completion) {
                        Object o = qcow2InfosByPsUuid.get(volume.getPrimaryStorageUuid());
                        if (o instanceof ErrorCode) {
                            synchronized (results) {
                                FixResult res = new FixResult();
                                res.setVolumeUuid(volume.getUuid());
                                res.setVolumeName(volume.getName());
                                res.setSuccess(false);
                                res.setError((ErrorCode) o);
                                results.add(res);
                            }
                        } else {
                            List<Qcow2FileInfo> info = (List<Qcow2FileInfo>) o;
                            SnapshotChainFixer fixer = new SnapshotChainFixer(volume, info);
                            synchronized (results) {
                                results.add(fixer.fix());
                            }
                        }

                        completion.done();
                    }
                };
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
