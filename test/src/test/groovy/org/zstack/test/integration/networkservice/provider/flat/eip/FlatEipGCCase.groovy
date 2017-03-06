package org.zstack.test.integration.networkservice.provider.flat.eip

import org.zstack.core.gc.GCStatus
import org.zstack.network.service.flat.FlatEipBackend
import org.zstack.sdk.EipInventory
import org.zstack.sdk.GarbageCollectorInventory
import org.zstack.sdk.HostInventory
import org.zstack.test.integration.networkservice.provider.NetworkServiceProviderTest
import org.zstack.test.integration.networkservice.provider.flat.NetworkServiceEnv
import org.zstack.testlib.EipSpec
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.HostSpec
import org.zstack.testlib.HttpError
import org.zstack.testlib.SubCase

import java.util.concurrent.TimeUnit

/**
 * Created by xing5 on 2017/3/6.
 */
class FlatEipGCCase extends SubCase {
    EnvSpec env

    EipInventory eip
    HostInventory host

    @Override
    void clean() {
        env.delete()
    }

    @Override
    void setup() {
        useSpring(NetworkServiceProviderTest.springSpec)
    }

    @Override
    void environment() {
        env = NetworkServiceEnv.oneFlatEipEnv()
    }


    void testGCSuccess() {
        env.afterSimulator(FlatEipBackend.DELETE_EIP_PATH) {
            throw new HttpError(403, "on purpose")
        }

        deleteEip {
            uuid = eip.uuid
        }

        GarbageCollectorInventory inv = queryGCJob {
            conditions=["context~=%${eip.guestIp}%".toString()]
        }[0]

        assert inv.status == GCStatus.Idle.toString()

        boolean called = false
        env.afterSimulator(FlatEipBackend.BATCH_DELETE_EIP_PATH) { rsp ->
            called = true
            return rsp
        }

        // trigger the GC
        reconnectHost {
            uuid = host.uuid
        }

        TimeUnit.SECONDS.sleep(2)

        assert called

        inv = queryGCJob {
            conditions=["context~=%${eip.guestIp}%".toString()]
        }[0]

        assert inv.status == GCStatus.Done.toString()

        // clean the GC job so it won't effect following cases
        deleteGCJob {
            uuid = inv.uuid
        }
    }

    void testGCCancelledAfterHostDeleted() {
        env.afterSimulator(FlatEipBackend.DELETE_EIP_PATH) {
            throw new HttpError(403, "on purpose")
        }

        deleteEip {
            uuid = eip.uuid
        }

        GarbageCollectorInventory inv = queryGCJob {
            conditions=["context~=%${eip.guestIp}%".toString()]
        }[0]

        assert inv.status == GCStatus.Idle.toString()

        boolean called = false
        env.afterSimulator(FlatEipBackend.BATCH_DELETE_EIP_PATH) { rsp ->
            called = true
            return rsp
        }

        deleteHost {
            uuid = host.uuid
        }

        TimeUnit.SECONDS.sleep(2)

        assert !called

        inv = queryGCJob {
            conditions=["context~=%${eip.guestIp}%".toString()]
        }[0]

        assert inv.status == GCStatus.Done.toString()
    }

    @Override
    void test() {
        env.create {
            eip = (env.specByName("eip") as EipSpec).inventory
            host = (env.specByName("kvm") as HostSpec).inventory

            testGCSuccess()

            eip = (env.recreate("eip") as EipSpec).inventory

            testGCCancelledAfterHostDeleted()
        }
    }
}
