package com.bt.nia.koala.robustness.commands;

import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.bt.nia.koala.robustness.parsers.TerminateInstanceOutputParser;
import com.ragstorooks.testrr.ScenarioListener;

public class TerminateInstanceScenarioCommand extends Ec2ScenarioCommandBase {
	private static final Log log = LogFactory.getLog(TerminateInstanceScenarioCommand.class);

	public TerminateInstanceScenarioCommand(String scenarioId, ScenarioListener scenarioListener, AtomicBoolean scenarioCompleted, Executor executor, Map<String, Object> params) {
		super(scenarioId, scenarioListener, scenarioCompleted, executor, params);
	}

	@Override
	public void execute(Map<String, Object> params) throws Throwable {
		String instanceId = params.get("instanceId").toString();

		log.info("Trying to terminate instance " + instanceId);
		List<String> output = executeEc2Command("ec2kill", new String[] { instanceId });

		TerminateInstanceOutputParser terminateInstanceOutputParser = new TerminateInstanceOutputParser();
		String terminatingInstanceId = terminateInstanceOutputParser.parse(output)[0];
		if (!instanceId.equals(terminatingInstanceId))
			throw new RuntimeException(String.format("Expected instance id %s for termination, but got %s", instanceId, terminatingInstanceId));
	}
}
