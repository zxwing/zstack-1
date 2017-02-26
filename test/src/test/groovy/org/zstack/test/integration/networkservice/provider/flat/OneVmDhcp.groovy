package org.zstack.test.integration.networkservice.provider.flat

import org.springframework.http.HttpEntity
import org.zstack.header.network.l3.UsedIpVO
import org.zstack.network.service.flat.BridgeNameFinder
import org.zstack.network.service.flat.FlatDhcpBackend
import org.zstack.network.service.flat.FlatNetworkSystemTags
import org.zstack.sdk.L3NetworkInventory
import org.zstack.sdk.VmInstanceInventory
import org.zstack.sdk.VmNicInventory
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.ImageSpec
import org.zstack.testlib.InstanceOfferingSpec
import org.zstack.testlib.L3NetworkSpec
import org.zstack.testlib.SubCase
import org.zstack.utils.gson.JSONObjectUtil

/**
 * Created by xing5 on 2017/2/26.
 */
class OneVmDhcp extends SubCase {
    EnvSpec env

    VmInstanceInventory vm
    String dhcpServerIp
    String dhcpServerIpUuid
    L3NetworkInventory l3

    @Override
    void setup() {
        useSpring(FlatNetworkProviderTest.springSpec)
    }

    @Override
    void environment() {
        env = Env.oneHostNoVmEnv()
    }

    @Override
    void test() {
        env.create {
            l3 = (env.specByName("l3") as L3NetworkSpec).inventory

            testSetDhcpWhenCreateVm()
        }
    }

    void testSetDhcpWhenCreateVm() {
        FlatDhcpBackend.ApplyDhcpCmd cmd = null

        env.afterSimulator(FlatDhcpBackend.APPLY_DHCP_PATH) { rsp, HttpEntity<String> e ->
            cmd = JSONObjectUtil.toObject(e.body, FlatDhcpBackend.ApplyDhcpCmd.class)
            return rsp
        }

        FlatDhcpBackend.PrepareDhcpCmd pcmd = null
        env.afterSimulator(FlatDhcpBackend.PREPARE_DHCP_PATH) { rsp, HttpEntity<String> e ->
            pcmd = JSONObjectUtil.toObject(e.body, FlatDhcpBackend.PrepareDhcpCmd.class)
            return rsp
        }

        ImageSpec image = env.specByName("image")
        InstanceOfferingSpec instanceOffering = env.specByName("instanceOffering")

        vm = createVmInstance {
            name = "vm"
            imageUuid = image.inventory.uuid
            l3NetworkUuids = [l3.uuid]
            instanceOfferingUuid = instanceOffering.inventory.uuid
        }

        // check ApplyDhcpCmd
        assert cmd != null
        assert cmd.l3NetworkUuid == l3.uuid
        assert cmd.dhcp.size() == 1

        FlatDhcpBackend.DhcpInfo info = cmd.dhcp[0]
        VmNicInventory vmNic = vm.vmNics[0]
        assert vmNic.ip == info.ip
        assert vmNic.netmask == info.netmask
        assert vmNic.gateway == info.gateway
        assert info.isDefaultL3Network

        String brName = new BridgeNameFinder().findByL3Uuid(l3.uuid)
        assert brName == info.bridgeName
        assert info.namespaceName != null

        // check PrepareDhcpCmd
        assert pcmd != null
        assert pcmd.namespaceName != null
        assert pcmd.bridgeName == brName
        assert pcmd.dhcpNetmask == vmNic.netmask

        // check the DHCP IP
        // the DHCP server will occupy an IP
        def tokens = FlatNetworkSystemTags.L3_NETWORK_DHCP_IP.getTokensByResourceUuid(l3.uuid)
        dhcpServerIp = tokens.get(FlatNetworkSystemTags.L3_NETWORK_DHCP_IP_TOKEN)
        dhcpServerIpUuid = tokens.get(FlatNetworkSystemTags.L3_NETWORK_DHCP_IP_UUID_TOKEN)
        assert dhcpServerIp == pcmd.dhcpServerIp

        UsedIpVO dhcpIpVO = dbFindByUuid(dhcpServerIpUuid, UsedIpVO.class)
        assert dhcpIpVO != null
        assert dhcpIpVO.ip == dhcpServerIp
        assert dhcpIpVO.netmask == vmNic.netmask
        assert dhcpIpVO.gateway == vmNic.gateway
        assert dhcpIpVO.l3NetworkUuid == l3.uuid
    }

    @Override
    void clean() {
        env.delete()
    }
}
