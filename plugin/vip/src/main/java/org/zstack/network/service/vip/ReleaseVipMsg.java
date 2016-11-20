package org.zstack.network.service.vip;

import org.zstack.header.message.NeedReplyMessage;

/**
 * Created by xing5 on 2016/11/20.
 */
public class ReleaseVipMsg extends NeedReplyMessage implements VipMessage {
    private String vipUuid;
    private boolean releasePeerL3Network;

    public boolean isReleasePeerL3Network() {
        return releasePeerL3Network;
    }

    public void setReleasePeerL3Network(boolean releasePeerL3Network) {
        this.releasePeerL3Network = releasePeerL3Network;
    }

    public void setVipUuid(String vipUuid) {
        this.vipUuid = vipUuid;
    }

    public ReleaseVipStruct toReleaseAndUnlockVipStruct() {
        ReleaseVipStruct s = new ReleaseVipStruct();
        s.setReleasePeerL3Network(isReleasePeerL3Network());
        return s;
    }


    @Override
    public String getVipUuid() {
        return vipUuid;
    }
}
