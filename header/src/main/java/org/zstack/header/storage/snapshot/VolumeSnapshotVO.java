package org.zstack.header.storage.snapshot;

import org.zstack.header.vo.EO;
import org.zstack.header.vo.NoView;
import org.zstack.header.vo.Resource;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.List;

/**
 */
@Entity
@Table
@Inheritance(strategy = InheritanceType.JOINED)
@EO(EOClazz = VolumeSnapshotEO.class)
public class VolumeSnapshotVO extends VolumeSnapshotAO implements Resource {
    @OneToMany(fetch = FetchType.EAGER)
    @JoinColumn(name = "volumeSnapshotUuid", insertable = false, updatable = false)
    @NoView
    private List<VolumeSnapshotBackupStorageRefVO> backupStorageRefs = new ArrayList<VolumeSnapshotBackupStorageRefVO>();

    public List<VolumeSnapshotBackupStorageRefVO> getBackupStorageRefs() {
        return backupStorageRefs;
    }

    public void setBackupStorageRefs(List<VolumeSnapshotBackupStorageRefVO> backupStorageRefs) {
        this.backupStorageRefs = backupStorageRefs;
    }
}
