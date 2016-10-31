package org.zstack.network.service.virtualrouter.vyos;

import org.zstack.network.service.virtualrouter.lb.VirtualRouterLoadBalancerBackend;

/**
 * Created by xing5 on 2016/10/31.
 */
public class VyosLbBackend extends VirtualRouterLoadBalancerBackend {
    @Override
    public String getNetworkServiceProviderType() {
        return VyosConstants.VYOS_ROUTER_PROVIDER_TYPE;
    }
}
