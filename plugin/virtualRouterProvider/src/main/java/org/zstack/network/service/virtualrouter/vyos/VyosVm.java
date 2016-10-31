package org.zstack.network.service.virtualrouter.vyos;

import org.zstack.appliancevm.ApplianceVmVO;
import org.zstack.core.CoreGlobalProperty;
import org.zstack.header.core.workflow.Flow;
import org.zstack.header.host.HypervisorType;
import org.zstack.network.service.virtualrouter.VirtualRouter;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by xing5 on 2016/10/31.
 */
public class VyosVm extends VirtualRouter {
    public VyosVm(ApplianceVmVO vo) {
        super(vo);
    }

    protected List<Flow> createBootstrapFlows(HypervisorType hvType) {
        List<Flow> flows = new ArrayList<Flow>();

        flows.add(apvmf.createBootstrapFlow(hvType));
        if (!CoreGlobalProperty.UNIT_TEST_ON) {
            flows.add(new VyosDeployAgentFlow());
        }

        return flows;
    }
}
