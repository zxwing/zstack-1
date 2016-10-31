package org.zstack.network.service.virtualrouter.vyos;

import org.zstack.header.network.service.NetworkServiceProviderType;
import org.zstack.network.service.virtualrouter.nat.VirtualRouterSnatBackend;

/**
 * Created by xing5 on 2016/10/31.
 */
public class VyosSnatBackend extends VirtualRouterSnatBackend {
    @Override
    public NetworkServiceProviderType getProviderType() {
        return VyosConstants.PROVIDER_TYPE;
    }
}
