package org.zstack.storage.boss.primary;

import org.zstack.header.query.APIQueryMessage;
import org.zstack.header.query.AutoQuery;
import org.zstack.header.storage.primary.APIQueryPrimaryStorageReply;

/**
 * Created by xxp on 2016/11/19.
 */
@AutoQuery(replyClass = APIQueryPrimaryStorageReply.class, inventoryClass = BossPrimaryStorageInventory.class)
public class APIQueryBossPrimaryStorageMsg extends APIQueryMessage {
}
