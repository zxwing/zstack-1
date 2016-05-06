package org.zstack.header.storage.backup;

import org.zstack.header.message.MessageReply;

/**
 * Created by xing5 on 2016/5/6.
 */
public class SyncImageActualSizeOnBackupStorageReply extends MessageReply {
    private long actualSize;

    public long getActualSize() {
        return actualSize;
    }

    public void setActualSize(long actualSize) {
        this.actualSize = actualSize;
    }
}
