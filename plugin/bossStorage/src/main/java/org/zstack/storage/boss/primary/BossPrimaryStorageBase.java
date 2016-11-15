package org.zstack.storage.boss.primary;

import org.zstack.header.core.Completion;
import org.zstack.header.core.ReturnValueCompletion;
import org.zstack.header.message.Message;
import org.zstack.header.storage.primary.*;
import org.zstack.storage.boss.BossCapacityUpdater;
import org.zstack.storage.boss.BossSystemTags;
import org.zstack.storage.primary.PrimaryStorageBase;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by XXPS-PC1 on 2016/10/31.
 */
public class BossPrimaryStorageBase extends PrimaryStorageBase {

    private static final CLogger logger = Utils.getLogger(BossPrimaryStorageBase.class);

    public static class ShellCommand {
        String clusterName;
        String uuid;

        public String getClusterName() {
            return clusterName;
        }

        public void setClusterName(String fsId) {
            this.clusterName = fsId;
        }

        public String getUuid() {
            return uuid;
        }

        public void setUuid(String uuid) {
            this.uuid = uuid;
        }
    }

    public static class ShellResponse {
        String error;
        boolean success = true;
        Long totalCapacity;
        Long availableCapacity;

        public String getError() {
            return error;
        }

        public void setError(String error) {
            this.error = error;
        }

        public boolean isSuccess() {
            return success;
        }

        public void setSuccess(boolean success) {
            this.success = success;
        }

        public Long getTotalCapacity() {
            return totalCapacity;
        }

        public void setTotalCapacity(Long totalCapacity) {
            this.totalCapacity = totalCapacity;
        }

        public Long getAvailableCapacity() {
            return availableCapacity;
        }

        public void setAvailableCapacity(Long availableCapacity) {
            this.availableCapacity = availableCapacity;
        }
    }

    public static class GetVolumeSizeCmd extends ShellCommand {
        public String volumeUuid;
        public String installPath;
    }

    public static class GetVolumeSizeRsp extends ShellResponse {
        public Long size;
        public Long actualSize;
    }

    public static class Pool {
        String name;
        boolean predefined;
    }

    public static class InitCmd extends ShellCommand {
        List<Pool> pools;

        public List<Pool> getPools() {
            return pools;
        }

        public void setPools(List<Pool> pools) {
            this.pools = pools;
        }
    }

    public static class InitRsp extends ShellResponse {
        String clusterName;

        public String getClusterName() {
            return clusterName;
        }

        public void setClusterName(String fsid) {
            this.clusterName = fsid;
        }
    }

    public BossPrimaryStorageBase(BossPrimaryStorageVO self) { super(self);}

    protected BossPrimaryStorageVO getSelf() {
        return (BossPrimaryStorageVO) self;
    }
    protected void handleLocalMessage(Message msg) {
        if (msg instanceof TakeSnapshotMsg) {
            handle((TakeSnapshotMsg) msg);
        } else if (msg instanceof MergeVolumeSnapshotOnPrimaryStorageMsg) {
            handle((MergeVolumeSnapshotOnPrimaryStorageMsg) msg);
        } else if (msg instanceof DeleteSnapshotOnPrimaryStorageMsg) {
            handle((DeleteSnapshotOnPrimaryStorageMsg) msg);
        } else if (msg instanceof RevertVolumeFromSnapshotOnPrimaryStorageMsg) {
            handle((RevertVolumeFromSnapshotOnPrimaryStorageMsg) msg);
        } else if (msg instanceof CreateVolumeFromVolumeSnapshotOnPrimaryStorageMsg) {
            handle((CreateVolumeFromVolumeSnapshotOnPrimaryStorageMsg) msg);
        } else if (msg instanceof BackupVolumeSnapshotFromPrimaryStorageToBackupStorageMsg) {
            handle((BackupVolumeSnapshotFromPrimaryStorageToBackupStorageMsg) msg);
        } else if (msg instanceof DeleteImageCacheOnPrimaryStorageMsg) {
            handle((DeleteImageCacheOnPrimaryStorageMsg) msg);
        } else {
            super.handleLocalMessage(msg);
        }
    }

    public void handle(TakeSnapshotMsg msg) {

    }

    public void handle(MergeVolumeSnapshotOnPrimaryStorageMsg msg) {

    }

    public void handle(DeleteSnapshotOnPrimaryStorageMsg msg) {

    }

    public void handle(RevertVolumeFromSnapshotOnPrimaryStorageMsg msg) {

    }

    public void handle(CreateVolumeFromVolumeSnapshotOnPrimaryStorageMsg msg) {

    }

    public void handle(BackupVolumeSnapshotFromPrimaryStorageToBackupStorageMsg msg) {

    }

    public void handle(DeleteImageCacheOnPrimaryStorageMsg msg) {

    }
    @Override
    protected void handle(InstantiateVolumeOnPrimaryStorageMsg msg) {
        System.out.print("yes!!");
    }

    @Override
    protected void handle(DeleteVolumeOnPrimaryStorageMsg msg) {

    }

    @Override
    protected void handle(CreateTemplateFromVolumeOnPrimaryStorageMsg msg) {

    }

    @Override
    protected void handle(DownloadDataVolumeToPrimaryStorageMsg msg) {

    }

    @Override
    protected void handle(DeleteBitsOnPrimaryStorageMsg msg) {

    }

    @Override
    protected void handle(DownloadIsoToPrimaryStorageMsg msg) {

    }

    @Override
    protected void handle(DeleteIsoFromPrimaryStorageMsg msg) {

    }

    @Override
    protected void handle(AskVolumeSnapshotCapabilityMsg msg) {

    }

    @Override
    protected void handle(SyncVolumeSizeOnPrimaryStorageMsg msg) {

    }

    @Override
    protected void connectHook(ConnectParam param, Completion completion) {
        //connect(param.isNewAdded(), completion);
        InitCmd cmd = new InitCmd();
        InitRsp rsp = new InitRsp();
        List<Pool> pools = new ArrayList<Pool>();

        Pool p = new Pool();
        p.name = getSelf().getImageCachePoolName();
        p.predefined = BossSystemTags.PREDEFINED_PRIMARY_STORAGE_IMAGE_CACHE_POOL.hasTag(self.getUuid());
        pools.add(p);

        p = new Pool();
        p.name = getSelf().getRootVolumePoolName();
        p.predefined = BossSystemTags.PREDEFINED_PRIMARY_STORAGE_ROOT_VOLUME_POOL.hasTag(self.getUuid());
        pools.add(p);

        p = new Pool();
        p.name = getSelf().getDataVolumePoolName();
        p.predefined = BossSystemTags.PREDEFINED_PRIMARY_STORAGE_DATA_VOLUME_POOL.hasTag(self.getUuid());
        pools.add(p);

        cmd.pools = pools;

        rsp.setClusterName(getSelf().getClusterName());
        rsp.availableCapacity = Long.valueOf(1024*1024*1024);
        rsp.totalCapacity = Long.valueOf(10*1024*1024*1024);
        BossCapacityUpdater updater = new BossCapacityUpdater();
        updater.update(rsp.clusterName,rsp.totalCapacity,rsp.availableCapacity,true);

        completion.success();
    }

    private void connect(final boolean newAdded, final Completion completion) {

    }

    @Override
    protected void pingHook(Completion completion) {

    }

    @Override
    protected void syncPhysicalCapacity(ReturnValueCompletion<PhysicalCapacityUsage> completion) {

    }
}
