package org.zstack.storage.boss.primary;

import javax.persistence.Column;
import javax.persistence.Id;
import javax.persistence.PreUpdate;
import java.sql.Timestamp;

/**
 * Created by XXPS-PC1 on 2016/10/31.
 */

public class BossCapacityVO {
    @Id
    @Column
    private String clusterName;

    @Column
    private long totalCapacity;

    @Column
    private long availableCapacity;

    @Column
    private Timestamp createDate;

    @Column
    private Timestamp lastOpDate;

    @PreUpdate
    private void preUpdate() {
        lastOpDate = null;
    }

    public String getClusterName() {return clusterName;}

    public void setClusterName(String clusterName) {
        this.clusterName = clusterName;
    }

    public long getTotalCapacity() {
        return totalCapacity;
    }

    public void setTotalCapacity(long totalCapacity) {
        this.totalCapacity = totalCapacity;
    }

    public long getAvailableCapacity() {
        return availableCapacity;
    }

    public void setAvailableCapacity(long availableCapacity) {
        this.availableCapacity = availableCapacity;
    }

    public Timestamp getCreateDate() {
        return createDate;
    }

    public void setCreateDate(Timestamp createDate) {
        this.createDate = createDate;
    }

    public Timestamp getLastOpDate() {
        return lastOpDate;
    }

    public void setLastOpDate(Timestamp lastOpDate) {
        this.lastOpDate = lastOpDate;
    }
}
