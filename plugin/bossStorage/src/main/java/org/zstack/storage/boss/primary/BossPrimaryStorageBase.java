package org.zstack.storage.boss.primary;

import org.zstack.header.core.Completion;
import org.zstack.header.core.ReturnValueCompletion;
import org.zstack.header.message.Message;
import org.zstack.header.storage.primary.*;
import org.zstack.storage.primary.PrimaryStorageBase;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

/**
 * Created by XXPS-PC1 on 2016/10/31.
 */
public class BossPrimaryStorageBase extends PrimaryStorageBase {

    private static final CLogger logger = Utils.getLogger(BossPrimaryStorageBase.class);


    public BossPrimaryStorageBase(BossPrimaryStorageVO self) { super(self);}


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

    }

    @Override
    protected void pingHook(Completion completion) {

    }

    @Override
    protected void syncPhysicalCapacity(ReturnValueCompletion<PhysicalCapacityUsage> completion) {

    }
}
