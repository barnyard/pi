package com.bt.pi.app.instancemanager.health;

import java.util.Map;

import javax.annotation.Resource;

import org.springframework.stereotype.Component;

import com.bt.pi.app.instancemanager.handlers.SystemResourceState;
import com.bt.pi.core.application.health.HeartBeatFiller;
import com.bt.pi.core.application.health.entity.HeartbeatEntity;

@Component
public class AvailableResourcesHeartBeatFiller implements HeartBeatFiller {
    public static final String FREE_DISK = "FreeDisk";
    public static final String FREE_MEMORY = "FreeMemory";
    public static final String FREE_CORES = "FreeCores";

    private SystemResourceState systemResourceState;

    public AvailableResourcesHeartBeatFiller() {
        systemResourceState = null;
    }

    @Override
    public HeartbeatEntity populate(HeartbeatEntity heartbeat) {
        Map<String, Long> availableResources = heartbeat.getAvailableResources();
        availableResources.put(FREE_MEMORY, systemResourceState.getFreeMemoryInMB());
        availableResources.put(FREE_CORES, (long) systemResourceState.getFreeCores());
        availableResources.put(FREE_DISK, systemResourceState.getFreeDiskInMB());
        return heartbeat;
    }

    @Resource
    public void setSystemResourceState(SystemResourceState aSystemResourceState) {
        this.systemResourceState = aSystemResourceState;
    }

}
