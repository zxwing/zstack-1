package org.zstack.storage.boss.primary;

import org.zstack.header.storage.primary.PrimaryStorageEO;
import org.zstack.header.storage.primary.PrimaryStorageVO;
import org.zstack.header.tag.AutoDeleteTag;
import org.zstack.header.vo.EO;
import org.zstack.header.vo.NoView;

import javax.persistence.*;

/**
 * Created by XXPS-PC1 on 2016/10/28.
 */

@Entity
@Table
@PrimaryKeyJoinColumn(name="uuid", referencedColumnName="uuid")
@EO(EOClazz = PrimaryStorageEO.class, needView = false)
@AutoDeleteTag
public class BossPrimaryStorageVO extends PrimaryStorageVO {
    @Column
    private String clusterName;

    @Column
    private String rootVolumePoolName;
    @Column
    private String dataVolumePoolName;
    @Column
    private String imageCachePoolName;

    public String getRootVolumePoolName() {
        return rootVolumePoolName;
    }

    public void setRootVolumePoolName(String rootVolumePoolName) {
        this.rootVolumePoolName = rootVolumePoolName;
    }

    public String getDataVolumePoolName() {
        return dataVolumePoolName;
    }

    public void setDataVolumePoolName(String dataVolumePoolName) {
        this.dataVolumePoolName = dataVolumePoolName;
    }

    public String getImageCachePoolName() {
        return imageCachePoolName;
    }

    public void setImageCachePoolName(String imageCachePoolName) {
        this.imageCachePoolName = imageCachePoolName;
    }

    public String getClusterName() { return clusterName; }

    public void setClusterName(String clusterName) {this.clusterName = clusterName; }

    public BossPrimaryStorageVO() {}

    public BossPrimaryStorageVO(PrimaryStorageVO other) { super(other); }

    public BossPrimaryStorageVO(BossPrimaryStorageVO other){
        super(other);
        this.clusterName = other.clusterName;
    }

}
