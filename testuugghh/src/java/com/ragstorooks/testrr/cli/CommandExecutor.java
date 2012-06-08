package com.ragstorooks.testrr.cli;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

public class CommandExecutor {
    public static final long NO_MAX_WAIT_TIME = -1L;
    private static final long THREAD_SLEEP_TIME = 250L;
    private static final Log LOG = LogFactory.getLog(CommandExecutor.class);
    private ErrorStreamGobbler errorGobbler = null;
    OutputStreamGobbler outputGobbler = null;
    private Executor executor;
    private Map<Runtime, ArrayList<Process>> shutdownHookRunTimesHash;

    public CommandExecutor(Executor executor) {
        this.executor = executor;
        shutdownHookRunTimesHash = new HashMap<Runtime, ArrayList<Process>>();
    }

    public List<String> getOutputLines() {
    	logThreadPoolInformation();
        return outputGobbler.getLines();
    }

	private void logThreadPoolInformation() {
		if (executor instanceof ThreadPoolTaskExecutor) {
    		ThreadPoolTaskExecutor threadPoolTaskExecutor = (ThreadPoolTaskExecutor) executor;
    		LOG.debug(String.format("Current threads being used: %d, max pool size: %d", threadPoolTaskExecutor.getActiveCount(), threadPoolTaskExecutor.getMaxPoolSize()));
    	}
	}

    public List<String> getErrorLines() {
    	logThreadPoolInformation();
        return errorGobbler.getLines();
    }

    public int executeScript(String[] command, Runtime runtime) throws IOException, InterruptedException {
        return executeScript(command, runtime, NO_MAX_WAIT_TIME);
    }

    public int executeScript(String[] command, Runtime runtime, long maxWaitTime) throws IOException, InterruptedException {
        return executeScript(command, runtime, maxWaitTime, false);
    }

    public int executeScript(String[] command, Runtime runtime, long maxWaitTime, boolean stopOnShutdown) throws IOException, InterruptedException {
        return executeScript(command, runtime, true, maxWaitTime, stopOnShutdown);
    }

    public int executeScript(String[] command, Runtime runtime, boolean logErrors) throws IOException, InterruptedException {
        return executeScript(command, runtime, logErrors, NO_MAX_WAIT_TIME);
    }

    public int executeScript(String[] command, Runtime runtime, boolean logErrors, long maxWaitTime) throws IOException, InterruptedException {
        return executeScript(command, runtime, logErrors, maxWaitTime, false);
    }

    public int executeScript(String[] command, Runtime runtime, boolean logErrors, long maxWaitTime, boolean stopOnShutdown) throws IOException, InterruptedException {
        return executeScript(null, command, runtime, logErrors, maxWaitTime, stopOnShutdown);
    }

    public int executeScript(Map<String, Object> mdcMap, String[] command, Runtime runtime, boolean logErrors) throws IOException, InterruptedException {
        return executeScript(mdcMap, command, runtime, logErrors, NO_MAX_WAIT_TIME);
    }

    public int executeScript(Map<String, Object> mdcMap, String[] command, Runtime runtime, boolean logErrors, long maxWaitTime) throws IOException, InterruptedException {
        return executeScript(mdcMap, command, runtime, logErrors, maxWaitTime, false);
    }

    public int executeScript(Map<String, Object> mdcMap, String[] command, Runtime runtime, boolean logErrors, long maxWaitTime, boolean stopOnShutdown) throws IOException,
            InterruptedException {
        if (executor == null)
            throw new RuntimeException("Thread pool executor not set");

        LOG.debug(String.format("executeScript(%s, %s)", Arrays.toString(command), runtime));
        final Process proc = runtime.exec(command);

        errorGobbler = new ErrorStreamGobbler(proc.getErrorStream(), logErrors, mdcMap);
        outputGobbler = new OutputStreamGobbler(proc.getInputStream(), mdcMap);

    	logThreadPoolInformation();
        executor.execute(errorGobbler);
        executor.execute(outputGobbler);

        if (stopOnShutdown) {
            // add shutdownHooktoRuntime
            addProcessToShutdownHookList(runtime, proc);
        }
        return waitForExitValue(proc, maxWaitTime);
    }

    private void addProcessToShutdownHookList(final Runtime runtime, final Process proc) {
        if (!shutdownHookRunTimesHash.containsKey(runtime)) {
            addShutDownHookForRuntime(runtime);
        }
        // add process to list.
        shutdownHookRunTimesHash.get(runtime).add(proc);
    }

    private void addShutDownHookForRuntime(final Runtime runtime) {
        shutdownHookRunTimesHash.put(runtime, new ArrayList<Process>());
        runtime.addShutdownHook(new Thread(new Runnable() {
            @Override
            public void run() {
                for (Process proc : shutdownHookRunTimesHash.get(runtime)) {
                    try {
                        LOG.debug("Stopping Process: " + proc);
                        proc.destroy();
                        LOG.info(String.format("Process %s exited with value %s", proc, proc.exitValue()));
                    } catch (Throwable t) {
                        LOG.warn(String.format("Process %s was not destroyed properly %s", proc, t));
                    }
                }
            }
        }));
    }

    private int waitForExitValue(Process proc, long maxWaitTime) throws InterruptedException {
        int processExitCode = -1;
        if (maxWaitTime > 0) {
            long start = System.currentTimeMillis();
            do {
                try {
                    processExitCode = proc.exitValue();
                } catch (IllegalThreadStateException e) {
                    LOG.debug(String.format("Oops. Process isn't done yet and we haven't reached the maximum time of %s millis. Thread State Message: %s ", maxWaitTime, e
                            .getLocalizedMessage()));
                }
                Thread.sleep(THREAD_SLEEP_TIME);
            } while (processExitCode == -1 && start + maxWaitTime > System.currentTimeMillis());
            if (processExitCode == -1) {
                LOG.debug(String.format("Forcing process  %s to exit as we are past the max wait time of %s millis.", proc, maxWaitTime));
                proc.destroy();
            }
        } else
            processExitCode = proc.waitFor();

        return processExitCode;
    }
}
