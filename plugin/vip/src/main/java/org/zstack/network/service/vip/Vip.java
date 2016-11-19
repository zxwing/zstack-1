package org.zstack.network.service.vip;

import org.zstack.header.message.Message;

/**
 * Created by xing5 on 2016/11/19.
 */
public interface Vip {
    void handleMessage(Message msg);
}
