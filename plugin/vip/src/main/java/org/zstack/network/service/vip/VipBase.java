package org.zstack.network.service.vip;

import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.zstack.core.cascade.CascadeConstant;
import org.zstack.core.cascade.CascadeFacade;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.MessageSafe;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.SimpleQuery;
import org.zstack.core.errorcode.ErrorFacade;
import org.zstack.core.thread.ChainTask;
import org.zstack.core.thread.SyncTaskChain;
import org.zstack.core.thread.ThreadFacade;
import org.zstack.core.workflow.FlowChainBuilder;
import org.zstack.core.workflow.ShareFlow;
import org.zstack.header.core.Completion;
import org.zstack.header.core.NopeCompletion;
import org.zstack.header.core.workflow.*;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.errorcode.OperationFailureException;
import org.zstack.header.errorcode.SysErrors;
import org.zstack.header.message.APIDeleteMessage;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.Message;
import org.zstack.header.network.l3.L3NetworkConstant;
import org.zstack.header.network.l3.ReturnIpMsg;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * Created by xing5 on 2016/11/19.
 */
@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class VipBase implements Vip {
    private static final CLogger logger = Utils.getLogger(VipBase.class);

    private VipVO self;

    @Autowired
    private DatabaseFacade dbf;
    @Autowired
    private CloudBus bus;
    @Autowired
    private ThreadFacade thdf;
    @Autowired
    private CascadeFacade casf;
    @Autowired
    private ErrorFacade errf;
    @Autowired
    private VipManager vipMgr;

    protected String getThreadSyncSignature() {
        return String.format("vip-%s-%s", self.getName(), self.getUuid());
    }

    protected VipVO getSelf() {
        return self;
    }

    protected VipInventory getSelfInventory() {
        return VipInventory.valueOf(getSelf());
    }

    public VipBase(VipVO self) {
        this.self = self;
    }

    private void refresh() {
        VipVO vo = dbf.reload(self);
        if (vo == null) {
            throw new OperationFailureException(errf.instantiateErrorCode(SysErrors.RESOURCE_NOT_FOUND,
                    String.format("cannot find the vip[name:%s, uuid:%s, ip:%s], it may have been deleted",
                            self.getName(), self.getUuid(), self.getIp())
            ));
        }

        self = vo;
    }

    @Override
    @MessageSafe
    public void handleMessage(Message msg) {
        if (msg instanceof APIMessage) {
            handleApiMessage((APIMessage) msg);
        } else {
            handleLocalMessage(msg);
        }
    }

    private void handleLocalMessage(Message msg) {
        if (msg instanceof VipDeletionMsg) {
            handle((VipDeletionMsg) msg);
        } else if (msg instanceof AcquireAndLockVipMsg) {
            handle((AcquireAndLockVipMsg) msg);
        } else if (msg instanceof AcquireVipMsg) {
            handle((AcquireVipMsg) msg);
        } else {
            bus.dealWithUnknownMessage(msg);
        }
    }

    private void handle(AcquireVipMsg msg) {
        AcquireVipReply reply = new AcquireVipReply();

        thdf.chainSubmit(new ChainTask(msg) {
            @Override
            public String getSyncSignature() {
                return getThreadSyncSignature();
            }

            @Override
            public void run(SyncTaskChain chain) {
                refresh();
                acquireVip(msg.toAcquireVipStruct(), new Completion(msg, chain) {
                    @Override
                    public void success() {
                        bus.reply(msg, reply);
                        chain.next();
                    }

                    @Override
                    public void fail(ErrorCode errorCode) {
                        reply.setError(errorCode);
                        bus.reply(msg, reply);
                        chain.next();
                    }
                });
            }

            @Override
            public String getName() {
                return "acquire-vip";
            }
        });
    }

    private void handle(AcquireAndLockVipMsg msg) {
        AcquireAndLockVipReply reply = new AcquireAndLockVipReply();

        thdf.chainSubmit(new ChainTask(msg) {
            @Override
            public String getSyncSignature() {
                return getThreadSyncSignature();
            }

            @Override
            public void run(SyncTaskChain chain) {
                acquireAndLock(msg.toAcquireAndLockVipStruct(), new Completion(msg, chain) {
                    @Override
                    public void success() {
                        self = dbf.reload(self);
                        reply.setVip(getSelfInventory());
                        bus.reply(msg, reply);
                        chain.next();
                    }

                    @Override
                    public void fail(ErrorCode errorCode) {
                        reply.setError(errorCode);
                        bus.reply(msg, reply);
                        chain.next();
                    }
                });
            }

            @Override
            public String getName() {
                return null;
            }
        });
    }

    protected void lock(String networkServiceType) {
        refresh();

        if ((self.getUseFor() != null && !self.getUseFor().equals(networkServiceType))) {
            throw new OperationFailureException(
                    errf.stringToOperationError(String.format("vip[uuid:%s, name:%s, ip:%s] has been occupied by usage[%s]",
                            self.getUuid(), self.getName(), self.getIp(), self.getUseFor()))
            );
        }

        if (networkServiceType.equals(self.getUseFor())) {
            return;
        }

        self.setUseFor(networkServiceType);
        self = dbf.updateAndRefresh(self);

        logger.debug(String.format("successfully locked vip[uuid:%s, name:%s, ip:%s] for %s",
                self.getUuid(), self.getName(), self.getIp(), networkServiceType));
    }

    protected void saveVipInfo(String vipUuid, String networkServiceType, String peerL3NetworkUuid) {
        VipVO vo = dbf.findByUuid(vipUuid, VipVO.class);
        vo.setServiceProvider(networkServiceType);
        if (vo.getPeerL3NetworkUuid() == null) {
            vo.setPeerL3NetworkUuid(peerL3NetworkUuid);
        }
        dbf.update(vo);
    }

    protected void acquireVip(AcquireVipStruct s, Completion completion) {
        if (self.getPeerL3NetworkUuid() != null && !self.getPeerL3NetworkUuid().equals(s.getPeerL3Network().getUuid())) {
            throw new OperationFailureException(errf.stringToOperationError(String.format("vip[uuid:%s, name:%s] has been serving l3Network[uuid:%s], can't serve l3Network[name:%s, uuid:%s]",
                    self.getUuid(), self.getName(), self.getPeerL3NetworkUuid(), s.getPeerL3Network().getName(), s.getPeerL3Network().getUuid())));
        }

        VipFactory f = vipMgr.getVipFactory(s.getNetworkServiceProviderType());
        VipBase vip = f.getVip(getSelf());
        vip.acquireVip(s, new Completion(completion) {
            @Override
            public void success() {
                saveVipInfo(self.getUuid(), s.getNetworkServiceProviderType(), s.getPeerL3Network().getUuid());

                logger.debug(String.format("successfully acquired vip[uuid:%s, name:%s, ip:%s] on service[%s]",
                        self.getUuid(), self.getName(), self.getIp(), s.getNetworkServiceProviderType()));
                completion.success();
            }

            @Override
            public void fail(ErrorCode errorCode) {
                completion.fail(errorCode);
            }
        });
    }

    protected void acquireAndLock(AcquireAndLockVipStruct s, Completion completion) {
        lock(s.getNetworkServiceType());
        acquireVip(s, completion);
    }

    private void handle(VipDeletionMsg msg) {
        VipDeletionReply reply = new VipDeletionReply();
        thdf.chainSubmit(new ChainTask(msg) {
            @Override
            public String getSyncSignature() {
                return getThreadSyncSignature();
            }

            @Override
            public void run(SyncTaskChain chain) {
                deleteVip(new Completion(msg, chain) {
                    @Override
                    public void success() {
                        bus.reply(msg, reply);
                        chain.next();
                    }

                    @Override
                    public void fail(ErrorCode errorCode) {
                        reply.setError(errorCode);
                        bus.reply(msg, reply);
                        chain.next();
                    }
                });
            }

            @Override
            public String getName() {
                return "delete-vip";
            }
        });
    }

    private void returnVip() {
        ReturnIpMsg msg = new ReturnIpMsg();
        msg.setL3NetworkUuid(self.getL3NetworkUuid());
        msg.setUsedIpUuid(self.getUsedIpUuid());
        bus.makeTargetServiceIdByResourceUuid(msg, L3NetworkConstant.SERVICE_ID, self.getL3NetworkUuid());
        bus.send(msg);
    }

    private void deleteVip(Completion completion) {
        refresh();

        if (self.getUseFor() == null) {
            returnVip();
            dbf.remove(self);
            logger.debug(String.format("'useFor' is not set, released vip[uuid:%s, ip:%s] on l3Network[uuid:%s]",
                    self.getUuid(), self.getIp(), self.getL3NetworkUuid()));
            completion.success();
            return;
        }

        FlowChain chain = vipMgr.getReleaseVipChain();
        chain.setName(String.format("api-release-vip-uuid-%s-ip-%s-name-%s", self.getUuid(), self.getIp(), self.getName()));
        chain.getData().put(VipConstant.Params.VIP.toString(), getSelfInventory());
        chain.done(new FlowDoneHandler(completion) {
            @Override
            public void handle(Map data) {
                returnVip();
                dbf.remove(self);
                completion.success();
            }
        }).error(new FlowErrorHandler(completion) {
            @Override
            public void handle(ErrorCode errCode, Map data) {
                completion.fail(errCode);
            }
        }).start();
    }

    private void handleApiMessage(APIMessage msg) {
        if (msg instanceof APIChangeVipStateMsg) {
            handle((APIChangeVipStateMsg) msg);
        } else if (msg instanceof APIDeleteVipMsg) {
            handle((APIDeleteVipMsg) msg);
        } else if (msg instanceof APIUpdateVipMsg)  {
            handle((APIUpdateVipMsg) msg);
        } else {
            bus.dealWithUnknownMessage(msg);
        }
    }

    private void handle(APIUpdateVipMsg msg) {
        VipVO vo = dbf.findByUuid(msg.getUuid(), VipVO.class);
        boolean update = false;
        if (msg.getName() != null) {
            vo.setName(msg.getName());
            update = true;
        }
        if (msg.getDescription() != null) {
            vo.setDescription(msg.getDescription());
            update = true;
        }
        if (update) {
            vo = dbf.updateAndRefresh(vo);
        }

        APIUpdateVipEvent evt = new APIUpdateVipEvent(msg.getId());
        evt.setInventory(VipInventory.valueOf(vo));
        bus.publish(evt);
    }

    private void handle(APIDeleteVipMsg msg) {
        final APIDeleteVipEvent evt = new APIDeleteVipEvent(msg.getId());

        final String issuer = VipVO.class.getSimpleName();
        final List<VipInventory> ctx = Arrays.asList(VipInventory.valueOf(self));
        FlowChain chain = FlowChainBuilder.newShareFlowChain();
        chain.setName(String.format("delete-vip-%s", self.getUuid()));
        chain.then(new ShareFlow() {
            @Override
            public void setup() {
                if (msg.getDeletionMode() == APIDeleteMessage.DeletionMode.Permissive) {
                    flow(new NoRollbackFlow() {
                        String __name__ = "delete-vip-permissive-check";

                        @Override
                        public void run(final FlowTrigger trigger, Map data) {
                            casf.asyncCascade(CascadeConstant.DELETION_CHECK_CODE, issuer, ctx, new Completion(trigger) {
                                @Override
                                public void success() {
                                    trigger.next();
                                }

                                @Override
                                public void fail(ErrorCode errorCode) {
                                    trigger.fail(errorCode);
                                }
                            });
                        }
                    });

                    flow(new NoRollbackFlow() {
                        String __name__ = "delete-vip-permissive-delete";

                        @Override
                        public void run(final FlowTrigger trigger, Map data) {
                            casf.asyncCascade(CascadeConstant.DELETION_DELETE_CODE, issuer, ctx, new Completion(trigger) {
                                @Override
                                public void success() {
                                    trigger.next();
                                }

                                @Override
                                public void fail(ErrorCode errorCode) {
                                    trigger.fail(errorCode);
                                }
                            });
                        }
                    });
                } else {
                    flow(new NoRollbackFlow() {
                        String __name__ = "delete-vip-force-delete";

                        @Override
                        public void run(final FlowTrigger trigger, Map data) {
                            casf.asyncCascade(CascadeConstant.DELETION_FORCE_DELETE_CODE, issuer, ctx, new Completion(trigger) {
                                @Override
                                public void success() {
                                    trigger.next();
                                }

                                @Override
                                public void fail(ErrorCode errorCode) {
                                    trigger.fail(errorCode);
                                }
                            });
                        }
                    });
                }

                done(new FlowDoneHandler(msg) {
                    @Override
                    public void handle(Map data) {
                        casf.asyncCascadeFull(CascadeConstant.DELETION_CLEANUP_CODE, issuer, ctx, new NopeCompletion());
                        bus.publish(evt);
                    }
                });

                error(new FlowErrorHandler(msg) {
                    @Override
                    public void handle(ErrorCode errCode, Map data) {
                        evt.setErrorCode(errf.instantiateErrorCode(SysErrors.DELETE_RESOURCE_ERROR, errCode));
                        bus.publish(evt);
                    }
                });
            }
        }).start();
    }

    private void handle(APIChangeVipStateMsg msg) {
        VipVO vip = dbf.findByUuid(msg.getUuid(), VipVO.class);
        VipStateEvent sevt = VipStateEvent.valueOf(msg.getStateEvent());
        vip.setState(vip.getState().nextState(sevt));
        vip = dbf.updateAndRefresh(vip);

        APIChangeVipStateEvent evt = new APIChangeVipStateEvent(msg.getId());
        evt.setInventory(VipInventory.valueOf(vip));
        bus.publish(evt);
    }
}
