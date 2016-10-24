package org.zstack.kvm;

import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.storage.primary.PrimaryStorageVO;
import org.zstack.header.volume.VolumeVO;

/**
 * Created by xing5 on 2016/10/24.
 */
public class APIKvmFixVolumeSnapshotChainMsg extends APIMessage {
    @APIParam(resourceType = VolumeVO.class, required = false)
    private String volumeUuid;
    @APIParam(resourceType = PrimaryStorageVO.class, required = false)
    private String primaryStorageUuid;

    public String getVolumeUuid() {
        return volumeUuid;
    }

    public void setVolumeUuid(String volumeUuid) {
        this.volumeUuid = volumeUuid;
    }

    public String getPrimaryStorageUuid() {
        return primaryStorageUuid;
    }

    public void setPrimaryStorageUuid(String primaryStorageUuid) {
        this.primaryStorageUuid = primaryStorageUuid;
    }
}
