package com.bt.pi.ops.website.controllers;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.stereotype.Controller;

import com.bt.pi.app.common.entities.ManagementRoles;

@Controller
@Path("/managementroles")
public class ManagementRolesController extends ControllerBase {

	private static final Log LOG = LogFactory.getLog(ManagementRolesController.class);

	public ManagementRolesController() {
	}

	@GET
	@Produces(MediaType.APPLICATION_JSON)
	public Response readAll() {
		LOG.debug(String.format("readAll()"));

		return Response.ok(getKoalaJsonParser().getJson(ManagementRoles.values()), MediaType.APPLICATION_JSON).build();
	}
}
