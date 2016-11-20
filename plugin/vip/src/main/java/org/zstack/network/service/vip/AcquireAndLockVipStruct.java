package org.zstack.network.service.vip;

/**
 * Created by xing5 on 2016/11/20.
 */
public class AcquireAndLockVipStruct extends AcquireVipStruct {
    private String networkServiceType;

    public String getNetworkServiceType() {
        return networkServiceType;
    }

    public void setNetworkServiceType(String networkServiceType) {
        this.networkServiceType = networkServiceType;
    }
}
