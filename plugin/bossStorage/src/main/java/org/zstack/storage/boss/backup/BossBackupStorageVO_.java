package org.zstack.storage.boss.backup;

import org.zstack.header.storage.backup.BackupStorageVO_;

import javax.persistence.metamodel.SingularAttribute;
import javax.persistence.metamodel.StaticMetamodel;


@StaticMetamodel(BossBackupStorageVO.class)
public class BossBackupStorageVO_ extends BackupStorageVO_ {
    public static volatile SingularAttribute<BossBackupStorageVO, String> clusterName;
    public static volatile SingularAttribute<BossBackupStorageVO, String> poolName;
}
