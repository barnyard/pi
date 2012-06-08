package com.bt.nia.koala.robustness.commands;

import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.bt.nia.koala.robustness.parsers.AssociateAddressOutputParser;
import com.ragstorooks.testrr.ScenarioListener;

public class AssociateAddressScenarioCommand extends Ec2ScenarioCommandBase {
	private static final Log log = LogFactory.getLog(AssociateAddressScenarioCommand.class);

	public AssociateAddressScenarioCommand(String scenarioId, ScenarioListener scenarioListener, AtomicBoolean scenarioCompleted, Executor executor, Map<String, Object> params) {
		super(scenarioId, scenarioListener, scenarioCompleted, executor, params);
	}

	@Override
	public void execute(Map<String, Object> params) throws Throwable {
		String instanceId = params.get("instanceId").toString();
		String ipAddress = params.get("allocatedIpAddress").toString();
		log.info(String.format("Trying to associate address %s to instance %s", ipAddress, instanceId));

		List<String> output = executeEc2Command("ec2assocaddr", new String[] { "-i", instanceId, ipAddress });
		AssociateAddressOutputParser associateAddressOutputParser = new AssociateAddressOutputParser();
		String associatedIpAddress = associateAddressOutputParser.parse(output)[0];
		if (!ipAddress.equals(associatedIpAddress))
			throw new RuntimeException(String.format("Expected address %s to be associated, but got %s", ipAddress, associatedIpAddress));
	}

	@Override
	protected Ec2ScenarioCommandBase getCompensationCommand() {
		return new DisassociateAddressScenarioCommand(getScenarioId(), null, null, getExecutor(), getParams());
	}
}
