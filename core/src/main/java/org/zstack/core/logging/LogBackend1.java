package org.zstack.core.logging;

/**
 */
public interface LogBackend1 {
    void write(LogVO log);

    String getLogBackendType();

    void start();

    void stop();
}
