package org.zstack.test.integration.kvm.vm

import org.springframework.http.HttpEntity
import org.zstack.core.db.DatabaseFacade
import org.zstack.core.gc.GarbageCollectorVO
import org.zstack.header.vm.VmInstanceVO
import org.zstack.kvm.KVMAgentCommands
import org.zstack.kvm.KVMConstant
import org.zstack.sdk.DestroyVmInstanceAction
import org.zstack.sdk.VmInstanceInventory
import org.zstack.test.integration.kvm.Env
import org.zstack.test.integration.kvm.KvmTest
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.HttpError
import org.zstack.testlib.SubCase
import org.zstack.testlib.VmSpec

import java.util.concurrent.TimeUnit

/**
 * Created by xing5 on 2017/3/3.
 */
class VmGCCase extends SubCase {
    EnvSpec env

    DatabaseFacade dbf

    @Override
    void setup() {
        useSpring(KvmTest.springSpec)
    }

    @Override
    void environment() {
        env = Env.oneVmBasicEnv()
    }

    void testDeleteVmWhenHostDisconnect() {
        VmInstanceInventory vm = (env.specByName("vm") as VmSpec).inventory

        env.afterSimulator(KVMConstant.KVM_DESTROY_VM_PATH) {
            throw new HttpError(403, "on purpose")
        }

        def a = new DestroyVmInstanceAction()
        a.uuid = vm.uuid
        DestroyVmInstanceAction.Result res = a.call()
        assert res.error != null
        // because of the GC, confirm the VM is deleted
        assert !dbIsExists(vm.uuid, VmInstanceVO.class)

        KVMAgentCommands.DestroyVmCmd cmd = null
        env.afterSimulator(KVMConstant.KVM_DESTROY_VM_PATH) { rsp, HttpEntity<String> e ->
            cmd = json(e.body, KVMAgentCommands.DestroyVmCmd.class)
            return rsp
        }

        // the host reconnecting will trigger the GC
        reconnectHost {
            uuid = vm.hostUuid
        }

        TimeUnit.SECONDS.sleep(1)

        assert !dbIsExists(vm.uuid, VmInstanceVO.class)
        assert cmd.uuid == vm.uuid
        assert dbf.count(GarbageCollectorVO.class) == 0
    }

    @Override
    void test() {
        dbf = bean(DatabaseFacade.class)

        env.create {
            testDeleteVmWhenHostDisconnect()
        }
    }

    @Override
    void clean() {
        env.delete()
    }
}
