package org.zstack.kvm;

import org.zstack.header.message.NeedReplyMessage;
import org.zstack.header.storage.primary.PrimaryStorageMessage;

import java.util.List;

/**
 * Created by xing5 on 2016/10/24.
 */
public class KvmGetQcow2FileInfoPrimaryStorageMsg extends NeedReplyMessage implements PrimaryStorageMessage {
    private String primaryStorageUuid;
    private List<String> volumeUuids;

    @Override
    public String getPrimaryStorageUuid() {
        return primaryStorageUuid;
    }

    public void setPrimaryStorageUuid(String primaryStorageUuid) {
        this.primaryStorageUuid = primaryStorageUuid;
    }

    public List<String> getVolumeUuids() {
        return volumeUuids;
    }

    public void setVolumeUuids(List<String> volumeUuids) {
        this.volumeUuids = volumeUuids;
    }
}
