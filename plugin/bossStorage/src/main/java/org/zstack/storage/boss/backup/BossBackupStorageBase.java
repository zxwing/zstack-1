package org.zstack.storage.boss.backup;

import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Configurable;
import org.zstack.header.core.Completion;
import org.zstack.header.image.ImageInventory;
import org.zstack.header.storage.backup.*;
import org.zstack.storage.backup.BackupStorageBase;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import java.util.List;

/**
 * Created by XXPS-PC1 on 2016/11/9.
 */
@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class BossBackupStorageBase extends BackupStorageBase{
    private static final CLogger logger = Utils.getLogger(BossBackupStorageBase.class);

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

    @Override
    protected void handle(DownloadImageMsg msg) {

    }

    @Override
    protected void handle(DownloadVolumeMsg msg) {

    }

    @Override
    protected void handle(DeleteBitsOnBackupStorageMsg msg) {

    }

    @Override
    protected void handle(BackupStorageAskInstallPathMsg msg) {

    }

    @Override
    protected void handle(GetImageSizeOnBackupStorageMsg msg) {

    }

    @Override
    protected void handle(SyncImageSizeOnBackupStorageMsg msg) {

    }

    @Override
    protected void connectHook(boolean newAdd, Completion completion) {
        completion.success();

    }

    @Override
    protected void pingHook(Completion completion) {
        completion.success();

    }
}
