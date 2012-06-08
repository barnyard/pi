package com.bt.nia.koala.robustness.commands;

import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.bt.nia.koala.robustness.parsers.DescribeImageOutputParser;
import com.ragstorooks.testrr.ScenarioListener;

public class CheckImageStatusScenarioCommand extends PollingStringComparisonScenarioCommand {
	private static final Log log = LogFactory.getLog(CheckImageStatusScenarioCommand.class);
	private String imageIdParameter = "imageId";

	public CheckImageStatusScenarioCommand(String imageIdParameter, ScenarioRunDetails runDetails, String goodValue) {
		this(runDetails.getScenarioId(), runDetails.getScenarioListener(), runDetails.getScenarioCompleted(), runDetails.getExecutor(), runDetails.getParams(), goodValue);
		this.imageIdParameter = imageIdParameter;
	}

	public CheckImageStatusScenarioCommand(ScenarioRunDetails runDetails, String goodValue) {
		this("imageId", runDetails, goodValue);
	}

	public CheckImageStatusScenarioCommand(String scenarioId, ScenarioListener scenarioListener, AtomicBoolean scenarioCompleted, Executor executor, Map<String, Object> params, String goodValue) {
		super(scenarioId, scenarioListener, scenarioCompleted, executor, params, goodValue);
	}

	public CheckImageStatusScenarioCommand(String scenarioId, ScenarioListener scenarioListener, AtomicBoolean scenarioCompleted, Executor executor, Map<String, Object> params, String goodValue,
			String failEarlyValue) {
		super(scenarioId, scenarioListener, scenarioCompleted, executor, params, goodValue, failEarlyValue);
	}

	public CheckImageStatusScenarioCommand(String scenarioId, ScenarioListener scenarioListener, AtomicBoolean scenarioCompleted, Executor executor, Map<String, Object> params, String goodValue,
			String[] failEarlyValues) {
		super(scenarioId, scenarioListener, scenarioCompleted, executor, params, goodValue, failEarlyValues);
	}

	@Override
	protected boolean executePollingCommand(Map<String, Object> params) throws Throwable {
		String imageId = params.get(imageIdParameter).toString();
		try {
			checkStatus(params, imageId);
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

	private void checkStatus(Map<String, Object> params, String imageId) throws Throwable {
		try {
			List<String> output = executeEc2Command("ec2dim", new String[] { imageId });

			DescribeImageOutputParser describeImageOutputParser = new DescribeImageOutputParser();
			String currentStatus = describeImageOutputParser.parse(output)[0];
			log.info(String.format("Image %s is %s ", imageId, currentStatus));

			checkString(currentStatus);

		} catch (RuntimeException ex) {
			if (ex.getMessage().contains("Exit status was 1"))
				log.info("Looks like a timeout, ignoring and carrying on");
			else
				throw ex;
		}
	}

}
