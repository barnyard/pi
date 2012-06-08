/* (c) British Telecommunications plc, 2010, All Rights Reserved */
package com.bt.pi.ops.website.controllers;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
@Path("/currentuser")
public class CurrentUserController extends ControllerBase {
	private static final Log LOG = LogFactory.getLog(CurrentUserController.class);

	public CurrentUserController() {
	}

	@GET
	@Produces( { MediaType.APPLICATION_JSON })
	public String getUserInfo() {
		LOG.debug(String.format("Getting current user info"));

		Authentication authentication = getAuthentication();
		String username = authentication.getName();

		StringBuilder rolesBuilder = new StringBuilder();
		for (GrantedAuthority ga : authentication.getAuthorities())
			rolesBuilder.append(",").append(ga.getAuthority());
		String roles = "";
		if (rolesBuilder.length() > 0)
			roles = rolesBuilder.substring(1);

		return String.format("{\"username\":\"%s\", \"roles\":\"%s\"}", username, roles);
	}

	protected Authentication getAuthentication() {
		return SecurityContextHolder.getContext().getAuthentication();
	}
}
