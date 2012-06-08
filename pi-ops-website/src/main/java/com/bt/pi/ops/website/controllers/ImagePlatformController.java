package com.bt.pi.ops.website.controllers;

import javax.annotation.Resource;
import javax.ws.rs.FormParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.stereotype.Component;

import com.bt.pi.app.management.ImageSeeder;

@Component
@Path("/images/{imageid}/platform")
public class ImagePlatformController extends ControllerBase {
	private static final Log LOG = LogFactory.getLog(ImagePlatformController.class);
	@Resource
	private ImageSeeder imageSeeder;

	public ImagePlatformController() {
		imageSeeder = null;
	}

	@POST
	@Produces( { MediaType.APPLICATION_JSON })
	public String changeImagePlatform(@PathParam("imageid") String imageId, @FormParam("image_platform") String imagePlatform) {
		LOG.debug(String.format("Changing image platform for image id: %s to %s", imageId, imagePlatform));
		imageSeeder.updateImagePlatform(imageId, imagePlatform);
		return imageId;
	}
}
