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
import com.bt.pi.core.id.PId;
import com.bt.pi.ops.website.reporting.HeartbeatRetriever;

@Component
@Path("/availabilityzones/{availabilityzone}/heartbeats")
public class HeartbeatsController extends ControllerBase {
	private static final Log LOG = LogFactory.getLog(HeartbeatsController.class);

	@Resource
	private HeartbeatRetriever heartbeatRetriever;

	public HeartbeatsController() {
		heartbeatRetriever = null;
	}

	@GET
	@Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
	public String getHeartbeats(@PathParam("availabilityzone") String availabilityZoneName) {
		LOG.debug(String.format("getHeartbeats(%s)", availabilityZoneName));

		PId availabilityZonesId = getPiIdBuilder().getAvailabilityZonesId();
		AvailabilityZones availabilityZones = getBlockingDhtCache().get(availabilityZonesId);
		AvailabilityZone availabilityZone = availabilityZones.getAvailabilityZoneByName(availabilityZoneName);
		if (availabilityZone == null)
			throw new IllegalArgumentException(String.format("Availability zone %s not found", availabilityZoneName));

		return getKoalaJsonParser().getJson(heartbeatRetriever.getAllHeartbeats(availabilityZone.getRegionCode(), availabilityZone.getAvailabilityZoneCodeWithinRegion()));
	}
}
