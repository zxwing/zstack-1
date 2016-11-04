package org.zstack.storage.boss;

import org.zstack.header.storage.backup.BackupStorageVO;
import org.zstack.header.storage.primary.PrimaryStorageVO;
import org.zstack.header.tag.TagDefinition;
import org.zstack.tag.PatternedSystemTag;

/**
 * Created by XXPS-PC1 on 2016/10/27.
 */
@TagDefinition
public class BossSystemTags {
    public static PatternedSystemTag PREDEFINED_BACKUP_STORAGE_POOL = new PatternedSystemTag("boss::predefinedPool", BackupStorageVO.class);
    public static PatternedSystemTag PREDEFINED_PRIMARY_STORAGE_IMAGE_CACHE_POOL = new PatternedSystemTag("boss::predefinedImageCachePool", PrimaryStorageVO.class);
    public static PatternedSystemTag PREDEFINED_PRIMARY_STORAGE_ROOT_VOLUME_POOL = new PatternedSystemTag("boss::predefinedRootVolumePool", PrimaryStorageVO.class);
    public static PatternedSystemTag PREDEFINED_PRIMARY_STORAGE_DATA_VOLUME_POOL = new PatternedSystemTag("boss::predefinedDataVolumePool", PrimaryStorageVO.class);
}
