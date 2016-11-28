package org.zstack.storage.boss.backup;

import org.zstack.header.identity.Action;
import org.zstack.header.query.APIQueryMessage;
import org.zstack.header.query.AutoQuery;
import org.zstack.header.storage.backup.APIQueryBackupStorageReply;
import org.zstack.header.storage.backup.BackupStorageConstant;

/**
 * Created by XXPS-PC1 on 2016/11/28.
 */
@Action(category = BackupStorageConstant.ACTION_CATEGORY, names = {"read"})
@AutoQuery(replyClass = APIQueryBackupStorageReply.class, inventoryClass = BossBackupStorageInventory.class)
public class APIQueryBossBackupStorageMsg extends APIQueryMessage {
}
