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
@Path("/404")
public class NotFoundController {
	private static final int FOUR_HUNDRED_AND_FOUR = 404;

	public NotFoundController() {
	}

	@GET
	@Produces(MediaType.TEXT_HTML)
	public Viewable get404AsHtml() {
		Map<String, Object> model = new HashMap<String, Object>();
		return new Viewable("404", model);
	}

	@GET
	@Produces( { MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
	public Response get404AsEntity() {
		return Response.status(FOUR_HUNDRED_AND_FOUR).entity(new SimpleErrorMessageEntity("Resource not found")).build();
	}
}
