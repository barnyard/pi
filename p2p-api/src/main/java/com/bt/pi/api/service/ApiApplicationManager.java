/* (c) British Telecommunications plc, 2009, All Rights Reserved */
package com.bt.pi.api.service;

import java.util.Arrays;
import java.util.List;

import javax.annotation.Resource;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.stereotype.Component;

import com.bt.pi.app.common.AbstractPublicManagedAddressingPiApplication;
import com.bt.pi.app.common.entities.AvailabilityZone;
import com.bt.pi.app.common.entities.AvailabilityZoneNotFoundException;
import com.bt.pi.app.common.entities.AvailabilityZones;
import com.bt.pi.app.common.entities.Region;
import com.bt.pi.app.common.entities.Regions;
import com.bt.pi.app.networkmanager.NetworkManagerApplication;
import com.bt.pi.core.application.MessageContext;
import com.bt.pi.core.application.activation.RegionScopedSharedRecordConditionalApplicationActivator;
import com.bt.pi.core.application.activation.SharedRecordConditionalApplicationActivator;
import com.bt.pi.core.conf.Property;
import com.bt.pi.core.dht.cache.BlockingDhtCache;
import com.bt.pi.sss.PisssApplicationManager;

@Component
public class ApiApplicationManager extends AbstractPublicManagedAddressingPiApplication {
    public static final String APPLICATION_NAME = "pi-api-manager";
    private static final Log LOG = LogFactory.getLog(ApiApplicationManager.class);

    private RegionScopedSharedRecordConditionalApplicationActivator applicationActivator;
    private int appPort;
    private BlockingDhtCache blockingDhtCache;

    public ApiApplicationManager() {
        super(APPLICATION_NAME);
        appPort = -1;
        blockingDhtCache = null;
    }

    @Property(key = "api.app.activation.check.period.secs", defaultValue = DEFAULT_ACTIVATION_CHECK_PERIOD_SECS)
    public void setActivationCheckPeriodSecs(int value) {
        super.setActivationCheckPeriodSecs(value);
    }

    @Property(key = "api.app.start.timeout.millis", defaultValue = DEFAULT_START_TIMEOUT_MILLIS)
    public void setStartTimeoutMillis(long value) {
        super.setStartTimeoutMillis(value);
    }

    @Property(key = "api.port", defaultValue = "8773")
    public void setPort(int value) {
        appPort = value;
    }

    @Override
    protected int getPort() {
        return appPort;
    }

    @Resource(name = "generalBlockingCache")
    public void setBlockingDhtCache(BlockingDhtCache aBlockingDhtCache) {
        blockingDhtCache = aBlockingDhtCache;
    }

    @Resource(type = RegionScopedSharedRecordConditionalApplicationActivator.class)
    public void setSharedRecordConditionalApplicationActivator(RegionScopedSharedRecordConditionalApplicationActivator aSharedRecordConditionalApplicationActivator) {
        applicationActivator = aSharedRecordConditionalApplicationActivator;
    }

    @Override
    public SharedRecordConditionalApplicationActivator getActivatorFromApplication() {
        return applicationActivator;
    }

    @Override
    public boolean becomeActive() {
        LOG.debug("becomeActive()");

        if (!callSuperBecomeActive()) {
            LOG.error("Activation failed for API app");
            return false;
        }

        return true;
    }

    protected boolean callSuperBecomeActive() {
        return super.becomeActive();
    }

    @Override
    public void handleNodeDeparture(String nodeId) {
        removeNodeIdFromApplicationRecord(nodeId);
    }

    public AvailabilityZone getAvailabilityZoneByName(String availabilityZoneName) {
        LOG.debug(String.format("getAvailabilityZoneByName(%s)", availabilityZoneName));
        AvailabilityZones availabilityZones = getAvailabilityZonesRecord();
        return availabilityZones.getAvailabilityZoneByName(availabilityZoneName);
    }

    public AvailabilityZone getAvailabilityZoneByGlobalAvailabilityZoneCode(int globalAvailabilityZoneCode) {
        LOG.debug(String.format("getAvailabilityZoneByGlobalAvailabilityZoneCode(%s)", globalAvailabilityZoneCode));
        AvailabilityZones availabilityZones = getAvailabilityZonesRecord();
        return availabilityZones.getAvailabilityZoneByGlobalAvailabilityZoneCode(globalAvailabilityZoneCode);
    }

    public AvailabilityZones getAvailabilityZonesRecord() {
        AvailabilityZones availabilityZones = (AvailabilityZones) blockingDhtCache.get(getAvailabilityZonesId());
        if (availabilityZones == null)
            throw new AvailabilityZoneNotFoundException(String.format("Could not look up availability zones record when trying to look up an availability zone"));
        return availabilityZones;
    }

    public Region getRegion(String regionName) {
        LOG.debug(String.format("getRegion(%s)", regionName));
        Regions regions = getRegions();
        if (regions != null) {
            return regions.getRegion(regionName);
        }
        return null;
    }

    public Regions getRegions() {
        return (Regions) blockingDhtCache.get(getRegionsId());
    }

    @Override
    public List<String> getPreferablyExcludedApplications() {

        return Arrays.asList(PisssApplicationManager.APPLICATION_NAME, NetworkManagerApplication.APPLICATION_NAME);
    }

    public MessageContext getMessageContext() {
        return new MessageContext(this);
    }

}
