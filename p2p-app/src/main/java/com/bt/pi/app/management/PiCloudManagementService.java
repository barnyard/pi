package com.bt.pi.app.management;

import javax.annotation.Resource;

import org.springframework.jmx.export.annotation.ManagedOperation;
import org.springframework.jmx.export.annotation.ManagedOperationParameter;
import org.springframework.jmx.export.annotation.ManagedOperationParameters;
import org.springframework.jmx.export.annotation.ManagedResource;
import org.springframework.stereotype.Component;

import com.bt.pi.app.common.entities.AvailabilityZone;
import com.bt.pi.app.common.entities.AvailabilityZones;
import com.bt.pi.app.common.entities.Region;
import com.bt.pi.app.common.entities.Regions;
import com.bt.pi.app.common.id.PiIdBuilder;
import com.bt.pi.core.continuation.UpdateResolver;
import com.bt.pi.core.dht.BlockingDhtWriter;
import com.bt.pi.core.dht.DhtClientFactory;
import com.bt.pi.core.id.PId;

@Component
@ManagedResource(description = "Helper service for managing and servicing the the cloud", objectName = "bean:name=piCloudManagementService")
public class PiCloudManagementService {

    private static final String REGION_CODE = "regionCode";
    private DhtClientFactory dhtClientFactory;
    private PiIdBuilder piIdBuilder;

    public PiCloudManagementService() {
        dhtClientFactory = null;
        piIdBuilder = null;
    }

    @Resource
    public void setDhtClientFactory(DhtClientFactory aDhtClientFactory) {
        this.dhtClientFactory = aDhtClientFactory;
    }

    @Resource
    public void setPiIdBuilder(PiIdBuilder aPiIdBuilder) {
        this.piIdBuilder = aPiIdBuilder;
    }

    @ManagedOperation(description = "Add a region to the regions list")
    @ManagedOperationParameters({ @ManagedOperationParameter(name = "regionName", description = "Name of the region to be created (should be unique)"),
            @ManagedOperationParameter(name = REGION_CODE, description = "Code of region to be added (should be unique)"), @ManagedOperationParameter(name = "regionEndpoint", description = "Endpoint of region to be created"),
            @ManagedOperationParameter(name = "pisssEndpoint", description = "PI-SSS Endpoint of region to be created") })
    public String addRegion(String regionName, int regionCode, String regionEndpoint, String pisssEndpoint) {
        final Region region = new Region(regionName, regionCode, regionEndpoint, pisssEndpoint);
        BlockingDhtWriter writer = dhtClientFactory.createBlockingWriter();
        PId regionsId = piIdBuilder.getRegionsId();
        writer.update(regionsId, null, new UpdateResolver<Regions>() {
            @Override
            public Regions update(Regions existingEntity, Regions requestedEntity) {
                existingEntity.addRegion(region);
                return existingEntity;
            }
        });
        return writer.getValueWritten() == null ? null : writer.getValueWritten().toString();
    }

    @ManagedOperation(description = "Add a availability zone to the availability zones list")
    @ManagedOperationParameters({ @ManagedOperationParameter(name = "availabilityZoneName", description = "Name of the availability zone to be created (should be unique)"),
            @ManagedOperationParameter(name = "availabilityZoneCode", description = "Code of availability zone to be added (should be unique)"),
            @ManagedOperationParameter(name = REGION_CODE, description = "Name of the region the availabilty zone belongs to."), @ManagedOperationParameter(name = "status", description = "Availability zone status") })
    public String addAvailabilityZone(String anAvailabilityZoneName, int anAvailabilityZoneCode, int aRegionCode, String aStatus) {

        final AvailabilityZone availabilityZone = new AvailabilityZone(anAvailabilityZoneName, anAvailabilityZoneCode, aRegionCode, aStatus);
        BlockingDhtWriter writer = dhtClientFactory.createBlockingWriter();
        PId availabilityZonesId = piIdBuilder.getAvailabilityZonesId();
        writer.update(availabilityZonesId, null, new UpdateResolver<AvailabilityZones>() {
            @Override
            public AvailabilityZones update(AvailabilityZones existingEntity, AvailabilityZones requestedEntity) {
                existingEntity.addAvailabilityZone(availabilityZone);
                return existingEntity;
            }
        });
        return writer.getValueWritten() == null ? null : writer.getValueWritten().toString();
    }
}
