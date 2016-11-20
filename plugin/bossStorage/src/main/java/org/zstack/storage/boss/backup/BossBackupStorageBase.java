package org.zstack.storage.boss.backup;

import jdk.nashorn.tools.Shell;
import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Configurable;
import org.springframework.transaction.annotation.Transactional;
import org.zstack.header.core.ApiTimeout;
import org.zstack.header.core.Completion;
import org.zstack.header.core.ReturnValueCompletion;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.errorcode.OperationFailureException;
import org.zstack.header.exception.CloudRuntimeException;
import org.zstack.header.image.APIAddImageMsg;
import org.zstack.header.image.ImageBackupStorageRefInventory;
import org.zstack.header.image.ImageInventory;
import org.zstack.header.message.Message;
import org.zstack.header.storage.backup.*;
import org.zstack.storage.backup.BackupStorageBase;
import org.zstack.storage.boss.BossCapacityUpdater;
import org.zstack.storage.boss.BossSystemTags;
import org.zstack.storage.boss.ExecuteShellCommand;
import org.zstack.storage.boss.primary.BossPrimaryStorageBase;
import org.zstack.utils.CollectionUtils;
import org.zstack.utils.ShellResult;
import org.zstack.utils.ShellUtils;
import org.zstack.utils.Utils;
import org.zstack.utils.filelocater.FileLocatorImpl;
import org.zstack.utils.function.Function;
import org.zstack.utils.gson.JSONObjectUtil;
import org.zstack.utils.logging.CLogger;

import javax.persistence.TypedQuery;
import java.io.File;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import static org.zstack.utils.CollectionDSL.list;

/**
 * Created by XXPS-PC1 on 2016/11/9.
 */
