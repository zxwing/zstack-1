package org.zstack.test.mevoco.logging;

import junit.framework.Assert;
import org.junit.Before;
import org.junit.Test;
import org.zstack.core.Platform;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.componentloader.ComponentLoader;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.logging.Log;
import org.zstack.header.allocator.HostCapacityOverProvisioningManager;
import org.zstack.header.identity.SessionInventory;
import org.zstack.header.storage.primary.PrimaryStorageOverProvisioningManager;
import org.zstack.logging.APIQueryLogMsg;
import org.zstack.logging.APIQueryLogReply;
import org.zstack.logging.LogConstants;
import org.zstack.logging.LogConstants.LogType;
import org.zstack.logging.LogInventory;
import org.zstack.network.service.flat.FlatNetworkServiceSimulatorConfig;
import org.zstack.simulator.kvm.KVMSimulatorConfig;
import org.zstack.storage.primary.local.LocalStorageSimulatorConfig;
import org.zstack.test.Api;
import org.zstack.test.ApiSenderException;
import org.zstack.test.DBUtil;
import org.zstack.test.WebBeanConstructor;
import org.zstack.test.deployer.Deployer;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import java.io.IOException;

/**
 */
public class TestLogging1 {
    CLogger logger = Utils.getLogger(TestLogging1.class);
    Deployer deployer;
    Api api;
    ComponentLoader loader;
    CloudBus bus;
    DatabaseFacade dbf;
    SessionInventory session;
    LocalStorageSimulatorConfig config;
    FlatNetworkServiceSimulatorConfig fconfig;
    KVMSimulatorConfig kconfig;
    PrimaryStorageOverProvisioningManager psRatioMgr;
    HostCapacityOverProvisioningManager hostRatioMgr;

    @Before
    public void setUp() throws Exception {
        DBUtil.reDeployDB();
        DBUtil.reDeployCassandra(LogConstants.KEY_SPACE);
        WebBeanConstructor con = new WebBeanConstructor();
        deployer = new Deployer("deployerXml/OnlyOneZone.xml", con);
        deployer.addSpringConfig("cassandra.xml");
        deployer.addSpringConfig("mevocoRelated.xml");
        deployer.load();

        loader = deployer.getComponentLoader();
        bus = loader.getComponent(CloudBus.class);
        dbf = loader.getComponent(DatabaseFacade.class);
        config = loader.getComponent(LocalStorageSimulatorConfig.class);
        fconfig = loader.getComponent(FlatNetworkServiceSimulatorConfig.class);
        kconfig = loader.getComponent(KVMSimulatorConfig.class);
        psRatioMgr = loader.getComponent(PrimaryStorageOverProvisioningManager.class);
        hostRatioMgr = loader.getComponent(HostCapacityOverProvisioningManager.class);

        deployer.build();
        api = deployer.getApi();
        session = api.loginAsAdmin();
    }
    
	@Test
	public void test() throws ApiSenderException, IOException {
        Log log = new Log(Platform.getUuid()).log(LogLabelTest.TEST1);

        APIQueryLogMsg msg = new APIQueryLogMsg();
        msg.setType(LogType.RESOURCE.toString());
        msg.setResourceUuid(log.getResourceUuid());
        APIQueryLogReply reply = api.queryCassandra(msg, APIQueryLogReply.class);
        Assert.assertEquals(1, reply.getInventories().size());
        LogInventory loginv = reply.getInventories().get(0);
        Assert.assertEquals("测试1", loginv.getText());
        Assert.assertEquals(LogType.RESOURCE.toString(), loginv.getType());
        Assert.assertEquals(log.getResourceUuid(), loginv.getResourceUuid());

        api.deleteLog(loginv.getUuid(), null);
        reply = api.queryCassandra(msg, APIQueryLogReply.class);
        Assert.assertEquals(0, reply.getInventories().size());

        log = new Log().log(LogLabelTest.TEST1);

        msg = new APIQueryLogMsg();
        msg.setType(LogType.SYSTEM.toString());
        msg.setResourceUuid(log.getResourceUuid());
        reply = api.queryCassandra(msg, APIQueryLogReply.class);
        Assert.assertEquals(1, reply.getInventories().size());
        loginv = reply.getInventories().get(0);
        Assert.assertEquals("测试1", loginv.getText());
        Assert.assertEquals(LogType.SYSTEM.toString(), loginv.getType());
        Assert.assertEquals(LogType.SYSTEM.toString(), loginv.getResourceUuid());
    }
}
