package org.zstack.storage.boss.primary;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.zstack.core.ansible.AnsibleFacade;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.CloudBusCallBack;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.errorcode.ErrorFacade;
import org.zstack.core.thread.ThreadFacade;
import org.zstack.header.Component;
import org.zstack.header.core.Completion;
import org.zstack.header.core.workflow.*;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.message.MessageReply;
import org.zstack.header.storage.backup.BackupStorageAskInstallPathMsg;
import org.zstack.header.storage.backup.BackupStorageAskInstallPathReply;
import org.zstack.header.storage.backup.BackupStorageConstant;
import org.zstack.header.storage.backup.DeleteBitsOnBackupStorageMsg;
import org.zstack.header.storage.primary.*;
import org.zstack.header.storage.snapshot.CreateTemplateFromVolumeSnapshotExtensionPoint;
import org.zstack.header.vm.VmInstanceInventory;
import org.zstack.header.vm.VmInstanceSpec;
import org.zstack.header.volume.SyncVolumeSizeMsg;
import org.zstack.header.volume.SyncVolumeSizeReply;
import org.zstack.header.volume.VolumeConstant;
import org.zstack.header.volume.VolumeInventory;
import org.zstack.kvm.*;
import org.zstack.storage.boss.BossCapacityUpdateExtensionPoint;
import org.zstack.storage.boss.BossConstants;
import org.zstack.storage.boss.BossSystemTags;
import org.zstack.storage.primary.PrimaryStorageCapacityUpdater;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import javax.persistence.TypedQuery;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Future;

/**
 * Created by XXPS-PC1 on 2016/10/28.
 */
