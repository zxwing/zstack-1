package org.zstack.compute.allocator;

import org.zstack.header.allocator.HostCapacityOverProvisioningManager;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by frank on 10/19/2015.
 */
public class HostCapacityOverProvisioningManagerImpl implements HostCapacityOverProvisioningManager {
    private double globalMemoryRatio = 1;
    private ConcurrentHashMap<String, Double> hostMemoryRatio = new ConcurrentHashMap<String, Double>();
    private long globalCpuRatio = 1;
    private ConcurrentHashMap<String, Long> hostCpuRatio = new ConcurrentHashMap<String, Long>();

    @Override
    public void setMemoryGlobalRatio(double ratio) {
        globalMemoryRatio = ratio;
    }

    @Override
    public double getMemoryGlobalRatio() {
        return globalMemoryRatio;
    }

    @Override
    public void setMemoryRatio(String hostUuid, double ratio) {
        hostMemoryRatio.put(hostUuid, ratio);
    }

    @Override
    public void deleteMemoryRatio(String hostUuid) {
        hostMemoryRatio.remove(hostUuid);
    }

    @Override
    public double getMemoryRatio(String hostUuid) {
        Double ratio =  hostMemoryRatio.get(hostUuid);
        ratio = ratio == null ? globalMemoryRatio : ratio;
        return ratio;
    }

    @Override
    public Map<String, Double> getAllMemoryRatio() {
        return hostMemoryRatio;
    }

    @Override
    public long calculateMemoryByRatio(String hostUuid, long capacity) {
        double ratio = getMemoryRatio(hostUuid);
        return Math.round(capacity / ratio);
    }

    @Override
    public long calculateHostAvailableMemoryByRatio(String hostUuid, long capacity) {
        double ratio = getMemoryRatio(hostUuid);
        return Math.round(capacity * ratio);
    }

    @Override
    public void setCpuGlobalRatio(long ratio) {
        globalCpuRatio = ratio;
    }

    @Override
    public long getCpuGlobalRatio() {
        return globalCpuRatio;
    }

    @Override
    public void setCpuRatio(String hostUuid, long ratio) {
        hostCpuRatio.put(hostUuid, ratio);
    }

    @Override
    public void deleteCpuRatio(String hostUuid) {
        hostCpuRatio.remove(hostUuid);
    }

    @Override
    public long getCpuRatio(String hostUuid) {
        Long c = hostCpuRatio.get(hostUuid);
        return c == null ? globalCpuRatio :c;
    }

    @Override
    public long calculateHostCpuByRatio(String hostUuid, long capacity) {
        return capacity * getCpuRatio(hostUuid);
    }
}
