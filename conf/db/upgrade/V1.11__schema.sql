CREATE TABLE  `zstack`.`TaskProgressVO` (
    `id` bigint unsigned NOT NULL UNIQUE AUTO_INCREMENT,
    `apiId` varchar(32) NOT NULL,
    `taskUuid` varchar(32) NOT NULL,
    `parentUuid` varchar(32) DEFAULT NULL,
    `taskName` varchar(1024) NOT NULL,
    `managementUuid` varchar(32) DEFAULT NULL,
    `type` varchar(255) NOT NULL,
    `content` text DEFAULT NULL,
    `arguments` text DEFAULT NULL,
    `opaque` text DEFAULT NULL,
    `time` bigint unsigned NOT NULL,
    PRIMARY KEY  (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

# Foreign keys for table TaskProgressVO

ALTER TABLE TaskProgressVO ADD CONSTRAINT fkTaskProgressVOManagementNodeVO FOREIGN KEY (managementUuid) REFERENCES ManagementNodeVO (uuid) ON DELETE SET NULL;

CREATE TABLE  `zstack`.`TaskStepVO` (
    `id` bigint unsigned NOT NULL UNIQUE AUTO_INCREMENT,
    `taskName` varchar(1024) NOT NULL,
    `content` text DEFAULT NULL,
    `lastOpDate` timestamp ON UPDATE CURRENT_TIMESTAMP,
    `createDate` timestamp,
    PRIMARY KEY  (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

DROP TABLE ProgressVO;
