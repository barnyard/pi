package com.bt.nia.koala.robustness.commands;

import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.bt.nia.koala.robustness.parsers.DescribeInstanceOutputParser;
import com.bt.nia.koala.robustness.parsers.DescribeInstancePrivateIpAddressParser;
import com.bt.nia.koala.robustness.parsers.DescribeInstancePublicIpAddressParser;
import com.ragstorooks.testrr.ScenarioListener;

public class CheckInstanceAssociatedAddressScenarioCommand extends Ec2ScenarioCommandBase {
	private static final Log log = LogFactory.getLog(CheckInstanceAssociatedAddressScenarioCommand.class);

	public CheckInstanceAssociatedAddressScenarioCommand(String scenarioId, ScenarioListener scenarioListener, AtomicBoolean scenarioCompleted, Executor executor, Map<String, Object> params) {
		super(scenarioId, scenarioListener, scenarioCompleted, executor, params);
	}

	@Override
	public void execute(Map<String, Object> params) throws Throwable {
		String associatedIpAddress = params.get("allocatedIpAddress").toString();
		String privateIpFromInstance = params.get("privateIpAddress").toString();
		String instanceId = params.get("instanceId").toString();

		log.info("Checking associate ip address " + associatedIpAddress + " to instance:" + instanceId);

		try {
			List<String> output = executeEc2Command("ec2din", new String[] { instanceId });

			DescribeInstanceOutputParser describeInstanceOutputParser = new DescribeInstanceOutputParser();
			String currentStatus = describeInstanceOutputParser.parse(output)[0];

			DescribeInstancePublicIpAddressParser describeInstanceIpAddressParser = new DescribeInstancePublicIpAddressParser();
			String ipAddress = describeInstanceIpAddressParser.parse(output)[0];

			String privateIp = new DescribeInstancePrivateIpAddressParser().parse(output)[0];
			log.info(String.format("Instance %s is %s and has public ip %s and private ip %s", instanceId, currentStatus, ipAddress, privateIp));

			if (!ipAddress.equals(associatedIpAddress) && !privateIpFromInstance.equals(privateIp))
				throw new RuntimeException(String.format("Expected address %s to be associated, but got %s", ipAddress, associatedIpAddress));

			params.put("ipAddress", ipAddress);

		} catch (RuntimeException ex) {
			if (ex.getMessage().contains("Exit status was 1"))
				log.info("Looks like a timeout, ignoring and carrying on");
			else
				throw ex;
		}
	}
}
