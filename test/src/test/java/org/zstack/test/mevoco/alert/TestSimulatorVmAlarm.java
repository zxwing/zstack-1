package org.zstack.test.mevoco.alert;

import junit.framework.Assert;
import org.junit.Before;
import org.junit.Test;
import org.zstack.alert.*;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.componentloader.ComponentLoader;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.header.vm.VmInstanceInventory;
import org.zstack.alert.VmAlarmFactory;
import org.zstack.test.Api;
import org.zstack.test.ApiSenderException;
import org.zstack.test.DBUtil;
import org.zstack.test.deployer.Deployer;

public class TestSimulatorVmAlarm {
    Deployer deployer;
    Api api;
    ComponentLoader loader;
    CloudBus bus;
    DatabaseFacade dbf;
    SimulatorAlarmFactory factory;

    @Before
    public void setUp() throws Exception {
        DBUtil.reDeployDB();
        deployer = new Deployer("deployerXml/vm/TestCreateVm.xml");
        deployer.addSpringConfig("alarmSimulator.xml");
        deployer.addSpringConfig("alert.xml");
        deployer.build();
        api = deployer.getApi();
        loader = deployer.getComponentLoader();
        bus = loader.getComponent(CloudBus.class);
        dbf = loader.getComponent(DatabaseFacade.class);
        factory = loader.getComponent(SimulatorAlarmFactory.class);
    }
    
    @Test
    public void test() throws ApiSenderException {
        VmInstanceInventory vm = deployer.vms.get("TestVm");

        APICreateVmCpuAlarmMsg msg = new APICreateVmCpuAlarmMsg();
        msg.setName("test");
        msg.setVmInstanceUuid(vm.getUuid());
        msg.setConditionDuration(1);
        msg.setConditionName(VmAlarmFactory.CPU_ALARM);
        msg.setConditionOperator(AlarmConditionOp.GT);
        msg.setConditionValue("1");
        APICreateAlarmEvent evt = api.sendApiMessage(msg, APICreateAlarmEvent.class);

        AlarmInventory inv = evt.getInventory();
        Assert.assertEquals(1, factory.alarms.size());
        Assert.assertEquals(vm.getUuid(), inv.getLabel(VmAlarmFactory.LABEL_VM_UUID));
        Assert.assertEquals(msg.getConditionName(), inv.getConditionName());
        Assert.assertEquals(msg.getConditionOperator().toString(), inv.getConditionOperator());
        Assert.assertEquals(msg.getConditionDuration(), inv.getConditionDuration().longValue());
        Assert.assertEquals(msg.getConditionValue(), inv.getConditionValue());
    }
}
