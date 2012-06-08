package com.bt.nia.koala.robustness.commands;

import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.ragstorooks.testrr.ScenarioListener;

public class RevokeAccessScenarioCommand extends Ec2ScenarioCommandBase {
	private static final Log log = LogFactory.getLog(RevokeAccessScenarioCommand.class);

	private boolean failOnError = false;

	public RevokeAccessScenarioCommand(String scenarioId, ScenarioListener scenarioListener, AtomicBoolean scenarioCompleted, Executor executor, Map<String, Object> params) {
		this(scenarioId, scenarioListener, scenarioCompleted, executor, params, true);
	}

	public RevokeAccessScenarioCommand(String scenarioId, ScenarioListener scenarioListener, AtomicBoolean scenarioCompleted, Executor executor, Map<String, Object> params, boolean failOnError) {
		super(scenarioId, scenarioListener, scenarioCompleted, executor, params);
		this.failOnError = failOnError;
	}

	@Override
	public void execute(Map<String, Object> params) throws Throwable {
		String securityGroup = params.get("securityGroup").toString();
		String protocol = "tcp";
		String port = "80";

		log.info(String.format("Trying to revoke access to security group %s using %s on port %s", securityGroup, protocol, port));

		executeEc2Command("ec2revoke", new String[] { securityGroup, "-P" + protocol, "-p" + port }, failOnError);
	}
}
