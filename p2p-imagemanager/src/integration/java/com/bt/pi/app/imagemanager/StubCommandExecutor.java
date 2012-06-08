package com.bt.pi.app.imagemanager;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.Executor;

import com.ragstorooks.testrr.cli.CommandExecutor;

public class StubCommandExecutor extends com.bt.pi.core.testing.StubCommandExecutor {
    private CommandExecutor realCommandExecutor;
    private String[] realCommands = new String[] { "/bin/tar", "/bin/gunzip" };

    public StubCommandExecutor(Executor executor) {
        super(executor);
        this.realCommandExecutor = new CommandExecutor(executor);
    }

    @Override
    public int executeScript(Map<String, Object> mdcMap, String[] command, Runtime runtime, boolean logErrors, long maxWaitTime, boolean shouldShutDown) throws IOException, InterruptedException {
        if (isReal(command))
            return this.realCommandExecutor.executeScript(mdcMap, command, runtime, logErrors);
        return super.executeScript(mdcMap, command, runtime, logErrors);
    }

    private boolean isReal(String[] command) {
        for (String part : command)
            for (String real : realCommands)
                if (part.contains(real))
                    return true;

        return false;
    }
}
