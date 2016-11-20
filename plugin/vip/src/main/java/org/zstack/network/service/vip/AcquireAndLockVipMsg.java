package org.zstack.network.service.vip;

/**
 * Created by xing5 on 2016/11/19.
 */
public class AcquireAndLockVipMsg extends AcquireVipMsg {
    private String networkServiceType;

    public AcquireAndLockVipStruct toAcquireAndLockVipStruct() {
        AcquireAndLockVipStruct s = new AcquireAndLockVipStruct();
        s.setVipUuid(getVipUuid());
        s.setNetworkServiceType(getNetworkServiceType());
        s.setNetworkServiceProviderType(getNetworkServiceProviderType());
        s.setPeerL3Network(getPeerL3Network());
        return s;
    }

    public String getNetworkServiceType() {
        return networkServiceType;
    }

    public void setNetworkServiceType(String networkServiceType) {
        this.networkServiceType = networkServiceType;
    }
}
