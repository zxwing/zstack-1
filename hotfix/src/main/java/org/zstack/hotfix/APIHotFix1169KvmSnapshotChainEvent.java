package org.zstack.hotfix;

import org.zstack.header.message.APIEvent;

/**
 * Created by xing5 on 2016/10/25.
 */
public class APIHotFix1169KvmSnapshotChainEvent extends APIEvent {
    private HotFix1169Result result;

    public APIHotFix1169KvmSnapshotChainEvent() {
    }

    public APIHotFix1169KvmSnapshotChainEvent(String apiId) {
        super(apiId);
    }

    public HotFix1169Result getResult() {
        return result;
    }

    public void setResult(HotFix1169Result result) {
        this.result = result;
    }
}
