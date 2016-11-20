package org.zstack.network.service.vip;

import org.zstack.header.network.l3.L3NetworkInventory;

/**
 * Created by xing5 on 2016/11/20.
 */
public class AcquireVipStruct {
    private String vipUuid;
    private L3NetworkInventory peerL3Network;
    private String networkServiceProviderType;

    public String getVipUuid() {
        return vipUuid;
    }

    public void setVipUuid(String vipUuid) {
        this.vipUuid = vipUuid;
    }

    public L3NetworkInventory getPeerL3Network() {
        return peerL3Network;
    }

    public void setPeerL3Network(L3NetworkInventory peerL3Network) {
        this.peerL3Network = peerL3Network;
    }

    public String getNetworkServiceProviderType() {
        return networkServiceProviderType;
    }

    public void setNetworkServiceProviderType(String networkServiceProviderType) {
        this.networkServiceProviderType = networkServiceProviderType;
    }
}
