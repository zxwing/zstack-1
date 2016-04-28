package org.zstack.header.storage.primary;

import org.zstack.header.message.NeedReplyMessage;
import org.zstack.header.storage.backup.BackupStorageInventory;
import org.zstack.header.storage.snapshot.VolumeSnapshotInventory;

import java.util.List;

/**
 */
public class CreateTemplateFromVolumeSnapshotOnPrimaryStorageMsg extends NeedReplyMessage implements PrimaryStorageMessage {
    private String imageUuid;
    private String primaryStorageUuid;
    private List<VolumeSnapshotInventory> snapshots;
    private VolumeSnapshotInventory current;

    public VolumeSnapshotInventory getCurrent() {
        return current;
    }

    public void setCurrent(VolumeSnapshotInventory current) {
        this.current = current;
    }

    public List<VolumeSnapshotInventory> getSnapshots() {
        return snapshots;
    }

    public void setSnapshots(List<VolumeSnapshotInventory> snapshots) {
        this.snapshots = snapshots;
    }

    public String getImageUuid() {
        return imageUuid;
    }

    public void setImageUuid(String imageUuid) {
        this.imageUuid = imageUuid;
    }

    public List<BackupStorageInventory> getBackupStorage() {
        return backupStorage;
    }

    public void setBackupStorage(List<BackupStorageInventory> backupStorage) {
        this.backupStorage = backupStorage;
    }

    public String getPrimaryStorageUuid() {
        return primaryStorageUuid;
    }

    public void setPrimaryStorageUuid(String primaryStorageUuid) {
        this.primaryStorageUuid = primaryStorageUuid;
    }

    public List<SnapshotDownloadInfo> getSnapshotsDownloadInfo() {
        return snapshotsDownloadInfo;
    }

    public void setSnapshotsDownloadInfo(List<SnapshotDownloadInfo> snapshotsDownloadInfo) {
        this.snapshotsDownloadInfo = snapshotsDownloadInfo;
    }

    public boolean isNeedDownload() {
        return needDownload;
    }

    public void setNeedDownload(boolean needDownload) {
        this.needDownload = needDownload;
    }
}
