package org.zstack.header.storage.primary;

import org.zstack.header.message.MessageReply;

import java.util.List;

/**
 * Created by xing5 on 2016/4/29.
 */
public class CreateTemplateFromVolumeSnapshotAndUploadToBackupStorageReply extends MessageReply {
    public static class Result {
        public String backupStorageUuid;
        public String installPath;
    }

    private long size;
    private long actualSize;
    private List<Result> results;

    public long getSize() {
        return size;
    }

    public void setSize(long size) {
        this.size = size;
    }

    public long getActualSize() {
        return actualSize;
    }

    public void setActualSize(long actualSize) {
        this.actualSize = actualSize;
    }

    public List<Result> getResults() {
        return results;
    }

    public void setResults(List<Result> results) {
        this.results = results;
    }
}
