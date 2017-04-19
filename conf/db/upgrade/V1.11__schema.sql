SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
--  Table structure for `IdentityZoneVO`
-- ----------------------------
CREATE TABLE `IdentityZoneVO` (
	  `uuid` varchar(32) NOT NULL,
	  `zoneId` varchar(32) NOT NULL,
	  `dataCenterUuid` varchar(32) NOT NULL,
	  `type` varchar(32) NOT NULL,
	  `zoneName` varchar(128) NOT NULL,
	  `deleted` varchar(1) DEFAULT NULL,
	  `defaultVSwitchUuid` varchar(32) DEFAULT NULL,
	  `description` varchar(1024) DEFAULT NULL,
	  `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
	  `lastOpDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
	  PRIMARY KEY (`uuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

SET FOREIGN_KEY_CHECKS = 1;

SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
--  Table structure for `EcsVSwitchVO`
-- ----------------------------
CREATE TABLE `EcsVSwitchVO` (
	  `uuid` varchar(32) NOT NULL,
	  `vSwitchId` varchar(32) NOT NULL,
	  `status` varchar(32) NOT NULL,
	  `cidrBlock` varchar(32) NOT NULL,
	  `availableIpAddressCount` int(10) NOT NULL,
	  `description` varchar(1024) DEFAULT NULL,
	  `vSwitchName` varchar(128) NOT NULL,
	  `ecsVpcUuid` varchar(32) NOT NULL,
	  `identityZoneUuid` varchar(32) NOT NULL,
	  `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
	  `lastOpDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
	  PRIMARY KEY (`uuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

SET FOREIGN_KEY_CHECKS = 1;

SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
--  Table structure for `EcsVpcVO`
-- ----------------------------
CREATE TABLE `EcsVpcVO` (
	  `uuid` varchar(32) NOT NULL,
	  `ecsVpcId` varchar(32) NOT NULL,
	  `dataCenterUuid` varchar(32) NOT NULL,
	  `status` varchar(32) NOT NULL,
	  `deleted` varchar(1) DEFAULT NULL,
	  `vpcName` varchar(128) NOT NULL,
	  `cidrBlock` varchar(32) NOT NULL,
	  `vRouterId` varchar(32) NOT NULL,
	  `description` varchar(1024) DEFAULT NULL,
	  `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
	  `lastOpDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
	  PRIMARY KEY (`uuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

SET FOREIGN_KEY_CHECKS = 1;

SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
--  Table structure for `EcsSecurityGroupRuleVO`
-- ----------------------------
CREATE TABLE `EcsSecurityGroupRuleVO` (
	  `uuid` varchar(32) NOT NULL,
	  `ecsSecurityGroupUuid` varchar(32) NOT NULL,
	  `portRange` varchar(32) NOT NULL,
	  `cidrIp` varchar(32) NOT NULL,
	  `protocol` varchar(32) NOT NULL,
	  `nicType` varchar(32) NOT NULL,
	  `policy` varchar(32) NOT NULL,
	  `sourceGroupId` varchar(128) NOT NULL,
	  `direction` varchar(128) NOT NULL,
	  `priority` varchar(128) NOT NULL,
	  `description` varchar(1024) DEFAULT NULL,
	  `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
	  `lastOpDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
	  PRIMARY KEY (`uuid`),
	  CONSTRAINT `fkEcsSecurityGroupRuleVOEcsSecurityGroupVO` FOREIGN KEY (`ecsSecurityGroupUuid`) REFERENCES `zstack`.`EcsSecurityGroupVO` (`uuid`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

SET FOREIGN_KEY_CHECKS = 1;

SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
--  Table structure for `EcsSecurityGroupVO`
-- ----------------------------
CREATE TABLE `EcsSecurityGroupVO` (
	  `uuid` varchar(32) NOT NULL,
	  `ecsVpcUuid` varchar(32) NOT NULL,
	  `securityGroupId` varchar(32) NOT NULL,
	  `securityGroupName` varchar(128) NOT NULL,
	  `description` varchar(1024) DEFAULT NULL,
	  `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
	  `lastOpDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
	  PRIMARY KEY (`uuid`),
	  UNIQUE KEY `ukEcsVpcUuidSecurityGroupId` (`ecsVpcUuid`,`securityGroupId`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

SET FOREIGN_KEY_CHECKS = 1;

SET FOREIGN_KEY_CHECKS = 0;

SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
--  Table structure for `EcsInstanceConsoleProxyVO`
-- ----------------------------
CREATE TABLE `EcsInstanceConsoleProxyVO` (
	  `uuid` varchar(32) NOT NULL,
	  `ecsInstanceUuid` varchar(32) NOT NULL,
	  `vncUrl` varchar(256) DEFAULT NULL,
	  `vncPassword` varchar(32) DEFAULT NULL,
	  `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
	  `lastOpDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
	  PRIMARY KEY (`uuid`),
	  CONSTRAINT `fkEcsInstanceConsoleProxyVOEcsInstanceVO` FOREIGN KEY (`ecsInstanceUuid`) REFERENCES `zstack`.`EcsInstanceVO` (`uuid`) ON DELETE CASCADE,
	  UNIQUE KEY `ecsInstanceUuid` (`ecsInstanceUuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

SET FOREIGN_KEY_CHECKS = 1;

SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
--  Table structure for `EcsInstanceVO`
-- ----------------------------
CREATE TABLE `EcsInstanceVO` (
	  `uuid` varchar(32) NOT NULL,
	  `localVmInstanceUuid` varchar(32) DEFAULT NULL,
	  `ecsInstanceId` varchar(32) NOT NULL,
	  `name` varchar(128) NOT NULL,
	  `ecsStatus` varchar(16) NOT NULL,
	  `ecsInstanceRootPassword` varchar(32) NOT NULL,
	  `cpuCores` int(10) NOT NULL,
	  `memorySize` bigint(20) NOT NULL,
	  `ecsInstanceType` varchar(32) NOT NULL,
	  `ecsBandWidth` bigint(20) NOT NULL,
	  `ecsRootVolumeId` varchar(32) NOT NULL,
	  `ecsRootVolumeCategory` varchar(32) NOT NULL,
	  `ecsRootVolumeSize` bigint(20) NOT NULL,
	  `privateIpAddress` varchar(32) NOT NULL,
	  `ecsEipUuid` varchar(32) DEFAULT NULL,
	  `ecsVpcUuid` varchar(32) DEFAULT NULL,
	  `ecsVSwitchUuid` varchar(32) DEFAULT NULL,
	  `ecsImageUuid` varchar(32) DEFAULT NULL,
	  `ecsSecurityGroupUuid` varchar(32) DEFAULT NULL,
	  `identityZoneUuid` varchar(32) NOT NULL,
	  `description` varchar(1024) DEFAULT NULL,
	  `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
	  `lastOpDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
	  PRIMARY KEY (`uuid`),
      KEY `fkEcsInstanceVOEcsImageVO` (`ecsImageUuid`),
      KEY `fkEcsInstanceVOEcsSecurityGroupVO` (`ecsSecurityGroupUuid`),
      KEY `fkEcsInstanceVOEcsVSwitchVO` (`ecsVSwitchUuid`),
      KEY `fkEcsInstanceVOEcsVpcVO` (`ecsVpcUuid`),
      KEY `fkEcsInstanceVOIdentityZoneVO` (`identityZoneUuid`),
      KEY `fkEcsInstanceVOVmInstanceEO` (`localVmInstanceUuid`),
      CONSTRAINT `fkEcsInstanceVOEcsImageVO` FOREIGN KEY (`ecsImageUuid`) REFERENCES `EcsImageVO` (`uuid`) ON DELETE SET NULL,
      CONSTRAINT `fkEcsInstanceVOEcsSecurityGroupVO` FOREIGN KEY (`ecsSecurityGroupUuid`) REFERENCES `EcsSecurityGroupVO` (`uuid`) ON DELETE SET NULL,
      CONSTRAINT `fkEcsInstanceVOEcsVpcVO` FOREIGN KEY (`ecsVpcUuid`) REFERENCES `EcsVpcVO` (`uuid`) ON DELETE SET NULL,
      CONSTRAINT `fkEcsInstanceVOEcsVSwitchVO` FOREIGN KEY (`ecsVSwitchUuid`) REFERENCES `EcsVSwitchVO` (`uuid`) ON DELETE SET NULL,
      CONSTRAINT `fkEcsInstanceVOIdentityZoneVO` FOREIGN KEY (`identityZoneUuid`) REFERENCES `IdentityZoneVO` (`uuid`) ON DELETE CASCADE,
      CONSTRAINT `fkEcsInstanceVOVmInstanceEO` FOREIGN KEY (`localVmInstanceUuid`) REFERENCES `VmInstanceEO` (`uuid`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

SET FOREIGN_KEY_CHECKS = 1;

SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
--  Table structure for `EcsImageVO`
-- ----------------------------
CREATE TABLE `EcsImageVO` (
	  `uuid` varchar(32) NOT NULL,
	  `localImageUuid` varchar(32) DEFAULT NULL,
	  `ecsImageId` varchar(32) NOT NULL,
	  `dataCenterUuid` varchar(32) DEFAULT NULL,
	  `name` varchar(128) NOT NULL,
	  `ecsImageSize` bigint(20) NOT NULL,
	  `platform` varchar(32) NOT NULL,
	  `type` varchar(32) NOT NULL,
	  `ossMd5Sum` varchar(32) NOT NULL,
	  `format` varchar(32) NOT NULL,
	  `description` varchar(1024) DEFAULT NULL,
	  `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
	  `lastOpDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
	  PRIMARY KEY (`uuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

SET FOREIGN_KEY_CHECKS = 1;

SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
--  Table structure for `HybridEipAddressVO`
-- ----------------------------
CREATE TABLE `HybridEipAddressVO` (
	  `uuid` varchar(32) NOT NULL,
	  `eipId` varchar(32) NOT NULL,
	  `bandWidth` varchar(32) NOT NULL,
	  `eipAddress` varchar(32) NOT NULL,
	  `allocateResourceUuid` varchar(32) DEFAULT NULL,
	  `allocateResourceType` varchar(32) DEFAULT NULL,
	  `status` varchar(16) NOT NULL,
	  `eipType` varchar(32) NOT NULL,
	  `description` varchar(1024) DEFAULT NULL,
	  `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
	  `lastOpDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
	  PRIMARY KEY (`uuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

SET FOREIGN_KEY_CHECKS = 1;

SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
--  Table structure for `EcsImageMd5SumMappingVO`
-- ----------------------------
CREATE TABLE `EcsImageMd5SumMappingVO` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  `qcow2Md5Sum` varchar(128) NOT NULL,
  `rawMd5Sum` varchar(128) NOT NULL,
  `ossBucketName` varchar(32) NOT NULL,
  `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
  `lastOpDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `rawMd5Sum` (`rawMd5Sum`),
  UNIQUE KEY `ossBucketName` (`ossBucketName`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

SET FOREIGN_KEY_CHECKS = 1;

SET FOREIGN_KEY_CHECKS = 0;
-- ----------------------------
--  Table structure for `OssBucketVO`
-- ----------------------------
CREATE TABLE `OssBucketVO` (
	  `uuid` varchar(32) NOT NULL,
	  `bucketName` varchar(32) NOT NULL,
	  `regionId` varchar(32) NOT NULL,
	  `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
	  `lastOpDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
	  PRIMARY KEY (`uuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

SET FOREIGN_KEY_CHECKS = 1;

SET FOREIGN_KEY_CHECKS = 0;
-- ----------------------------
--  Table structure for `OssBucketEcsDataCenterRefVO`
-- ----------------------------
CREATE TABLE `OssBucketEcsDataCenterRefVO` (
	  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
	  `ossBucketUuid` varchar(32) NOT NULL,
	  `dataCenterUuid` varchar(32) NOT NULL,
	  `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
	  `lastOpDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
	  PRIMARY KEY (`id`),
	  CONSTRAINT `fkOssBucketEcsDataCenterRefVOOssBucketVO` FOREIGN KEY (`ossBucketUuid`) REFERENCES `zstack`.`OssBucketVO` (`uuid`) ON DELETE CASCADE,
      CONSTRAINT `fkOssBucketEcsDataCenterRefVODataCenterVO` FOREIGN KEY (`dataCenterUuid`) REFERENCES `zstack`.`DataCenterVO` (`uuid`) ON DELETE CASCADE,
      UNIQUE KEY `dataCenterUuid` (`dataCenterUuid`),
      UNIQUE KEY `ossBucketUuid` (`ossBucketUuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

SET FOREIGN_KEY_CHECKS = 1;

SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
--  Table structure for `DataCenterVO`
-- ----------------------------
CREATE TABLE `DataCenterVO` (
	  `uuid` varchar(32) NOT NULL,
	  `deleted` varchar(1) DEFAULT NULL,
	  `regionName` varchar(1024) NOT NULL,
	  `dcType` varchar(32) NOT NULL,
	  `defaultVpcUuid` varchar(32) DEFAULT NULL,
	  `regionId` varchar(32) NOT NULL,
	  `description` varchar(1024) DEFAULT NULL,
	  `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
	  `lastOpDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
	  PRIMARY KEY (`uuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

SET FOREIGN_KEY_CHECKS = 1;

SET FOREIGN_KEY_CHECKS = 0;
-- ----------------------------
--  Table structure for `AvailableInstanceTypesVO`
-- ----------------------------
CREATE TABLE `AvailableInstanceTypesVO` (
	  `uuid` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
	  `accountUuid` varchar(32) NOT NULL,
	  `instanceType` varchar(4096) NOT NULL,
	  `diskCategories` varchar(256) NOT NULL,
	  `resourceType` varchar(256) NOT NULL,
	  `izUuid` varchar(32) NOT NULL,
	  `description` varchar(1024) DEFAULT NULL,
	  `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
	  `lastOpDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
	  PRIMARY KEY (`uuid`),
	  UNIQUE KEY `ukAccountUuidizUuid` (`accountUuid`,`izUuid`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

SET FOREIGN_KEY_CHECKS = 1;

SET FOREIGN_KEY_CHECKS = 0;
-- ----------------------------
--  Table structure for `AvailableIdentityZonesVO`
-- ----------------------------
CREATE TABLE `AvailableIdentityZonesVO` (
	  `uuid` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
	  `accountUuid` varchar(32) NOT NULL,
	  `dataCenterUuid` varchar(32) NOT NULL,
	  `type` varchar(32) NOT NULL,
	  `zoneId` varchar(32) NOT NULL,
	  `description` varchar(1024) DEFAULT NULL,
	  `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
	  `lastOpDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
	  PRIMARY KEY (`uuid`),
	  UNIQUE KEY `ukAccountUuidizUuid` (`accountUuid`,`dataCenterUuid`,`zoneId`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
SET FOREIGN_KEY_CHECKS = 1;

SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
--  Table structure for `HybridAccountVO`
-- ----------------------------
CREATE TABLE `HybridAccountVO` (
  `uuid` varchar(32) NOT NULL,
  `accountUuid` varchar(32) NOT NULL,
  `userUuid` varchar(32) DEFAULT NULL,
  `type` varchar(32) NOT NULL,
  `akey` varchar(32) NOT NULL,
  `secret` varchar(64) NOT NULL,
  `description` varchar(1024) DEFAULT NULL,
  `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
  `lastOpDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`uuid`),
  UNIQUE KEY `idxHybridAccountVOKey` (`akey`) USING BTREE,
  KEY `accountUuid` (`accountUuid`),
  KEY `userUuid` (`userUuid`),
  CONSTRAINT `fkHybridAccountVOAccountVO` FOREIGN KEY (`accountUuid`) REFERENCES `zstack`.`AccountVO` (`uuid`) ON DELETE CASCADE,
  CONSTRAINT `fkHybridAccountVOUserVO` FOREIGN KEY (`userUuid`) REFERENCES `zstack`.`UserVO` (`uuid`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

SET FOREIGN_KEY_CHECKS = 1;

CREATE UNIQUE INDEX uniqAccountUuid on  HybridAccountVO(accountUuid);
ALTER TABLE HybridAccountVO drop index accountUuid;

CREATE UNIQUE INDEX uniqUserUuid on  HybridAccountVO(userUuid);
ALTER TABLE HybridAccountVO drop index userUuid;

# Foreign keys for table DataCenterVO

ALTER TABLE DataCenterVO ADD CONSTRAINT fkDataCenterVOEcsVpcVO FOREIGN KEY (defaultVpcUuid) REFERENCES EcsVpcVO (uuid) ON DELETE SET NULL;

# Foreign keys for table EcsImageVO

ALTER TABLE EcsImageVO ADD CONSTRAINT fkEcsImageVOImageEO FOREIGN KEY (localImageUuid) REFERENCES ImageEO (uuid) ON DELETE SET NULL;
ALTER TABLE EcsImageVO ADD CONSTRAINT fkEcsImageVODataCenterVO FOREIGN KEY (dataCenterUuid) REFERENCES DataCenterVO (uuid) ON DELETE SET NULL;

# Foreign keys for table AvailableInstanceTypesVO

ALTER TABLE AvailableInstanceTypesVO ADD CONSTRAINT fkAvailableInstanceTypesVOAccountVO FOREIGN KEY (accountUuid) REFERENCES AccountVO (uuid) ON DELETE CASCADE;
ALTER TABLE AvailableInstanceTypesVO ADD CONSTRAINT fkAvailableInstanceTypesVOIdentityZoneVO FOREIGN KEY (izUuid) REFERENCES IdentityZoneVO (uuid) ON DELETE CASCADE;

# Foreign keys for table AvailableIdentityZonesVO

ALTER TABLE AvailableIdentityZonesVO ADD CONSTRAINT fkAvailableIdentityZonesVOAccountVO FOREIGN KEY (accountUuid) REFERENCES AccountVO (uuid) ON DELETE CASCADE;
ALTER TABLE AvailableIdentityZonesVO ADD CONSTRAINT fkAvailableIdentityZonesVODataCenterVO FOREIGN KEY (dataCenterUuid) REFERENCES DataCenterVO (uuid) ON DELETE CASCADE;

# Foreign keys for table IdentityZoneVO

ALTER TABLE IdentityZoneVO ADD CONSTRAINT fkIdentityZoneVODataCenterVO FOREIGN KEY (dataCenterUuid) REFERENCES DataCenterVO (uuid) ON DELETE CASCADE;
ALTER TABLE IdentityZoneVO ADD CONSTRAINT fkIdentityZoneVOEcsVSwitchVO FOREIGN KEY (defaultVSwitchUuid) REFERENCES EcsVSwitchVO (uuid) ON DELETE SET NULL;

# Foreign keys for table EcsSecurityGroupVO

ALTER TABLE EcsSecurityGroupVO ADD CONSTRAINT fkEcsSecurityGroupVOEcsVpcVO FOREIGN KEY (ecsVpcUuid) REFERENCES EcsVpcVO (uuid) ON DELETE CASCADE;

# Foreign keys for table EcsVpcVO

ALTER TABLE EcsVpcVO ADD CONSTRAINT fkEcsVpcVODataCenterVO FOREIGN KEY (dataCenterUuid) REFERENCES DataCenterVO (uuid) ON DELETE CASCADE;

# Foreign keys for table EcsVSwitchVO

ALTER TABLE EcsVSwitchVO ADD CONSTRAINT fkEcsVSwitchVOEcsVpcVO FOREIGN KEY (ecsVpcUuid) REFERENCES EcsVpcVO (uuid) ON DELETE CASCADE;
ALTER TABLE EcsVSwitchVO ADD CONSTRAINT fkEcsVSwitchVOIdentityZoneVO FOREIGN KEY (identityZoneUuid) REFERENCES IdentityZoneVO (uuid) ON DELETE CASCADE;

# VxlanNetwork
CREATE TABLE `zstack`.`VxlanNetworkPoolVO` (
  `uuid` varchar(32) NOT NULL UNIQUE,
  PRIMARY KEY  (`uuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `zstack`.`VtepVO` (
  `uuid` varchar(32) NOT NULL UNIQUE,
  `hostUuid` varchar(32) NOT NULL,
  `vtepIp` varchar(32) NOT NULL,
  `port` int NOT NULL,
  `clusterUuid` varchar(32) NOT NULL,
  `type` varchar(32) NOT NULL,
  `poolUuid` varchar(32) NOT NULL,
  PRIMARY KEY  (`uuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `zstack`.`VxlanNetworkVO` (
  `uuid` varchar(32) NOT NULL UNIQUE,
  `vni` int NOT NULL,
  `poolUuid` varchar(32),
  PRIMARY KEY  (`uuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `zstack`.`VniRangeVO` (
  `uuid` varchar(32) NOT NULL UNIQUE COMMENT 'uuid',
  `name` varchar(255) DEFAULT NULL COMMENT 'name',
  `description` varchar(2048) DEFAULT NULL COMMENT 'description',
  `l2NetworkUuid` varchar(32) NOT NULL COMMENT 'l3 network uuid',
  `startVni` INT NOT NULL COMMENT 'start vni',
  `endVni` INT NOT NULL COMMENT 'end vni',
  PRIMARY KEY  (`uuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

ALTER TABLE VxlanNetworkVO ADD CONSTRAINT fkVxlanNetworkVOL2NetworkEO FOREIGN KEY (uuid) REFERENCES L2NetworkEO (uuid) ON UPDATE RESTRICT ON DELETE CASCADE;
ALTER TABLE VxlanNetworkVO ADD CONSTRAINT fkVxlanNetworkVOVxlanNetworkPoolVO FOREIGN KEY (poolUuid) REFERENCES VxlanNetworkPoolVO (uuid) ON UPDATE RESTRICT ON DELETE CASCADE;
ALTER TABLE VxlanNetworkPoolVO ADD CONSTRAINT fkVxlanNetworkPoolVOL2NetworkEO FOREIGN KEY (uuid) REFERENCES L2NetworkEO (uuid) ON UPDATE RESTRICT ON DELETE CASCADE;

ALTER TABLE VtepVO ADD CONSTRAINT fkVtepVOHostEO FOREIGN KEY (hostUuid) REFERENCES HostEO (uuid) ON DELETE RESTRICT;
ALTER TABLE VtepVO ADD CONSTRAINT fkVtepVOClusterEO FOREIGN KEY (clusterUuid) REFERENCES ClusterEO (uuid) ON DELETE RESTRICT;

ALTER TABLE VniRangeVO ADD CONSTRAINT fkVniRangeVOL2NetworkEO  FOREIGN KEY (l2NetworkUuid) REFERENCES L2NetworkEO (uuid) ON DELETE CASCADE;

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
ALTER TABLE TaskProgressVO ADD COLUMN createDate timestamp;
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

CREATE TABLE  `zstack`.`NotificationVO` (
    `uuid` varchar(32) NOT NULL UNIQUE,
    `name` varchar(1024) NOT NULL,
    `content` text NOT NULL,
    `arguments` text DEFAULT NULL,
    `sender` varchar(1024) NOT NULL,
    `status` varchar(255) NOT NULL,
    `type` varchar(255) NOT NULL,
    `resourceUuid` varchar(255) DEFAULT NULL,
    `resourceType` varchar(255) DEFAULT NULL,
    `opaque` text DEFAULT NULL,
    `time` bigint unsigned DEFAULT NULL,
    `lastOpDate` timestamp ON UPDATE CURRENT_TIMESTAMP,
    `createDate` timestamp,
    PRIMARY KEY  (`uuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE  `zstack`.`NotificationSubscriptionVO` (
    `uuid` varchar(32) NOT NULL UNIQUE,
    `name` varchar(1024) NOT NULL,
    `description` varchar(2048) DEFAULT NULL,
    `notificationName` varchar(1024) NOT NULL,
    `filter` varchar(2048) DEFAULT NULL,
    `lastOpDate` timestamp ON UPDATE CURRENT_TIMESTAMP,
    `createDate` timestamp,
    PRIMARY KEY  (`uuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;


ALTER TABLE LocalStorageResourceRefVO DROP FOREIGN KEY `fkLocalStorageResourceRefVOHostEO`;

ALTER TABLE VipVO ADD CONSTRAINT fkUsedIpVO FOREIGN KEY (`usedIpUuid`) REFERENCES `UsedIpVO` (`uuid`) ON DELETE CASCADE;

DROP TRIGGER IF EXISTS trigger_clean_AccountResourceRefVO_for_VolumeEO;
DROP TRIGGER IF EXISTS trigger_cleanup_for_VolumeEO_hard_delete;
DROP TRIGGER IF EXISTS trigger_cleanup_for_VmInstanceEO_hard_delete;
DROP TRIGGER IF EXISTS trigger_clean_AccountResourceRefVO_for_DiskOfferingEO;
DROP TRIGGER IF EXISTS trigger_clean_AccountResourceRefVO_for_EipVO;
DROP TRIGGER IF EXISTS trigger_clean_AccountResourceRefVO_for_ImageEO;
DROP TRIGGER IF EXISTS trigger_clean_AccountResourceRefVO_for_InstanceOfferingEO;
DROP TRIGGER IF EXISTS trigger_clean_AccountResourceRefVO_for_IpRangeEO;
DROP TRIGGER IF EXISTS trigger_clean_AccountResourceRefVO_for_L3NetworkEO;
DROP TRIGGER IF EXISTS trigger_clean_AccountResourceRefVO_for_LoadBalancerListenerVO;
DROP TRIGGER IF EXISTS trigger_clean_AccountResourceRefVO_for_LoadBalancerVO;
DROP TRIGGER IF EXISTS trigger_clean_AccountResourceRefVO_for_PolicyVO;
DROP TRIGGER IF EXISTS trigger_clean_AccountResourceRefVO_for_PortForwardingRuleVO;
DROP TRIGGER IF EXISTS trigger_clean_AccountResourceRefVO_for_QuotaVO;
DROP TRIGGER IF EXISTS trigger_clean_AccountResourceRefVO_for_SchedulerVO;
DROP TRIGGER IF EXISTS trigger_clean_AccountResourceRefVO_for_SecurityGroupVO;
DROP TRIGGER IF EXISTS trigger_clean_AccountResourceRefVO_for_UserGroupVO;
DROP TRIGGER IF EXISTS trigger_clean_AccountResourceRefVO_for_UserVO;
DROP TRIGGER IF EXISTS trigger_clean_AccountResourceRefVO_for_VipVO;
DROP TRIGGER IF EXISTS trigger_clean_AccountResourceRefVO_for_VmInstanceEO;
DROP TRIGGER IF EXISTS trigger_clean_AccountResourceRefVO_for_VmNicVO;
DROP TRIGGER IF EXISTS trigger_clean_AccountResourceRefVO_for_VolumeEO;
DROP TRIGGER IF EXISTS trigger_clean_AccountResourceRefVO_for_VolumeSnapshotEO;

CREATE TABLE `ResourceVO` (
    `uuid` varchar(32) NOT NULL,
    `name` varchar(255) DEFAULT NULL,
    `type` varchar(255) DEFAULT NULL,
    `diskOfferingUuid` varchar(32) DEFAULT NULL,
    `eipUuid` varchar(32) DEFAULT NULL,
    `imageUuid` varchar(32) DEFAULT NULL,
    `instanceOfferingUuid` varchar(32) DEFAULT NULL,
    `ipRangeUuid` varchar(32) DEFAULT NULL,
    `l3NetworkUuid` varchar(32) DEFAULT NULL,
    `loadBalancerListenerUuid` varchar(32) DEFAULT NULL,
    `loadBalancerUuid` varchar(32) DEFAULT NULL,
    `policyUuid` varchar(32) DEFAULT NULL,
    `portForwardingRuleUuid` varchar(32) DEFAULT NULL,
    `schedulerUuid` varchar(32) DEFAULT NULL,
    `securityGroupUuid` varchar(32) DEFAULT NULL,
    `userGroupUuid` varchar(32) DEFAULT NULL,
    `userUuid` varchar(32) DEFAULT NULL,
    `vipUuid` varchar(32) DEFAULT NULL,
    `vmInstanceUuid` varchar(32) DEFAULT NULL,
    `vmNicUuid` varchar(32) DEFAULT NULL,
    `volumeSnapshotUuid` varchar(32) DEFAULT NULL,
    `volumeUuid` varchar(32) DEFAULT NULL,
    PRIMARY KEY (`uuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;



ALTER TABLE ResourceVO ADD CONSTRAINT fkResourceVODiskOfferingVO FOREIGN KEY (diskOfferingUuid) REFERENCES DiskOfferingEO (uuid) ON DELETE CASCADE;
ALTER TABLE ResourceVO ADD CONSTRAINT fkResourceVOEipVO FOREIGN KEY (eipUuid) REFERENCES EipVO (uuid) ON DELETE CASCADE;
ALTER TABLE ResourceVO ADD CONSTRAINT fkResourceVOImageVO FOREIGN KEY (imageUuid) REFERENCES ImageEO (uuid) ON DELETE CASCADE;
ALTER TABLE ResourceVO ADD CONSTRAINT fkResourceVOInstanceOfferingVO FOREIGN KEY (instanceOfferingUuid) REFERENCES InstanceOfferingEO (uuid) ON DELETE CASCADE;
ALTER TABLE ResourceVO ADD CONSTRAINT fkResourceVOIpRangeVO FOREIGN KEY (ipRangeUuid) REFERENCES IpRangeEO (uuid) ON DELETE CASCADE;
ALTER TABLE ResourceVO ADD CONSTRAINT fkResourceVOL3NetworkVO FOREIGN KEY (l3NetworkUuid) REFERENCES L3NetworkEO (uuid) ON DELETE CASCADE;
ALTER TABLE ResourceVO ADD CONSTRAINT fkResourceVOLoadBalancerListenerVO FOREIGN KEY (loadBalancerListenerUuid) REFERENCES LoadBalancerListenerVO (uuid) ON DELETE CASCADE;
ALTER TABLE ResourceVO ADD CONSTRAINT fkResourceVOLoadBalancerVO FOREIGN KEY (loadBalancerUuid) REFERENCES LoadBalancerVO (uuid) ON DELETE CASCADE;
ALTER TABLE ResourceVO ADD CONSTRAINT fkResourceVOPolicyVO FOREIGN KEY (policyUuid) REFERENCES PolicyVO (uuid) ON DELETE CASCADE;
ALTER TABLE ResourceVO ADD CONSTRAINT fkResourceVOPortForwardingRuleVO FOREIGN KEY (portForwardingRuleUuid) REFERENCES PortForwardingRuleVO (uuid) ON DELETE CASCADE;
ALTER TABLE ResourceVO ADD CONSTRAINT fkResourceVOSchedulerVO FOREIGN KEY (schedulerUuid) REFERENCES SchedulerVO (uuid) ON DELETE CASCADE;
ALTER TABLE ResourceVO ADD CONSTRAINT fkResourceVOSecurityGroupVO FOREIGN KEY (securityGroupUuid) REFERENCES SecurityGroupVO (uuid) ON DELETE CASCADE;
ALTER TABLE ResourceVO ADD CONSTRAINT fkResourceVOUserGroupVO FOREIGN KEY (userGroupUuid) REFERENCES UserGroupVO (uuid) ON DELETE CASCADE;
ALTER TABLE ResourceVO ADD CONSTRAINT fkResourceVOUserVO FOREIGN KEY (userUuid) REFERENCES UserVO (uuid) ON DELETE CASCADE;
ALTER TABLE ResourceVO ADD CONSTRAINT fkResourceVOVipVO FOREIGN KEY (vipUuid) REFERENCES VipVO (uuid) ON DELETE CASCADE;
ALTER TABLE ResourceVO ADD CONSTRAINT fkResourceVOVmInstanceVO FOREIGN KEY (vmInstanceUuid) REFERENCES VmInstanceEO (uuid) ON DELETE CASCADE;
ALTER TABLE ResourceVO ADD CONSTRAINT fkResourceVOVmNicVO FOREIGN KEY (vmNicUuid) REFERENCES VmNicVO (uuid) ON DELETE CASCADE;
ALTER TABLE ResourceVO ADD CONSTRAINT fkResourceVOVolumeSnapshotVO FOREIGN KEY (volumeSnapshotUuid) REFERENCES VolumeSnapshotEO (uuid) ON DELETE CASCADE;
ALTER TABLE ResourceVO ADD CONSTRAINT fkResourceVOVolumeVO FOREIGN KEY (volumeUuid) REFERENCES VolumeEO (uuid) ON DELETE CASCADE;

INSERT INTO ResourceVO (uuid, name, diskOfferingUuid, type) SELECT t.uuid, t.name, t.uuid, "DiskOfferingVO" FROM DiskOfferingVO t;
INSERT INTO ResourceVO (uuid, name, eipUuid, type) SELECT t.uuid, t.name, t.uuid, "EipVO" FROM EipVO t;
INSERT INTO ResourceVO (uuid, name, imageUuid, type) SELECT t.uuid, t.name, t.uuid, "ImageVO" FROM ImageVO t;
INSERT INTO ResourceVO (uuid, name, instanceOfferingUuid, type) SELECT t.uuid, t.name, t.uuid, "InstanceOfferingVO" FROM InstanceOfferingVO t;
INSERT INTO ResourceVO (uuid, name, ipRangeUuid, type) SELECT t.uuid, t.name, t.uuid, "IpRangeVO" FROM IpRangeVO t;
INSERT INTO ResourceVO (uuid, name, l3NetworkUuid, type) SELECT t.uuid, t.name, t.uuid, "L3NetworkVO" FROM L3NetworkVO t;
INSERT INTO ResourceVO (uuid, name, loadBalancerListenerUuid, type) SELECT t.uuid, t.name, t.uuid, "LoadBalancerListenerVO" FROM LoadBalancerListenerVO t;
INSERT INTO ResourceVO (uuid, name, loadBalancerUuid, type) SELECT t.uuid, t.name, t.uuid, "LoadBalancerVO" FROM LoadBalancerVO t;
INSERT INTO ResourceVO (uuid, name, policyUuid, type) SELECT t.uuid, t.name, t.uuid, "PolicyVO" FROM PolicyVO t;
INSERT INTO ResourceVO (uuid, name, portForwardingRuleUuid, type) SELECT t.uuid, t.name, t.uuid, "PortForwardingRuleVO" FROM PortForwardingRuleVO t;
INSERT INTO ResourceVO (uuid, name, schedulerUuid, type) SELECT t.uuid, t.schedulerName, t.uuid, "SchedulerVO" FROM SchedulerVO t;
INSERT INTO ResourceVO (uuid, name, securityGroupUuid, type) SELECT t.uuid, t.name, t.uuid, "SecurityGroupVO" FROM SecurityGroupVO t;
INSERT INTO ResourceVO (uuid, name, userGroupUuid, type) SELECT t.uuid, t.name, t.uuid, "UserGroupVO" FROM UserGroupVO t;
INSERT INTO ResourceVO (uuid, name, userUuid, type) SELECT t.uuid, t.name, t.uuid, "UserVO" FROM UserVO t;
INSERT INTO ResourceVO (uuid, name, vipUuid, type) SELECT t.uuid, t.name, t.uuid, "VipVO" FROM VipVO t;
INSERT INTO ResourceVO (uuid, name, vmInstanceUuid, type) SELECT t.uuid, t.name, t.uuid, "VmInstanceVO" FROM VmInstanceVO t;
INSERT INTO ResourceVO (uuid, vmNicUuid, type) SELECT t.uuid, t.uuid, "VmNicVO" FROM VmNicVO t;
INSERT INTO ResourceVO (uuid, name, volumeSnapshotUuid, type) SELECT t.uuid, t.name, t.uuid, "VolumeSnapshotVO" FROM VolumeSnapshotVO t;
INSERT INTO ResourceVO (uuid, name, volumeUuid, type) SELECT t.uuid, t.name, t.uuid, "VolumeVO" FROM VolumeVO t;
