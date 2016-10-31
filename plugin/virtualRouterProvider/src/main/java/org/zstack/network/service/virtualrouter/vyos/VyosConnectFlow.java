package org.zstack.network.service.virtualrouter.vyos;

import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.zstack.appliancevm.ApplianceVmConstant;
import org.zstack.appliancevm.ApplianceVmSpec;
import org.zstack.core.errorcode.ErrorFacade;
import org.zstack.core.workflow.FlowChainBuilder;
import org.zstack.core.workflow.ShareFlow;
import org.zstack.header.core.Completion;
import org.zstack.header.core.workflow.*;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.rest.RESTFacade;
import org.zstack.header.vm.VmInstanceConstant;
import org.zstack.header.vm.VmInstanceSpec;
import org.zstack.header.vm.VmNicInventory;
import org.zstack.network.service.virtualrouter.VirtualRouterConstant;
import org.zstack.network.service.virtualrouter.VirtualRouterManager;
import org.zstack.network.service.virtualrouter.VirtualRouterVmInventory;
import org.zstack.utils.DebugUtils;

import java.util.Map;

/**
 * Created by xing5 on 2016/10/31.
 */
@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class VyosConnectFlow extends NoRollbackFlow {
    @Autowired
    private VirtualRouterManager vrMgr;
    @Autowired
    private RESTFacade restf;
    @Autowired
    private ErrorFacade errf;

    @Override
    public void run(FlowTrigger trigger, Map data) {
        final VirtualRouterVmInventory vr = (VirtualRouterVmInventory) data.get(VirtualRouterConstant.Param.VR.toString());
        VmNicInventory mgmtNic;
        if (vr != null) {
            mgmtNic = vr.getManagementNic();
        } else {
            final VmInstanceSpec spec = (VmInstanceSpec) data.get(VmInstanceConstant.Params.VmInstanceSpec.toString());
            final ApplianceVmSpec aspec = spec.getExtensionData(ApplianceVmConstant.Params.applianceVmSpec.toString(), ApplianceVmSpec.class);
            mgmtNic = spec.getDestNics().stream().filter(n->n.getL3NetworkUuid().equals(aspec.getManagementNic().getL3NetworkUuid())).findAny().get();
            DebugUtils.Assert(mgmtNic!=null, String.format("cannot find management nic for virtual router[uuid:%s, name:%s]", spec.getVmInventory().getUuid(), spec.getVmInventory().getName()));
        }

        final FlowChain chain = FlowChainBuilder.newShareFlowChain();
        chain.setName(String.format("virtual-router-%s-continue-connecting", mgmtNic.getVmInstanceUuid()));
        chain.then(new ShareFlow() {
            @Override
            public void setup() {
                flow(new NoRollbackFlow() {
                    String __name__ = "echo";

                    @Override
                    public void run(FlowTrigger trigger, Map data) {
                        String url = vrMgr.buildUrl(mgmtNic.getIp(), VirtualRouterConstant.VR_ECHO_PATH);
                        restf.echo(url, new Completion(trigger) {
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

                done(new FlowDoneHandler(trigger) {
                    @Override
                    public void handle(Map data) {
                        trigger.next();
                    }
                });

                error(new FlowErrorHandler(trigger) {
                    @Override
                    public void handle(ErrorCode errCode, Map data) {
                        trigger.fail(errCode);
                    }
                });
            }
        }).start();
    }
}
