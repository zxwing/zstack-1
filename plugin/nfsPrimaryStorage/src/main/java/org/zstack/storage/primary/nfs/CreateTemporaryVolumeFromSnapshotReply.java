package org.zstack.storage.primary.nfs;

import org.zstack.header.message.MessageReply;

/**
 * Created by xing5 on 2016/4/29.
 */
public class CreateTemporaryVolumeFromSnapshotReply extends MessageReply {
    private String installPath;

    public String getInstallPath() {
        return installPath;
    }

    public void setInstallPath(String installPath) {
        this.installPath = installPath;
    }
}
