package org.zstack.test.integration.kvm.vm

import org.springframework.http.HttpEntity
import org.zstack.core.db.DatabaseFacade
import org.zstack.core.gc.GarbageCollectorVO
import org.zstack.header.vm.VmInstanceState
import org.zstack.header.vm.VmInstanceVO
import org.zstack.kvm.KVMAgentCommands
import org.zstack.kvm.KVMConstant
import org.zstack.sdk.DestroyVmInstanceAction
import org.zstack.sdk.VmInstanceInventory
import org.zstack.test.integration.kvm.Env
import org.zstack.test.integration.kvm.KvmTest
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.HttpError
import org.zstack.testlib.ImageSpec
import org.zstack.testlib.InstanceOfferingSpec
import org.zstack.testlib.L3NetworkSpec
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

    private VmInstanceInventory createGCCandidateVm() {
        def vm = createVmInstance {
            name = "the-vm"
            instanceOfferingUuid = (env.specByName("instanceOffering") as InstanceOfferingSpec).inventory.uuid
            imageUuid = (env.specByName("image1") as ImageSpec).inventory.uuid
            l3NetworkUuids = [(env.specByName("l3") as L3NetworkSpec).inventory.uuid]
        } as VmInstanceInventory

        env.afterSimulator(KVMConstant.KVM_DESTROY_VM_PATH) {
            throw new HttpError(403, "on purpose")
        }

        def a = new DestroyVmInstanceAction()
        a.uuid = vm.uuid
        a.sessionId = adminSession()
        DestroyVmInstanceAction.Result res = a.call()
        // because of the GC, confirm the VM is deleted
        assert res.error == null
        assert dbFindByUuid(vm.uuid, VmInstanceVO.class).state == VmInstanceState.Destroyed
        assert dbf.count(GarbageCollectorVO.class) != 0

        return vm
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
        a.sessionId = adminSession()
        DestroyVmInstanceAction.Result res = a.call()
        // because of the GC, confirm the VM is deleted
        assert res.error == null
        assert dbFindByUuid(vm.uuid, VmInstanceVO.class).state == VmInstanceState.Destroyed

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

        // confirm the destroy command is sent
        assert cmd != null
        assert cmd.uuid == vm.uuid
        assert dbf.count(GarbageCollectorVO.class) == 0
    }

    void testGCJobCancelAfterHostDelete() {
        VmInstanceInventory vm = createGCCandidateVm()

        deleteHost {
            uuid = vm.hostUuid
        }

        TimeUnit.SECONDS.sleep(1)

        //confirm the GC job cancelled
        assert dbf.count(GarbageCollectorVO.class) == 0
    }

    void testGCJobCancelAfterVmRecovered() {
        VmInstanceInventory vm = createGCCandidateVm()

        recoverVmInstance {
            uuid = vm.uuid
        }

        // the host reconnecting will trigger the GC
        reconnectHost {
            uuid = vm.hostUuid
        }

        KVMAgentCommands.DestroyVmCmd cmd = null
        env.afterSimulator(KVMConstant.KVM_DESTROY_VM_PATH) { rsp, HttpEntity<String> e ->
            cmd = json(e.body, KVMAgentCommands.DestroyVmCmd.class)
            return rsp
        }

        // no destroy command sent beacuse the vm is recovered
        assert cmd == null
        //confirm the GC job cancelled
        assert dbf.count(GarbageCollectorVO.class) == 0
    }

    @Override
    void test() {
        dbf = bean(DatabaseFacade.class)

        env.create {
            testDeleteVmWhenHostDisconnect()
            testGCJobCancelAfterVmRecovered()
            testGCJobCancelAfterHostDelete()
        }
    }

    @Override
    void clean() {
        env.delete()
    }
}
