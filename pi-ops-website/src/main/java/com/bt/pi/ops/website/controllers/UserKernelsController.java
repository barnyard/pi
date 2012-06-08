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

import com.bt.pi.api.service.ImageServiceImpl;
import com.bt.pi.api.service.ManagementImageService;
import com.bt.pi.app.common.entities.MachineType;

@Component
@Path("/users/{username}/kernels")
public class UserKernelsController extends ControllerBase {
	private static final Log LOG = LogFactory.getLog(UserKernelsController.class);
	private static final String USERNAME = "username";

	private ManagementImageService imageService;

	public UserKernelsController() {
		imageService = null;
	}

	@Resource
	public void setImageService(ImageServiceImpl theService) {
		imageService = theService;
	}

	@POST
	@Produces({ MediaType.APPLICATION_JSON })
	public String registerKernel(@PathParam(USERNAME) String username, @FormParam("image_manifest_location") String imageManifestLocation) {
		LOG.info(String.format("Registering image %s for user %s as a kernel", imageManifestLocation, username));
		String kernelId = imageService.registerImage(username, imageManifestLocation, MachineType.KERNEL);
		return String.format("{\"kernelId\":\"%s\"}", kernelId);
	}

	@DELETE
	@Path("/{kernelId}")
	@Produces({ MediaType.APPLICATION_JSON })
	public String deregisterKernel(@PathParam(USERNAME) String username, @PathParam("kernelId") String kernelId) {
		LOG.info(String.format("Deregistering kernel id %s for user %s", kernelId, username));
		boolean succeeded = imageService.deregisterImage(username, kernelId, MachineType.KERNEL);
		String res = succeeded ? "ok" : "failed";
		return String.format("{\"status\":\"%s\"}", res);
	}
}
