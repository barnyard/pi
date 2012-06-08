package com.bt.nia.koala.robustness.commands;

import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.atomic.AtomicBoolean;

import com.ragstorooks.testrr.ScenarioListener;

public abstract class PollingStringComparisonScenarioCommand extends PollingScenarioCommand {

	protected String[] goodValues;
	protected String[] badValues;
	protected String[] ignoreValues;

	public PollingStringComparisonScenarioCommand(String scenarioId, ScenarioListener scenarioListener, AtomicBoolean scenarioCompleted, Executor executor, Map<String, Object> params,
			String[] goodValues, String[] failEarlyValues, String[] ignoreValues) {
		super(scenarioId, scenarioListener, scenarioCompleted, executor, params);
		this.goodValues = goodValues;
		this.badValues = failEarlyValues;
		this.ignoreValues = ignoreValues;
	}

	public PollingStringComparisonScenarioCommand(String scenarioId, ScenarioListener scenarioListener, AtomicBoolean scenarioCompleted, Executor executor, Map<String, Object> params,
			String goodValue, String[] failEarlyValues) {
		this(scenarioId, scenarioListener, scenarioCompleted, executor, params, new String[] { goodValue }, failEarlyValues, new String[0]);
	}

	public PollingStringComparisonScenarioCommand(String scenarioId, ScenarioListener scenarioListener, AtomicBoolean scenarioCompleted, Executor executor, Map<String, Object> params,
			String goodValue, String failEarlyValue) {
		this(scenarioId, scenarioListener, scenarioCompleted, executor, params, new String[] { goodValue }, new String[] { failEarlyValue }, new String[0]);
	}

	public PollingStringComparisonScenarioCommand(String scenarioId, ScenarioListener scenarioListener, AtomicBoolean scenarioCompleted, Executor executor, Map<String, Object> params,
			String[] goodValues) {
		this(scenarioId, scenarioListener, scenarioCompleted, executor, params, goodValues, new String[0], new String[0]);
	}

	public PollingStringComparisonScenarioCommand(String scenarioId, ScenarioListener scenarioListener, AtomicBoolean scenarioCompleted, Executor executor, Map<String, Object> params, String goodValue) {
		this(scenarioId, scenarioListener, scenarioCompleted, executor, params, new String[] { goodValue }, new String[0], new String[0]);
	}

	public PollingStringComparisonScenarioCommand(String scenarioId, ScenarioListener scenarioListener, AtomicBoolean scenarioCompleted, ScheduledExecutorService executor, Map<String, Object> params,
			String goodValue, String[] failEarlyValues, String ignoreValue) {
		this(scenarioId, scenarioListener, scenarioCompleted, executor, params, new String[] { goodValue }, failEarlyValues, new String[] { ignoreValue });
	}

	protected void checkString(String value) {
		boolean found = false;
		for (String goodValue : goodValues) {
			if (goodValue.equals(value)) {
				found = true;
				break;
			}
		}
		if (!found) {
			String message = "Received unexpected value of " + value;
			for (String badValue : badValues) {
				if (badValue.equals(value)) {
					throw new FailStateInOutputException(message);
				}
			}
			boolean ignore = false;
			for (String ignoreValue : ignoreValues) {
				if (ignoreValue.equals(value)) {
					ignore = true;
					break;
				}
			}
			// if (!ignore)
			throw new UnexpectedOutputException(message);
		}

	}

}
