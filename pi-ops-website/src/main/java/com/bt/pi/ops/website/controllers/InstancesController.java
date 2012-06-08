package com.bt.pi.ops.website.controllers;

import javax.ws.rs.FormParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.stereotype.Component;

import com.bt.pi.app.common.entities.InstanceAction;
import com.bt.pi.app.common.entities.PublicIpAllocationIndex;
import com.bt.pi.app.common.entities.Region;
import com.bt.pi.app.common.entities.Regions;
import com.bt.pi.app.common.net.utils.IpAddressUtils;
import com.bt.pi.core.dht.BlockingDhtReader;
import com.bt.pi.core.id.PId;

@Component
@Path("/instances")
public class InstancesController extends ControllerBase {
	private static final Log LOG = LogFactory.getLog(InstancesController.class);
	private static final String INSTANCE_ID = "instanceid";
	private static final String IP_ADDRESS = "ipAddress";

	public InstancesController() {
	}

	@POST
	@Path("/{instanceid}/pause")
	public Response pauseInstance(@PathParam(INSTANCE_ID) final String instanceId) {
		LOG.debug(String.format("pauseInstance(%s)", instanceId));
		return pauseInstanceUsingHelper(instanceId);
	}

	@POST
	@Path("/pause")
	public Response pauseInstanceByIpAddress(@FormParam(IP_ADDRESS) String ipAddress) {
		LOG.debug(String.format("pauseInstanceByIpAddress(%s)", ipAddress));
		return processInstanceByIpAddress(ipAddress, InstanceAction.PAUSE);
	}

	@POST
	@Path("/unpause")
	public Response unPauseInstanceByIpAddress(@FormParam(IP_ADDRESS) String ipAddress) {
		LOG.debug(String.format("unPauseInstanceByIpAddress(%s)", ipAddress));
		return processInstanceByIpAddress(ipAddress, InstanceAction.UNPAUSE);
	}

	private Response processInstanceByIpAddress(String ipAddress, InstanceAction action) {
		Long ipAddressLong = IpAddressUtils.ipToLong(ipAddress);

		PId regionsId = getPiIdBuilder().getRegionsId();
		BlockingDhtReader blockingDhtReader = getDhtClientFactory().createBlockingReader();
		Regions regions = (Regions) blockingDhtReader.get(regionsId);
		for (Region region : regions.getRegions().values()) {
			LOG.debug(String.format("Looking up public ip addresses in region: %d", region.getRegionCode()));
			PId publicIpAddressId = getPiIdBuilder().getPId(PublicIpAllocationIndex.URL).forRegion(region.getRegionCode());
			blockingDhtReader = getDhtClientFactory().createBlockingReader();
			PublicIpAllocationIndex publicIpAddressAllocationIndex = (PublicIpAllocationIndex) blockingDhtReader.get(publicIpAddressId);
			if (publicIpAddressAllocationIndex != null && publicIpAddressAllocationIndex.getAllocationMap().containsKey(ipAddressLong)) {
				if (InstanceAction.PAUSE.equals(action))
					return pauseInstanceUsingHelper(publicIpAddressAllocationIndex.getAllocationMap().get(ipAddressLong).getInstanceId());
				if (InstanceAction.UNPAUSE.equals(action))
					return unpauseInstanceUsingHelper(publicIpAddressAllocationIndex.getAllocationMap().get(ipAddressLong).getInstanceId());
			}
		}

		LOG.warn("Unable to find instance with ip:" + ipAddress);
		return Response.status(Status.NOT_FOUND).build();
	}

	@POST
	@Path("/{instanceid}/unpause")
	public Response unPauseInstance(@PathParam(INSTANCE_ID) final String instanceId) {
		LOG.debug(String.format("unpauseInstance(%s)", instanceId));
		return unpauseInstanceUsingHelper(instanceId);
	}
}