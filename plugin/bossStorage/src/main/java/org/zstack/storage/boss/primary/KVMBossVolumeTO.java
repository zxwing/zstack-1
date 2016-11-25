package org.zstack.storage.boss.primary;

import org.zstack.kvm.KVMAgentCommands;

/**
 * Created by XXPS-PC1 on 2016/11/22.
 */
public class KVMBossVolumeTO extends KVMAgentCommands.VolumeTO {
    public KVMBossVolumeTO() {
    }

    public KVMBossVolumeTO(KVMAgentCommands.VolumeTO other) {
        super(other);
    }
}
