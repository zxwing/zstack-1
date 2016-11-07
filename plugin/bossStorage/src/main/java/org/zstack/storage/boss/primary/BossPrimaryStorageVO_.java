package org.zstack.storage.boss.primary;

import org.zstack.header.storage.primary.PrimaryStorageVO_;

import javax.persistence.metamodel.SingularAttribute;
import javax.persistence.metamodel.StaticMetamodel;

/**
 * Created by XXPS-PC1 on 2016/11/7.
 */
@StaticMetamodel(BossPrimaryStorageVO.class)
public class BossPrimaryStorageVO_ extends PrimaryStorageVO_ {
    public static volatile SingularAttribute<BossPrimaryStorageVO, String> clusterName;
    public static volatile SingularAttribute<BossPrimaryStorageVO, String> rootVolumePoolName;
    public static volatile SingularAttribute<BossPrimaryStorageVO, String> dataVolumePoolName;
    public static volatile SingularAttribute<BossPrimaryStorageVO, String> imageCachePoolName;
}
