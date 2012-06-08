package com.bt.nia.koala.robustness.commands;

import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.bt.nia.koala.robustness.parsers.DescribeSecurityGroupOutputParser;
import com.ragstorooks.testrr.ScenarioListener;

public class CheckSecurityGroupStatusScenarioCommand extends PollingStringComparisonScenarioCommand {
	private static final Log log = LogFactory.getLog(CheckSecurityGroupStatusScenarioCommand.class);

	public CheckSecurityGroupStatusScenarioCommand(String scenarioId, ScenarioListener scenarioListener, AtomicBoolean scenarioCompleted, Executor executor, Map<String, Object> params,
			String goodValue) {
		super(scenarioId, scenarioListener, scenarioCompleted, executor, params, goodValue);
	}

	@Override
	protected boolean executePollingCommand(Map<String, Object> params) throws Throwable {
		String securityGroupId = params.get("securityGroup").toString();
		try {
			checkStatus(params, securityGroupId);
			return true;
		} catch (UnexpectedOutputException e) {
			log.info(e.getMessage());
			return false;
		}
	}

	@Override
	protected int getMaxNumPolls() {
		return 50;
	}

	@Override
	protected long getPollingIntervalMillis() {
		return 20 * 1000;
	}

	private void checkStatus(Map<String, Object> params, String securityGroupId) throws Throwable {
		try {
			List<String> output = executeEc2Command("ec2dgrp", new String[] { securityGroupId });

			DescribeSecurityGroupOutputParser describeSecurityGroupOutputParser = new DescribeSecurityGroupOutputParser();
			String currentStatus = describeSecurityGroupOutputParser.parse(output)[0];

			log.info(String.format("Security group access is authorised:", currentStatus));
			checkString(currentStatus);

		} catch (RuntimeException ex) {
			if (ex.getMessage().contains("Exit status was 1"))
				log.info("Looks like a timeout, ignoring and carrying on");
			else
				throw ex;
		}
	}
}
