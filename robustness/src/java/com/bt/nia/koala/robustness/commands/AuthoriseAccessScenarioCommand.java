package com.bt.nia.koala.robustness.commands;

import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.ragstorooks.testrr.ScenarioListener;

public class AuthoriseAccessScenarioCommand extends Ec2ScenarioCommandBase {

	private static final Log LOG = LogFactory.getLog(AuthoriseAccessScenarioCommand.class);

	public AuthoriseAccessScenarioCommand(String scenarioId, ScenarioListener scenarioListener, AtomicBoolean scenarioCompleted, Executor executor, Map<String, Object> params) {
		super(scenarioId, scenarioListener, scenarioCompleted, executor, params);

	}

	@Override
	public void execute(Map<String, Object> params) throws Throwable {
		try {
			String securityGroup = params.get("securityGroup").toString();
			String protocol = "tcp";
			String port = "80";
			LOG.info(String.format("Authorising security group ingress on group %s for protocol %s and port(s) %s", securityGroup, protocol, port));

			executeEc2Command("ec2auth", new String[] { securityGroup, "-P" + protocol, "-p" + port });
		} catch (RuntimeException ex) {
			if (ex.getMessage().contains("ALLOWS"))
				LOG.error("ec2auth command returned error, but auth is setup");
			else
				throw ex;
		}
	}

	@Override
	protected Ec2ScenarioCommandBase getCompensationCommand() {
		return new RevokeAccessScenarioCommand(getScenarioId(), null, null, getExecutor(), getParams());
	}
}
