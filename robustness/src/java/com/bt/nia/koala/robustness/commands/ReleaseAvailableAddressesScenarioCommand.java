package com.bt.nia.koala.robustness.commands;

import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicBoolean;

import com.ragstorooks.testrr.ScenarioListener;

public class ReleaseAvailableAddressesScenarioCommand extends Ec2ScenarioCommandBase {

	public ReleaseAvailableAddressesScenarioCommand(String scenarioId,
			ScenarioListener scenarioListener, AtomicBoolean scenarioCompleted,
			Executor executor, Map<String, Object> params) {
		super(scenarioId, scenarioListener, scenarioCompleted, executor, params);
	}

	@Override
	protected void execute(Map<String, Object> params) throws Throwable {
		List<String> output = executeEc2Command("ec2-describe-addresses", new String[] {}, false);
		
		for(String line: output) {
			if (line.contains("ADDRESS") && line.contains("available (eucalyptus)")) {
				String ipAddress = line.split("\\t")[1];
				executeEc2Command("ec2reladdr", new String[]{ipAddress}, false);
			}
		}
	}
}
