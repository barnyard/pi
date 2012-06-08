/* (c) British Telecommunications plc, 2009, All Rights Reserved */
package com.bt.pi.api.service;

import javax.annotation.Resource;

import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import com.bt.pi.app.common.entities.AvailabilityZone;
import com.bt.pi.app.common.entities.Region;
import com.bt.pi.core.dht.cache.BlockingDhtCache;
import com.bt.pi.core.id.KoalaIdFactory;

public class ServiceHelperBase extends ServiceBaseImpl {
    private BlockingDhtCache blockingDhtCache;
    @Resource
    private ThreadPoolTaskExecutor taskExecutor;
    @Resource
    private KoalaIdFactory koalaIdFactory;

    public ServiceHelperBase() {
        super();
        this.blockingDhtCache = null;
        this.taskExecutor = null;
        this.koalaIdFactory = null;
    }

    protected BlockingDhtCache getBlockingDhtCache() {
        return blockingDhtCache;
    }

    protected ThreadPoolTaskExecutor getTaskExecutor() {
        return taskExecutor;
    }

    @Resource(name = "userBlockingCache")
    public void setBlockingDhtCache(BlockingDhtCache aBlockingDhtCache) {
        this.blockingDhtCache = aBlockingDhtCache;
    }

    protected int getGlobalAvailabilityZoneCodeFromAvailabilityZoneName(String availabilityZoneName) {
        AvailabilityZone instanceZone = getAvailabilityZoneByName(availabilityZoneName);
        return instanceZone.getGlobalAvailabilityZoneCode();
    }

    protected AvailabilityZone getAvailabilityZoneByName(String availabilityZoneName) {
        return getApiApplicationManager().getAvailabilityZoneByName(availabilityZoneName);
    }

    protected Region getRegionByName(String regionName) {
        return getApiApplicationManager().getRegion(regionName);
    }

    protected AvailabilityZone getLocalAvailabilityZone() {
        int globalAvzCodeForLocalAvz = koalaIdFactory.getGlobalAvailabilityZoneCode();
        return getApiApplicationManager().getAvailabilityZoneByGlobalAvailabilityZoneCode(globalAvzCodeForLocalAvz);
    }
}
