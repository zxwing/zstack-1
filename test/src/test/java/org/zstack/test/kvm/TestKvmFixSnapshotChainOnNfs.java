package org.zstack.test.kvm;

import junit.framework.Assert;
import org.junit.Before;
import org.junit.Test;
import org.zstack.core.Platform;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.componentloader.ComponentLoader;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.SimpleQuery;
import org.zstack.core.db.SimpleQuery.Op;
import org.zstack.header.identity.SessionInventory;
import org.zstack.header.storage.snapshot.VolumeSnapshotInventory;
import org.zstack.header.storage.snapshot.VolumeSnapshotTree;
import org.zstack.header.storage.snapshot.VolumeSnapshotTree.SnapshotLeaf;
import org.zstack.header.storage.snapshot.VolumeSnapshotVO;
import org.zstack.header.storage.snapshot.VolumeSnapshotVO_;
import org.zstack.header.vm.VmInstanceInventory;
import org.zstack.header.volume.VolumeVO;
import org.zstack.kvm.APIKvmFixVolumeSnapshotChainEvent.FixResult;
import org.zstack.kvm.APIKvmFixVolumeSnapshotChainMsgHandler.Qcow2FileInfo;
import org.zstack.simulator.storage.primary.nfs.NfsPrimaryStorageSimulatorConfig;
import org.zstack.test.Api;
import org.zstack.test.ApiSenderException;
import org.zstack.test.DBUtil;
import org.zstack.test.WebBeanConstructor;
import org.zstack.test.deployer.Deployer;
import org.zstack.utils.Utils;
import org.zstack.utils.data.SizeUnit;
import org.zstack.utils.function.Function;
import org.zstack.utils.logging.CLogger;

import java.util.List;

/*
* take snapshot from vm's root volume
*/
public class TestKvmFixSnapshotChainOnNfs {
    CLogger logger = Utils.getLogger(TestKvmFixSnapshotChainOnNfs.class);
    Deployer deployer;
    Api api;
    ComponentLoader loader;
    CloudBus bus;
    DatabaseFacade dbf;
    SessionInventory session;
    NfsPrimaryStorageSimulatorConfig config;

    @Before
    public void setUp() throws Exception {
        DBUtil.reDeployDB();
        WebBeanConstructor con = new WebBeanConstructor();
        deployer = new Deployer("deployerXml/kvm/TestCreateVmOnKvm.xml", con);
        deployer.addSpringConfig("KVMRelated.xml");
        deployer.build();
        api = deployer.getApi();
        loader = deployer.getComponentLoader();
        bus = loader.getComponent(CloudBus.class);
        dbf = loader.getComponent(DatabaseFacade.class);
        config = loader.getComponent(NfsPrimaryStorageSimulatorConfig.class);
        session = api.loginAsAdmin();
    }
    
	@Test
	public void test() throws ApiSenderException {
        VmInstanceInventory vm = deployer.vms.get("TestVm");
        String volUuid = vm.getRootVolumeUuid();
        VolumeSnapshotInventory inv1 = api.createSnapshot(volUuid);
        VolumeSnapshotInventory inv2 = api.createSnapshot(volUuid);
        VolumeVO root = dbf.findByUuid(volUuid, VolumeVO.class);

        Qcow2FileInfo info1 = new Qcow2FileInfo();
        info1.setPath(inv1.getPrimaryStorageInstallPath());
        info1.setSize(inv1.getSize());
        info1.setBackingFile(null);
        config.qcow2FileInfos.add(info1);

        Qcow2FileInfo info2 = new Qcow2FileInfo();
        info2.setPath(inv2.getPrimaryStorageInstallPath());
        info2.setSize(inv2.getSize());
        info2.setBackingFile(info1.getPath());
        config.qcow2FileInfos.add(info2);

        // the missing one
        Qcow2FileInfo info3 = new Qcow2FileInfo();
        info3.setPath(root.getInstallPath());
        info3.setSize(root.getSize());
        info3.setBackingFile(inv2.getPrimaryStorageInstallPath());
        config.qcow2FileInfos.add(info3);

        // the missing one
        Qcow2FileInfo info4 = new Qcow2FileInfo();
        info4.setPath(Platform.getUuid());
        info4.setSize(SizeUnit.BYTE.toByte(100));
        info4.setBackingFile(info3.getPath());
        config.qcow2FileInfos.add(info4);

        List<FixResult> results = api.kvmFixVolumeSnapshotChain(null);
        Assert.assertEquals(1, results.size());
        FixResult res = results.get(0);
        Assert.assertTrue(res.isSuccess());
        Assert.assertEquals(root.getUuid(), res.getVolumeUuid());
        Assert.assertEquals(root.getName(), res.getVolumeName());

        SimpleQuery<VolumeSnapshotVO> q = dbf.createQuery(VolumeSnapshotVO.class);
        q.add(VolumeSnapshotVO_.volumeUuid, Op.EQ, root.getUuid());
        List<VolumeSnapshotVO> sps = q.list();

        Assert.assertEquals(3, sps.size());
        VolumeSnapshotTree tree = VolumeSnapshotTree.fromVOs(sps);
        SnapshotLeaf sp1 = tree.findSnapshot(new Function<Boolean, VolumeSnapshotInventory>() {
            @Override
            public Boolean call(VolumeSnapshotInventory arg) {
                return arg.getPrimaryStorageInstallPath().equals(info1.getPath());
            }
        });
        Assert.assertNotNull(sp1);
        Assert.assertEquals(info1.getSize(), sp1.getInventory().getSize());
        Assert.assertNull(sp1.getParent());

        SnapshotLeaf sp2 = tree.findSnapshot(new Function<Boolean, VolumeSnapshotInventory>() {
            @Override
            public Boolean call(VolumeSnapshotInventory arg) {
                return arg.getPrimaryStorageInstallPath().equals(info2.getPath());
            }
        });
        Assert.assertNotNull(sp2);
        Assert.assertEquals(info2.getSize(), sp2.getInventory().getSize());
        Assert.assertNotNull(sp2.getParent());
        Assert.assertEquals(info1.getPath(), sp2.getParent().getInventory().getPrimaryStorageInstallPath());

        SnapshotLeaf sp3 = tree.findSnapshot(new Function<Boolean, VolumeSnapshotInventory>() {
            @Override
            public Boolean call(VolumeSnapshotInventory arg) {
                return arg.getPrimaryStorageInstallPath().equals(info3.getPath());
            }
        });
        Assert.assertNotNull(sp3);
        Assert.assertEquals(info3.getSize(), sp3.getInventory().getSize());
        Assert.assertNotNull(sp3.getParent());
        Assert.assertEquals(info2.getPath(), sp3.getParent().getInventory().getPrimaryStorageInstallPath());

        Assert.assertEquals(info4.getPath(), root.getInstallPath());
        Assert.assertEquals(info4.getSize(), root.getSize());
    }
}
