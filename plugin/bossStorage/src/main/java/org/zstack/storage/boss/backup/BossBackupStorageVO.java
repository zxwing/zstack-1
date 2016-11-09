package org.zstack.storage.boss.backup;

import org.zstack.header.storage.backup.BackupStorageEO;
import org.zstack.header.storage.backup.BackupStorageVO;
import org.zstack.header.tag.AutoDeleteTag;
import org.zstack.header.vo.EO;
import org.zstack.header.vo.NoView;

import javax.persistence.*;
import java.util.HashSet;
import java.util.Set;

/**
 * Created by XXPS-PC1 on 2016/11/9.
 */
@Entity
@Table
@PrimaryKeyJoinColumn(name="uuid", referencedColumnName="uuid")
@EO(EOClazz = BackupStorageEO.class, needView = false)
@AutoDeleteTag
public class BossBackupStorageVO extends BackupStorageVO{
    @Column
    private String poolName;

    public String getPoolName() {
        return poolName;
    }

    public void setPoolName(String poolName) {
        this.poolName = poolName;
    }

    @Column
    private String clusterName;

    public String getClusterName() {
        return clusterName;
    }

    public void setClusterName(String clusterName) {
        this.clusterName = clusterName;
    }

    public BossBackupStorageVO() {
    }

    public BossBackupStorageVO(BackupStorageVO vo) {
        super(vo);
    }

    public BossBackupStorageVO(BossBackupStorageVO other) {
        super(other);
    }
}
