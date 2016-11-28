package org.zstack.storage.boss.primary;

import org.zstack.core.cloudbus.CloudBusCallBack;
import org.zstack.core.db.SimpleQuery;
import org.zstack.core.thread.ChainTask;
import org.zstack.core.thread.SyncTaskChain;
import org.zstack.core.workflow.FlowChainBuilder;
import org.zstack.core.workflow.ShareFlow;
import org.zstack.header.core.ApiTimeout;
import org.zstack.header.core.Completion;
import org.zstack.header.core.ReturnValueCompletion;
import org.zstack.header.core.workflow.*;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.errorcode.OperationFailureException;
import org.zstack.header.exception.CloudRuntimeException;
import org.zstack.header.image.*;
import org.zstack.header.message.Message;
import org.zstack.header.message.MessageReply;
import org.zstack.header.storage.backup.*;
import org.zstack.header.storage.primary.*;
import org.zstack.header.storage.snapshot.VolumeSnapshotConstant;
import org.zstack.header.storage.snapshot.VolumeSnapshotInventory;
import org.zstack.header.vm.*;
import org.zstack.header.volume.*;
import org.zstack.kvm.*;
import org.zstack.storage.backup.sftp.SftpBackupStorageConstant;
import org.zstack.storage.boss.BossCapacityUpdater;
import org.zstack.storage.boss.BossConstants;
import org.zstack.storage.boss.BossSystemTags;
import org.zstack.storage.boss.ExecuteShellCommand;
import org.zstack.storage.boss.backup.BossBackupStorageVO;
import org.zstack.storage.boss.backup.BossBackupStorageVO_;
import org.zstack.storage.primary.PrimaryStorageBase;
import org.zstack.utils.*;
import org.zstack.utils.function.Function;
import org.zstack.utils.logging.CLogger;

