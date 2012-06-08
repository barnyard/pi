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
@Path("/500")
public class InternalServerErrorController {
	private static final int FIVE_HUNDRED = 500;

	public InternalServerErrorController() {
	}

	@GET
	@Produces(MediaType.TEXT_HTML)
	public Viewable get500AsHtml() {
		Map<String, Object> model = new HashMap<String, Object>();
		return new Viewable("500", model);
	}

	@GET
	@Produces( { MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
	public Response get500AsEntity() {
		return Response.status(FIVE_HUNDRED).entity(new SimpleErrorMessageEntity("An internal error has occurred")).build();
	}
}
