CREATE TABLE `MonitorTriggerVO` (
  `uuid` varchar(32) NOT NULL UNIQUE,
  `name` varchar(255) NOT NULL,
  `expression` varchar(2048) NOT NULL,
  `recoveryExpression` varchar(2048) DEFAULT NULL,
  `description` varchar(2048) DEFAULT NULL,
  `status` varchar(64) NOT NULL,
  `state` varchar(64) NOT NULL,
  `targetResourceUuid` varchar(32) NOT NULL,
  `lastStatusChangeTime` timestamp DEFAULT '0000-00-00 00:00:00',
  `lastOpDate` timestamp ON UPDATE CURRENT_TIMESTAMP,
  `createDate` timestamp,
  PRIMARY KEY  (`uuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
