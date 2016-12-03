package org.zstack.test.virtualrouter;

import junit.framework.Assert;
import org.junit.Before;
import org.junit.Test;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.componentloader.ComponentLoader;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.thread.AsyncThread;
import org.zstack.core.thread.ThreadGlobalProperty;
import org.zstack.header.exception.CloudRuntimeException;
import org.zstack.header.identity.SessionInventory;
import org.zstack.header.network.l3.L3NetworkInventory;
import org.zstack.header.vm.VmInstanceInventory;
import org.zstack.network.service.portforwarding.PortForwardingProtocolType;
import org.zstack.network.service.portforwarding.PortForwardingRuleInventory;
import org.zstack.network.service.portforwarding.PortForwardingRuleVO;
import org.zstack.network.service.vip.VipInventory;
import org.zstack.network.service.vip.VipVO;
import org.zstack.simulator.appliancevm.ApplianceVmSimulatorConfig;
import org.zstack.simulator.virtualrouter.VirtualRouterSimulatorConfig;
import org.zstack.test.Api;
import org.zstack.test.ApiSenderException;
import org.zstack.test.DBUtil;
import org.zstack.test.WebBeanConstructor;
import org.zstack.test.deployer.Deployer;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;

/**
 * 
 * @author frank
 * 
 * @condition
 * 1. create a vm
 * 2. acquire a public ip: pub1
 * 3. set port forwarding to vm using pub1
 *
 * @test
 * confirm port forwarding rules on vm are correct
 */
public class TestVirtualRouterPortForwarding34 {
    Deployer deployer;
    Api api;
    ComponentLoader loader;
    CloudBus bus;
    DatabaseFacade dbf;
    SessionInventory session;
    VirtualRouterSimulatorConfig vconfig;
    ApplianceVmSimulatorConfig aconfig;

    @Before
    public void setUp() throws Exception {
        DBUtil.reDeployDB();
        WebBeanConstructor con = new WebBeanConstructor();
        deployer = new Deployer("deployerXml/virtualRouter/TestVirtualRouterPortForwarding34.xml", con);
        ThreadGlobalProperty.MAX_THREAD_NUM = 500;
        deployer.addSpringConfig("VirtualRouter.xml");
        deployer.addSpringConfig("VirtualRouterSimulator.xml");
        deployer.addSpringConfig("KVMRelated.xml");
        deployer.addSpringConfig("PortForwarding.xml");
        deployer.addSpringConfig("vip.xml");
        deployer.addSpringConfig("vyos.xml");
        deployer.build();
        api = deployer.getApi();
        loader = deployer.getComponentLoader();
        vconfig = loader.getComponent(VirtualRouterSimulatorConfig.class);
        aconfig = loader.getComponent(ApplianceVmSimulatorConfig.class);
        bus = loader.getComponent(CloudBus.class);
        dbf = loader.getComponent(DatabaseFacade.class);
        session = api.loginAsAdmin();
    }
    
    @Test
    public void test() throws ApiSenderException, InterruptedException {
        L3NetworkInventory publicNw = deployer.l3Networks.get("PublicNetwork");
        VmInstanceInventory vm = deployer.vms.get("TestVm");
        VipInventory vip = api.acquireIp(publicNw.getUuid());

        List<PortForwardingRuleInventory> rules = new ArrayList<>();

        CountDownLatch latch = new CountDownLatch(100);
        for (int i=100; i<200; i++) {
            int finalI = i;
            new Runnable() {
                @Override
                @AsyncThread
                public void run() {
                    PortForwardingRuleInventory rule = new PortForwardingRuleInventory();
                    rule.setName("test name");
                    rule.setAllowedCidr("72.1.1.1/24");
                    rule.setPrivatePortEnd(finalI);
                    rule.setPrivatePortStart(finalI);
                    rule.setProtocolType(PortForwardingProtocolType.TCP.toString());
                    rule.setVipUuid(vip.getUuid());
                    rule.setVipPortEnd(finalI);
                    rule.setVipPortStart(finalI);
                    rule.setVmNicUuid(vm.getVmNics().get(0).getUuid());
                    try {
                        rule =  api.createPortForwardingRuleByFullConfig(rule);
                        synchronized (rules) {
                            rules.add(rule);
                        }
                    } catch (ApiSenderException e) {
                        throw new CloudRuntimeException(e);
                    } finally {
                        latch.countDown();
                    }
                }
            }.run();
        }

        latch.await();

        Assert.assertEquals(100, rules.size());
        CountDownLatch latch1 = new CountDownLatch(100);

        for (PortForwardingRuleInventory rule : rules) {
            new Runnable() {
                @Override
                @AsyncThread
                public void run() {
                    try {
                        api.revokePortForwardingRule(rule.getUuid());
                    } catch (ApiSenderException e) {
                        throw new CloudRuntimeException(e);
                    } finally {
                        latch1.countDown();
                    }
                }
            }.run();
        }

        latch1.await();

        long count = dbf.count(PortForwardingRuleVO.class);
        Assert.assertEquals(0, count);
        count = dbf.count(VipVO.class);
        Assert.assertEquals(0, count);
    }
}
