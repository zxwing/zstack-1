package org.zstack.kvm;

import org.zstack.header.message.MessageReply;
import org.zstack.kvm.APIKvmFixVolumeSnapshotChainMsgHandler.Qcow2FileInfo;

import java.util.List;

/**
 * Created by xing5 on 2016/10/24.
 */
public class KvmGetQcow2FileInfoPrimaryStorageReply extends MessageReply {
    private List<Qcow2FileInfo> infos;

    public List<Qcow2FileInfo> getInfos() {
        return infos;
    }

    public void setInfos(List<Qcow2FileInfo> infos) {
        this.infos = infos;
    }
}
