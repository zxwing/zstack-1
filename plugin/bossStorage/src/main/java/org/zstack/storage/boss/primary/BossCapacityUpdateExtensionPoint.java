package org.zstack.storage.boss.primary;

/**
 * Created by XXPS-PC1 on 2016/10/31.
 */
public interface BossCapacityUpdateExtensionPoint {
    public void update( String clusterName, long total, long avail);
}
