package org.zstack.storage.boss.primary;

import org.zstack.header.message.NeedReplyMessage;
import org.zstack.header.storage.primary.PrimaryStorageMessage;
import org.zstack.kvm.KvmSetupSelfFencerExtensionPoint;

/**
 * Created by XXPS-PC1 on 2016/11/22.
 */
public class CancelSelfFencerOnKvmHostMsg extends NeedReplyMessage implements PrimaryStorageMessage {
    private KvmSetupSelfFencerExtensionPoint.KvmCancelSelfFencerParam param;

    public KvmSetupSelfFencerExtensionPoint.KvmCancelSelfFencerParam getParam() {
        return param;
    }

    public void setParam(KvmSetupSelfFencerExtensionPoint.KvmCancelSelfFencerParam param) {
        this.param = param;
    }

    @Override
    public String getPrimaryStorageUuid() {
        return param.getPrimaryStorage().getUuid();
    }
}
