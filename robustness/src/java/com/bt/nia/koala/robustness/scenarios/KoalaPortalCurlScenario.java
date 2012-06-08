package com.bt.nia.koala.robustness.scenarios;

import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.atomic.AtomicBoolean;

import com.bt.nia.koala.robustness.PircData;
import com.bt.nia.koala.robustness.commands.CheckKoalaPortalScenarioCommand;
import com.ragstorooks.testrr.ScenarioCommand;
import com.ragstorooks.testrr.ScenarioCommanderBase;

public class KoalaPortalCurlScenario extends ScenarioCommanderBase {
	private final PircData pircData;

	public KoalaPortalCurlScenario(ScheduledExecutorService executor, PircData pircData) {
		super(executor);
		this.pircData = pircData;
	}

	@Override
	protected void addCommands(final String scenarioId, final AtomicBoolean scenarioCompleted, Map<String, Object> params, Queue<ScenarioCommand> commandQueue) {
		commandQueue.add(new CheckKoalaPortalScenarioCommand(scenarioId, getScenarioListener(), scenarioCompleted, getExecutor(), params, pircData));
	}
}
