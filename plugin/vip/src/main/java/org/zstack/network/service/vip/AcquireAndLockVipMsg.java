package org.zstack.network.service.vip;

/**
 * Created by xing5 on 2016/11/19.
 */
public class AcquireAndLockVipMsg extends AcquireVipMsg {
    private String networkServiceType;

    public String getNetworkServiceType() {
        return networkServiceType;
    }

    public void setNetworkServiceType(String networkServiceType) {
        this.networkServiceType = networkServiceType;
    }
}
