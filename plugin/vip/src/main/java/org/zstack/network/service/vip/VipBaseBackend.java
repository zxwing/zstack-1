package org.zstack.network.service.vip;

import org.zstack.header.core.Completion;

/**
 * Created by xing5 on 2016/11/20.
 */
public abstract class VipBaseBackend extends VipBase {
    public VipBaseBackend(VipVO self) {
        super(self);
    }

    protected abstract void releaseVipOnBackend(ReleaseVipStruct s, Completion completion);
    protected abstract void acquireVipOnBackend(AcquireVipStruct s, Completion completion);
}
