package com.bt.nia.koala.robustness.commands;

import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.bt.nia.koala.robustness.parsers.DescribeSnapshotOutputParser;
import com.ragstorooks.testrr.ScenarioListener;

public class CheckSnapshotStatusScenarioCommand extends PollingStringComparisonScenarioCommand {

	private static final Log log = LogFactory.getLog(CheckSnapshotStatusScenarioCommand.class);
	private boolean skipOutputCheck = false;

	public CheckSnapshotStatusScenarioCommand(String scenarioId, ScenarioListener scenarioListener, AtomicBoolean scenarioCompleted, Executor executor, Map<String, Object> params, String goodValue) {
		super(scenarioId, scenarioListener, scenarioCompleted, executor, params, goodValue);
	}

	public CheckSnapshotStatusScenarioCommand(String scenarioId, ScenarioListener scenarioListener, AtomicBoolean scenarioCompleted, Executor executor, Map<String, Object> params, String goodValue,
			boolean skipOutputCheckforCommand) {
		super(scenarioId, scenarioListener, scenarioCompleted, executor, params, goodValue);
		skipOutputCheck = skipOutputCheckforCommand;
	}

	public CheckSnapshotStatusScenarioCommand(String scenarioId, ScenarioListener scenarioListener, AtomicBoolean scenarioCompleted, Executor executor, Map<String, Object> params, String goodValue,
			String[] failEarlyValues) {
		super(scenarioId, scenarioListener, scenarioCompleted, executor, params, goodValue, failEarlyValues);
	}

	public CheckSnapshotStatusScenarioCommand(String scenarioId, ScenarioListener scenarioListener, AtomicBoolean scenarioCompleted, Executor executor, Map<String, Object> params, String goodValue,
			String failEarlyValue) {
		super(scenarioId, scenarioListener, scenarioCompleted, executor, params, goodValue, failEarlyValue);
	}

	@Override
	public boolean executePollingCommand(Map<String, Object> params) throws Throwable {
		String snapshotId = params.get("snapshotId").toString();
		try {
			checkSnapshotStatus(snapshotId);
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
		return 20;
	}

	private void checkSnapshotStatus(String snapshotId) throws Throwable {
		List<String> output = executeEc2Command("ec2dsnap", new String[] { snapshotId });

		DescribeSnapshotOutputParser describeSnapshotOutputParser = new DescribeSnapshotOutputParser();
		try {
			String currentStatus = describeSnapshotOutputParser.parse(output)[0];

			checkString(currentStatus);
		} catch (UnexpectedOutputException ex) {
			if (!skipOutputCheck) {
				throw ex;
			} else {
				log.debug("No Output was returned as expected.");
			}
		}
	}
}