public class BossPrimaryStorageFactory implements PrimaryStorageFactory,BossCapacityUpdateExtensionPoint, KVMStartVmExtensionPoint,
        KVMAttachVolumeExtensionPoint, KVMDetachVolumeExtensionPoint, CreateTemplateFromVolumeSnapshotExtensionPoint,
        KvmSetupSelfFencerExtensionPoint, KVMPreAttachIsoExtensionPoint, Component {
    private static final CLogger logger = Utils.getLogger(BossPrimaryStorageFactory.class);
    public static final PrimaryStorageType type = new PrimaryStorageType(BossConstants.BOSS_PRIMARY_STORAGE_TYPE);

    @Autowired
    private DatabaseFacade dbf;
    @Autowired
    private ErrorFacade errf;
    @Autowired
    private AnsibleFacade asf;
    @Autowired
    private ThreadFacade thdf;
    @Autowired
    private CloudBus bus;

    private Future imageCacheCleanupThread;

    static {
        type.setSupportHeartbeatFile(true);
        type.setSupportPingStorageGateway(true);
    }

    void init() {
        type.setPrimaryStorageFindBackupStorage(new PrimaryStorageFindBackupStorage() {
            @Override
            @Transactional(readOnly = true)
            public List<String> findBackupStorage(String primaryStorageUuid) {
                String sql = "select b.uuid from BossPrimaryStorageVO p, BossBackupStorageVO b where b.clusterName = p.clusterName" +
                        " and p.uuid = :puuid";
                TypedQuery<String> q = dbf.getEntityManager().createQuery(sql, String.class);
                q.setParameter("puuid", primaryStorageUuid);
                return q.getResultList();
            }
        });
    }

    @Override
    public PrimaryStorageType getPrimaryStorageType() {
        return type;
    }

    @Override
    @Transactional
    public PrimaryStorageInventory createPrimaryStorage(PrimaryStorageVO vo, APIAddPrimaryStorageMsg msg) {
        APIAddBossPrimaryStorageMsg bmsg = (APIAddBossPrimaryStorageMsg) msg;

        BossPrimaryStorageVO bvo = new BossPrimaryStorageVO(vo);
        bvo.setType(BossConstants.BOSS_PRIMARY_STORAGE_TYPE);
        bvo.setMountPath(BossConstants.BOSS_PRIMARY_STORAGE_TYPE);
        bvo.setClusterName(bmsg.getClusterName() == null ? String.format("pri-c-%s",vo.getUuid()) : bmsg.getClusterName());
        bvo.setRootVolumePoolName(bmsg.getRootVolumePoolName() == null ? String.format("pri-v-r-%s", vo.getUuid()) : bmsg.getRootVolumePoolName());
        bvo.setDataVolumePoolName(bmsg.getDataVolumePoolName() == null ? String.format("pri-v-d-%s", vo.getUuid()) : bmsg.getDataVolumePoolName());
        bvo.setImageCachePoolName(bmsg.getImageCachePoolName() == null ? String.format("pri-c-%s", vo.getUuid()) : bmsg.getImageCachePoolName());

        dbf.getEntityManager().persist(bvo);

        if (bmsg.getImageCachePoolName() != null) {
            BossSystemTags.PREDEFINED_PRIMARY_STORAGE_IMAGE_CACHE_POOL.createInherentTag(bvo.getUuid());
        }
        if (bmsg.getRootVolumePoolName() != null) {
            BossSystemTags.PREDEFINED_PRIMARY_STORAGE_ROOT_VOLUME_POOL.createInherentTag(bvo.getUuid());
        }
        if (bmsg.getDataVolumePoolName() != null) {
            BossSystemTags.PREDEFINED_PRIMARY_STORAGE_DATA_VOLUME_POOL.createInherentTag(bvo.getUuid());
        }


        return PrimaryStorageInventory.valueOf(bvo);
    }

    @Override
    public PrimaryStorage getPrimaryStorage(PrimaryStorageVO vo) {
        BossPrimaryStorageVO bvo = dbf.findByUuid(vo.getUuid(), BossPrimaryStorageVO.class);
        return new BossPrimaryStorageBase(bvo);
    }

    @Override
    public PrimaryStorageInventory getInventory(String uuid) {
        return BossPrimaryStorageInventory.valueOf(dbf.findByUuid(uuid, BossPrimaryStorageVO.class));
    }
    @Override
    public void update(String clusterName, final long total, final long avail) {
        String sql = "select cap from PrimaryStorageCapacityVO cap, BossPrimaryStorageVO pri where pri.uuid = cap.uuid and pri.clusterName = :clusterName";
        TypedQuery<PrimaryStorageCapacityVO> q = dbf.getEntityManager().createQuery(sql, PrimaryStorageCapacityVO.class);
        q.setParameter("clusterName", clusterName);
        PrimaryStorageCapacityUpdater updater = new PrimaryStorageCapacityUpdater(q);
        updater.run(new PrimaryStorageCapacityUpdaterRunnable() {
            @Override
            public PrimaryStorageCapacityVO call(PrimaryStorageCapacityVO cap) {
                if (cap.getTotalCapacity() == 0 && cap.getAvailableCapacity() == 0) {
                    // init
                    cap.setTotalCapacity(total);
                    cap.setAvailableCapacity(avail);
                }

                cap.setTotalPhysicalCapacity(total);
                cap.setAvailablePhysicalCapacity(avail);

                return cap;
            }
        });
    }


    @Override
    public boolean start() {
        return true;
    }

    @Override
    public boolean stop() {
        return true;
    }

    @Override
    public void preAttachIsoExtensionPoint(KVMHostInventory host, KVMAgentCommands.AttachIsoCmd cmd) {

    }

    @Override
    public void beforeStartVmOnKvm(KVMHostInventory host, VmInstanceSpec spec, KVMAgentCommands.StartVmCmd cmd) throws KVMException {

    }

    @Override
    public void startVmOnKvmSuccess(KVMHostInventory host, VmInstanceSpec spec) {

    }

    @Override
    public void startVmOnKvmFailed(KVMHostInventory host, VmInstanceSpec spec, ErrorCode err) {

    }

    @Override
    public void beforeAttachVolume(KVMHostInventory host, VmInstanceInventory vm, VolumeInventory volume, KVMAgentCommands.AttachDataVolumeCmd cmd) {

    }

    @Override
    public void afterAttachVolume(KVMHostInventory host, VmInstanceInventory vm, VolumeInventory volume, KVMAgentCommands.AttachDataVolumeCmd cmd) {

    }

    @Override
    public void attachVolumeFailed(KVMHostInventory host, VmInstanceInventory vm, VolumeInventory volume, KVMAgentCommands.AttachDataVolumeCmd cmd, ErrorCode err) {

    }

    @Override
    public void beforeDetachVolume(KVMHostInventory host, VmInstanceInventory vm, VolumeInventory volume, KVMAgentCommands.DetachDataVolumeCmd cmd) {

    }

    @Override
    public void afterDetachVolume(KVMHostInventory host, VmInstanceInventory vm, VolumeInventory volume, KVMAgentCommands.DetachDataVolumeCmd cmd) {

    }

    @Override
    public void detachVolumeFailed(KVMHostInventory host, VmInstanceInventory vm, VolumeInventory volume, KVMAgentCommands.DetachDataVolumeCmd cmd, ErrorCode err) {

    }

    @Override
    public String kvmSetupSelfFencerStorageType() {
        return null;
    }

    @Override
    public void kvmSetupSelfFencer(KvmSetupSelfFencerParam param, Completion completion) {

    }

    @Override
    public void kvmCancelSelfFencer(KvmCancelSelfFencerParam param, Completion completion) {

    }

    @Override
    public WorkflowTemplate createTemplateFromVolumeSnapshot(ParamIn paramIn) {
        WorkflowTemplate template = new WorkflowTemplate();
        template.setCreateTemporaryTemplate(new NoRollbackFlow() {
            @Override
            public void run(final FlowTrigger trigger, final Map data) {
                SyncVolumeSizeMsg msg = new SyncVolumeSizeMsg();
                msg.setVolumeUuid(paramIn.getSnapshot().getVolumeUuid());
                bus.makeTargetServiceIdByResourceUuid(msg, VolumeConstant.SERVICE_ID, paramIn.getSnapshot().getVolumeUuid());
                bus.send(msg, new CloudBusCallBack(trigger) {
                    @Override
                    public void run(MessageReply reply) {
                        if (reply.isSuccess()) {
                            ParamOut paramOut = (ParamOut) data.get(ParamOut.class);
                            SyncVolumeSizeReply gr = reply.castReply();
                            paramOut.setActualSize(gr.getActualSize());
                            paramOut.setSize(gr.getSize());
                            trigger.next();
                        } else {
                            trigger.fail(reply.getError());
                        }
                    }
                });
            }
        });

        template.setUploadToBackupStorage(new Flow() {
            String __name__ = "upload-to-backup-storage";

            @Override
            public void run(final FlowTrigger trigger, Map data) {
                final ParamOut out = (ParamOut) data.get(ParamOut.class);

                BackupStorageAskInstallPathMsg ask = new BackupStorageAskInstallPathMsg();
                ask.setImageUuid(paramIn.getImage().getUuid());
                ask.setBackupStorageUuid(paramIn.getBackupStorageUuid());
                ask.setImageMediaType(paramIn.getImage().getMediaType());
                bus.makeTargetServiceIdByResourceUuid(ask, BackupStorageConstant.SERVICE_ID, paramIn.getBackupStorageUuid());
                MessageReply ar = bus.call(ask);
                if (!ar.isSuccess()) {
                    trigger.fail(ar.getError());
                    return;
                }

                String bsInstallPath = ((BackupStorageAskInstallPathReply)ar).getInstallPath();

                UploadBitsToBackupStorageMsg msg = new UploadBitsToBackupStorageMsg();
                msg.setPrimaryStorageUuid(paramIn.getPrimaryStorageUuid());
                msg.setPrimaryStorageInstallPath(paramIn.getSnapshot().getPrimaryStorageInstallPath());
                msg.setBackupStorageUuid(paramIn.getBackupStorageUuid());
                msg.setBackupStorageInstallPath(bsInstallPath);
                bus.makeTargetServiceIdByResourceUuid(msg, PrimaryStorageConstant.SERVICE_ID, paramIn.getPrimaryStorageUuid());

                bus.send(msg, new CloudBusCallBack(trigger) {
                    @Override
                    public void run(MessageReply reply) {
                        if (!reply.isSuccess()) {
                            trigger.fail(reply.getError());
                        } else {
                            out.setBackupStorageInstallPath(bsInstallPath);
                            trigger.next();
                        }
                    }
                });
            }

            @Override
            public void rollback(FlowRollback trigger, Map data) {
                final ParamOut out = (ParamOut) data.get(ParamOut.class);
                if (out.getBackupStorageInstallPath() != null) {
                    DeleteBitsOnBackupStorageMsg msg = new DeleteBitsOnBackupStorageMsg();
                    msg.setInstallPath(out.getBackupStorageInstallPath());
                    msg.setBackupStorageUuid(paramIn.getBackupStorageUuid());
                    bus.makeTargetServiceIdByResourceUuid(msg, BackupStorageConstant.SERVICE_ID, paramIn.getBackupStorageUuid());
                    bus.send(msg);
                }

                trigger.rollback();
            }
        });

        template.setDeleteTemporaryTemplate(new NopeFlow());

        return template;
    }

    @Override
    public String createTemplateFromVolumeSnapshotPrimaryStorageType() {
        return BossConstants.BOSS_PRIMARY_STORAGE_TYPE;
    }
}