@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class BossBackupStorageBase extends BackupStorageBase {
    private static final CLogger logger = Utils.getLogger(BossBackupStorageBase.class);

    public static class ShellCommand {
        String clusterName;
        String uuid;

        public String getClusterName() {
            return clusterName;
        }

        public void setClusterName(String clusterName) {
            this.clusterName = clusterName;
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
        Long availableCapacity = 0L;

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

    public static class Pool {
        String name;
        boolean predefined;
    }
    public static class InitCmd extends ShellCommand {
        List<Pool> pools;
    }

    public static class InitRsp extends ShellResponse {
        String clusterName;

        public String getClusetName() {
            return clusterName;
        }

        public void setClusterName(String clusterName) {
            this.clusterName = clusterName;
        }
    }


    @ApiTimeout(apiClasses = {APIAddImageMsg.class})
    public static class DownloadCmd extends ShellCommand {
        String url;
        String installPath;
        String imageUuid;
        boolean inject = false;

        public String getImageUuid() {
            return imageUuid;
        }

        public void setImageUuid(String imageUuid) {
            this.imageUuid = imageUuid;
        }

        public String getUrl() {
            return url;
        }

        public void setUrl(String url) {
            this.url = url;
        }

        public String getInstallPath() {
            return installPath;
        }

        public void setInstallPath(String installPath) {
            this.installPath = installPath;
        }

        public boolean isInject() {
            return inject;
        }

        public void setInject(boolean inject) {
            this.inject = inject;
        }
    }

    public static class DownloadRsp extends ShellResponse {
        long size;
        Long actualSize;
        String format;

        public String getFormat() {
            return format;
        }

        public void setFormat(String format) {
            this.format = format;
        }

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

    public static class DeleteCmd extends ShellCommand {
        String installPath;

        public String getInstallPath() {
            return installPath;
        }

        public void setInstallPath(String installPath) {
            this.installPath = installPath;
        }
    }

    public static class DeleteRsp extends ShellResponse {
    }

    public static class GetImageSizeCmd extends ShellCommand {
        public String imageUuid;
        public String installPath;
    }

    public static class GetImageSizeRsp extends ShellResponse {
        public Long size;
        public Long actualSize;
    }

    private void updateCapacityIfNeeded(ShellResponse rsp) {
        if (rsp.getTotalCapacity() != null && rsp.getAvailableCapacity() != null) {
            new BossCapacityUpdater().update(getSelf().getClusterName(), rsp.totalCapacity, rsp.availableCapacity);
        }
    }

    protected String makeImageInstallPath(String imageUuid) {
        return String.format("%s://%s/Image-%s", getSelf().getClusterName().trim().toString(), getSelf().getPoolName(), imageUuid);
    }

    public BossBackupStorageBase(BackupStorageVO self) {
        super(self);
    }

    protected BossBackupStorageVO getSelf() {
        return (BossBackupStorageVO) self;
    }

    protected BossBackupStorageInventory getInventory() {
        return BossBackupStorageInventory.valueOf(getSelf());
    }

    @Override
    public List<ImageInventory> scanImages() {
        return null;
    }

    protected Long getNetFileSize(String filePath) {
        HttpURLConnection urlcon = null;
        Long filesize = null;
        try {
            //create url link
            URL url = new URL(filePath);
            //open url
            urlcon = (HttpURLConnection) url.openConnection();
            //get url properties
            filesize = Long.valueOf(urlcon.getContentLength());
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            //close connect
            urlcon.disconnect();
        }
        return filesize;
    }

    protected Long getLocalFileSize(String path) {
        File file = new File(path);
        return file.length();
    }


    protected String getFilePath(String url) {
        String srcPath = "";
        try {
            File file = new File(url);
            srcPath = file.getCanonicalPath();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return srcPath;
    }

    protected boolean isFile(String path) {
        File file = new File(path);

        if (file.exists()) {
            return true;
        } else {
            return false;
        }
    }

    protected void handleDownload(DownloadCmd cmd, DownloadRsp rsp, Message msg, ReturnValueCompletion<DownloadRsp> completion) {
        List<ErrorCode> errorCodes = new ArrayList<ErrorCode>();
        if (msg instanceof DownloadImageMsg || msg instanceof DownloadVolumeMsg) {
            String tmpImageName = String.format("tmp-%s",cmd.imageUuid);
            String tmpImagePath = "/home/"+tmpImageName;

            if (cmd.url.startsWith("http://") || cmd.url.startsWith("https://")) {
                ShellUtils.run(String.format("wget --no-check-certificate -q -O %s %s",tmpImagePath,cmd.url.toString()),true);
                rsp.actualSize = getNetFileSize(cmd.url);
            } else if (cmd.url.startsWith("file://")) {
                String srcPath = getFilePath(cmd.url.replace("file:", ""));

                if (isFile(srcPath)) {
                    tmpImagePath = srcPath;
                    rsp.actualSize = getLocalFileSize(srcPath);
                } else {
                    completion.fail(errf.stringToOperationError(String.format("can not find the file[%s],errors are %s", srcPath, JSONObjectUtil.toJsonString(errorCodes))));
                    return;
                    //throw new OperationFailureException(errf.stringToOperationError(String.format("can not find the file[%s],errors are %s", srcPath, JSONObjectUtil.toJsonString(errorCodes))));
                }
            } else {
                completion.fail(errf.stringToOperationError(String.format("unknow url[%s]", cmd.url)));
                //throw new OperationFailureException(errf.stringToOperationError(String.format("unknow url[%s]", cmd.url)));
                return;
            }

            String fileFormat = ShellUtils.runAndReturn(String.format("/usr/local/bin/qemu-img info %s | grep 'file format' " +
                    "| cut -d ':' -f 2", tmpImagePath), true).getStdout().trim();

            if (fileFormat.equals("qcow2") || fileFormat.equals("raw")) {
                //get the virtual size of the image
                String imageVirtualSize = ShellUtils.runAndReturn(String.format("/usr/local/bin/qemu-img info %s | grep 'virtual size' " +
                        "| awk '{print $4}'", tmpImagePath), true).getStdout().trim().replace("(","");
                //create a blank volume in boss
                ShellUtils.run(String.format("volume_create -p %s -v Image-%s -s %s -r 3 ",getSelf().getPoolName(),cmd.imageUuid,Long.valueOf(imageVirtualSize)));

                //import the image into boss
                int exitCode = ShellUtils.runAndReturn(String.format("/usr/local/bin/qemu-img convert -O raw %s %s", tmpImagePath, cmd.installPath),true).getRetCode();
                if (exitCode != 0) {
                    completion.fail(errf.stringToOperationError("Download image failed"));
                    return;
                }
                rsp.format = fileFormat;
                String fileSize = ShellUtils.runAndReturn(String.format("volume_info -p %s -v Image-%s | grep 'volume size' " +
                        "| awk '{print $3}'", getSelf().getPoolName(), cmd.imageUuid),true).getStdout();
                String unit = ShellUtils.runAndReturn(String.format("volume_info -p %s -v Image-%s | grep 'volume size' " +
                        "| awk '{print $4}'", getSelf().getPoolName(), cmd.imageUuid),true).getStdout();
                rsp.size = Math.round(Double.valueOf(fileSize.trim()) * unitConvert(unit.trim()));
                rsp.availableCapacity = getPoolAvailableSize(getSelf().getPoolName());
                rsp.totalCapacity = getPoolTotalSize(getSelf().getPoolName());
            } else {
                throw new OperationFailureException(errf.stringToOperationError(String.format("unknow image format[%s]", fileFormat)));
            }
            completion.success(rsp);
            updateCapacityIfNeeded(rsp);

            //delete the temp image
            if(cmd.url.startsWith("http://") || cmd.url.startsWith("https://")){
                ShellUtils.run(String.format("rm -rf %s",tmpImagePath));
            }
        }

    }

    @Override
    protected void handle(DownloadImageMsg msg) {
        //List<ErrorCode> errorCodes = new ArrayList<ErrorCode>();
        final DownloadCmd cmd = new DownloadCmd();
        final DownloadRsp rsp = new DownloadRsp();
        cmd.url = msg.getImageInventory().getUrl();
        cmd.installPath = makeImageInstallPath(msg.getImageInventory().getUuid());
        cmd.imageUuid = msg.getImageInventory().getUuid();
        cmd.inject = msg.isInject();
        final DownloadImageReply reply = new DownloadImageReply();
        handleDownload(cmd, rsp, msg, new ReturnValueCompletion<DownloadRsp>(msg) {
            @Override
            public void fail(ErrorCode err) {
                reply.setError(err);
                bus.reply(msg, reply);
            }

            @Override
            public void success(DownloadRsp ret) {
                reply.setInstallPath(cmd.installPath);
                reply.setSize(ret.size);

                // current boss has no way to get the actual size
                // if we cannot get the actual size from HTTP, use the virtual size
                long asize = ret.actualSize == null ? ret.size : ret.actualSize;
                reply.setActualSize(asize);
                reply.setMd5sum("not calculated");
                if (msg.getFormat().equals("iso") && ret.format.equals("raw")) {
                    reply.setFormat("iso");
                } else {
                    reply.setFormat(ret.format);
                }
                bus.reply(msg, reply);
            }
        });

    }

    @Override
    protected void handle(GetImageSizeOnBackupStorageMsg msg) {
        GetImageSizeCmd cmd = new GetImageSizeCmd();
        cmd.imageUuid = msg.getImageUuid();
        cmd.installPath = msg.getImageUrl();
        GetImageSizeRsp rsp = new GetImageSizeRsp();

        final GetImageSizeOnBackupStorageReply reply = new GetImageSizeOnBackupStorageReply();

        ReturnValueCompletion<GetImageSizeRsp> completion = new ReturnValueCompletion<GetImageSizeRsp>(msg) {
            @Override
            public void fail(ErrorCode err) {
                reply.setError(err);
                bus.reply(msg, reply);
            }

            @Override
            public void success(GetImageSizeRsp ret) {
                reply.setSize(ret.size);
                bus.reply(msg, reply);
            }
        };
        try{
            String fileSize = ShellUtils.runAndReturn(String.format("volume_info -p %s -v Image-%s | grep 'volume size' " +
                    "| awk '{print $3}'", getSelf().getPoolName(), cmd.imageUuid),true).getStdout();
            String unit = ShellUtils.runAndReturn(String.format("volume_info -p %s -v Image-%s | grep 'volume size' " +
                    "| awk '{print $4}'", getSelf().getPoolName(), cmd.imageUuid),true).getStdout();
            rsp.size = Math.round(Double.valueOf(fileSize.trim()) * unitConvert(unit.trim()));
            completion.success(rsp);
        } catch (Exception e){
            completion.fail(errf.stringToOperationError(String.format("get the size if image[%s] failed", cmd.imageUuid)));
        }
    }

    @Override
    protected void handle(DownloadVolumeMsg msg) {
        final DownloadCmd cmd = new DownloadCmd();
        final DownloadRsp rsp = new DownloadRsp();
        cmd.url = msg.getUrl();
        cmd.installPath = makeImageInstallPath(msg.getVolume().getUuid());
        cmd.imageUuid = msg.getVolume().getUuid();
        final DownloadVolumeReply reply = new DownloadVolumeReply();
        handleDownload(cmd, rsp, msg, new ReturnValueCompletion<DownloadRsp>(msg) {
            @Override
            public void fail(ErrorCode err) {
                reply.setError(err);
                bus.reply(msg, reply);
            }

            @Override
            public void success(DownloadRsp ret) {
                reply.setInstallPath(cmd.installPath);
                reply.setSize(ret.size);
                reply.setMd5sum("not calculated");
                bus.reply(msg, reply);
            }
        });

    }

    @Transactional(readOnly = true)
    private boolean canDelete(String installPath) {
        String sql = "select count(c) from ImageBackupStorageRefVO img, ImageCacheVO c where img.imageUuid = c.imageUuid and img.backupStorageUuid = :bsUuid and img.installPath = :installPath";
        TypedQuery<Long> q = dbf.getEntityManager().createQuery(sql, Long.class);
        q.setParameter("bsUuid", self.getUuid());
        q.setParameter("installPath", installPath);
        return q.getSingleResult() == 0;
    }

    @Override
    protected void handle(DeleteBitsOnBackupStorageMsg msg) {
        final DeleteBitsOnBackupStorageReply reply = new DeleteBitsOnBackupStorageReply();
        if (!canDelete(msg.getInstallPath())) {
            //TODO: the image is still referred, need to cleanup
            bus.reply(msg, reply);
            return;
        }
        DeleteCmd cmd = new DeleteCmd();
        String imageName = msg.getInstallPath().replace(String.format("%s://%s/",getSelf().getClusterName(),getSelf().getPoolName()),"");
        DeleteRsp rsp = new DeleteRsp();

        ReturnValueCompletion<DeleteRsp> completion = new ReturnValueCompletion<DeleteRsp>(msg) {
            @Override
            public void fail(ErrorCode err) {
                //TODO
                reply.setError(err);
                bus.reply(msg, reply);
            }

            @Override
            public void success(DeleteRsp ret) {
                bus.reply(msg, reply);
            }
        };
        ShellResult shellResult = ShellUtils.runAndReturn(String.format("yes | volume_delete -p %s -v %s",getSelf().getPoolName(),imageName));
        if(shellResult.getRetCode() == 0){
            rsp.availableCapacity = getPoolAvailableSize(getSelf().getPoolName());
            rsp.totalCapacity = getPoolTotalSize(getSelf().getPoolName());
            completion.success(rsp);
            updateCapacityIfNeeded(rsp);
        }else{
            completion.fail(errf.stringToOperationError(String.format("Delete image[%s] failed",imageName)));
        }




    }

    @Override
    protected void handle(BackupStorageAskInstallPathMsg msg) {
        BackupStorageAskInstallPathReply reply = new BackupStorageAskInstallPathReply();
        reply.setInstallPath(makeImageInstallPath(msg.getImageUuid()));
        bus.reply(msg, reply);
    }

    @Override
    protected void handle(SyncImageSizeOnBackupStorageMsg msg) {
        GetImageSizeCmd cmd = new GetImageSizeCmd();
        cmd.imageUuid = msg.getImage().getUuid();
        GetImageSizeRsp rsp = new GetImageSizeRsp();

        ImageBackupStorageRefInventory ref = CollectionUtils.find(msg.getImage().getBackupStorageRefs(), new Function<ImageBackupStorageRefInventory, ImageBackupStorageRefInventory>() {
            @Override
            public ImageBackupStorageRefInventory call(ImageBackupStorageRefInventory arg) {
                return self.getUuid().equals(arg.getBackupStorageUuid()) ? arg : null;
            }
        });

        if (ref == null) {
            throw new CloudRuntimeException(String.format("cannot find ImageBackupStorageRefInventory of image[uuid:%s] for" +
                    " the backup storage[uuid:%s]", msg.getImage().getUuid(), self.getUuid()));
        }

        final SyncImageSizeOnBackupStorageReply reply = new SyncImageSizeOnBackupStorageReply();
        cmd.installPath = ref.getInstallPath();

        ReturnValueCompletion<GetImageSizeRsp> completion = new ReturnValueCompletion<GetImageSizeRsp>(msg) {
            @Override
            public void success(GetImageSizeRsp rsp) {
                reply.setSize(rsp.size);

                // current ceph cannot get actual size
                long asize = rsp.actualSize == null ? msg.getImage().getActualSize() : rsp.actualSize;
                reply.setActualSize(asize);
                bus.reply(msg, reply);
            }

            @Override
            public void fail(ErrorCode errorCode) {
                reply.setError(errorCode);
                bus.reply(msg, reply);
            }
        };
        try{
            String fileSize = ShellUtils.runAndReturn(String.format("volume_info -p %s -v Image-%s | grep 'volume size' " +
                    "| awk '{print $3}'", getSelf().getPoolName(), cmd.imageUuid),true).getStdout();
            String unit = ShellUtils.runAndReturn(String.format("volume_info -p %s -v Image-%s | grep 'volume size' " +
                    "| awk '{print $4}'", getSelf().getPoolName(), cmd.imageUuid),true).getStdout();
            rsp.size = Math.round(Double.valueOf(fileSize.trim()) * unitConvert(unit.trim()));
            completion.success(rsp);
        } catch (Exception e){
            completion.fail(errf.stringToOperationError(String.format("get the size if image[%s] failed", cmd.imageUuid)));
        }
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
        String totalSize = ShellUtils.runAndReturn(String.format("pool_list -l | grep %s | awk '{print $3}'" , poolName)).getStdout();
        String unit = ShellUtils.runAndReturn(String.format("pool_list -l | grep %s | awk '{print $4}'" , poolName)).getStdout();
        if(totalSize != "") {
            return Math.round(Double.valueOf(totalSize.trim()) * unitConvert(unit.trim()));
        }else{
            return 0L;
        }
    }

    protected Long getPoolAvailableSize(String poolName){
        String totalSize = ShellUtils.runAndReturn(String.format("pool_list -l | grep %s | awk '{print $5}'" , poolName)).getStdout();
        String unit = ShellUtils.runAndReturn(String.format("pool_list -l | grep %s | awk '{print $6}'" , poolName)).getStdout();
        if(totalSize != "") {
            return Math.round(Double.valueOf(totalSize.trim()) * unitConvert(unit.trim()));
        }else{
            return 0L;
        }
    }


    @Override
    protected void connectHook(boolean newAdd, Completion completion) {
        InitCmd cmd = new InitCmd();
        InitRsp rsp = new InitRsp();
        Pool p = new Pool();
        p.name = getSelf().getPoolName();
        p.predefined = BossSystemTags.PREDEFINED_BACKUP_STORAGE_POOL.hasTag(self.getUuid());

        rsp.totalCapacity = getPoolTotalSize(p.name);
        rsp.availableCapacity = getPoolAvailableSize(p.name);

        rsp.setClusterName(getSelf().getClusterName());
        BossCapacityUpdater updater = new BossCapacityUpdater();
        updater.update(rsp.clusterName, rsp.totalCapacity, rsp.availableCapacity, true);
        completion.success();


    }

    @Override
    protected void pingHook(Completion completion) {
        completion.success();

    }
}
