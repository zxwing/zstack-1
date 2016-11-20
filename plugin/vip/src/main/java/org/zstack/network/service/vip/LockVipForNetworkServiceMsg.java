package org.zstack.network.service.vip;

import org.zstack.header.message.NeedReplyMessage;

/**
 * Created by xing5 on 2016/11/20.
 */
public class LockVipForNetworkServiceMsg extends NeedReplyMessage implements VipMessage {
    private String vipUuid;
    private String networkServiceType;

    public String getNetworkServiceType() {
        return networkServiceType;
    }

    public void setNetworkServiceType(String networkServiceType) {
        this.networkServiceType = networkServiceType;
    }

    @Override
    public String getVipUuid() {
        return vipUuid;
    }

    public void setVipUuid(String vipUuid) {
        this.vipUuid = vipUuid;
    }
}
