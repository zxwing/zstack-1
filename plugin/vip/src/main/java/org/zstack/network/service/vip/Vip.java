package org.zstack.network.service.vip;

import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.CloudBusCallBack;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.header.core.Completion;
import org.zstack.header.core.ReturnValueCompletion;
import org.zstack.header.errorcode.OperationFailureException;
import org.zstack.header.message.MessageReply;
import org.zstack.utils.DebugUtils;

@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class Vip {
    @Autowired
    private CloudBus bus;
    @Autowired
    private DatabaseFacade dbf;

    public Vip(String uuid) {
        this.uuid = uuid;
    }

    private String uuid;
    private String serviceProvider;
    private String useFor;
    private String peerL3NetworkUuid;

    public String getServiceProvider() {
        return serviceProvider;
    }

    public void setServiceProvider(String serviceProvider) {
        this.serviceProvider = serviceProvider;
    }

    public String getUseFor() {
        return useFor;
    }

    public void setUseFor(String useFor) {
        this.useFor = useFor;
    }

    public String getPeerL3NetworkUuid() {
        return peerL3NetworkUuid;
    }

    public void setPeerL3NetworkUuid(String peerL3NetworkUuid) {
        this.peerL3NetworkUuid = peerL3NetworkUuid;
    }

    public void acquire(ReturnValueCompletion<VipInventory> completion) {
        DebugUtils.Assert(serviceProvider != null, "serviceProvider cannot be null");
        DebugUtils.Assert(peerL3NetworkUuid != null, "peerL3NetworkUuid cannot be null");

        AcquireVipMsg msg = new AcquireVipMsg();
        msg.setVipUuid(uuid);
        msg.setServiceProvider(serviceProvider);
        msg.setPeerL3NetworkUuid(peerL3NetworkUuid);

        if (useFor != null) {
            msg.setUseFor(useFor);
        }

        bus.makeTargetServiceIdByResourceUuid(msg, VipConstant.SERVICE_ID, uuid);
        bus.send(msg, new CloudBusCallBack(completion) {
            @Override
            public void run(MessageReply reply) {
                if (!reply.isSuccess()) {
                    throw new OperationFailureException(reply.getError());
                }

                completion.success(VipInventory.valueOf(dbf.findByUuid(uuid, VipVO.class)));
            }
        });
    }

    public void release(Completion completion) {
        release(false, completion);
    }

    public void release(boolean unlock, Completion completion) {
        ReleaseVipMsg msg = new ReleaseVipMsg();
        msg.setVipUuid(uuid);
        msg.setPeerL3NetworkUuid(null);

        if (unlock) {
            msg.setUseFor(null);
        }

        bus.makeTargetServiceIdByResourceUuid(msg, VipConstant.SERVICE_ID, uuid);
        bus.send(msg, new CloudBusCallBack(completion) {
            @Override
            public void run(MessageReply reply) {
                if (!reply.isSuccess()) {
                    throw new OperationFailureException(reply.getError());
                }

                completion.success();
            }
        });
    }

    public void modify(ModifyVipAttributesStruct s, ReturnValueCompletion<UnmodifyVip> completion) {
        ModifyVipAttributesMsg msg = new ModifyVipAttributesMsg();
        msg.setStruct(s);
        msg.setVipUuid(uuid);
        bus.makeTargetServiceIdByResourceUuid(msg, VipConstant.SERVICE_ID, uuid);
        bus.send(msg, new CloudBusCallBack(completion) {
            @Override
            public void run(MessageReply reply) {
                if (!reply.isSuccess()) {
                    throw new OperationFailureException(reply.getError());
                }

                ModifyVipAttributesReply r = reply.castReply();

                UnmodifyVip ret = new UnmodifyVip() {
                    @Override
                    public void unmodify(Completion completion) {
                        ModifyVipAttributesMsg  umsg = new ModifyVipAttributesMsg();
                        umsg.setStruct(r.getStruct());
                        umsg.setVipUuid(uuid);
                        bus.makeTargetServiceIdByResourceUuid(umsg, VipConstant.SERVICE_ID, uuid);

                        bus.send(umsg, new CloudBusCallBack(completion) {
                            @Override
                            public void run(MessageReply reply) {
                                if (!reply.isSuccess()) {
                                    completion.fail(reply.getError());
                                    return;
                                }

                                completion.success();
                            }
                        });
                    }
                };

                completion.success(ret);
            }
        });
    }
}
