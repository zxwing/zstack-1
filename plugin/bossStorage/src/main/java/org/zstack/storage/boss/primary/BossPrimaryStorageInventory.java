package org.zstack.storage.boss.primary;

import org.zstack.header.search.Inventory;
import org.zstack.header.search.Parent;
import org.zstack.header.storage.primary.PrimaryStorageInventory;
import org.zstack.storage.boss.BossConstants;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Created by XXPS-PC1 on 2016/10/31.
 */

@Inventory(mappingVOClass = BossPrimaryStorageVO.class, collectionValueOfMethod = "valueOf1",
        parent = {@Parent(inventoryClass = PrimaryStorageInventory.class, type = BossConstants.BOSS_PRIMARY_STORAGE_TYPE)})
public class BossPrimaryStorageInventory extends PrimaryStorageInventory {
    private String clusterName;
    private String rootVolumePoolName;
    private String dataVolumePoolName;
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

    public BossPrimaryStorageInventory() {
    }

    public BossPrimaryStorageInventory(BossPrimaryStorageVO vo) {
        super(vo);
        setClusterName(vo.getClusterName());
        rootVolumePoolName = vo.getRootVolumePoolName();
        dataVolumePoolName = vo.getDataVolumePoolName();
        imageCachePoolName = vo.getImageCachePoolName();
    }

    public static BossPrimaryStorageInventory valueOf(BossPrimaryStorageVO vo) {
        return new BossPrimaryStorageInventory(vo);
    }

    public static List<BossPrimaryStorageInventory> valueOf1(Collection<BossPrimaryStorageVO> vos) {
        List<BossPrimaryStorageInventory> invs = new ArrayList<BossPrimaryStorageInventory>();
        for (BossPrimaryStorageVO vo : vos) {
            invs.add(valueOf(vo));
        }

        return invs;
    }

    public String getClusterName() {
        return clusterName;
    }

    public void setClusterName(String clusterName) {
        this.clusterName = clusterName;
    }


}
