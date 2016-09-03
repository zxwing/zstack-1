package org.zstack.test.mevoco.alert;

import org.zstack.alert.AlarmCreator;
import org.zstack.alert.AlarmFactory;
import org.zstack.alert.AlarmFactoryIdentifiedByHypervisorType;
import org.zstack.alert.AlarmStruct;
import org.zstack.header.core.Completion;
import org.zstack.header.simulator.SimulatorConstant;
import org.zstack.alert.VmAlarmFactory;
import org.zstack.utils.DebugUtils;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by xing5 on 2016/9/3.
 */
public class SimulatorAlarmFactory implements AlarmFactory, AlarmFactoryIdentifiedByHypervisorType {
    public Map<String, AlarmStruct> alarms = new HashMap<>();

    @Override
    public AlarmCreator getAlarmCreator(AlarmStruct struct) {
        return new AlarmCreator() {
            @Override
            public void createAlarm(Completion completion) {
                String vmUuid = struct.getInventory().getLabel(VmAlarmFactory.LABEL_VM_UUID);
                DebugUtils.Assert(vmUuid != null, "why vmUuid is null");
                alarms.put(vmUuid, struct);
                completion.success();
            }
        };
    }

    @Override
    public String getSupportedHypervisorType() {
        return SimulatorConstant.SIMULATOR_HYPERVISOR_TYPE;
    }
}
