package com.bt.pi.ops.website.controllers;

import java.util.Arrays;
import java.util.Map;

import javax.annotation.Resource;
import javax.ws.rs.DELETE;
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
import com.bt.pi.app.instancemanager.handlers.InstanceStateTransition;
import com.bt.pi.app.instancemanager.handlers.TerminateInstanceServiceHelper;
import com.bt.pi.core.id.PId;
import com.bt.pi.ops.website.reporting.InstancesRetriever;

@Component
@Path("/availabilityzones/{availabilityzone}/instances")
public class AvailabilityZoneInstancesStatusController extends ControllerBase {
	private static final Log LOG = LogFactory.getLog(AvailabilityZoneInstancesStatusController.class);
	private static final String AVAILABILITY_ZONE = "availabilityzone";

	@Resource
	private InstancesRetriever instancesRetriever;
	@Resource
	private TerminateInstanceServiceHelper terminateInstanceServiceHelper;

	public AvailabilityZoneInstancesStatusController() {
		instancesRetriever = null;
		terminateInstanceServiceHelper = null;
	}

	@GET
	@Path("/running")
	@Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
	public String getRunningInstances(@PathParam(AVAILABILITY_ZONE) String availabilityZoneName) {
		LOG.debug(String.format("getRunningInstances(%s)", availabilityZoneName));

		AvailabilityZone availabilityZone = getAvailabilityZone(availabilityZoneName);
		return getKoalaJsonParser().getJson(instancesRetriever.getAllRunningInstances(availabilityZone.getRegionCode(), availabilityZone.getAvailabilityZoneCodeWithinRegion()));
	}

	@GET
	@Path("/zombie")
	@Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
	public String getZombieInstances(@PathParam(AVAILABILITY_ZONE) String availabilityZoneName) {
		LOG.debug(String.format("getZombieInstances(%s)", availabilityZoneName));

		AvailabilityZone availabilityZone = getAvailabilityZone(availabilityZoneName);
		return getKoalaJsonParser().getJson(instancesRetriever.getAllZombieInstances(availabilityZone.getRegionCode(), availabilityZone.getAvailabilityZoneCodeWithinRegion()));
	}

	@DELETE
	@Path("/terminate/{instanceid}")
	@Produces({ MediaType.APPLICATION_JSON })
	public String terminateBuriedInstance(@PathParam("instanceid") final String instanceId) {
		LOG.debug(String.format("terminateBuriedInstance(%s)", instanceId));

		Map<String, InstanceStateTransition> buriedInstance = terminateInstanceServiceHelper.terminateBuriedInstance(Arrays.asList(new String[] { instanceId }));
		final InstanceStateTransition stateTransition = buriedInstance.get(instanceId);

		TerminateZombieInstanceStatus terminateZombieInstanceStatus = new TerminateZombieInstanceStatus(instanceId, stateTransition);
		String result = getKoalaJsonParser().getJson(terminateZombieInstanceStatus);

		LOG.debug(result);
		return result;
	}

	private AvailabilityZone getAvailabilityZone(String availabilityZoneName) {
		PId availabilityZonesId = getPiIdBuilder().getAvailabilityZonesId();
		AvailabilityZones availabilityZones = getBlockingDhtCache().get(availabilityZonesId);
		AvailabilityZone availabilityZone = availabilityZones.getAvailabilityZoneByName(availabilityZoneName);
		if (availabilityZone == null)
			throw new IllegalArgumentException(String.format("Availability zone %s not found", availabilityZoneName));
		return availabilityZone;
	}

	public static final class TerminateZombieInstanceStatus {
		private InstanceStateTransition stateTransition;
		private String instanceId;

		private TerminateZombieInstanceStatus(String anInstanceId, InstanceStateTransition aStateTransition) {
			this.stateTransition = aStateTransition;
			this.instanceId = anInstanceId;
		}

		public String getInstanceId() {
			return instanceId;
		}

		public String getOldState() {
			return stateTransition == null ? "" : stateTransition.getPreviousState().toString();
		}

		public String getNewState() {
			return stateTransition == null ? "" : stateTransition.getNextState().toString();
		}
	}
}
