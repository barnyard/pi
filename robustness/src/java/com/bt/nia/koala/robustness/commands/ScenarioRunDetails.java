/* (c) British Telecommunications plc, 2010, All Rights Reserved */
package com.bt.nia.koala.robustness.commands;

import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.atomic.AtomicBoolean;

import com.ragstorooks.testrr.ScenarioCommand;
import com.ragstorooks.testrr.ScenarioListener;

public class ScenarioRunDetails {
	private String scenarioId;
	private AtomicBoolean scenarioCompleted;
	private ScheduledExecutorService executor;
	private Map<String, Object> params;
	private Queue<ScenarioCommand> commandQueue;
	private ScenarioListener scenarioListener;

	public ScenarioRunDetails(String scenarioId, AtomicBoolean scenarioCompleted, ScheduledExecutorService executor, Map<String, Object> params, Queue<ScenarioCommand> commandQueue,
			ScenarioListener scenarioListener) {
		this.scenarioId = scenarioId;
		this.scenarioCompleted = scenarioCompleted;
		this.executor = executor;
		this.params = params;
		this.commandQueue = commandQueue;
		this.scenarioListener = scenarioListener;
	}

	public String getScenarioId() {
		return scenarioId;
	}

	public AtomicBoolean getScenarioCompleted() {
		return scenarioCompleted;
	}

	public ScheduledExecutorService getExecutor() {
		return executor;
	}

	public Map<String, Object> getParams() {
		return params;
	}

	public Queue<ScenarioCommand> getCommandQueue() {
		return commandQueue;
	}

	public ScenarioListener getScenarioListener() {
		return scenarioListener;
	}

}
