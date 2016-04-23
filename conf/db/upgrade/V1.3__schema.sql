ALTER TABLE ImageEO ADD actualSize bigint unsigned DEFAULT NULL;
DROP VIEW IF EXISTS `zstack`.`ImageVO`;
CREATE VIEW `zstack`.`ImageVO` AS SELECT uuid, name, description, status, state, size, actualSize, md5Sum, platform, type, format, url, system, mediaType, createDate, lastOpDate, guestOsType FROM `zstack`.`ImageEO` WHERE deleted IS NULL;
UPDATE ImageVO set actualSize = size;
