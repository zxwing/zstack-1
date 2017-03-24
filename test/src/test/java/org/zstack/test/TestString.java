package org.zstack.test;

import javassist.*;
import org.apache.logging.log4j.ThreadContext;
import org.junit.Test;
import org.zstack.core.Platform;
import org.zstack.header.core.workflow.Flow;
import org.zstack.header.core.workflow.FlowRollback;
import org.zstack.header.core.workflow.FlowTrigger;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import java.lang.reflect.InvocationTargetException;
import java.util.Map;

/**
 */
public class TestString {
    CLogger logger = Utils.getLogger(TestString.class);

    @Test
    public void test() throws InterruptedException, IllegalAccessException, NoSuchMethodException, InvocationTargetException, NotFoundException {
        ClassPool pool = ClassPool.getDefault();
        Flow flow = new Flow() {
            @Override
            public void run(FlowTrigger trigger, Map data) {
            }

            @Override
            public void rollback(FlowRollback trigger, Map data) {

            }
        };

        CtClass cc = pool.get(flow.getClass().getName());
        cc.stopPruning(true);
        CtMethod m = cc.getDeclaredMethod("run");
        System.out.println(String.format("xxxxxxxxx %s:%s", cc.getClassFile().getSourceFile(), m.getMethodInfo().getLineNumber(0)));
        //println("xxx ${m.getMethodInfo().getLineNumber(0)}")
    }
}
