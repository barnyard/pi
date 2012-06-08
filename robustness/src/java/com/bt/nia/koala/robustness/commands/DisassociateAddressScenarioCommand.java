package com.bt.nia.koala.robustness.commands;

import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.bt.nia.koala.robustness.parsers.AllocateAddressOutputParser;
import com.ragstorooks.testrr.ScenarioListener;

public class DisassociateAddressScenarioCommand extends Ec2ScenarioCommandBase {
	private static final Log log = LogFactory.getLog(DisassociateAddressScenarioCommand.class);

	public DisassociateAddressScenarioCommand(String scenarioId, ScenarioListener scenarioListener, AtomicBoolean scenarioCompleted, Executor executor, Map<String, Object> params) {
		super(scenarioId, scenarioListener, scenarioCompleted, executor, params);
	}

	@Override
	public void execute(Map<String, Object> params) throws Throwable {
		String ipAddress = params.get("allocatedIpAddress").toString();
		log.info("Trying to disassociate the ip address " + ipAddress);
		List<String> output = executeEc2Command("ec2disaddr", new String[] { ipAddress });
		AllocateAddressOutputParser allocateAddressOutputParser = new AllocateAddressOutputParser();
		String disassociatedIpAddress = allocateAddressOutputParser.parse(output)[0];
		if (!ipAddress.equals(disassociatedIpAddress))
			throw new RuntimeException(String.format("Expected address %s to be disassociated, but got %s", ipAddress, disassociatedIpAddress));
	}
}
