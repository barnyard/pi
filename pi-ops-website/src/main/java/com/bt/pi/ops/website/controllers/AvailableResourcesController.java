package com.bt.pi.ops.website.controllers;

import javax.annotation.Resource;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.stereotype.Component;

import com.bt.pi.app.common.entities.AvailabilityZone;
import com.bt.pi.app.common.entities.AvailabilityZones;
import com.bt.pi.app.common.entities.InstanceTypeConfiguration;
import com.bt.pi.app.common.entities.InstanceTypes;
import com.bt.pi.app.instancemanager.health.AvailableResourcesHeartBeatFiller;
import com.bt.pi.core.application.health.entity.HeartbeatEntity;
import com.bt.pi.core.application.health.entity.HeartbeatEntityCollection;
import com.bt.pi.core.id.PId;
import com.bt.pi.ops.website.entities.AvailableResources;
import com.bt.pi.ops.website.reporting.HeartbeatRetriever;

@Component
@Path("/resources/{availabilityzone}")
public class AvailableResourcesController extends ControllerBase {

	private static final int ONE_OH_TWO_FOUR = 1024;

	private static final Log LOG = LogFactory.getLog(AvailableResourcesController.class);

	@Resource
	private HeartbeatRetriever heartbeatRetriever;

	public AvailableResourcesController() {
	}

	@GET
	@Produces(MediaType.APPLICATION_JSON)
	public String getAvailableResources(@PathParam("availabilityzone") String availabilityZoneName) {
		AvailabilityZone availabilityZone = getAvailabilityZoneFromCache(availabilityZoneName);
		HeartbeatEntityCollection heartbeatEntityCollection = heartbeatRetriever.getAllHeartbeats(availabilityZone.getRegionCode(), availabilityZone.getAvailabilityZoneCodeWithinRegion());
		AvailableResources availableResources = getFreeResourcesInAvailabilityZone(heartbeatEntityCollection);

		return getKoalaJsonParser().getJson(availableResources);
	}

	private void calculateAndStoreAvailableInstances(AvailableResources availableResources, HeartbeatEntity heartbeatEntity, InstanceTypes instanceTypes) {
		LOG.debug(String.format("calculateAndStoreAvailableInstances(%s, %s, %s)", availableResources, heartbeatEntity, instanceTypes));
		for (InstanceTypeConfiguration instanceTypeConfiguration : instanceTypes.getInstanceTypes().values()) {
			if (instanceTypeConfiguration.isDeprecated())
				continue;
			if (instanceTypeConfiguration.getDiskSizeInGB() > 0 && instanceTypeConfiguration.getMemorySizeInMB() > 0 && instanceTypeConfiguration.getNumCores() > 0) {
				Long freeMemory = heartbeatEntity.getAvailableResources().get(AvailableResourcesHeartBeatFiller.FREE_MEMORY);
				long memoryNumber = null == freeMemory ? 0 : freeMemory / instanceTypeConfiguration.getMemorySizeInMB();

				Long freeDisk = heartbeatEntity.getAvailableResources().get(AvailableResourcesHeartBeatFiller.FREE_DISK);
				long diskNumber = null == freeDisk ? 0 : freeDisk / ((long) instanceTypeConfiguration.getDiskSizeInGB() * ONE_OH_TWO_FOUR);

				Long freeCores = heartbeatEntity.getAvailableResources().get(AvailableResourcesHeartBeatFiller.FREE_CORES);
				long coreNumber = null == freeCores ? 0 : freeCores / instanceTypeConfiguration.getNumCores();
				long numberOfInstances = Math.min(coreNumber, Math.min(memoryNumber, diskNumber));
				availableResources.addInstancesToType(instanceTypeConfiguration.getInstanceType(), numberOfInstances);
			}
		}
	}

	private AvailableResources getFreeResourcesInAvailabilityZone(HeartbeatEntityCollection heartbeatEntityCollection) {
		InstanceTypes instanceTypes = getInstanceTypes();
		AvailableResources result = new AvailableResources();
		long freeMemoryInMB = 0;
		long freeDiskInMb = 0;
		int freeCores = 0;
		for (HeartbeatEntity heartbeatEntity : heartbeatEntityCollection.getEntities()) {
			Long long1 = heartbeatEntity.getAvailableResources().get(AvailableResourcesHeartBeatFiller.FREE_MEMORY);
			if (null != long1)
				freeMemoryInMB += long1;

			Long long2 = heartbeatEntity.getAvailableResources().get(AvailableResourcesHeartBeatFiller.FREE_DISK);
			if (null != long2)
				freeDiskInMb += long2;

			Long long3 = heartbeatEntity.getAvailableResources().get(AvailableResourcesHeartBeatFiller.FREE_CORES);
			if (null != long3)
				freeCores += long3;

			if (instanceTypes != null)
				calculateAndStoreAvailableInstances(result, heartbeatEntity, instanceTypes);
		}
		result.setFreeCores(freeCores);
		result.setFreeDiskInMB(freeDiskInMb);
		result.setFreeMemoryInMB(freeMemoryInMB);
		LOG.debug(String.format("returning " + result));
		return result;
	}

	private AvailabilityZone getAvailabilityZoneFromCache(String availabilityZoneName) {
		PId availabilityZonesId = getPiIdBuilder().getAvailabilityZonesId();
		AvailabilityZones availabilityZones = getBlockingDhtCache().get(availabilityZonesId);
		AvailabilityZone availabilityZone = availabilityZones.getAvailabilityZoneByName(availabilityZoneName);
		if (availabilityZone == null)
			throw new IllegalArgumentException(String.format("Availability zone %s not found", availabilityZoneName));
		return availabilityZone;
	}

	private InstanceTypes getInstanceTypes() {
		PId instanceTypesId = getPiIdBuilder().getPId(InstanceTypes.URL_STRING);
		return getBlockingDhtCache().get(instanceTypesId);
	}
}
