package com.bt.pi.ops.website.controllers;

import javax.annotation.Resource;
import javax.ws.rs.FormParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.stereotype.Component;

import com.bt.pi.app.management.SuperNodeSeeder;
import com.bt.pi.core.application.reporter.ReportingApplication;

@Component
@Path("/supernodes")
public class SupernodeController extends ControllerBase {
	private static final Log LOG = LogFactory.getLog(ReportingApplication.class);

	private SuperNodeSeeder superNodeSeeder;

	public SupernodeController() {
		superNodeSeeder = null;
	}

	@Resource
	public void setSuperNodeSeeder(SuperNodeSeeder aSuperNodeSeeder) {
		superNodeSeeder = aSuperNodeSeeder;
	}

	@POST
	@Produces( { MediaType.APPLICATION_JSON })
	public String configureSuperNodes(@FormParam("superNodeApplication") String applicationName, @FormParam("number") int numberOfSuperNodes, @FormParam("offset") int offset) {
		LOG.debug(String.format("configureSuperNode(%s, %d, %d)", applicationName, numberOfSuperNodes, offset));

		if (StringUtils.isEmpty(applicationName) || (offset < 0))
			throw new IllegalArgumentException("ApplicationName is null or empty Or Offset is negative");

		superNodeSeeder.configureNumberOfSuperNodes(applicationName, numberOfSuperNodes, offset);

		return String.format("Application (%s) configured", applicationName);
	}
}
