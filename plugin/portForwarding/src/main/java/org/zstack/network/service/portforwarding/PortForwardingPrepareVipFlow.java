package org.zstack.network.service.portforwarding;

import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.CloudBusCallBack;
import org.zstack.header.core.Completion;
import org.zstack.header.core.workflow.Flow;
import org.zstack.header.core.workflow.FlowRollback;
import org.zstack.header.core.workflow.FlowTrigger;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.errorcode.OperationFailureException;
import org.zstack.header.message.MessageReply;
import org.zstack.header.network.l3.L3NetworkInventory;
import org.zstack.network.service.portforwarding.PortForwardingConstant.Params;
import org.zstack.network.service.vip.*;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import java.util.Map;

/**
 */
@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class PortForwardingPrepareVipFlow implements Flow {
    private static final CLogger logger = Utils.getLogger(PortForwardingPrepareVipFlow.class);

    @Autowired
    private CloudBus bus;

    private static final String SUCCESS = PortForwardingPrepareVipFlow.class.getName();

    public void run(final FlowTrigger trigger, final Map data) {
        final VipInventory vip = (VipInventory) data.get(VipConstant.Params.VIP.toString());
        final String serviceProviderType = (String) data.get(VipConstant.Params.VIP_SERVICE_PROVIDER_TYPE.toString());
        final L3NetworkInventory peerL3 = (L3NetworkInventory) data.get(VipConstant.Params.GUEST_L3NETWORK_VIP_FOR.toString());
        boolean needLockVip = data.containsKey(Params.NEED_LOCK_VIP.toString());

        AcquireVipMsg msg = new AcquireVipMsg();
        msg.setPeerL3NetworkUuid(peerL3.getUuid());
        msg.setServiceProvider(serviceProviderType);
        msg.setVipUuid(vip.getUuid());
        if (needLockVip) {
            msg.setUseFor(PortForwardingConstant.PORTFORWARDING_NETWORK_SERVICE_TYPE);
        }
        bus.makeTargetServiceIdByResourceUuid(msg, VipConstant.SERVICE_ID, vip.getUuid());
        bus.send(msg, new CloudBusCallBack(trigger) {
            @Override
            public void run(MessageReply reply) {
                if (!reply.isSuccess()) {
                    throw new OperationFailureException(reply.getError());
                }

                trigger.next();
            }
        });
    }

    @Override
    public void rollback(final FlowRollback trigger, Map data) {
        final VipInventory vip = (VipInventory) data.get(VipConstant.Params.VIP.toString());
        if (!data.containsKey(SUCCESS)) {
            trigger.rollback();
            return;
        }

        boolean needLockVip = data.containsKey(Params.NEED_LOCK_VIP.toString());
        ReleaseVipMsg msg = new ReleaseVipMsg();
        msg.setVipUuid(vip.getUuid());
        msg.setPeerL3NetworkUuid(null);
        if (needLockVip) {
            msg.setUseFor(null);
        }
        bus.makeTargetServiceIdByResourceUuid(msg, VipConstant.SERVICE_ID, vip.getUuid());
        bus.send(msg, new CloudBusCallBack(trigger) {
            @Override
            public void run(MessageReply reply) {
                if (!reply.isSuccess()) {
                    logger.warn(reply.getError().toString());
                }

                trigger.rollback();
            }
        });
    }
}
