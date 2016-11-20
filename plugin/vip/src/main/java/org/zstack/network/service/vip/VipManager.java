package org.zstack.network.service.vip;

import org.zstack.header.Service;
import org.zstack.header.core.Completion;
import org.zstack.header.core.workflow.FlowChain;
import org.zstack.header.network.l3.L3NetworkInventory;

/**
 */
public interface VipManager extends Service {
    VipBackend getVipBackend(String providerType);

    void saveVipInfo(String vipUuid, String networkServiceType, String peerL3NetworkUuid);

    void lockVip(VipInventory vip,  String networkServiceType);

    void unlockVip(VipInventory vip);

    VipReleaseExtensionPoint getVipReleaseExtensionPoint(String useFor);

    FlowChain getReleaseVipChain();

    VipFactory getVipFactory(String networkServiceProviderType);
}
