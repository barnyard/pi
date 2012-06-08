package com.bt.pi.ops.website.controllers.errorpages;

import java.util.HashMap;
import java.util.Map;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.springframework.stereotype.Component;

import com.bt.pi.ops.website.entities.SimpleErrorMessageEntity;
import com.sun.jersey.api.view.Viewable;

@Component
@Path("/403")
public class NotAuthorizedController {
	private static final int FOUR_HUNDRED_AND_THREE = 403;

	public NotAuthorizedController() {
	}

	@GET
	@Produces(MediaType.TEXT_HTML)
	public Viewable get403AsHtml() {
		Map<String, Object> model = new HashMap<String, Object>();
		return new Viewable("403", model);
	}

	@GET
	@Produces( { MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
	public Response get403AsEntity() {
		return Response.status(FOUR_HUNDRED_AND_THREE).entity(new SimpleErrorMessageEntity("Access to this resource is forbidden")).build();
	}
}
