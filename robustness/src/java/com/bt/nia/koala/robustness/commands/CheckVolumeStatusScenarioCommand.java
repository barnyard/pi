package com.bt.nia.koala.robustness.commands;

import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.bt.nia.koala.robustness.parsers.DescribeVolumeOutputParser;
import com.ragstorooks.testrr.ScenarioListener;

public class CheckVolumeStatusScenarioCommand extends PollingStringComparisonScenarioCommand {

	private static final Log log = LogFactory.getLog(CheckVolumeStatusScenarioCommand.class);
	private boolean skipOutputCheck = false;

	public CheckVolumeStatusScenarioCommand(String scenarioId, ScenarioListener scenarioListener, AtomicBoolean scenarioCompleted, Executor executor, Map<String, Object> params, String goodValue) {
		super(scenarioId, scenarioListener, scenarioCompleted, executor, params, goodValue);
	}

	public CheckVolumeStatusScenarioCommand(String scenarioId, ScenarioListener scenarioListener, AtomicBoolean scenarioCompleted, Executor executor, Map<String, Object> params, String goodValue,
			boolean skipOutputCheckforCommand) {
		super(scenarioId, scenarioListener, scenarioCompleted, executor, params, goodValue);
		skipOutputCheck = skipOutputCheckforCommand;
	}

	public CheckVolumeStatusScenarioCommand(String scenarioId, ScenarioListener scenarioListener, AtomicBoolean scenarioCompleted, Executor executor, Map<String, Object> params, String goodValue,
			String[] failEarlyValues) {
		super(scenarioId, scenarioListener, scenarioCompleted, executor, params, goodValue, failEarlyValues);
	}

	public CheckVolumeStatusScenarioCommand(String scenarioId, ScenarioListener scenarioListener, AtomicBoolean scenarioCompleted, Executor executor, Map<String, Object> params, String goodValue,
			String failEarlyValue) {
		super(scenarioId, scenarioListener, scenarioCompleted, executor, params, goodValue, failEarlyValue);
	}

	@Override
	public boolean executePollingCommand(Map<String, Object> params) throws Throwable {
		String volumeId = params.get("volumeId").toString();
		try {
			checkVolumeStatus(volumeId);
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

	private void checkVolumeStatus(String volumeId) throws Throwable {
		List<String> output = executeEc2Command("ec2dvol", new String[] { volumeId });

		boolean isFromSnapshot = !getParams().containsKey("isFromSnapshot") ? false : Boolean.parseBoolean(getParams().get("isFromSnapshot").toString());
		DescribeVolumeOutputParser describeVolumeOutputParser = isFromSnapshot ? new DescribeVolumeOutputParser(System.getenv("KOALA_AVAILABILITY_ZONE"), isFromSnapshot)
				: new DescribeVolumeOutputParser(System.getenv("KOALA_AVAILABILITY_ZONE"));
		try {
			String currentStatus = describeVolumeOutputParser.parse(output)[0];

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
