package org.zstack.network.service.vip;

import org.zstack.header.Service;
import org.zstack.header.core.Completion;
import org.zstack.header.core.workflow.FlowChain;
import org.zstack.header.network.l3.L3NetworkInventory;

/**
 */
public interface VipManager extends Service {
    void saveVipInfo(String vipUuid, String networkServiceType, String peerL3NetworkUuid);

    VipReleaseExtensionPoint getVipReleaseExtensionPoint(String useFor);

    FlowChain getReleaseVipChain();

    VipFactory getVipFactory(String networkServiceProviderType);
}
