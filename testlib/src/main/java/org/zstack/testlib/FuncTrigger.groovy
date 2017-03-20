package org.zstack.testlib

import java.util.concurrent.BlockingQueue
import java.util.concurrent.LinkedBlockingQueue

/**
 * Created by xing5 on 2017/3/23.
 */
class FuncTrigger {
    private BlockingQueue<String> queue = new LinkedBlockingQueue<>()
    private String quitObject = "quit"

    Closure func

    void trigger() {
        queue.add("")
    }

    void quit() {
        queue.add(quitObject)
    }

    void run() {
        assert func != null: "func cannot be null"

        while (true) {
            String token = queue.take()
            if (token == quitObject) {
                break
            }

            func()
        }
    }
}
