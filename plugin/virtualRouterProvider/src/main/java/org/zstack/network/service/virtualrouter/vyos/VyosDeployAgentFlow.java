package org.zstack.network.service.virtualrouter.vyos;

import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.zstack.appliancevm.*;
import org.zstack.appliancevm.ApplianceVmConstant.Params;
import org.zstack.core.ansible.AnsibleFacade;
import org.zstack.core.ansible.AnsibleRunner;
import org.zstack.core.ansible.SshFileMd5Checker;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.header.core.Completion;
import org.zstack.header.core.workflow.FlowTrigger;
import org.zstack.header.core.workflow.NoRollbackFlow;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.vm.VmInstanceConstant;
import org.zstack.header.vm.VmInstanceConstant.VmOperation;
import org.zstack.header.vm.VmInstanceSpec;
import org.zstack.header.vm.VmNicInventory;
import org.zstack.utils.CollectionUtils;
import org.zstack.utils.function.Function;
import org.zstack.utils.path.PathUtil;

import java.util.Map;

/**
 * Created by xing5 on 2016/10/31.
 */
@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class VyosDeployAgentFlow extends NoRollbackFlow {
    @Autowired
    private AnsibleFacade asf;
    @Autowired
    private DatabaseFacade dbf;

    @Override
    public void run(FlowTrigger trigger, Map data) {
        boolean isReconnect = Boolean.valueOf((String) data.get(Params.isReconnect.toString()));

        String mgmtNicIp;
        if (!isReconnect) {
            VmNicInventory mgmtNic;
            final VmInstanceSpec spec = (VmInstanceSpec) data.get(VmInstanceConstant.Params.VmInstanceSpec.toString());
            if (spec.getCurrentVmOperation() == VmOperation.NewCreate) {
                final ApplianceVmSpec aspec = spec.getExtensionData(ApplianceVmConstant.Params.applianceVmSpec.toString(), ApplianceVmSpec.class);
                mgmtNic = CollectionUtils.find(spec.getDestNics(), new Function<VmNicInventory, VmNicInventory>() {
                    @Override
                    public VmNicInventory call(VmNicInventory arg) {
                        return arg.getL3NetworkUuid().equals(aspec.getManagementNic().getL3NetworkUuid()) ? arg : null;
                    }
                });
            } else {
                ApplianceVmVO avo = dbf.findByUuid(spec.getVmInventory().getUuid(), ApplianceVmVO.class);
                ApplianceVmInventory ainv = ApplianceVmInventory.valueOf(avo);
                mgmtNic = ainv.getManagementNic();
            }
            mgmtNicIp = mgmtNic.getIp();
        } else {
            mgmtNicIp = (String) data.get(Params.managementNicIp.toString());
        }

        final String username = "vyos";
        final String privKey = asf.getPrivateKey();

        SshFileMd5Checker checker = new SshFileMd5Checker();
        checker.setTargetIp(mgmtNicIp);
        checker.setUsername(username);
        checker.setPrivateKey(privKey);
        checker.addSrcDestPair(PathUtil.findFileOnClassPath("ansible/vyos/zvr.bin", true).getAbsolutePath(),
                "/home/vyos/zvr.bin");
        checker.addSrcDestPair(PathUtil.findFileOnClassPath("ansible/vyos/zvrboot.bin", true).getAbsolutePath(),
                "/home/vyos/zvrboot.bin");

        AnsibleRunner runner = new AnsibleRunner();
        runner.installChecker(checker);
        runner.setUsername(username);
        runner.setPlayBookName(VyosConstants.ANSIBLE_PLAYBOOK_NAME);
        runner.setPrivateKey(privKey);
        runner.setAgentPort(ApplianceVmGlobalProperty.AGENT_PORT);
        runner.setTargetIp(mgmtNicIp);
        runner.run(new Completion(trigger) {
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
}
