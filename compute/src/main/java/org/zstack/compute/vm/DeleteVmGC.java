package org.zstack.compute.vm;

import org.zstack.core.cloudbus.CloudBusCallBack;
import org.zstack.core.gc.EventBasedGarbageCollector;
import org.zstack.core.gc.GCCompletion;
import org.zstack.header.host.HostCanonicalEvents;
import org.zstack.header.host.HostConstant;
import org.zstack.header.host.HostStatus;
import org.zstack.header.host.HostVO;
import org.zstack.header.message.MessageReply;
import org.zstack.header.vm.VmDirectlyDestroyOnHypervisorMsg;
import org.zstack.header.vm.VmInstanceInventory;
import org.zstack.header.vm.VmInstanceVO;

/**
 * Created by xing5 on 2017/3/3.
 */
public class DeleteVmGC extends EventBasedGarbageCollector {
    public String hostUuid;
    public VmInstanceInventory inventory;

    @Override
    protected void setup() {
        onEvent(HostCanonicalEvents.HOST_DELETED_PATH, (tokens, data) -> {
            HostCanonicalEvents.HostDeletedData d = (HostCanonicalEvents.HostDeletedData) data;
            return hostUuid.equals(d.getHostUuid());
        });

        onEvent(HostCanonicalEvents.HOST_STATUS_CHANGED_PATH, (tokens, data) -> {
            HostCanonicalEvents.HostStatusChangedData d = (HostCanonicalEvents.HostStatusChangedData) data;
            return d.getHostUuid().equals(hostUuid) && d.getNewStatus().equals(HostStatus.Connected.toString());
        });
    }

    @Override
    protected void triggerNow(GCCompletion completion) {
        if (!dbf.isExist(hostUuid, HostVO.class)) {
            completion.success();
            return;
        }

        if (!dbf.isExist(inventory.getUuid(), VmInstanceVO.class)) {
            completion.success();
            return;
        }

        VmDirectlyDestroyOnHypervisorMsg msg = new VmDirectlyDestroyOnHypervisorMsg();
        msg.setHostUuid(hostUuid);
        msg.setVmUuid(inventory.getUuid());
        bus.makeTargetServiceIdByResourceUuid(msg, HostConstant.SERVICE_ID, hostUuid);
        bus.send(msg, new CloudBusCallBack(completion) {
            @Override
            public void run(MessageReply reply) {
                if (reply.isSuccess()) {
                    completion.success();
                } else {
                    completion.fail(reply.getError());
                }
            }
        });
    }
}
