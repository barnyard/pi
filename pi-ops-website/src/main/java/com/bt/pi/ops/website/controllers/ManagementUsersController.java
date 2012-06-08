package com.bt.pi.ops.website.controllers;

import java.net.URI;
import java.util.Locale;

import javax.annotation.Resource;
import javax.ws.rs.DELETE;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.stereotype.Controller;

import com.bt.pi.app.common.entities.ManagementUser;
import com.bt.pi.app.common.entities.ManagementUsers;
import com.bt.pi.app.management.ManagementUsersSeeder;
import com.bt.pi.ops.website.entities.ReadOnlyManagementUser;
import com.bt.pi.ops.website.entities.ReadOnlyManagementUsers;

@Controller
@Path("/managementusers")
public class ManagementUsersController extends ControllerBase {

	private static final Log LOG = LogFactory.getLog(ManagementUsersController.class);
	private static final String USERNAME = "username";
	private static final String PASSWORD = "password";
	private static final String ROLES = "roles";
	private static final String SEMI_COLON = ";";
	private static final String SLASH_USERNAME = "/{username}";

	@Resource
	private ManagementUsersSeeder managementUsersSeeder;

	public ManagementUsersController() {
		managementUsersSeeder = null;
	}

	@POST
	public Response create(@FormParam(USERNAME) String username, @FormParam(PASSWORD) String password, @FormParam(ROLES) String roles) {
		LOG.debug(String.format("create(%s, %.3s..., %s)", username, password, roles));
		if (managementUsersSeeder.getUser(username) == null) {
			managementUsersSeeder.addUser(username, getEncodedPassword(password), roles.split(SEMI_COLON));
			return Response.created(URI.create(username.toLowerCase(Locale.getDefault()))).build();
		} else {
			return Response.status(Status.CONFLICT).build();
		}
	}

	@PUT
	@Path(SLASH_USERNAME)
	public Response update(@PathParam(USERNAME) String username, @FormParam(PASSWORD) String password, @FormParam(ROLES) String roles) {
		LOG.debug(String.format("update(%s, %.3s..., %s)", username, password, roles));
		if (managementUsersSeeder.getUser(username) != null) {
			managementUsersSeeder.addUser(username, getEncodedPassword(password), roles.split(SEMI_COLON));
			return Response.ok().build();
		} else {
			return Response.status(Status.NOT_FOUND).build();
		}
	}

	@GET
	@Produces(MediaType.APPLICATION_JSON)
	public Response readAll() {
		LOG.debug(String.format("readAll()"));
		ManagementUsers allManagementUsers = managementUsersSeeder.getAllUsers();

		return Response.ok(getKoalaJsonParser().getJson(new ReadOnlyManagementUsers(allManagementUsers)), MediaType.APPLICATION_JSON).build();
	}

	@GET
	@Path(SLASH_USERNAME)
	@Produces(MediaType.APPLICATION_JSON)
	public Response read(@PathParam(USERNAME) String username) {
		LOG.debug(String.format("read(%s)", username));
		ManagementUser user = managementUsersSeeder.getUser(username);
		if (user == null)
			return Response.status(Status.NOT_FOUND).build();

		return Response.ok(getKoalaJsonParser().getJson(new ReadOnlyManagementUser(user)), MediaType.APPLICATION_JSON).build();
	}

	@DELETE
	@Path(SLASH_USERNAME)
	public Response delete(@PathParam(USERNAME) String username) {
		LOG.debug(String.format("delete(%s)", username));
		if (managementUsersSeeder.getUser(username) != null) {
			managementUsersSeeder.deleteUser(username);
			return Response.ok().build();
		} else {
			return Response.status(Status.NOT_FOUND).build();
		}
	}
}
