CREATE TABLE `MonitorTriggerVO` (
  `uuid` varchar(32) NOT NULL UNIQUE,
  `name` varchar(255) NOT NULL,
  `expression` varchar(2048) NOT NULL,
  `recoveryExpression` varchar(2048) DEFAULT NULL,
  `description` varchar(2048) DEFAULT NULL,
  `duration` int unsigned NOT NULL,
  `status` varchar(64) NOT NULL,
  `state` varchar(64) NOT NULL,
  `targetResourceUuid` varchar(32) NOT NULL,
  `lastStatusChangeTime` timestamp DEFAULT '0000-00-00 00:00:00',
  `lastOpDate` timestamp ON UPDATE CURRENT_TIMESTAMP,
  `createDate` timestamp,
  PRIMARY KEY  (`uuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `MonitorTriggerActionVO` (
  `uuid` varchar(32) NOT NULL UNIQUE,
  `name` varchar(255) NOT NULL,
  `description` varchar(2048) DEFAULT NULL,
  `type` varchar(64) NOT NULL,
  `state` varchar(64) NOT NULL,
  `postScript` text DEFAULT NULL,
  `lastOpDate` timestamp ON UPDATE CURRENT_TIMESTAMP,
  `createDate` timestamp,
  PRIMARY KEY  (`uuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `MediaVO` (
  `uuid` varchar(32) NOT NULL UNIQUE,
  `name` varchar(255) NOT NULL,
  `description` varchar(2048) DEFAULT NULL,
  `type` varchar(64) NOT NULL,
  `state` varchar(64) NOT NULL,
  `lastOpDate` timestamp ON UPDATE CURRENT_TIMESTAMP,
  `createDate` timestamp,
  PRIMARY KEY  (`uuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `EmailMediaVO` (
  `uuid` varchar(32) NOT NULL UNIQUE,
  `smtpServer` varchar(512) NOT NULL,
  `smtpPort` int unsigned NOT NULL,
  `emailAddress` varchar(512) NOT NULL,
  `username` varchar(512) DEFAULT NULL,
  `password` varchar(512) DEFAULT NULL,
  PRIMARY KEY  (`uuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `MonitorTriggerActionMediaRefVO` (
  `actionUuid` varchar(32) NOT NULL,
  `mediaUuid` varchar(32) NOT NULL,
  `lastOpDate` timestamp ON UPDATE CURRENT_TIMESTAMP,
  `createDate` timestamp,
  PRIMARY KEY  (`actionUuid`, `mediaUuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `MonitorTriggerActionRefVO` (
  `actionUuid` varchar(32) NOT NULL,
  `triggerUuid` varchar(32) NOT NULL,
  `lastOpDate` timestamp ON UPDATE CURRENT_TIMESTAMP,
  `createDate` timestamp,
  PRIMARY KEY  (`actionUuid`, `triggerUuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

DROP TABLE IF EXISTS AlertLabelVO;
DROP TABLE IF EXISTS AlertTimestampVO;
DROP TABLE IF EXISTS AlertVO;
CREATE TABLE `AlertVO` (
  `uuid` varchar(32) NOT NULL UNIQUE,
  `targetResourceUuid` varchar(32) NOT NULL,
  `triggerUuid` varchar(32) NOT NULL,
  `triggerStatus` varchar(64) NOT NULL,
  `content` text DEFAULT NULL,
  `rawData` text DEFAULT NULL,
  `lastOpDate` timestamp ON UPDATE CURRENT_TIMESTAMP,
  `createDate` timestamp,
  PRIMARY KEY  (`uuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

# Foreign keys for table EmailMediaVO

ALTER TABLE EmailMediaVO ADD CONSTRAINT fkEmailMediaVOMediaVO FOREIGN KEY (uuid) REFERENCES MediaVO (uuid) ON UPDATE RESTRICT ON DELETE CASCADE;

# Foreign keys for table MonitorTriggerActionMediaRefVO

ALTER TABLE MonitorTriggerActionMediaRefVO ADD CONSTRAINT fkMonitorTriggerActionMediaRefVOMediaVO FOREIGN KEY (mediaUuid) REFERENCES MediaVO (uuid) ON DELETE CASCADE;
ALTER TABLE MonitorTriggerActionMediaRefVO ADD CONSTRAINT fkMonitorTriggerActionMediaRefVOMonitorTriggerActionVO FOREIGN KEY (actionUuid) REFERENCES MonitorTriggerActionVO (uuid) ON DELETE CASCADE;

# Foreign keys for table MonitorTriggerActionRefVO

ALTER TABLE MonitorTriggerActionRefVO ADD CONSTRAINT fkMonitorTriggerActionRefVOMonitorTriggerActionVO FOREIGN KEY (actionUuid) REFERENCES MonitorTriggerActionVO (uuid) ON DELETE CASCADE;
ALTER TABLE MonitorTriggerActionRefVO ADD CONSTRAINT fkMonitorTriggerActionRefVOMonitorTriggerVO FOREIGN KEY (triggerUuid) REFERENCES MonitorTriggerVO (uuid) ON DELETE CASCADE;

# Foreign keys for table MonitorTriggerVO

ALTER TABLE MonitorTriggerVO ADD CONSTRAINT fkMonitorTriggerVOResourceVO FOREIGN KEY (targetResourceUuid) REFERENCES ResourceVO (uuid) ON DELETE CASCADE;

