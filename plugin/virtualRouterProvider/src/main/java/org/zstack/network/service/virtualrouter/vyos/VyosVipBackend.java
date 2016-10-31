package org.zstack.network.service.virtualrouter.vyos;

import org.zstack.network.service.virtualrouter.vip.VirtualRouterVipBackend;

/**
 * Created by xing5 on 2016/10/31.
 */
public class VyosVipBackend extends VirtualRouterVipBackend {
    @Override
    public String getServiceProviderTypeForVip() {
        return VyosConstants.VYOS_ROUTER_PROVIDER_TYPE;
    }
}
