/* (c) British Telecommunications plc, 2010, All Rights Reserved */
package com.bt.pi.ops.website.controllers;

import javax.annotation.Resource;
import javax.ws.rs.DELETE;
import javax.ws.rs.FormParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.stereotype.Component;

import com.bt.pi.api.service.ManagementImageService;
import com.bt.pi.app.common.entities.MachineType;

@Component
@Path("/users/{username}/ramdisks")
public class UserRamdisksController extends ControllerBase {
	private static final Log LOG = LogFactory.getLog(UserRamdisksController.class);
	private static final String USERNAME = "username";

	private ManagementImageService imageService;

	public UserRamdisksController() {
		imageService = null;
	}

	@Resource
	public void setImageService(ManagementImageService theService) {
		imageService = theService;
	}

	@POST
	@Produces({ MediaType.APPLICATION_JSON })
	public String registerRamdisk(@PathParam(USERNAME) String username, @FormParam("image_manifest_location") String imageManifestLocation) {
		LOG.info(String.format("Registering image %s for user %s as a ramdisk", imageManifestLocation, username));
		String ramdiskId = imageService.registerImage(username, imageManifestLocation, MachineType.RAMDISK);
		return String.format("{\"ramdiskId\":\"%s\"}", ramdiskId);
	}

	@DELETE
	@Path("/{ramdiskId}")
	@Produces({ MediaType.APPLICATION_JSON })
	public String deregisterRamdisk(@PathParam(USERNAME) String username, @PathParam("ramdiskId") String ramdiskId) {
		LOG.info(String.format("Deregistering ramdisk id %s for user %s", ramdiskId, username));
		boolean succeeded = imageService.deregisterImage(username, ramdiskId, MachineType.RAMDISK);
		String res = succeeded ? "ok" : "failed";
		return String.format("{\"status\":\"%s\"}", res);
	}
}
