package org.zstack.image;

import org.zstack.core.gc.GC;
import org.zstack.core.gc.GCCompletion;
import org.zstack.core.gc.TimeBasedGarbageCollector;

/**
 * Created by xing5 on 2017/3/5.
 */
public class BackupStorageDeleteBitGC extends TimeBasedGarbageCollector {
    @GC
    public String backupStorageUuid;
    @GC
    public String installPath;
    @GC
    public String imageUuid;

    @Override
    protected void triggerNow(GCCompletion completion) {

    }
}
