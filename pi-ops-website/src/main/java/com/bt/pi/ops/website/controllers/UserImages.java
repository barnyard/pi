/* (c) British Telecommunications plc, 2010, All Rights Reserved */
package com.bt.pi.ops.website.controllers;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.Resource;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.stereotype.Component;

import com.bt.pi.api.service.ManagementImageService;
import com.bt.pi.api.service.UserNotFoundException;
import com.bt.pi.app.common.entities.Image;
import com.bt.pi.ops.website.entities.ReadOnlyImage;
import com.sun.jersey.api.NotFoundException;
import com.sun.jersey.api.view.Viewable;

@Component
@Path("/users/{username}/images")
public class UserImages extends ControllerBase {
	private static final Log LOG = LogFactory.getLog(UserImages.class);

	private static final String IMAGES = "images";
	private static final String USER_IMAGES = "user_images";
	private static final String IMAGEID = "imageid";
	private static final String USERNAME = "username";
	private static final String SLASH_IMAGEID = "/{imageid}";

	private ManagementImageService imageService;

	public UserImages() {
		imageService = null;
	}

	@Resource
	public void setImageService(ManagementImageService theService) {
		imageService = theService;
	}

	@GET
	@Produces(MediaType.TEXT_HTML)
	public Viewable getAllHtml(@PathParam(USERNAME) String username) {
		Map<String, Object> model = new HashMap<String, Object>();
		model.put(IMAGES, getAllImages(username));
		model.put(USERNAME, username);

		return new Viewable(USER_IMAGES, model);
	}

	@GET
	@Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
	public List<ReadOnlyImage> getAllImages(@PathParam(USERNAME) String username) {
		LOG.debug("Getting all images for user " + username);

		Set<Image> images;
		try {
			images = imageService.describeImages(username, null);
		} catch (UserNotFoundException e) {
			throw new NotFoundException();
		}

		List<ReadOnlyImage> readOnlyImages = new ArrayList<ReadOnlyImage>(images.size());

		for (Image image : images) {
			readOnlyImages.add(new ReadOnlyImage(image));
		}

		return readOnlyImages;
	}

	@Path(SLASH_IMAGEID)
	@Produces(MediaType.TEXT_HTML)
	@GET
	public Viewable getImageHtml(@PathParam(USERNAME) String username, @PathParam(IMAGEID) String imageid) {

		Map<String, Object> model = new HashMap<String, Object>();
		model.put(IMAGES, Arrays.asList(getImage(username, imageid)));
		model.put(USERNAME, username);

		return new Viewable(USER_IMAGES, model);
	}

	@Path(SLASH_IMAGEID)
	@Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
	@GET
	public ReadOnlyImage getImage(@PathParam(USERNAME) String username, @PathParam(IMAGEID) String imageid) {
		checkNotNullOrEmpty(username);
		// String s = IMAGEID;
		LOG.debug("Getting user " + username);

		Set<Image> images;
		try {
			images = imageService.describeImages(username, Arrays.asList(imageid));
		} catch (UserNotFoundException e) {
			throw new NotFoundException();
		}
		return new ReadOnlyImage(images.iterator().next());
	}
}
