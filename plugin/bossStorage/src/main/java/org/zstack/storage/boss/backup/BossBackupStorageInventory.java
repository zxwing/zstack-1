package org.zstack.storage.boss.backup;

import org.zstack.header.storage.backup.BackupStorageInventory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Created by XXPS-PC1 on 2016/11/9.
 */
public class BossBackupStorageInventory extends BackupStorageInventory{

    private String clusterName;
    private String poolName;

    public BossBackupStorageInventory(BossBackupStorageVO vo) {
        super(vo);
        clusterName = vo.getClusterName();
        poolName = vo.getPoolName();
    }

    public BossBackupStorageInventory() {
    }

    public String getPoolName() {
        return poolName;
    }

    public void setPoolName(String poolName) {
        this.poolName = poolName;
    }

    public String getClusterName() {
        return clusterName;
    }

    public void setClusterName(String clusterName) {
        this.clusterName = clusterName;
    }

    public static BossBackupStorageInventory valueOf(BossBackupStorageVO vo) {
        return new BossBackupStorageInventory(vo);
    }

    public static List<BossBackupStorageInventory> valueOf1(Collection<BossBackupStorageVO> vos) {
        List<BossBackupStorageInventory> invs = new ArrayList<BossBackupStorageInventory>();
        for (BossBackupStorageVO vo : vos) {
            invs.add(new BossBackupStorageInventory(vo));
        }

        return invs;
    }
}
