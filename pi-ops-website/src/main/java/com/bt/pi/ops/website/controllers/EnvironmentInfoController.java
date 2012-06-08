package com.bt.pi.ops.website.controllers;

import javax.annotation.Resource;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.stereotype.Component;

import com.bt.pi.core.cli.commands.CommandExecutionException;
import com.bt.pi.core.util.common.CommandResult;
import com.bt.pi.core.util.common.CommandRunner;

@Path("/environment")
@Component
public class EnvironmentInfoController extends ControllerBase {
	private static final String RPM_QUERY_COMMAND = "rpm -q --queryformat 'Build time: %{buildtime:date} - Install time: %{installtime:date} Name: %{name} Version: %{version} Release: %{release}\n' pi-cloud";

	private static final Log LOG = LogFactory.getLog(EnvironmentInfoController.class);

	@Resource
	private CommandRunner commandRunner;

	public EnvironmentInfoController() {
		commandRunner = null;
	}

	@GET
	@Path("/rpm")
	@Produces(MediaType.TEXT_PLAIN)
	public Response getRpmDetails() {
		LOG.debug("getRpmDetail()");
		try {
			CommandResult commandResult = commandRunner.run(RPM_QUERY_COMMAND);
			return Response.ok(commandResult.getOutputLines().toString()).build();
		} catch (CommandExecutionException cex) {
			LOG.warn("Command failed", cex);
			return Response.ok("RPM details not available").build();
		}
	}
}
