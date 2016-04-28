package org.zstack.header.storage.primary;

import org.zstack.header.message.NeedReplyMessage;
import org.zstack.header.storage.snapshot.VolumeSnapshotInventory;

/**
 * Created by xing5 on 2016/4/29.
 */
public class CreateTemplateFromVolumeSnapshotAndUploadToBackupStorageMsg extends NeedReplyMessage implements PrimaryStorageMessage {
    private VolumeSnapshotInventory snapshot;
    private UploadTemplateBackupStorageSelector backupStorageSelector;
    private String imageUuid;
    private String primaryStorageUuid;

    public VolumeSnapshotInventory getSnapshot() {
        return snapshot;
    }

    public void setSnapshot(VolumeSnapshotInventory snapshot) {
        this.snapshot = snapshot;
    }

    public UploadTemplateBackupStorageSelector getBackupStorageSelector() {
        return backupStorageSelector;
    }

    public void setBackupStorageSelector(UploadTemplateBackupStorageSelector backupStorageSelector) {
        this.backupStorageSelector = backupStorageSelector;
    }

    public String getImageUuid() {
        return imageUuid;
    }

    public void setImageUuid(String imageUuid) {
        this.imageUuid = imageUuid;
    }

    public String getPrimaryStorageUuid() {
        return primaryStorageUuid;
    }

    public void setPrimaryStorageUuid(String primaryStorageUuid) {
        this.primaryStorageUuid = primaryStorageUuid;
    }
}
