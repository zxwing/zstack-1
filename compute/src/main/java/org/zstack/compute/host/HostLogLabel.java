package org.zstack.compute.host;

import org.zstack.core.logging.LogLabel;

/**
 * Created by xing5 on 2016/6/1.
 */
public class HostLogLabel {
    @LogLabel(messages = {
            "en_US = save information to the database",
            "zh_CN = 保存信息到数据库"
    })
    public static String ADD_HOST_WRITE_DB = "add.host.writeDb";
}
