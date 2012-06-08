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
import com.bt.pi.ops.website.reporting.LogMessageRetriever;

@Component
@Path("/availabilityzones/{availabilityzone}/alerts")
public class AlertsController extends ControllerBase {
	private static final Log LOG = LogFactory.getLog(AlertsController.class);

	@Resource
	private LogMessageRetriever logMessageRetriever;

	public AlertsController() {
		logMessageRetriever = null;
	}

	@GET
	@Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
	public String getAlerts(@PathParam("availabilityzone") String availabilityZoneName) {
		LOG.debug(String.format("getAlerts(%s)", availabilityZoneName));

		PId availabilityZonesId = getPiIdBuilder().getAvailabilityZonesId();
		AvailabilityZones availabilityZones = getBlockingDhtCache().get(availabilityZonesId);
		AvailabilityZone availabilityZone = availabilityZones.getAvailabilityZoneByName(availabilityZoneName);
		if (availabilityZone == null)
			throw new IllegalArgumentException(String.format("Availability zone %s not found", availabilityZoneName));

		return getKoalaJsonParser().getJson(logMessageRetriever.getAllLogMessages(availabilityZone.getRegionCode(), availabilityZone.getAvailabilityZoneCodeWithinRegion()));
	}
}
