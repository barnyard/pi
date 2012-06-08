package com.bt.nia.koala.robustness.commands;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.bt.nia.koala.robustness.parsers.AllocateAddressOutputParser;
import com.ragstorooks.testrr.ScenarioListener;

public class AllocateAddressScenarioCommand extends Ec2ScenarioCommandBase {
	private static final Log log = LogFactory.getLog(AllocateAddressScenarioCommand.class);

	public AllocateAddressScenarioCommand(String scenarioId, ScenarioListener scenarioListener, AtomicBoolean scenarioCompleted, Executor executor, Map<String, Object> params) {
		super(scenarioId, scenarioListener, scenarioCompleted, executor, params);
	}

	@Override
	public void execute(Map<String, Object> params) throws Throwable {
		List<String> output = executeEc2Command("ec2allocaddr", new String[] {});
		log.info(output);

		AllocateAddressOutputParser allocateAddressOutputParser = new AllocateAddressOutputParser();
		String ipAddress = allocateAddressOutputParser.parse(output)[0];

		log.info(String.format("Allocated address %s", ipAddress));
		params.put("allocatedIpAddress", ipAddress);
	}

	@Override
	protected Ec2ScenarioCommandBase getCompensationCommand() {
		return new ReleaseAddressScenarioCommand(getScenarioId(), null, null, getExecutor(), getParams());
	}
}
