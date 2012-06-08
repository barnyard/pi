package com.bt.nia.koala.robustness.commands;

import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.bt.nia.koala.robustness.parsers.DescribeInstanceOutputParser;
import com.bt.nia.koala.robustness.parsers.DescribeInstancePrivateIpAddressParser;
import com.bt.nia.koala.robustness.parsers.DescribeInstancePublicIpAddressParser;
import com.ragstorooks.testrr.ScenarioListener;

public class CheckInstanceStatusScenarioCommand extends PollingStringComparisonScenarioCommand {
	private static final Log log = LogFactory.getLog(CheckInstanceStatusScenarioCommand.class);

	public CheckInstanceStatusScenarioCommand(String scenarioId, ScenarioListener scenarioListener, AtomicBoolean scenarioCompleted, Executor executor, Map<String, Object> params, String goodValue) {
		super(scenarioId, scenarioListener, scenarioCompleted, executor, params, goodValue);
	}

	public CheckInstanceStatusScenarioCommand(String scenarioId, ScenarioListener scenarioListener, AtomicBoolean scenarioCompleted, Executor executor, Map<String, Object> params, String goodValue,
			String failEarlyValue) {
		super(scenarioId, scenarioListener, scenarioCompleted, executor, params, goodValue, failEarlyValue);
	}

	public CheckInstanceStatusScenarioCommand(String scenarioId, ScenarioListener scenarioListener, AtomicBoolean scenarioCompleted, Executor executor, Map<String, Object> params, String goodValue,
			String[] failEarlyValues) {
		super(scenarioId, scenarioListener, scenarioCompleted, executor, params, goodValue, failEarlyValues);
	}

	public CheckInstanceStatusScenarioCommand(String scenarioId, ScenarioListener scenarioListener, AtomicBoolean scenarioCompleted, ScheduledExecutorService executor, Map<String, Object> params,
			String goodValue, String[] failEarlyValues, String ignoreValue) {
		super(scenarioId, scenarioListener, scenarioCompleted, executor, params, goodValue, failEarlyValues, ignoreValue);
	}

	@Override
	public boolean executePollingCommand(Map<String, Object> params) throws Throwable {
		String instanceId = params.get("instanceId").toString();
		try {
			// log.info(String.format("Checking the status of instance %s",
			// instanceId));
			checkStatus(params, instanceId);
			return true;
		} catch (UnexpectedOutputException e) {
			log.info(e.getMessage());
			return false;
		}
	}

	@Override
	protected long getPollingIntervalMillis() {
		return 20 * 1000;
	}

	@Override
	protected int getMaxNumPolls() {
		return 50;
	}

	private void checkStatus(Map<String, Object> params, String instanceId) throws Throwable {
		try {
			List<String> output = executeEc2Command("ec2din", new String[] { instanceId });
			for (String line : output)
				log.info(line);

			DescribeInstanceOutputParser describeInstanceOutputParser = new DescribeInstanceOutputParser();
			String currentStatus = describeInstanceOutputParser.parse(output)[0];

			DescribeInstancePublicIpAddressParser describeInstanceIpAddressParser = new DescribeInstancePublicIpAddressParser();
			String ipAddress = describeInstanceIpAddressParser.parse(output)[0];

			String privateIp = new DescribeInstancePrivateIpAddressParser().parse(output)[0];
			log.info(String.format("Instance %s is %s and has public ip %s and private ip %s", instanceId, currentStatus, ipAddress, privateIp));

			checkString(currentStatus);

			params.put("ipAddress", ipAddress);
			params.put("privateIpAddress", privateIp);

		} catch (RuntimeException ex) {
			if (ex.getMessage().contains("Exit status was 1"))
				log.info("Looks like a timeout, ignoring and carrying on");
			else
				throw ex;
		}
	}
}
