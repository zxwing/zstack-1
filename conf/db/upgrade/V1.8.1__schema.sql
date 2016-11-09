CREATE TABLE  `zstack`.`BossBackupStorageVO` (
  `uuid` varchar(32) NOT NULL UNIQUE,
  `clusterName` varchar(64) NOT NULL,
  `poolName` varchar(255) NOT NULL,
  PRIMARY KEY  (`uuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE  `zstack`.`BossPrimaryStorageVO` (
  `uuid` varchar(32) NOT NULL UNIQUE,
  `clusterName` varchar(64) NOT NULL,
  `rootVolumePoolName` varchar(255) NOT NULL,
  `dataVolumePoolName` varchar(255) NOT NULL,
  `imageCachePoolName` varchar(255) NOT NULL,
  PRIMARY KEY  (`uuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE  `zstack`.`BossCapacityVO` (
  `clusterName` varchar(64) NOT NULL UNIQUE,
  `totalCapacity` bigint unsigned DEFAULT 0,
  `availableCapacity` bigint unsigned DEFAULT 0,
  `lastOpDate` timestamp ON UPDATE CURRENT_TIMESTAMP,
  `createDate` timestamp,
  PRIMARY KEY  (`clusterName`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

# Foreign keys for table BossBackupStorageVO

ALTER TABLE BossBackupStorageVO ADD CONSTRAINT fkBossBackupStorageVOBackupStorageEO FOREIGN KEY (uuid) REFERENCES BackupStorageEO (uuid) ON UPDATE RESTRICT ON DELETE CASCADE;

# Foreign keys for table BossPrimaryStorageVO

ALTER TABLE BossPrimaryStorageVO ADD CONSTRAINT fkBossPrimaryStorageVOPrimaryStorageEO FOREIGN KEY (uuid) REFERENCES PrimaryStorageEO (uuid) ON UPDATE RESTRICT ON DELETE CASCADE;
