package com.ragstorooks.testrr;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.log4j.MDC;

public abstract class ScenarioCommandBase implements ScenarioCommand {
    private static final Log log = LogFactory.getLog(ScenarioCommandBase.class);
    private CountDownLatch isDone = new CountDownLatch(1);
    private String scenarioId;
    private ScenarioListener scenarioListener;
    private AtomicBoolean scenarioCompleted;
    private Executor executor;
    private Map<String, Object> params;

    public ScenarioCommandBase(String scenarioId, ScenarioListener scenarioListener, AtomicBoolean scenarioCompleted, Executor executor, Map<String, Object> params) {
        this.scenarioId = scenarioId;
        this.scenarioListener = scenarioListener;
        this.scenarioCompleted = scenarioCompleted;
        this.executor = executor;
        this.params = params;
    }

    public CountDownLatch isDone() {
        return this.isDone;
    }

    protected String getScenarioId() {
        return scenarioId;
    }

    protected Executor getExecutor() {
        return executor;
    }

    protected Map<String, Object> getParams() {
        return params;
    }

    protected ScenarioListener getScenarioListener() {
        return scenarioListener;
    }

    protected AtomicBoolean getScenarioCompleted() {
        return scenarioCompleted;
    }

    protected abstract void execute(Map<String, Object> params) throws Throwable;

    protected abstract ScenarioCommandBase getCompensationCommand();

    protected abstract void cleanup(Map<String, Object> params) throws Throwable;

    @SuppressWarnings("unchecked")
    public void run() {
        try {
            MDC.put("scenarioName", "");
            MDC.put("scenarioId", scenarioId);

            execute(getParams());

            List<ScenarioCommandBase> purgedCompensationCommands = new ArrayList<ScenarioCommandBase>();
            List<ScenarioCommandBase> compensationCommands = (List<ScenarioCommandBase>) getParams().get("compensationCommands");
            Iterator iter = compensationCommands.iterator();
            while (iter.hasNext()) {
                ScenarioCommandBase sc = (ScenarioCommandBase) iter.next();
                if (sc.getClass().isAssignableFrom(this.getClass())) {
                    log.debug(String.format("Removing compensation command %s after executing %s", sc.getClass().getSimpleName(), this.getClass().getSimpleName()));
                } else
                    purgedCompensationCommands.add(sc);
            }
            getParams().put("compensationCommands", purgedCompensationCommands);

            ScenarioCommandBase compensationCommand = getCompensationCommand();
            if (compensationCommand != null) {
                log.debug(String.format("Adding compensation command %s", compensationCommand.getClass().getSimpleName()));
                ((List<ScenarioCommandBase>) getParams().get("compensationCommands")).add(compensationCommand);
            }
        } catch (Throwable t) {
            log.error(t.getMessage(), t);
            getScenarioCompleted().set(true);

            try {
                cleanup(getParams());
                getScenarioListener().scenarioFailure(getScenarioId(), t.getMessage());
            } catch (Throwable tt) {
                log.error(tt.getMessage(), tt);
            }
        } finally {
            isDone().countDown();
        }
    }

}
