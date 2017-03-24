package org.zstack.core.workflow;

import org.zstack.header.core.workflow.Flow;
import org.zstack.header.core.workflow.FlowChain;

import java.util.Map;

/**
 * Created by xing5 on 2017/3/24.
 */
public interface BeforeFlowRunExtensionPoint {
    void beforeFlowRun(String flowName, Flow flow, FlowChain chain, Map data);
}
