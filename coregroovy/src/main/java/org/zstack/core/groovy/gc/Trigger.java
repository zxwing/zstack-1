package org.zstack.core.groovy.gc;

import java.util.Map;

/**
 * Created by xing5 on 2017/3/3.
 */
public interface Trigger {
    void run(Map<String, String> tokens, Object data);
}
