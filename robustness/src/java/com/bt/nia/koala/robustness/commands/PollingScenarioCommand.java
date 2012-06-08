package com.bt.nia.koala.robustness.commands;

import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import com.ragstorooks.testrr.ScenarioListener;

public abstract class PollingScenarioCommand extends Ec2ScenarioCommandBase {
	public PollingScenarioCommand(String scenarioId, ScenarioListener scenarioListener, AtomicBoolean scenarioCompleted, Executor executor, Map<String, Object> params) {
		super(scenarioId, scenarioListener, scenarioCompleted, executor, params);
	}

	@Override public void execute(Map<String, Object> params) throws Throwable {
		int numPolls = 0;
		CountDownLatch succeeded = new CountDownLatch(1);
		do {
			if (executePollingCommand(params))
				succeeded.countDown();
			if (++numPolls > getMaxNumPolls())
				throw new RuntimeException(String.format("Maximum number of polls (%d) exceeded for scenario %s", getMaxNumPolls(), getScenarioId()));
		} while (!succeeded.await(getPollingIntervalMillis(), TimeUnit.MILLISECONDS));
	}
	
	protected abstract boolean executePollingCommand(Map<String, Object> params) throws Throwable;

	protected abstract long getPollingIntervalMillis();
	protected abstract int getMaxNumPolls();
}
