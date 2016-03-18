package org.zstack.core.simulator;

/**
 * NOTE: only for unit test
 */
public interface BeforeDeliverResponseInterceptor<T> {
    void beforeDeliverResponse(T rsp);
}
