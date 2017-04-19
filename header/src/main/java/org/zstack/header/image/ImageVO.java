package org.zstack.header.image;

import org.zstack.header.tag.AutoDeleteTag;
import org.zstack.header.vo.EO;
import org.zstack.header.vo.NoView;
import org.zstack.header.vo.Resource;

import javax.persistence.*;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table
@Inheritance(strategy = InheritanceType.JOINED)
@EO(EOClazz = ImageEO.class)
@AutoDeleteTag
public class ImageVO extends ImageAO implements Resource {
    @OneToMany(fetch = FetchType.EAGER)
    @JoinColumn(name = "imageUuid", insertable = false, updatable = false)
    @NoView
    private Set<ImageBackupStorageRefVO> backupStorageRefs = new HashSet<ImageBackupStorageRefVO>();

    public Set<ImageBackupStorageRefVO> getBackupStorageRefs() {
        return backupStorageRefs;
    }

    public void setBackupStorageRefs(Set<ImageBackupStorageRefVO> backupStorageRefs) {
        this.backupStorageRefs = backupStorageRefs;
    }
}
