package org.zstack.storage.boss.primary;

import org.zstack.header.core.Completion;
import org.zstack.header.core.ReturnValueCompletion;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.message.Message;
import org.zstack.header.storage.primary.*;
import org.zstack.storage.boss.BossCapacityUpdater;
import org.zstack.storage.boss.BossSystemTags;
import org.zstack.storage.boss.ExecuteShellCommand;
import org.zstack.storage.primary.PrimaryStorageBase;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import java.util.ArrayList;
import java.util.HashSet;
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
        Long totalCapacity = 0L;
        Long availableCapacity= 0L;

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

    protected static Long unitConvert(String unit){
        switch (unit){
            case "B": return 1L;
            case "KB": return 1024L;
            case "MB": return 1024*1024L;
            case "GB": return 1024*1024*1024L;
            case "TB": return 1024*1024*1024*1024L;
            default: return 1L;
        }
    }

    protected Long getPoolTotalSize(String poolName){
        ExecuteShellCommand esc;
        esc = new ExecuteShellCommand();
        String totalSize = esc.executeCommand(String.format("pool_list -l | grep %s | awk '{print $3}'" , poolName),errf);
        String unit = esc.executeCommand(String.format("pool_list -l | grep %s | awk '{print $4}'" , poolName),errf);
        return Math.round(Double.valueOf(totalSize.trim()) * unitConvert(unit.trim()));
    }

    protected Long getPoolAvailableSize(String poolName){
        ExecuteShellCommand esc;
        esc = new ExecuteShellCommand();
        String totalSize = esc.executeCommand(String.format("pool_list -l | grep %s | awk '{print $5}'" , poolName),errf);
        String unit = esc.executeCommand(String.format("pool_list -l | grep %s | awk '{print $6}'" , poolName),errf);
        return Math.round(Double.valueOf(totalSize.trim()) * unitConvert(unit.trim()));
    }

    @Override
    protected void connectHook(ConnectParam param, Completion completion) {
        try {
            //connect(param.isNewAdded(), completion);
            InitCmd cmd = new InitCmd();
            InitRsp rsp = new InitRsp();
            List<Pool> pools = new ArrayList<Pool>();
            HashSet<String> poolNames = new HashSet<>();

            Pool p = new Pool();
            p.name = getSelf().getImageCachePoolName();
            p.predefined = BossSystemTags.PREDEFINED_PRIMARY_STORAGE_IMAGE_CACHE_POOL.hasTag(self.getUuid());
            pools.add(p);
            poolNames.add(p.name);

            p = new Pool();
            p.name = getSelf().getRootVolumePoolName();
            p.predefined = BossSystemTags.PREDEFINED_PRIMARY_STORAGE_ROOT_VOLUME_POOL.hasTag(self.getUuid());
            pools.add(p);
            poolNames.add(p.name);

            p = new Pool();
            p.name = getSelf().getDataVolumePoolName();
            p.predefined = BossSystemTags.PREDEFINED_PRIMARY_STORAGE_DATA_VOLUME_POOL.hasTag(self.getUuid());
            pools.add(p);
            poolNames.add(p.name);
            cmd.pools = pools;
            for (String poolname : poolNames) {
                rsp.totalCapacity = rsp.totalCapacity + getPoolTotalSize(poolname);
                rsp.availableCapacity = rsp.availableCapacity + getPoolAvailableSize(poolname);
            }


            rsp.setClusterName(getSelf().getClusterName());
            BossCapacityUpdater updater = new BossCapacityUpdater();
            updater.update(rsp.clusterName, rsp.totalCapacity, rsp.availableCapacity, true);

            completion.success();
        } catch (Exception e){
            completion.fail(errf.stringToOperationError("initialize boss failed"));
        }
    }

    private void connect(final boolean newAdded, final Completion completion) {

    }

    @Override
    protected void pingHook(Completion completion) {
        completion.success();
    }

    @Override
    protected void syncPhysicalCapacity(ReturnValueCompletion<PhysicalCapacityUsage> completion) {

    }
}
