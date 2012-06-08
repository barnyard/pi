package com.ragstorooks.testrr.cli;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.isA;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.concurrent.Executor;

import org.junit.Before;
import org.junit.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

public class CommandExecutorTest {
    private CommandExecutor commandExecutor;
    private Thread shutdownHookThread;

    @Before
    public void before() {
        commandExecutor = new CommandExecutor(mock(Executor.class));
    }

    /**
     * Require task executor
     */
    @Test(expected = RuntimeException.class)
    public void requireTaskExecutor() throws Exception {
        // setup
        commandExecutor = new CommandExecutor(null);

        // act
        commandExecutor.executeScript(null, null);
    }

    /**
     * Verify that executeScript returns value after waiting for process to
     * finish
     */
    @Test
    public void shouldReturnProcessResult() throws Exception {
        // setup
        String[] command = new String[] { "a", "b" };
        int returnValue = 200;

        Process process = mock(Process.class);
        when(process.waitFor()).thenReturn(returnValue);
        Runtime runtime = mock(Runtime.class);
        when(runtime.exec(command)).thenReturn(process);

        // act
        int result = commandExecutor.executeScript(command, runtime);

        // assert
        assertEquals(returnValue, result);
    }

    @Test
    public void shouldForceProcessToExitAferTimeOut() throws Exception {
        // setup
        String[] command = new String[] { "a", "b" };
        Process process = mock(Process.class);
        when(process.exitValue()).thenThrow(new IllegalThreadStateException("Leave me alone! I am still running."));

        Runtime runtime = mock(Runtime.class);
        when(runtime.exec(command)).thenReturn(process);

        // act
        int result = commandExecutor.executeScript(command, runtime, 5000L, false);

        assertEquals(-1, result);
        verify(process).destroy();
    }

    @Test
    public void shouldAddShutdownHookForProcessAndInvokeDestroy() throws Exception {
        // setup
        String[] command = new String[] { "a", "b" };
        Process process = mock(Process.class);
        when(process.exitValue()).thenThrow(new IllegalThreadStateException("Leave me alone! I am still running."));

        Runtime runtime = mock(Runtime.class);
        when(runtime.exec(command)).thenReturn(process);

        doAnswer(new Answer<Object>() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                shutdownHookThread = ((Thread) invocation.getArguments()[0]);
                return null;
            }
        }).when(runtime).addShutdownHook(isA(Thread.class));
        commandExecutor.executeScript(command, runtime, 100L, true);

        // act
        shutdownHookThread.run();

        // verify
        verify(process, times(2)).destroy();

    }
}
