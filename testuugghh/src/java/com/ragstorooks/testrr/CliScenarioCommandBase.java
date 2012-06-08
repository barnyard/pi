package com.ragstorooks.testrr;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicBoolean;

import com.ragstorooks.testrr.cli.CommandExecutor;

public class CliScenarioCommandBase extends ScenarioCommandBase {

    public CliScenarioCommandBase(String arg0, ScenarioListener arg1, AtomicBoolean arg2, Executor arg3, Map<String, Object> arg4) {
        super(arg0, arg1, arg2, arg3, arg4);
    }

    @Override
    protected void cleanup(Map<String, Object> arg0) throws Throwable {
    }

    @Override
    protected void execute(Map<String, Object> arg0) throws Throwable {
    }

    @Override
    protected ScenarioCommandBase getCompensationCommand() {
        return null;
    }

    public long getDelayMillis() {
        return 0;
    }

    protected List<String> executeCommand(String path, String[] params) throws Throwable {
        return executeCommand(path, params, true);
    }

    protected List<String> executeCommand(String path, String[] params, boolean failOnError) throws Throwable {
        CommandExecutor e = new CommandExecutor(getExecutor());

        String[] cmd = new String[params.length + 1];
        cmd[0] = path;
        for (int i = 0; i < params.length; i++)
            cmd[i + 1] = params[i];

        int exitStatus = e.executeScript(createMdcMap(), cmd, Runtime.getRuntime(), failOnError);
        if (exitStatus > 0 && failOnError) {
            StringBuilder output = new StringBuilder();
            for (int i = 0; i < e.getOutputLines().size(); i++)
                output.append(e.getOutputLines().get(i));
            throw new RuntimeException(String.format("Exit status was %d.\n %s", exitStatus, output.toString()));
        }

        return e.getOutputLines();
    }

    private Map<String, Object> createMdcMap() {
        Map<String, Object> mdcMap = new HashMap<String, Object>();
        mdcMap.put("scenarioId", getScenarioId());
        mdcMap.put("scenarioName", "");
        return mdcMap;
    }
}