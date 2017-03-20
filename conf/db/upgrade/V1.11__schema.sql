CREATE TABLE  `zstack`.`TaskProgressVO` (
    `id` bigint unsigned NOT NULL UNIQUE AUTO_INCREMENT,
    `apiId` varchar(32) NOT NULL,
    `taskUuid` varchar(32) NOT NULL,
    `parentUuid` varchar(32) DEFAULT NULL,
    `managementUuid` varchar(32) DEFAULT NULL,
    `type` varchar(255) NOT NULL,
    `content` text DEFAULT NULL,
    `arguments` text DEFAULT NULL,
    `opaque` text DEFAULT NULL,
    `time` bigint unsigned NOT NULL,
    PRIMARY KEY  (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

