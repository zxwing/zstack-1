package org.zstack.storage.boss.backup;

import jdk.nashorn.tools.Shell;
import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Configurable;
import org.zstack.header.core.ApiTimeout;
import org.zstack.header.core.Completion;
import org.zstack.header.core.ReturnValueCompletion;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.errorcode.OperationFailureException;
import org.zstack.header.image.APIAddImageMsg;
import org.zstack.header.image.ImageInventory;
import org.zstack.header.message.Message;
import org.zstack.header.storage.backup.*;
import org.zstack.storage.backup.BackupStorageBase;
import org.zstack.storage.boss.BossCapacityUpdater;
import org.zstack.storage.boss.BossSystemTags;
import org.zstack.storage.boss.ExecuteShellCommand;
import org.zstack.storage.boss.primary.BossPrimaryStorageBase;
import org.zstack.utils.Utils;
import org.zstack.utils.gson.JSONObjectUtil;
import org.zstack.utils.logging.CLogger;

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

    protected String makeImageInstallPath(String imageUuid) {
        return String.format("%s://%s/%s", getSelf().getClusterName().trim().toString(), getSelf().getPoolName(), imageUuid);
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

        if (file.isFile()) {
            return true;
        } else {
            return false;
        }
    }

    protected void handleDownload(DownloadCmd cmd, DownloadRsp rsp, Message msg, ReturnValueCompletion<DownloadRsp> completion) {
        List<ErrorCode> errorCodes = new ArrayList<ErrorCode>();
        if (msg instanceof DownloadImageMsg || msg instanceof DownloadVolumeMsg) {
            String tmpImageName = cmd.imageUuid;
            String tmpImagePath = null;

            ExecuteShellCommand esc = new ExecuteShellCommand();

            if (cmd.url.startsWith("http://") || cmd.url.startsWith("https://")) {
                esc.executeCommand(String.format("set -o pipefail; wget --no-check-certificate -q -O %s %s", tmpImageName,
                        cmd.url), errf);
                tmpImagePath = getFilePath(tmpImageName);
                rsp.actualSize = getNetFileSize(cmd.url);
            } else if (cmd.url.startsWith("file://")) {
                String srcPath = getFilePath(cmd.url.replace("file:", ""));

                if (isFile(srcPath)) {
                    tmpImagePath = srcPath;
                    rsp.actualSize = getLocalFileSize(srcPath);
                } else {
                    completion.fail(errf.stringToOperationError(String.format("can not find the file[%s],errors are %s", srcPath, JSONObjectUtil.toJsonString(errorCodes))));
                    throw new OperationFailureException(errf.stringToOperationError(String.format("can not find the file[%s],errors are %s", srcPath, JSONObjectUtil.toJsonString(errorCodes))));
                }
            } else {
                completion.fail(errf.stringToOperationError(String.format("unknow url[%s]", cmd.url)));
                throw new OperationFailureException(errf.stringToOperationError(String.format("unknow url[%s]", cmd.url)));
            }

            String fileFormat = esc.executeCommand(String.format("qemu-img info %s | grep 'file format' " +
                    "| cut -d ':' -f 2", tmpImagePath), errf);

            if (fileFormat.equals("qcow2") || fileFormat.equals("raw")) {
                esc.executeCommand(String.format("qemu-img convert -O raw %s %s", tmpImagePath, cmd.installPath), errf);
                rsp.format = fileFormat;
                String fileSize = esc.executeCommand(String.format("volume_info -p %s -v %s | grep 'volume size' | " +
                        "cut -d ':' -f 2", getSelf().getPoolName(), getSelf().getUuid()), errf);
                rsp.size = Math.round(Double.valueOf(fileSize.trim().split(" ")[0]) * 1024 * 1024);
            } else {
                completion.fail(errf.stringToOperationError(String.format("unknow image format[%s]", fileFormat)));
                throw new OperationFailureException(errf.stringToOperationError(String.format("unknow image format[%s]", fileFormat)));
            }
            completion.success(rsp);
        }

    }

    @Override
    protected void handle(DownloadImageMsg msg) {
        List<ErrorCode> errorCodes = new ArrayList<ErrorCode>();
        final DownloadCmd cmd = new DownloadCmd();
        final DownloadRsp rsp = new DownloadRsp();
        cmd.url = msg.getImageInventory().getUrl();
        cmd.installPath = makeImageInstallPath(msg.getImageInventory().getUuid());
        cmd.imageUuid = msg.getImageInventory().getUuid();
        cmd.inject = msg.isInject();
        final DownloadImageReply reply = new DownloadImageReply();
        ReturnValueCompletion<DownloadRsp> completion = new ReturnValueCompletion<DownloadRsp>(msg) {
            @Override
            public void fail(ErrorCode err) {
                reply.setError(err);
                bus.reply(msg, reply);
            }

            @Override
            public void success(DownloadRsp ret) {
                reply.setInstallPath(cmd.installPath);
                reply.setSize(ret.size);

                // current ceph has no way to get the actual size
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
        };

        String tmpImageName = String.format("tmp-%s", msg.getImageInventory().getUuid());
        String tmpImagePath = null;

        ExecuteShellCommand esc = new ExecuteShellCommand();

        if (cmd.url.startsWith("http://") || cmd.url.startsWith("https://")) {
            esc.executeCommand(String.format("wget --no-check-certificate -q -O %s %s",tmpImageName,cmd.url.toString()), errf);
            tmpImagePath = getFilePath(tmpImageName);
            rsp.actualSize = getNetFileSize(cmd.url);
        } else if (cmd.url.startsWith("file://")) {
            String srcPath = getFilePath(cmd.url.replace("file:", ""));

            if (isFile(srcPath)) {
                tmpImagePath = srcPath;
                rsp.actualSize = getLocalFileSize(srcPath);
            } else {
                throw new OperationFailureException(errf.stringToOperationError(String.format("can not find the file[%s],errors are %s", srcPath, JSONObjectUtil.toJsonString(errorCodes))));
            }
        } else {
            throw new OperationFailureException(errf.stringToOperationError(String.format("unknow url[%s]", cmd.url)));
        }

        String fileFormat = esc.executeCommand(String.format("qemu-img info %s | grep 'file format' " +
                "| cut -d ':' -f 2", tmpImagePath), errf).trim();

        if (fileFormat.equals("qcow2") || fileFormat.equals("raw")) {
            esc.executeCommand(String.format("qemu-img convert -O raw %s %s", tmpImagePath, cmd.installPath), errf);
            if (esc.getExitValue() == 0) {
                completion.success(rsp);
            } else {
                completion.fail(errf.stringToOperationError(String.format("download image failed,cause[%s]", errorCodes)));
            }
            rsp.format = fileFormat;
            String fileSize = esc.executeCommand(String.format("volume_info -p %s -v %s | grep 'volume size' | " +
                    "cut -d ':' -f 2", getSelf().getPoolName(), getSelf().getUuid()), errf);
            rsp.size = Math.round(Double.valueOf(fileSize.trim().split(" ")[0]) * 1024 * 1024);
        } else {
            throw new OperationFailureException(errf.stringToOperationError(String.format("unknow image format[%s]", fileFormat)));
        }

    }

    @Override
    protected void handle(GetImageSizeOnBackupStorageMsg msg) {
        GetImageSizeCmd cmd = new GetImageSizeCmd();
        cmd.imageUuid = msg.getImageUuid();
        cmd.installPath = msg.getImageUrl();
        GetImageSizeRsp rsp = new GetImageSizeRsp();
        ExecuteShellCommand esc = new ExecuteShellCommand();

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

        String fileSize = esc.executeCommand(String.format("volume_info -p %s -v %s | grep 'volume size' | " +
                "cut -d ':' -f 2", getSelf().getPoolName(), getSelf().getUuid()), errf);
        if (esc.getExitValue() == 0) {
            rsp.size = Math.round(Double.valueOf(fileSize.trim().split(" ")[0]) * 1024 * 1024);
            completion.success(rsp);
        } else {
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

    @Override
    protected void handle(DeleteBitsOnBackupStorageMsg msg) {

    }

    @Override
    protected void handle(BackupStorageAskInstallPathMsg msg) {
        BackupStorageAskInstallPathReply reply = new BackupStorageAskInstallPathReply();
        reply.setInstallPath(makeImageInstallPath(msg.getImageUuid()));
        bus.reply(msg, reply);
    }

    @Override
    protected void handle(SyncImageSizeOnBackupStorageMsg msg) {

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
