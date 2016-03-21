package org.zstack.test.actualsize;

import junit.framework.Assert;
import org.junit.Before;
import org.junit.Test;
import org.zstack.core.Platform;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.componentloader.ComponentLoader;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.header.configuration.DiskOfferingInventory;
import org.zstack.header.configuration.InstanceOfferingInventory;
import org.zstack.header.identity.SessionInventory;
import org.zstack.header.image.ImageConstant.ImageMediaType;
import org.zstack.header.image.ImageInventory;
import org.zstack.header.network.l3.L3NetworkInventory;
import org.zstack.header.storage.backup.BackupStorageInventory;
import org.zstack.header.vm.VmInstanceInventory;
import org.zstack.header.volume.VolumeInventory;
import org.zstack.header.volume.VolumeType;
import org.zstack.simulator.storage.backup.sftp.SftpBackupStorageSimulatorConfig;
import org.zstack.simulator.storage.primary.nfs.NfsPrimaryStorageSimulatorConfig;
import org.zstack.storage.primary.local.LocalStorageSimulatorConfig;
import org.zstack.storage.primary.local.LocalStorageSimulatorConfig.Capacity;
import org.zstack.test.*;
import org.zstack.test.deployer.Deployer;
import org.zstack.utils.CollectionUtils;
import org.zstack.utils.Utils;
import org.zstack.utils.data.SizeUnit;
import org.zstack.utils.function.Function;
import org.zstack.utils.logging.CLogger;

/**
 * Use local primary storage
 *
 * 1. add an image
 *
 * confirm the size and actual size are correct
 *
 * 2. create a vm from the image and with a data volume
 *
 * confirm the size and actual size of the root volume and data volume are correct
 *
 * 3. attach a data volume to the vm
 *
 * confirm the size and actual size of the new attached data volume are correct
 *
 */
public class TestActualSize2 {
    CLogger logger = Utils.getLogger(TestActualSize2.class);
    Deployer deployer;
    Api api;
    ComponentLoader loader;
    CloudBus bus;
    DatabaseFacade dbf;
    SessionInventory session;
    LocalStorageSimulatorConfig config;
    SftpBackupStorageSimulatorConfig sconfig;

    @Before
    public void setUp() throws Exception {
        DBUtil.reDeployDB();
        WebBeanConstructor con = new WebBeanConstructor();
        deployer = new Deployer("deployerXml/actualSize/TestActualSize2.xml", con);
        deployer.addSpringConfig("KVMRelated.xml");
        deployer.addSpringConfig("localStorageSimulator.xml");
        deployer.addSpringConfig("localStorage.xml");
        deployer.load();

        loader = deployer.getComponentLoader();
        bus = loader.getComponent(CloudBus.class);
        dbf = loader.getComponent(DatabaseFacade.class);
        config = loader.getComponent(LocalStorageSimulatorConfig.class);
        sconfig = loader.getComponent(SftpBackupStorageSimulatorConfig.class);

        Capacity c = new Capacity();
        c.total = SizeUnit.TERABYTE.toByte(1);
        c.avail = SizeUnit.TERABYTE.toByte(1);

        config.capacityMap.put("host1", c);

        deployer.build();
        api = deployer.getApi();

        session = api.loginAsAdmin();
    }

	@Test
	public void test() throws ApiSenderException {
        BackupStorageInventory sftp = deployer.backupStorages.get("sftp");
        InstanceOfferingInventory iov = deployer.instanceOfferings.get("TestInstanceOffering");
        L3NetworkInventory l3 = deployer.l3Networks.get("TestL3Network1");
        DiskOfferingInventory data = deployer.diskOfferings.get("data");

        ImageInventory iinv = new ImageInventory();
        iinv.setUuid(Platform.getUuid());
        iinv.setName("Test Image");
        iinv.setDescription("Test Image");
        iinv.setMediaType(ImageMediaType.RootVolumeTemplate.toString());
        iinv.setGuestOsType("Window7");
        iinv.setFormat("qcow2");
        iinv.setUrl("http://zstack.org/download/win7.qcow2");

        long size = SizeUnit.GIGABYTE.toByte(20);
        long actualSize = SizeUnit.GIGABYTE.toByte(1);
        sconfig.imageSizes.put(iinv.getUuid(), size);
        sconfig.imageActualSizes.put(iinv.getUuid(), actualSize);

        iinv = api.addImage(iinv, sftp.getUuid());
        Assert.assertEquals(size, iinv.getSize());
        Assert.assertEquals(actualSize, iinv.getActualSize().longValue());

        VmCreator creator = new VmCreator(api);
        creator.addL3Network(l3.getUuid());
        creator.addDisk(data.getUuid());
        creator.instanceOfferingUuid = iov.getUuid();
        creator.imageUuid = iinv.getUuid();
        creator.name = "vm";
        VmInstanceInventory vm = creator.create();

        VolumeInventory root = vm.getRootVolume();
        Assert.assertEquals(size, root.getSize());
        Assert.assertEquals(actualSize, root.getActualSize().longValue());

        VolumeInventory dvol1 = CollectionUtils.find(vm.getAllVolumes(), new Function<VolumeInventory, VolumeInventory>() {
            @Override
            public VolumeInventory call(VolumeInventory arg) {
                return VolumeType.Data.toString().equals(arg.getType()) ? arg : null;
            }
        });

        Assert.assertEquals(data.getDiskSize(), dvol1.getSize());
        Assert.assertEquals(0, dvol1.getActualSize().longValue());

        VolumeInventory dvol2 = api.createDataVolume("data2", data.getUuid());
        dvol2 = api.attachVolumeToVm(vm.getUuid(), dvol2.getUuid());
        Assert.assertEquals(data.getDiskSize(), dvol2.getSize());
        Assert.assertEquals(0, dvol2.getActualSize().longValue());
    }
}