import java.util.*;

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

    public static class CreateEmptyVolumeCmd extends ShellCommand {
        String installPath;
        String poolName;
        String volumeName;
        long size;

        public String getInstallPath() {
            return installPath;
        }

        public void setInstallPath(String installPath) {
            this.installPath = installPath;
        }

        public String getPoolName(){return poolName;}

        public void setPoolName(String poolName){this.poolName = poolName;}

        public String getVolumeName(String volumeName){return volumeName;}

        public void setVolumeName(String volumeName){this.volumeName = volumeName;}

        public long getSize() {
            return size;
        }

        public void setSize(long size) {
            this.size = size;
        }
    }

    public static class CreateEmptyVolumeRsp extends ShellResponse {
    }

    public static class DeleteCmd extends ShellCommand {
        String installPath;
        String poolName;
        String volumeName;

        public String getInstallPath() {
            return installPath;
        }

        public void setInstallPath(String installPath) {
            this.installPath = installPath;
        }

        public String getPoolName(){return poolName;}

        public void setPoolName(String poolName){this.poolName = poolName;}

        public String getVolumeName(String volumeName){return volumeName;}

        public void setVolumeName(String volumeName){this.volumeName = volumeName;}
    }

    public static class DeleteRsp extends ShellResponse {

    }

    @ApiTimeout(apiClasses = {
            APICreateRootVolumeTemplateFromRootVolumeMsg.class,
            APICreateDataVolumeTemplateFromVolumeMsg.class,
            APICreateDataVolumeFromVolumeSnapshotMsg.class,
            APICreateRootVolumeTemplateFromVolumeSnapshotMsg.class
    })
    public static class CpCmd extends ShellCommand {
        String resourceUuid;
        String srcPath;
        String dstPath;
        String srcPoolName;
        String srcVolumeName;
        String dstPoolName;
        String dstVolumeName;
    }

    public static class CpRsp extends ShellResponse {
        Long size;
        Long actualSize;
    }

    public static class CreateSnapshotCmd extends ShellCommand {
        boolean skipOnExisting;
        String snapshotPath;
        String volumeUuid;
        String snapShotPoolName;
        String snapShotName;

        public String getVolumeUuid() {
            return volumeUuid;
        }

        public void setVolumeUuid(String volumeUuid) {
            this.volumeUuid = volumeUuid;
        }

        public boolean isSkipOnExisting() {
            return skipOnExisting;
        }

        public void setSkipOnExisting(boolean skipOnExisting) {
            this.skipOnExisting = skipOnExisting;
        }

        public String getSnapshotPath() {
            return snapshotPath;
        }

        public void setSnapshotPath(String snapshotPath) {
            this.snapshotPath = snapshotPath;
        }
    }

    public static class CreateSnapshotRsp extends ShellResponse {
        Long size;
        Long actualSize;

        public Long getActualSize() {
            return actualSize;
        }

        public void setActualSize(Long actualSize) {
            this.actualSize = actualSize;
        }

        public long getSize() {
            return size;
        }

        public void setSize(long size) {
            this.size = size;
        }
    }
    public static class DeleteSnapshotCmd extends ShellCommand {
        String snapshotPath;
        String snapShotPoolName;
        String snapShotName;

        public String getSnapshotPath() {
            return snapshotPath;
        }

        public void setSnapshotPath(String snapshotPath) {
            this.snapshotPath = snapshotPath;
        }
    }

    public static class DeleteSnapshotRsp extends ShellResponse {
    }
    @ApiTimeout(apiClasses = {APICreateVmInstanceMsg.class})
    public static class CloneCmd extends ShellCommand {
        String srcPath;
        String dstPath;
        String srcPoolName;
        String srcVolumeName;
        String dstPoolName;
        String dstVolumeName;

        public String getSrcPath() {
            return srcPath;
        }

        public void setSrcPath(String srcPath) {
            this.srcPath = srcPath;
        }

        public String getDstPath() {
            return dstPath;
        }

        public void setDstPath(String dstPath) {
            this.dstPath = dstPath;
        }
    }

    public static class CloneRsp extends ShellResponse {
    }

    public static class RollbackSnapshotCmd extends ShellCommand {
        String snapshotPath;

        public String getSnapshotPath() {
            return snapshotPath;
        }

        public void setSnapshotPath(String snapshotPath) {
            this.snapshotPath = snapshotPath;
        }
    }

    public static class RollbackSnapshotRsp extends ShellResponse {
    }

    public static class CreateKvmSecretCmd extends KVMAgentCommands.AgentCommand {
        String userKey;
        String uuid;

        public String getUserKey() {
            return userKey;
        }

        public void setUserKey(String userKey) {
            this.userKey = userKey;
        }

        public String getUuid() {
            return uuid;
        }

        public void setUuid(String uuid) {
            this.uuid = uuid;
        }
    }

    public static class CreateKvmSecretRsp extends ShellResponse {

    }

    public static class DeletePoolCmd extends ShellCommand {
        List<String> poolNames;

        public List<String> getPoolNames() {
            return poolNames;
        }

        public void setPoolNames(List<String> poolNames) {
            this.poolNames = poolNames;
        }
    }

    public static class DeletePoolRsp extends ShellResponse {
    }

    public static class KvmSetupSelfFencerCmd extends ShellCommand {
        public String heartbeatImagePath;
        public String hostUuid;
        public long interval;
        public int maxAttempts;
        public int storageCheckerTimeout;
        public String userKey;
        public List<String> monUrls;
    }

    public static class KvmCancelSelfFencerCmd extends ShellCommand {
        public String hostUuid;
    }

    public static class GetFactsCmd extends ShellCommand {
        public String monUuid;
    }

    public static class GetFactsRsp extends ShellResponse {
        public String fsid;
        public String monAddr;
    }

    public static class DeleteImageCacheCmd extends ShellCommand {
        public String imagePath;
        public String snapshotPath;
        public String imagePoolName;
        public String snapShotPoolName;
        public String snapShotName;
        public String imageName;
    }

    public static final String KVM_HA_SETUP_SELF_FENCER = "/ha/boss/setupselffencer";
    public static final String KVM_HA_CANCEL_SELF_FENCER = "/ha/boss/cancelselffencer";


    abstract class MediatorParam {
    }

    class DownloadParam extends MediatorParam {
        VmInstanceSpec.ImageSpec image;
        String installPath;
    }

    class UploadParam extends MediatorParam {
        ImageInventory image;
        String primaryStorageInstallPath;
        String backupStorageInstallPath;
    }

    abstract class BackupStorageMediator {
        BackupStorageInventory backupStorage;
        MediatorParam param;

        protected void checkParam() {
            DebugUtils.Assert(backupStorage != null, "backupStorage cannot be null");
            DebugUtils.Assert(param != null, "param cannot be null");
        }

        abstract void download(ReturnValueCompletion<String> completion);

        abstract void upload(ReturnValueCompletion<String> completion);

        abstract boolean deleteWhenRollabackDownload();
    }

    private BackupStorageMediator getBackupStorageMediator(String bsUuid) {
        BackupStorageVO bsvo = dbf.findByUuid(bsUuid, BackupStorageVO.class);
        BackupStorageMediator mediator = backupStorageMediators.get(bsvo.getType());
        if (mediator == null) {
            throw new CloudRuntimeException(String.format("cannot find BackupStorageMediator for type[%s]", bsvo.getType()));
        }

        mediator.backupStorage = BackupStorageInventory.valueOf(bsvo);
        return mediator;
    }

    private String makeRootVolumeInstallPath(String volUuid) {
        return String.format("boss://%s/RV-%s", getSelf().getRootVolumePoolName(), volUuid);
    }

    private String makeDataVolumeInstallPath(String volUuid) {
        return String.format("boss://%s/DV-%s", getSelf().getDataVolumePoolName(), volUuid);
    }

    private String makeCacheInstallPath(String uuid) {
        return String.format("boss://%s/IC-%s", getSelf().getImageCachePoolName(), uuid);
    }

    private String getBossPoolNameFromPath(String path){
        String poolName = path.replace(String.format("%s://",getSelfInventory().getClusterName()),"").trim().split("/")[0];
        return poolName;
    }

    private String getBossVolumeNameFromPath(String path){
        String volumeName = path.replace(String.format("%s://",getSelfInventory().getClusterName()),"").trim().split("/")[1];
        return volumeName;
    }

    private Long getAvailableCapacity(BossPrimaryStorageVO vo){
        Long availableCapacity = 0L;
        HashSet<String> poolNames = new HashSet<>();
        String pool;
        pool = vo.getDataVolumePoolName();
        poolNames.add(pool);
        pool = vo.getImageCachePoolName();
        poolNames.add(pool);
        pool = vo.getRootVolumePoolName();
        poolNames.add(pool);

        for (String poolname : poolNames) {
            availableCapacity = availableCapacity + getPoolAvailableSize(poolname);
        }

        return availableCapacity;
    }

    private Long getTotalCapacity(BossPrimaryStorageVO vo){
        Long totalCapacity = 0L;
        HashSet<String> poolNames = new HashSet<>();
        String pool;
        pool = vo.getDataVolumePoolName();
        poolNames.add(pool);
        pool = vo.getImageCachePoolName();
        poolNames.add(pool);
        pool = vo.getRootVolumePoolName();
        poolNames.add(pool);

        for (String poolname : poolNames) {
            totalCapacity = totalCapacity + getPoolAvailableSize(poolname);
        }

        return totalCapacity;
    }


    protected Long unitConvert(String unit){
        switch (unit){
            case "B": return 1L;
            case "KB": return 1024L;
            case "MB": return 1024*1024L;
            case "GB": return 1024*1024*1024L;
            case "TB": return 1024*1024*1024*1024L;
            default: return 1L;
        }
    }

    private Long getVolumeSizeFromPathInBoss(String path){
        String poolName = getBossPoolNameFromPath(path);
        String volumeName = getBossVolumeNameFromPath(path);
        String volumeSize = ShellUtils.runAndReturn(String.format("volume_info -p %s -v %s | grep 'volume size' " +
                "| awk '{print $3}'", poolName, volumeName),true).getStdout();
        String unit = ShellUtils.runAndReturn(String.format("volume_info -p %s -v %s | grep 'volume size' " +
                "| awk '{print $4}'", poolName, volumeName),true).getStdout();
        return Math.round(Double.valueOf(volumeSize.trim()) * unitConvert(unit.trim()));
    }

    private void updateCapacity(ShellResponse rsp){
        BossCapacityUpdater updater = new BossCapacityUpdater();
        updater.update(getSelf().getClusterName(),rsp.totalCapacity,rsp.availableCapacity);
    }


    private final Map<String, BackupStorageMediator> backupStorageMediators = new HashMap<String, BackupStorageMediator>();

    {
        backupStorageMediators.put(SftpBackupStorageConstant.SFTP_BACKUP_STORAGE_TYPE, new SftpBackupStorageMediator());
        backupStorageMediators.put(BossConstants.BOSS_BACKUP_STORAGE_TYPE, new BossBackupStorageMediator());
    }

    class SftpBackupStorageMediator extends BackupStorageMediator{

        @Override
        void download(ReturnValueCompletion<String> completion) {

        }

        @Override
        void upload(ReturnValueCompletion<String> completion) {

        }

        @Override
        boolean deleteWhenRollabackDownload() {
            return false;
        }
    }

    class BossBackupStorageMediator extends BackupStorageMediator{

        protected void checkParam() {
            super.checkParam();

            SimpleQuery<BossBackupStorageVO> q = dbf.createQuery(BossBackupStorageVO.class);
            q.select(BossBackupStorageVO_.clusterName);
            q.add(BossBackupStorageVO_.uuid, SimpleQuery.Op.EQ, backupStorage.getUuid());
            String bsClusterName = q.findValue();
            if (!getSelf().getClusterName().equals(bsClusterName)) {
                throw new OperationFailureException(errf.stringToOperationError(
                        String.format("the backup storage[uuid:%s, name:%s, clusername:%s] is not in the same boss cluster" +
                                        " with the primary storage[uuid:%s, name:%s, clusername:%s]", backupStorage.getUuid(),
                                backupStorage.getName(), bsClusterName, self.getUuid(), self.getName(), getSelf().getClusterName())
                ));
            }
        }

        @Override
        void download(final ReturnValueCompletion<String> completion) {
            checkParam();

            final DownloadParam dparam = (DownloadParam) param;
            if (ImageConstant.ImageMediaType.DataVolumeTemplate.toString().equals(dparam.image.getInventory().getMediaType())) {
                CpCmd cmd = new CpCmd();
                CpRsp rsp = new CpRsp();
                cmd.srcPath = dparam.image.getSelectedBackupStorage().getInstallPath();
                cmd.srcPoolName = getBossPoolNameFromPath(cmd.srcPath);
                cmd.srcVolumeName = getBossVolumeNameFromPath(cmd.srcPath);
                cmd.dstPath = dparam.installPath;
                cmd.dstPoolName = getBossPoolNameFromPath(cmd.dstPath);
                cmd.dstVolumeName = getBossVolumeNameFromPath(cmd.dstPath);

                ReturnValueCompletion<CpRsp> CpCompletion = new ReturnValueCompletion<CpRsp>(completion){
                    @Override
                    public void success(CpRsp returnValue) {
                        completion.success(dparam.installPath);
                    }

                    @Override
                    public void fail(ErrorCode errorCode) {
                        completion.fail(errorCode);
                    }
                };

                ShellResult shellResult = ShellUtils.runAndReturn(String.format("volume_copy -sp %s -sv %s -dp %s -dv %s",
                        cmd.srcPoolName,cmd.srcVolumeName,cmd.dstPoolName,cmd.dstVolumeName));
                if (shellResult.getRetCode() == 0){
                    rsp.size = getVolumeSizeFromPathInBoss(cmd.dstPath);
                    rsp.availableCapacity = getAvailableCapacity(getSelf());
                    rsp.totalCapacity = getTotalCapacity(getSelf());
                    updateCapacity(rsp);
                    CpCompletion.success(rsp);
                } else {
                    CpCompletion.fail(errf.stringToOperationError(String.format("download volume from backupStorage failed," +
                            "causes[%s]",shellResult.getStderr())));
                }
            } else {
                completion.success(dparam.image.getSelectedBackupStorage().getInstallPath());
            }
        }

        @Override
        void upload(final ReturnValueCompletion<String> completion) {
            checkParam();

            final UploadParam uparam = (UploadParam) param;

            FlowChain chain = FlowChainBuilder.newShareFlowChain();
            chain.setName(String.format("upload-image-boss-%s-to-boss-%s", self.getUuid(), backupStorage.getUuid()));
            chain.then(new ShareFlow() {
                String backupStorageInstallPath;

                @Override
                public void setup() {
                    flow(new NoRollbackFlow() {
                        String __name__ = "get-backup-storage-install-path";

                        @Override
                        public void run(final FlowTrigger trigger, Map data) {
                            BackupStorageAskInstallPathMsg msg = new BackupStorageAskInstallPathMsg();
                            msg.setBackupStorageUuid(backupStorage.getUuid());
                            msg.setImageUuid(uparam.image.getUuid());
                            msg.setImageMediaType(uparam.image.getMediaType());
                            bus.makeTargetServiceIdByResourceUuid(msg, BackupStorageConstant.SERVICE_ID, backupStorage.getUuid());
                            bus.send(msg, new CloudBusCallBack(trigger) {
                                @Override
                                public void run(MessageReply reply) {
                                    if (!reply.isSuccess()) {
                                        trigger.fail(reply.getError());
                                    } else {
                                        backupStorageInstallPath = ((BackupStorageAskInstallPathReply) reply).getInstallPath();
                                        trigger.next();
                                    }
                                }
                            });
                        }
                    });

                    flow(new NoRollbackFlow() {
                        String __name__ = "cp-to-the-image";

                        @Override
                        public void run(final FlowTrigger trigger, Map data) {
                            CpCmd cmd = new CpCmd();
                            CpRsp rsp = new CpRsp();
                            cmd.srcPath = uparam.primaryStorageInstallPath;
                            cmd.srcPoolName = getBossPoolNameFromPath(cmd.srcPath);
                            cmd.srcVolumeName = getBossVolumeNameFromPath(cmd.srcPath);
                            cmd.dstPath = backupStorageInstallPath;
                            cmd.dstPoolName = getBossPoolNameFromPath(cmd.dstPath);
                            cmd.dstVolumeName = getBossVolumeNameFromPath(cmd.dstPath);
                            ReturnValueCompletion<CpRsp> CpCompletion = new ReturnValueCompletion<CpRsp>(completion){
                                @Override
                                public void success(CpRsp returnValue) {
                                    trigger.next();
                                }

                                @Override
                                public void fail(ErrorCode errorCode) {
                                    trigger.fail(errorCode);
                                }
                            };
                            ShellResult shellResult = ShellUtils.runAndReturn(String.format("volume_copy -sp %s -sv %s -dp %s -dv %s",
                                    cmd.srcPoolName,cmd.srcVolumeName,cmd.dstPoolName,cmd.dstVolumeName));
                            if (shellResult.getRetCode() == 0){
                                rsp.size = getVolumeSizeFromPathInBoss(cmd.dstPath);
                                rsp.availableCapacity = getAvailableCapacity(getSelf());
                                rsp.totalCapacity = getTotalCapacity(getSelf());
                                updateCapacity(rsp);
                                CpCompletion.success(rsp);
                            } else {
                                CpCompletion.fail(errf.stringToOperationError(String.format("upload volume to backupStorage failed," +
                                        "causes[%s]",shellResult.getStderr())));
                            }

                        }
                    });

                    done(new FlowDoneHandler(completion) {
                        @Override
                        public void handle(Map data) {
                            completion.success(backupStorageInstallPath);
                        }
                    });

                    error(new FlowErrorHandler(completion) {
                        @Override
                        public void handle(ErrorCode errCode, Map data) {
                            completion.fail(errCode);
                        }
                    });
                }
            }).start();
        }

        @Override
        boolean deleteWhenRollabackDownload() {
            return false;
        }
    }

    public BossPrimaryStorageBase(BossPrimaryStorageVO self) { super(self);}


    protected BossPrimaryStorageVO getSelf() {
        return (BossPrimaryStorageVO) self;
    }

    protected BossPrimaryStorageInventory getSelfInventory() { return BossPrimaryStorageInventory.valueOf(getSelf());}
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
        } else if (msg instanceof UploadBitsToBackupStorageMsg) {
            handle((UploadBitsToBackupStorageMsg) msg);
        } else if (msg instanceof SetupSelfFencerOnKvmHostMsg) {
            handle((SetupSelfFencerOnKvmHostMsg) msg);
        } else if (msg instanceof CancelSelfFencerOnKvmHostMsg) {
            handle((CancelSelfFencerOnKvmHostMsg) msg);
        } else if (msg instanceof DeleteImageCacheOnPrimaryStorageMsg) {
            handle((DeleteImageCacheOnPrimaryStorageMsg) msg);
        } else {
            super.handleLocalMessage(msg);
        }
    }

    private void handle(DeleteImageCacheOnPrimaryStorageMsg msg) {
        DeleteImageCacheOnPrimaryStorageReply reply = new DeleteImageCacheOnPrimaryStorageReply();

        DeleteImageCacheCmd cmd = new DeleteImageCacheCmd();
        ShellResponse rsp = new ShellResponse();
        cmd.setClusterName(getSelf().getClusterName());
        cmd.setUuid(self.getUuid());
        cmd.imagePath = msg.getInstallPath().split("@")[0];
        cmd.snapshotPath = msg.getInstallPath();
        cmd.imagePoolName = getBossPoolNameFromPath(cmd.imagePath);
        cmd.snapShotPoolName = getBossPoolNameFromPath(cmd.snapshotPath);
        cmd.snapShotName = getBossVolumeNameFromPath(cmd.snapshotPath);

        ReturnValueCompletion<ShellResponse> deleteImageCacheCompletion = new ReturnValueCompletion<ShellResponse>(msg) {
            @Override
            public void success(ShellResponse rsp) {
                if (!rsp.isSuccess()) {
                    reply.setError(errf.stringToOperationError(rsp.getError()));
                }

                bus.reply(msg, reply);
            }

            @Override
            public void fail(ErrorCode errorCode) {
                reply.setError(errorCode);
                bus.reply(msg, reply);
            }
        };

        ShellResult imageInfo = ShellUtils.runAndReturn(String.format("volume_info -p %s -v %s",cmd.imagePoolName,cmd.imageName));

        if(imageInfo.getRetCode()!= 0){
            deleteImageCacheCompletion.success(rsp);
            return;
        }
        /*
        ShellResult deleteSnapShot = ShellUtils.runAndReturn(String.format("snap_delete -p %s -s %s",cmd.snapShotPoolName,cmd.snapShotName));
        if(deleteSnapShot.getRetCode() != 0){
            deleteImageCacheCompletion.fail(errf.stringToOperationError(String.format("the image cache[%s] is still in used.",cmd.imagePath)));
            return;
        }
        */
        ShellResult deleteImageCache = ShellUtils.runAndReturn(String.format("volume_delete -p %s -v %s",cmd.imagePoolName,cmd.imageName));

        if(deleteImageCache.getRetCode() == 0){
            rsp.availableCapacity = getAvailableCapacity(getSelf());
            rsp.totalCapacity = getTotalCapacity(getSelf());
            updateCapacity(rsp);
            deleteImageCacheCompletion.success(rsp);
        } else {
            deleteImageCacheCompletion.fail(errf.stringToOperationError(String.format("delete image cache[%s] failed , errors :%s",
                    cmd.imagePath,deleteImageCache.getStderr())));
        }
    }

    private void handle(CancelSelfFencerOnKvmHostMsg msg) {
        KvmSetupSelfFencerExtensionPoint.KvmCancelSelfFencerParam param = msg.getParam();
        KvmCancelSelfFencerCmd cmd = new KvmCancelSelfFencerCmd();
        cmd.uuid = self.getUuid();
        cmd.clusterName = getSelf().getClusterName();
        cmd.hostUuid = param.getHostUuid();

        CancelSelfFencerOnKvmHostReply reply = new CancelSelfFencerOnKvmHostReply();
        new KvmCommandSender(param.getHostUuid()).send(cmd, KVM_HA_CANCEL_SELF_FENCER, wrapper -> {
            ShellResponse rsp = wrapper.getResponse(ShellResponse.class);
            return rsp.isSuccess() ? null : errf.stringToOperationError(rsp.getError());
        }, new ReturnValueCompletion<KvmResponseWrapper>(msg) {
            @Override
            public void success(KvmResponseWrapper w) {
                bus.reply(msg, reply);
            }

            @Override
            public void fail(ErrorCode errorCode) {
                reply.setError(errorCode);
                bus.reply(msg, reply);
            }
        });
    }

    private void handle(final SetupSelfFencerOnKvmHostMsg msg) {
        KvmSetupSelfFencerExtensionPoint.KvmSetupSelfFencerParam param = msg.getParam();
        KvmSetupSelfFencerCmd cmd = new KvmSetupSelfFencerCmd();
        cmd.uuid = self.getUuid();
        cmd.clusterName = getSelf().getClusterName();
        cmd.hostUuid = param.getHostUuid();
        cmd.interval = param.getInterval();
        cmd.maxAttempts = param.getMaxAttempts();
        cmd.storageCheckerTimeout = param.getStorageCheckerTimeout();
        cmd.heartbeatImagePath = String.format("%s/boss-primary-storage-%s-heartbeat-file", getSelf().getRootVolumePoolName(), self.getUuid());

        final SetupSelfFencerOnKvmHostReply reply = new SetupSelfFencerOnKvmHostReply();
        new KvmCommandSender(param.getHostUuid()).send(cmd, KVM_HA_SETUP_SELF_FENCER, new KvmCommandFailureChecker() {
            @Override
            public ErrorCode getError(KvmResponseWrapper wrapper) {
                ShellResponse rsp = wrapper.getResponse(ShellResponse.class);
                return rsp.isSuccess() ? null : errf.stringToOperationError(rsp.getError());
            }
        }, new ReturnValueCompletion<KvmResponseWrapper>(msg) {
            @Override
            public void success(KvmResponseWrapper wrapper) {
                bus.reply(msg, reply);
            }

            @Override
            public void fail(ErrorCode errorCode) {
                reply.setError(errorCode);
                bus.reply(msg, reply);
            }
        });
    }


    public void handle(TakeSnapshotMsg msg) {
        final TakeSnapshotReply reply = new TakeSnapshotReply();

        final VolumeSnapshotInventory sp = msg.getStruct().getCurrent();
        SimpleQuery<VolumeVO> q = dbf.createQuery(VolumeVO.class);
        q.select(VolumeVO_.installPath);
        q.add(VolumeVO_.uuid, SimpleQuery.Op.EQ, sp.getVolumeUuid());
        String volumePath = q.findValue();

        final String spPath = String.format("%s@%s", volumePath, sp.getUuid());
        String volumeName = getBossVolumeNameFromPath(volumePath);
        CreateSnapshotCmd cmd = new CreateSnapshotCmd();
        CreateSnapshotRsp rsp = new CreateSnapshotRsp();
        cmd.volumeUuid = sp.getVolumeUuid();
        cmd.snapshotPath = spPath;
        cmd.snapShotName = getBossVolumeNameFromPath(cmd.snapshotPath);
        cmd.snapShotPoolName = getBossPoolNameFromPath(cmd.snapshotPath);
        ReturnValueCompletion<CreateSnapshotRsp> takeSnapshotCompletion = new ReturnValueCompletion<CreateSnapshotRsp>(msg) {
            @Override
            public void success(CreateSnapshotRsp rsp) {
                // current boss has no way to get actual size
                long asize = rsp.getActualSize() == null ? 1 : rsp.getActualSize();
                sp.setSize(asize);
                sp.setPrimaryStorageUuid(self.getUuid());
                sp.setPrimaryStorageInstallPath(spPath);
                sp.setType(VolumeSnapshotConstant.STORAGE_SNAPSHOT_TYPE.toString());
                sp.setFormat(VolumeConstant.VOLUME_FORMAT_RAW);
                reply.setInventory(sp);
                bus.reply(msg, reply);
            }

            @Override
            public void fail(ErrorCode errorCode) {
                reply.setError(errorCode);
                bus.reply(msg, reply);
            }
        };

        ShellResult createSnapShot = ShellUtils.runAndReturn(String.format("snap_create -p %s -v %s -s %s",cmd.snapShotPoolName,volumeName,cmd.snapShotName));
        if(createSnapShot.getRetCode() == 0){
            rsp.size = getVolumeSizeFromPathInBoss(cmd.snapshotPath);
            rsp.availableCapacity = getAvailableCapacity(getSelf());
            rsp.totalCapacity = getTotalCapacity(getSelf());
            updateCapacity(rsp);
            takeSnapshotCompletion.success(rsp);
        } else {
            takeSnapshotCompletion.fail(errf.stringToOperationError(
                    String.format("create snapshot %s failed,causes[%s]", cmd.snapshotPath, createSnapShot.getStderr())));
        }
    }

    public void handle(MergeVolumeSnapshotOnPrimaryStorageMsg msg) {
        MergeVolumeSnapshotOnPrimaryStorageReply reply = new MergeVolumeSnapshotOnPrimaryStorageReply();
        bus.reply(msg, reply);
    }

    public void handle(DeleteSnapshotOnPrimaryStorageMsg msg) {
        DeleteSnapshotCmd cmd = new DeleteSnapshotCmd();
        DeleteSnapshotRsp rsp = new DeleteSnapshotRsp();

        cmd.snapshotPath = msg.getSnapshot().getPrimaryStorageInstallPath();
        cmd.snapShotName = getBossVolumeNameFromPath(cmd.snapshotPath);
        cmd.snapShotPoolName = getBossPoolNameFromPath(cmd.snapshotPath);
        final DeleteSnapshotOnPrimaryStorageReply reply = new DeleteSnapshotOnPrimaryStorageReply();
        ReturnValueCompletion<DeleteSnapshotRsp> deleteSnapshotCompletion = new ReturnValueCompletion<DeleteSnapshotRsp>(msg){
            @Override
            public void success(DeleteSnapshotRsp returnValue) {
                bus.reply(msg, reply);
            }

            @Override
            public void fail(ErrorCode errorCode) {
                reply.setError(errorCode);
                bus.reply(msg, reply);
            }
        };
        ShellResult deleteSnapShot = ShellUtils.runAndReturn(String.format("snap_delete -p %s -s %s",cmd.snapShotPoolName,cmd.snapShotName));

        if(deleteSnapShot.getRetCode() == 0){
            rsp.availableCapacity = getAvailableCapacity(getSelf());
            rsp.totalCapacity = getTotalCapacity(getSelf());
            updateCapacity(rsp);
            deleteSnapshotCompletion.success(rsp);
        } else {
            deleteSnapshotCompletion.fail(errf.stringToOperationError(String.format("delete snapshot[%s] failed , errors :%s",
                    cmd.snapshotPath,deleteSnapShot.getStderr())));
        }


    }

    public void handle(RevertVolumeFromSnapshotOnPrimaryStorageMsg msg) {
        final RevertVolumeFromSnapshotOnPrimaryStorageReply reply  = new RevertVolumeFromSnapshotOnPrimaryStorageReply();

        if (msg.getVolume().getVmInstanceUuid() != null) {
            SimpleQuery<VmInstanceVO> q = dbf.createQuery(VmInstanceVO.class);
            q.select(VmInstanceVO_.state);
            q.add(VmInstanceVO_.uuid, SimpleQuery.Op.EQ, msg.getVolume().getVmInstanceUuid());
            VmInstanceState state = q.findValue();
            if (state != VmInstanceState.Stopped) {
                reply.setError(errf.stringToOperationError(
                        String.format("unable to revert volume[uuid:%s] to snapshot[uuid:%s], the vm[uuid:%s] volume attached to is not in Stopped state, current state is %s",
                                msg.getVolume().getUuid(), msg.getSnapshot().getUuid(), msg.getVolume().getVmInstanceUuid(), state)
                ));

                bus.reply(msg, reply);
                return;
            }
        }

        RollbackSnapshotCmd cmd = new RollbackSnapshotCmd();
        RollbackSnapshotRsp rsp = new RollbackSnapshotRsp();
        cmd.snapshotPath = msg.getSnapshot().getPrimaryStorageInstallPath();
        String snapshotName = getBossVolumeNameFromPath(cmd.snapshotPath);
        String snapshotPoolName = getBossPoolNameFromPath(cmd.snapshotPath);
        String currentVolumeName = getBossVolumeNameFromPath(msg.getVolume().getInstallPath());
        String currentVolumePoolName = getBossPoolNameFromPath(msg.getVolume().getInstallPath());
        ReturnValueCompletion<RollbackSnapshotRsp> rollbackCompletion = new ReturnValueCompletion<RollbackSnapshotRsp>(msg) {
            @Override
            public void success(RollbackSnapshotRsp returnValue) {
                reply.setNewVolumeInstallPath(msg.getVolume().getInstallPath());
                bus.reply(msg, reply);
            }

            @Override
            public void fail(ErrorCode errorCode) {
                reply.setError(errorCode);
                bus.reply(msg, reply);
            }
        };
        if(snapshotName != currentVolumePoolName){
            rollbackCompletion.fail(errf.stringToOperationError("roll back failed!"));
            return;
        }

        ShellResult deleteVolume = ShellUtils.runAndReturn(String.format("volume_delete -p %s -v %s",currentVolumePoolName,currentVolumeName));

        ShellResult rollback = ShellUtils.runAndReturn(String.format("snap_clone -p %s -v %s -s %s",currentVolumePoolName,currentVolumeName,snapshotName));

        if(rollback.getRetCode() == 0){
            rsp.availableCapacity = getAvailableCapacity(getSelf());
            rsp.totalCapacity = getTotalCapacity(getSelf());
            updateCapacity(rsp);
            rollbackCompletion.success(rsp);
        } else {
            rollbackCompletion.fail(errf.stringToOperationError(String.format("rollback volume[%s] failed!,causes[%s]",currentVolumeName,rollback.getStderr())));
        }


    }

    public void handle(CreateVolumeFromVolumeSnapshotOnPrimaryStorageMsg msg) {


    }

    public void handle(BackupVolumeSnapshotFromPrimaryStorageToBackupStorageMsg msg) {
        BackupVolumeSnapshotFromPrimaryStorageToBackupStorageReply reply = new BackupVolumeSnapshotFromPrimaryStorageToBackupStorageReply();
        reply.setError(errf.stringToOperationError("backing up snapshots to backup storage is a depreciated feature, which will be removed in future version"));
        bus.reply(msg, reply);
    }

    private void createEmptyVolume(final InstantiateVolumeOnPrimaryStorageMsg msg) {
        final CreateEmptyVolumeCmd cmd = new CreateEmptyVolumeCmd();
        final CreateEmptyVolumeRsp rsp = new CreateEmptyVolumeRsp();
        String poolName = null;
        String volumeName = null;
        if(VolumeType.Root.toString().equals(msg.getVolume().getType())){
            volumeName = "RV-"+msg.getVolume().getUuid();
            poolName = getSelfInventory().getRootVolumePoolName();
            cmd.installPath = makeRootVolumeInstallPath(msg.getVolume().getUuid());
        } else {
            poolName = getSelfInventory().getDataVolumePoolName();
            volumeName = "DV-"+msg.getVolume().getUuid();
            cmd.installPath = makeDataVolumeInstallPath(msg.getVolume().getUuid());
        }
        cmd.size = msg.getVolume().getSize();

        final InstantiateVolumeOnPrimaryStorageReply reply = new InstantiateVolumeOnPrimaryStorageReply();

        ReturnValueCompletion<CreateEmptyVolumeRsp> completion = new  ReturnValueCompletion<CreateEmptyVolumeRsp>(msg){

            @Override
            public void success(CreateEmptyVolumeRsp rsp) {
                VolumeInventory vol = msg.getVolume();
                vol.setInstallPath(cmd.getInstallPath());
                vol.setFormat(VolumeConstant.VOLUME_FORMAT_RAW);
                reply.setVolume(vol);
                bus.reply(msg, reply);
            }

            @Override
            public void fail(ErrorCode err) {
                reply.setError(err);
                bus.reply(msg, reply);
            }
        };

        ShellResult shellResult = ShellUtils.runAndReturn(String.format("volume_create -p %s -v -s %s -r 3"
                ,poolName,volumeName,cmd.size));

        if(shellResult.getRetCode() == 0){
            completion.success(rsp);
        } else {
            completion.fail(errf.stringToOperationError(String.format("create volume failed,causes[%s]",shellResult.getStderr())));
        }

    }

    class DownloadToCache {
        VmInstanceSpec.ImageSpec image;

        private void doDownload(final ReturnValueCompletion<ImageCacheVO> completion) {
            SimpleQuery<ImageCacheVO> q = dbf.createQuery(ImageCacheVO.class);
            q.add(ImageCacheVO_.imageUuid, SimpleQuery.Op.EQ, image.getInventory().getUuid());
            q.add(ImageCacheVO_.primaryStorageUuid, SimpleQuery.Op.EQ, self.getUuid());
            ImageCacheVO cache = q.find();
            if (cache != null) {
                completion.success(cache);
                return;
            }

            final FlowChain chain = FlowChainBuilder.newShareFlowChain();
            chain.setName(String.format("prepare-image-cache-boss-%s", self.getUuid()));
            chain.then(new ShareFlow() {
                String cachePath;
                String snapshotPath;

                @Override
                public void setup() {
                    flow(new Flow() {
                        String __name__ = "allocate-primary-storage-capacity-for-image-cache";

                        boolean s = false;

                        @Override
                        public void run(final FlowTrigger trigger, Map data) {
                            AllocatePrimaryStorageMsg amsg = new AllocatePrimaryStorageMsg();
                            amsg.setRequiredPrimaryStorageUuid(self.getUuid());
                            amsg.setSize(image.getInventory().getActualSize());
                            amsg.setPurpose(PrimaryStorageAllocationPurpose.DownloadImage.toString());
                            amsg.setNoOverProvisioning(true);
                            bus.makeLocalServiceId(amsg, PrimaryStorageConstant.SERVICE_ID);
                            bus.send(amsg, new CloudBusCallBack(trigger) {
                                @Override
                                public void run(MessageReply reply) {
                                    if (!reply.isSuccess()) {
                                        trigger.fail(reply.getError());
                                    } else {
                                        s = true;
                                        trigger.next();
                                    }
                                }
                            });
                        }

                        @Override
                        public void rollback(FlowRollback trigger, Map data) {
                            if (s) {
                                ReturnPrimaryStorageCapacityMsg rmsg = new ReturnPrimaryStorageCapacityMsg();
                                rmsg.setNoOverProvisioning(true);
                                rmsg.setPrimaryStorageUuid(self.getUuid());
                                rmsg.setDiskSize(image.getInventory().getActualSize());
                                bus.makeLocalServiceId(rmsg, PrimaryStorageConstant.SERVICE_ID);
                                bus.send(rmsg);
                            }

                            trigger.rollback();
                        }
                    });

                    flow(new Flow() {
                        String __name__ = "download-from-backup-storage";

                        boolean deleteOnRollback;

                        @Override
                        public void run(final FlowTrigger trigger, Map data) {
                            DownloadParam param = new DownloadParam();
                            param.image = image;
                            param.installPath = makeCacheInstallPath(image.getInventory().getUuid());
                            BackupStorageMediator mediator = getBackupStorageMediator(image.getSelectedBackupStorage().getBackupStorageUuid());
                            mediator.param = param;

                            deleteOnRollback = mediator.deleteWhenRollabackDownload();
                            mediator.download(new ReturnValueCompletion<String>(trigger) {
                                @Override
                                public void success(String path) {
                                    cachePath = path;
                                    trigger.next();
                                }

                                @Override
                                public void fail(ErrorCode errorCode) {
                                    trigger.fail(errorCode);
                                }
                            });
                        }

                        @Override
                        public void rollback(FlowRollback trigger, Map data) {
                            if (deleteOnRollback && cachePath != null) {
                                DeleteCmd cmd = new DeleteCmd();
                                DeleteRsp rsp = new DeleteRsp();
                                cmd.installPath = cachePath;
                                cmd.poolName = getBossPoolNameFromPath(cachePath);
                                cmd.volumeName = getBossVolumeNameFromPath(cachePath);
                                ReturnValueCompletion<DeleteRsp> deleteCompletion = new ReturnValueCompletion<DeleteRsp>() {
                                    @Override
                                    public void success(DeleteRsp returnValue) {
                                        logger.debug(String.format("successfully deleted %s", cachePath));
                                    }

                                    @Override
                                    public void fail(ErrorCode errorCode) {
                                        //TODO
                                        logger.warn(String.format("unable to delete %s, %s. Need a cleanup", cachePath, errorCode));
                                    }
                                };

                                //?
                                ShellResult shellResult = ShellUtils.runAndReturn(String.format("snap_list -p %s | grep %s",cmd.poolName,cmd.volumeName));
                                if(!shellResult.getStdout().isEmpty()){
                                    deleteCompletion.fail(errf.stringToOperationError(
                                            String.format("unable to delete %s; the volume still has snapshots",cmd.installPath)));
                                    return;
                                } else {
                                    ShellResult deleteResult = ShellUtils.runAndReturn(
                                            String.format("volume_delete -p %s -v %s",cmd.poolName,cmd.volumeName));
                                    if(deleteResult.getRetCode() == 0){
                                        rsp.availableCapacity = getAvailableCapacity(getSelf());
                                        rsp.totalCapacity = getTotalCapacity(getSelf());
                                        BossCapacityUpdater updater = new BossCapacityUpdater();
                                        updater.update(getSelf().getClusterName(),rsp.totalCapacity,rsp.availableCapacity);
                                        deleteCompletion.success(rsp);
                                    } else {
                                        deleteCompletion.fail(errf.stringToOperationError(String.format("causes[%s]",deleteResult.getStderr())));
                                    }
                                }
                            }

                            trigger.rollback();
                        }
                    });

                    flow(new Flow() {
                        String __name__ = "create-snapshot";

                        boolean needCleanup = false;

                        @Override
                        public void run(final FlowTrigger trigger, Map data) {
                            snapshotPath = String.format("%s@%s", cachePath, image.getInventory().getUuid());
                            CreateSnapshotCmd cmd = new CreateSnapshotCmd();
                            CreateSnapshotRsp rsp = new CreateSnapshotRsp();
                            cmd.skipOnExisting = true;
                            cmd.snapshotPath = snapshotPath;
                            cmd.snapShotPoolName = getBossPoolNameFromPath(cmd.snapshotPath);
                            cmd.snapShotName = getBossVolumeNameFromPath(cmd.snapshotPath);
                            ReturnValueCompletion<CreateSnapshotRsp> createSnapshotCompletion = new ReturnValueCompletion<CreateSnapshotRsp>(trigger) {
                                @Override
                                public void success(CreateSnapshotRsp returnValue) {
                                    needCleanup = true;
                                    trigger.next();
                                }

                                @Override
                                public void fail(ErrorCode errorCode) {
                                    trigger.fail(errorCode);
                                }
                            };

                            boolean doCreate = true;

                            if (cmd.skipOnExisting) {
                                String volumeName = getBossVolumeNameFromPath(cachePath);
                                String[] snaplist = ShellUtils.runAndReturn(
                                        String.format("snap_list -p %s | grep %s | awk '{print $7}'", cmd.snapShotPoolName, volumeName)).getStdout().split("\n");
                                for (String s : snaplist) {
                                    if (cmd.snapShotName == s) {
                                        doCreate = false;
                                    }
                                }
                            }

                            if (doCreate) {
                                ShellResult snapCreateResult = ShellUtils.runAndReturn(
                                        String.format("snap_create -p %s -v %s -s %s", cmd.snapShotPoolName, getBossVolumeNameFromPath(cachePath), cmd.snapShotName));
                                if (snapCreateResult.getRetCode() == 0) {
                                    rsp.size = getVolumeSizeFromPathInBoss(cmd.snapshotPath);
                                    rsp.availableCapacity = getAvailableCapacity(getSelf());
                                    rsp.totalCapacity = getTotalCapacity(getSelf());
                                    updateCapacity(rsp);
                                    createSnapshotCompletion.success(rsp);
                                } else {
                                    createSnapshotCompletion.fail(errf.stringToOperationError(
                                            String.format("create snapshot %s failed,causes[%s]", cmd.snapshotPath, snapCreateResult.getStderr())));
                                }
                            }
                        }


                        @Override
                        public void rollback(FlowRollback trigger, Map data) {
                            if (needCleanup) {
                                DeleteSnapshotCmd cmd = new DeleteSnapshotCmd();
                                DeleteSnapshotRsp rsp = new DeleteSnapshotRsp();
                                cmd.snapshotPath = snapshotPath;
                                cmd.snapShotName = getBossPoolNameFromPath(snapshotPath);
                                cmd.snapShotPoolName = getBossVolumeNameFromPath(snapshotPath);
                                ReturnValueCompletion<DeleteSnapshotRsp> deleteSnapShotCompletion = new ReturnValueCompletion<DeleteSnapshotRsp>() {
                                    @Override
                                    public void success(DeleteSnapshotRsp returnValue) {
                                        logger.debug(String.format("successfully deleted the snapshot %s", snapshotPath));
                                    }

                                    @Override
                                    public void fail(ErrorCode errorCode) {
                                        //TODO
                                        logger.warn(String.format("unable to delete the snapshot %s, %s. Need a cleanup", snapshotPath, errorCode));
                                    }
                                };

                                ShellResult deleteSnapShotResult = ShellUtils.runAndReturn(String.format("snap_delete -p %s -s %s",cmd.snapShotPoolName,cmd.snapShotName));
                                if(deleteSnapShotResult.getRetCode() == 0){
                                    rsp.availableCapacity = getAvailableCapacity(getSelf());
                                    rsp.totalCapacity = getTotalCapacity(getSelf());
                                    BossCapacityUpdater updater = new BossCapacityUpdater();
                                    updater.update(getSelf().getClusterName(), rsp.totalCapacity, rsp.availableCapacity);
                                    deleteSnapShotCompletion.success(rsp);

                                } else {
                                    deleteSnapShotCompletion.fail(errf.stringToOperationError(deleteSnapShotResult.getStderr()));
                                }

                            }

                            trigger.rollback();
                        }
                    });

                    flow(new NoRollbackFlow() {
                        String __name__ = "protect-snapshot";

                        @Override
                        public void run(final FlowTrigger trigger, Map data) {
                            trigger.next();
                        }
                    });

                    done(new FlowDoneHandler(completion) {
                        @Override
                        public void handle(Map data) {
                            ImageCacheVO cvo = new ImageCacheVO();
                            cvo.setMd5sum("not calculated");
                            cvo.setSize(image.getInventory().getActualSize());
                            cvo.setInstallUrl(snapshotPath);
                            cvo.setImageUuid(image.getInventory().getUuid());
                            cvo.setPrimaryStorageUuid(self.getUuid());
                            cvo.setMediaType(ImageConstant.ImageMediaType.valueOf(image.getInventory().getMediaType()));
                            cvo.setState(ImageCacheState.ready);
                            cvo = dbf.persistAndRefresh(cvo);

                            completion.success(cvo);
                        }
                    });

                    error(new FlowErrorHandler(completion) {
                        @Override
                        public void handle(ErrorCode errCode, Map data) {
                            completion.fail(errCode);
                        }
                    });
                }
            }).start();
        }

        void download(final ReturnValueCompletion<ImageCacheVO> completion) {
            thdf.chainSubmit(new ChainTask(completion) {
                @Override
                public String getSyncSignature() {
                    return String.format("ceph-p-%s-download-image-%s", self.getUuid(), image.getInventory().getUuid());
                }

                @Override
                public void run(final SyncTaskChain chain) {
                    doDownload(new ReturnValueCompletion<ImageCacheVO>(chain) {
                        @Override
                        public void success(ImageCacheVO returnValue) {
                            completion.success(returnValue);
                            chain.next();
                        }

                        @Override
                        public void fail(ErrorCode errorCode) {
                            completion.fail(errorCode);
                            chain.next();
                        }
                    });
                }

                @Override
                public String getName() {
                    return getSyncSignature();
                }
            });
        }
    }

    private void createVolumeFromTemplate(final InstantiateRootVolumeFromTemplateOnPrimaryStorageMsg msg) {
        final ImageInventory img = msg.getTemplateSpec().getInventory();

        final InstantiateVolumeOnPrimaryStorageReply reply = new InstantiateVolumeOnPrimaryStorageReply();

        FlowChain chain = FlowChainBuilder.newShareFlowChain();
        chain.setName(String.format("create-root-volume-%s", msg.getVolume().getUuid()));
        chain.then(new ShareFlow() {
            String cloneInstallPath;
            String volumePath = makeRootVolumeInstallPath(msg.getVolume().getUuid());
            ImageCacheVO cache;

            @Override
            public void setup() {
                flow(new NoRollbackFlow() {
                    String __name__ ="download-image-to-cache";

                    @Override
                    public void run(final FlowTrigger trigger, Map data) {
                        DownloadToCache downloadToCache = new DownloadToCache();
                        downloadToCache.image = msg.getTemplateSpec();
                        downloadToCache.download(new ReturnValueCompletion<ImageCacheVO>(trigger) {
                            @Override
                            public void success(ImageCacheVO returnValue) {
                                cloneInstallPath = returnValue.getInstallUrl();
                                cache = returnValue;
                                trigger.next();
                            }

                            @Override
                            public void fail(ErrorCode errorCode) {
                                trigger.fail(errorCode);
                            }
                        });
                    }
                });


                flow(new NoRollbackFlow() {
                    String __name__ = "clone-image";

                    @Override
                    public void run(final FlowTrigger trigger, Map data) {
                        CloneCmd cmd = new CloneCmd();
                        CloneRsp rsp = new CloneRsp();
                        cmd.srcPath = cloneInstallPath;
                        cmd.dstPath = volumePath;
                        cmd.dstPoolName = getBossPoolNameFromPath(cmd.dstPath);
                        cmd.dstVolumeName = getBossVolumeNameFromPath(cmd.dstPath);
                        cmd.srcPoolName = getBossPoolNameFromPath(cmd.srcPath);
                        cmd.srcVolumeName = getBossVolumeNameFromPath(cmd.srcPath);
                        String clonePoolName = null;
                        ReturnValueCompletion<CloneRsp> cloneCompletion = new ReturnValueCompletion<CloneRsp>(trigger) {
                            @Override
                            public void fail(ErrorCode err) {
                                trigger.fail(err);
                            }

                            @Override
                            public void success(CloneRsp ret) {
                                trigger.next();
                            }
                        };

                        if(cmd.dstPoolName == cmd.srcPoolName){
                            clonePoolName = cmd.dstPoolName;
                        } else {
                            cloneCompletion.fail(errf.stringToOperationError("can't clone between different pools!"));
                        }

                        ShellResult cloneShellResult = ShellUtils.runAndReturn(String.format("snap_clone -p %s -v %s -s %s",clonePoolName,cmd.srcVolumeName,cmd.dstVolumeName));
                        if(cloneShellResult.getRetCode() == 0){
                            rsp.totalCapacity = getTotalCapacity(getSelf());
                            rsp.availableCapacity = getAvailableCapacity(getSelf());
                            BossCapacityUpdater updater = new BossCapacityUpdater();
                            updater.update(getSelf().getClusterName(), rsp.totalCapacity, rsp.availableCapacity);
                            cloneCompletion.success(rsp);
                        }

                    }
                });

                done(new FlowDoneHandler(msg) {
                    @Override
                    public void handle(Map data) {
                        VolumeInventory vol = msg.getVolume();
                        vol.setInstallPath(volumePath);
                        vol.setFormat(VolumeConstant.VOLUME_FORMAT_RAW);
                        reply.setVolume(vol);

                        ImageCacheVolumeRefVO ref = new ImageCacheVolumeRefVO();
                        ref.setImageCacheId(cache.getId());
                        ref.setPrimaryStorageUuid(self.getUuid());
                        ref.setVolumeUuid(vol.getUuid());
                        dbf.persist(ref);

                        bus.reply(msg, reply);
                    }
                });

                error(new FlowErrorHandler(msg) {
                    @Override
                    public void handle(ErrorCode errCode, Map data) {
                        reply.setError(errCode);
                        bus.reply(msg, reply);
                    }
                });
            }
        }).start();

    }

    protected void handle(InstantiateVolumeOnPrimaryStorageMsg msg) {
        if (msg instanceof InstantiateRootVolumeFromTemplateOnPrimaryStorageMsg) {
            createVolumeFromTemplate((InstantiateRootVolumeFromTemplateOnPrimaryStorageMsg) msg);
        } else {
            createEmptyVolume(msg);
        }
    }

    @Override
    protected void handle(DeleteVolumeOnPrimaryStorageMsg msg) {
        DeleteCmd cmd = new DeleteCmd();
        DeleteRsp rsp = new DeleteRsp();
        cmd.installPath = msg.getVolume().getInstallPath();
        cmd.volumeName = getBossVolumeNameFromPath(cmd.installPath);
        cmd.poolName = getBossPoolNameFromPath(cmd.installPath);

        final DeleteVolumeOnPrimaryStorageReply reply = new DeleteVolumeOnPrimaryStorageReply();

        ReturnValueCompletion<DeleteRsp> deleteCompletion = new ReturnValueCompletion<DeleteRsp>(msg) {
            @Override
            public void fail(ErrorCode err) {
                reply.setError(err);
                bus.reply(msg, reply);
            }

            @Override
            public void success(DeleteRsp ret) {
                bus.reply(msg, reply);
            }
        };

        ShellResult deleteVolume = ShellUtils.runAndReturn(String.format("volume_delete -p %s -v %s",cmd.poolName,cmd.volumeName));

        if(deleteVolume.getRetCode() == 0){
            rsp.availableCapacity = getAvailableCapacity(getSelf());
            rsp.totalCapacity = getTotalCapacity(getSelf());
            updateCapacity(rsp);
            deleteCompletion.success(rsp);
        } else {
            deleteCompletion.fail(errf.stringToOperationError(String.format("delete volume[%s] failed , errors :%s",
                    cmd.installPath,deleteVolume.getStderr())));
        }

    }

    @Override
    protected void handle(CreateTemplateFromVolumeOnPrimaryStorageMsg msg) {
        final CreateTemplateFromVolumeOnPrimaryStorageReply reply = new CreateTemplateFromVolumeOnPrimaryStorageReply();
        BackupStorageMediator mediator = getBackupStorageMediator(msg.getBackupStorageUuid());

        UploadParam param = new UploadParam();
        param.image = msg.getImageInventory();
        param.primaryStorageInstallPath = msg.getVolumeInventory().getInstallPath();
        mediator.param = param;
        mediator.upload(new ReturnValueCompletion<String>(msg) {
            @Override
            public void success(String returnValue) {
                reply.setTemplateBackupStorageInstallPath(returnValue);
                reply.setFormat(VolumeConstant.VOLUME_FORMAT_RAW);
                bus.reply(msg, reply);
            }

            @Override
            public void fail(ErrorCode errorCode) {
                reply.setError(errorCode);
                bus.reply(msg, reply);
            }
        });
    }

    @Override
    protected void handle(DownloadDataVolumeToPrimaryStorageMsg msg) {
        final DownloadDataVolumeToPrimaryStorageReply reply = new DownloadDataVolumeToPrimaryStorageReply();

        BackupStorageMediator mediator = getBackupStorageMediator(msg.getBackupStorageRef().getBackupStorageUuid());
        VmInstanceSpec.ImageSpec spec = new VmInstanceSpec.ImageSpec();
        spec.setInventory(msg.getImage());
        spec.setSelectedBackupStorage(msg.getBackupStorageRef());
        DownloadParam param = new DownloadParam();
        param.image = spec;
        param.installPath = makeDataVolumeInstallPath(msg.getVolumeUuid());
        mediator.param = param;
        mediator.download(new ReturnValueCompletion<String>(msg) {
            @Override
            public void success(String returnValue) {
                reply.setInstallPath(returnValue);
                reply.setFormat(VolumeConstant.VOLUME_FORMAT_RAW);
                bus.reply(msg, reply);
            }

            @Override
            public void fail(ErrorCode errorCode) {
                reply.setError(errorCode);
                bus.reply(msg, reply);
            }
        });
    }

    @Override
    protected void handle(DeleteBitsOnPrimaryStorageMsg msg) {
        DeleteCmd cmd = new DeleteCmd();
        DeleteRsp rsp = new DeleteRsp();
        cmd.installPath = msg.getInstallPath();
        cmd.volumeName = getBossVolumeNameFromPath(cmd.installPath);
        cmd.poolName = getBossPoolNameFromPath(cmd.installPath);

        final DeleteVolumeOnPrimaryStorageReply reply = new DeleteVolumeOnPrimaryStorageReply();

        ReturnValueCompletion<DeleteRsp> deleteCompletion = new ReturnValueCompletion<DeleteRsp>(msg) {
            @Override
            public void fail(ErrorCode err) {
                reply.setError(err);
                bus.reply(msg, reply);
            }

            @Override
            public void success(DeleteRsp ret) {
                bus.reply(msg, reply);
            }
        };

        ShellResult deleteVolume = ShellUtils.runAndReturn(String.format("volume_delete -p %s -v %s",cmd.poolName,cmd.volumeName));

        if(deleteVolume.getRetCode() == 0){
            rsp.availableCapacity = getAvailableCapacity(getSelf());
            rsp.totalCapacity = getTotalCapacity(getSelf());
            updateCapacity(rsp);
            deleteCompletion.success(rsp);
        } else {
            deleteCompletion.fail(errf.stringToOperationError(String.format("delete volume[%s] failed , errors :%s",
                    cmd.installPath,deleteVolume.getStderr())));
        }
    }

    @Override
    protected void handle(DownloadIsoToPrimaryStorageMsg msg) {
        final DownloadIsoToPrimaryStorageReply reply = new DownloadIsoToPrimaryStorageReply();
        DownloadToCache downloadToCache = new DownloadToCache();
        downloadToCache.image = msg.getIsoSpec();
        downloadToCache.download(new ReturnValueCompletion<ImageCacheVO>(msg) {
            @Override
            public void success(ImageCacheVO returnValue) {
                reply.setInstallPath(returnValue.getInstallUrl());
                bus.reply(msg, reply);
            }

            @Override
            public void fail(ErrorCode errorCode) {
                reply.setError(errorCode);
                bus.reply(msg, reply);
            }
        });
    }

    @Override
    protected void handle(DeleteIsoFromPrimaryStorageMsg msg) {
        DeleteIsoFromPrimaryStorageReply reply = new DeleteIsoFromPrimaryStorageReply();
        bus.reply(msg, reply);
    }

    @Override
    protected void handle(AskVolumeSnapshotCapabilityMsg msg) {
        AskVolumeSnapshotCapabilityReply reply = new AskVolumeSnapshotCapabilityReply();
        VolumeSnapshotCapability cap = new VolumeSnapshotCapability();
        cap.setSupport(true);
        cap.setArrangementType(VolumeSnapshotCapability.VolumeSnapshotArrangementType.INDIVIDUAL);
        reply.setCapability(cap);
        bus.reply(msg, reply);
    }

    @Override
    protected void handle(SyncVolumeSizeOnPrimaryStorageMsg msg) {
        final SyncVolumeSizeOnPrimaryStorageReply reply = new SyncVolumeSizeOnPrimaryStorageReply();
        final VolumeVO vol = dbf.findByUuid(msg.getVolumeUuid(), VolumeVO.class);

        String installPath = vol.getInstallPath();
        GetVolumeSizeCmd cmd = new GetVolumeSizeCmd();
        GetVolumeSizeRsp rsp = new GetVolumeSizeRsp();
        cmd.clusterName = getSelf().getClusterName();
        cmd.uuid = self.getUuid();
        cmd.volumeUuid = msg.getVolumeUuid();
        cmd.installPath = installPath;

        ReturnValueCompletion<GetVolumeSizeRsp> getVolumeSizeCompletion = new ReturnValueCompletion<GetVolumeSizeRsp>(msg) {
            @Override
            public void success(GetVolumeSizeRsp rsp) {
                // current boss has no way to get actual size
                long asize = rsp.actualSize == null ? vol.getActualSize() : rsp.actualSize;
                reply.setActualSize(asize);
                reply.setSize(rsp.size);
                bus.reply(msg, reply);
            }

            @Override
            public void fail(ErrorCode errorCode) {
                reply.setError(errorCode);
                bus.reply(msg, reply);
            }
        };
        rsp.size = getVolumeSizeFromPathInBoss(cmd.installPath);
        getVolumeSizeCompletion.success(rsp);

    }

    private void handle(final UploadBitsToBackupStorageMsg msg) {
        SimpleQuery<BackupStorageVO> q = dbf.createQuery(BackupStorageVO.class);
        q.select(BackupStorageVO_.type);
        q.add(BackupStorageVO_.uuid, SimpleQuery.Op.EQ, msg.getBackupStorageUuid());
        String bsType = q.findValue();

        if (!BossConstants.BOSS_BACKUP_STORAGE_TYPE.equals(bsType)) {
            throw new OperationFailureException(errf.stringToOperationError(
                    String.format("unable to upload bits to the backup storage[type:%s], we only support BOSS", bsType)
            ));
        }

        final UploadBitsToBackupStorageReply reply = new UploadBitsToBackupStorageReply();

        CpCmd cmd = new CpCmd();
        CpRsp rsp = new CpRsp();
        cmd.clusterName = getSelf().getClusterName();
        cmd.srcPath = msg.getPrimaryStorageInstallPath();
        cmd.dstPath = msg.getBackupStorageInstallPath();
        cmd.srcPoolName = getBossPoolNameFromPath(cmd.srcPath);
        cmd.srcVolumeName = getBossVolumeNameFromPath(cmd.srcPath);
        cmd.dstPoolName = getBossPoolNameFromPath(cmd.dstPath);
        cmd.dstVolumeName = getBossVolumeNameFromPath(cmd.dstPath);

        ReturnValueCompletion<CpRsp> CpCompletion = new ReturnValueCompletion<CpRsp>(msg){
            @Override
            public void success(CpRsp rsp) {
                bus.reply(msg, reply);
            }

            @Override
            public void fail(ErrorCode errorCode) {
                reply.setError(errorCode);
                bus.reply(msg, reply);
            }
        };

        ShellResult shellResult = ShellUtils.runAndReturn(String.format("volume_copy -sp %s -sv %s -dp %s -dv %s",
                cmd.srcPoolName,cmd.srcVolumeName,cmd.dstPoolName,cmd.dstVolumeName));
        if (shellResult.getRetCode() == 0){
            rsp.size = getVolumeSizeFromPathInBoss(cmd.dstPath);
            rsp.availableCapacity = getAvailableCapacity(getSelf());
            rsp.totalCapacity = getTotalCapacity(getSelf());
            BossCapacityUpdater updater = new BossCapacityUpdater();
            updater.update(getSelf().getClusterName(),rsp.totalCapacity,rsp.availableCapacity);
            CpCompletion.success(rsp);
        } else {
            CpCompletion.fail(errf.stringToOperationError(String.format("upload bits to backupStorage failed," +
                    "causes[%s]",shellResult.getStderr())));
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
        PrimaryStorageCapacityVO cap = dbf.findByUuid(self.getUuid(), PrimaryStorageCapacityVO.class);
        PhysicalCapacityUsage usage = new PhysicalCapacityUsage();
        usage.availablePhysicalSize = cap.getAvailablePhysicalCapacity();
        usage.totalPhysicalSize =  cap.getTotalPhysicalCapacity();
        completion.success(usage);
    }
}
