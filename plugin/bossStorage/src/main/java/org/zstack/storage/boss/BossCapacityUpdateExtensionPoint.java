package org.zstack.storage.boss;

/**
 * Created by XXPS-PC1 on 2016/10/31.
 */
public interface BossCapacityUpdateExtensionPoint {
    public void update(String clusterName, long total, long avail);
}
