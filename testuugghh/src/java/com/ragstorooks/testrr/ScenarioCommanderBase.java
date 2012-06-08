package com.ragstorooks.testrr;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.log4j.MDC;

public abstract class ScenarioCommanderBase extends ScenarioBase {
    private final Log log = LogFactory.getLog(getClass());
    private ScheduledExecutorService executor;

    public ScenarioCommanderBase(ScheduledExecutorService executor) {
        this.executor = executor;
    }

    protected ScheduledExecutorService getExecutor() {
        return executor;
    }

    @Override
    public void run(final String scenarioId) {
        MDC.put("scenarioId", scenarioId);
        MDC.put("scenarioName", getClass().getSimpleName());

        final AtomicBoolean scenarioCompleted = new AtomicBoolean(false);

        Map<String, Object> params = new HashMap<String, Object>();
        List<ScenarioCommand> compensationCommands = new ArrayList<ScenarioCommand>();
        params.put("compensationCommands", compensationCommands);

        Queue<ScenarioCommand> commandQueue = new LinkedList<ScenarioCommand>();
        addCommands(scenarioId, scenarioCompleted, params, commandQueue);

        log.info(String.format("Starting instance for scenario %s", scenarioId));
        while (!scenarioCompleted.get() && commandQueue.peek() != null) {
            ScenarioCommand next = commandQueue.poll();

            log.debug(String.format("Scheduling command to run in %d milliseconds", next.getDelayMillis()));
            executor.schedule(next, next.getDelayMillis(), TimeUnit.MILLISECONDS);
            try {
                next.isDone().await();
            } catch (InterruptedException e) {
                log.error(e.getMessage(), e);
            }
        }

        if (!scenarioCompleted.get() && commandQueue.peek() == null) {
            getScenarioListener().scenarioSuccess(scenarioId);
            scenarioCompleted.set(true);
        } else {
            String msg = String.format("Scenario %s not completed but no more commands left!", scenarioId);
            log.error(msg);
            throw new RuntimeException(msg);
        }
    }

    protected abstract void addCommands(String scenarioId, AtomicBoolean scenarioCompleted, Map<String, Object> params, Queue<ScenarioCommand> commandQueue);
}
