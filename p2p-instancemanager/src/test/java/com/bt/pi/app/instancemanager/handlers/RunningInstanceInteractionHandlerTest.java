package com.bt.pi.app.instancemanager.handlers;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.isA;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.runners.MockitoJUnitRunner;
import org.mockito.stubbing.Answer;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import com.bt.pi.app.common.entities.ConsoleOutput;
import com.bt.pi.app.common.entities.Instance;
import com.bt.pi.app.common.id.PiIdBuilder;
import com.bt.pi.app.common.images.platform.ImagePlatform;
import com.bt.pi.app.common.resource.PiQueue;
import com.bt.pi.app.instancemanager.images.InstanceImageManager;
import com.bt.pi.app.instancemanager.libvirt.LibvirtManager;
import com.bt.pi.core.application.ReceivedMessageContext;
import com.bt.pi.core.application.watcher.task.TaskProcessingQueueHelper;
import com.bt.pi.core.entity.EntityResponseCode;
import com.bt.pi.core.id.PId;
import com.bt.pi.core.util.common.CommandResult;
import com.bt.pi.core.util.common.CommandRunner;

@RunWith(MockitoJUnitRunner.class)
public class RunningInstanceInteractionHandlerTest {
    @InjectMocks
    private RunningInstanceInteractionHandler runningInstanceHandler = new RunningInstanceInteractionHandler();

    private static final String XM_CONSOLE_COMMAND = "xm console %s";
    private static final String TEST_OUT_FILENAME = "%s.out";
    private static final String PUT_IT_BACK = "Put it back.";
    private static final String SLAP = "*slap*";
    private static final String LINE2 = "Buzz";
    private static final String LINE1 = "Google";
    private static final String I_KARATE = "i-Karate";
    @Mock
    private ThreadPoolTaskExecutor threadPoolTaskExecutor;
    @Mock
    private InstanceImageManager instanceImageManager;
    @Mock
    private LibvirtManager libvirtManager;
    @Mock
    private Instance instance;
    private ConsoleOutput consoleOutput;
    private ArrayList<String> outputLines;
    @Mock
    private ReceivedMessageContext receivedMessageContext;
    private File consoleOutputFile;
    private long maxWaitTime = 10L;
    @Mock
    private TaskProcessingQueueHelper taskProcessingQueueHelper;
    @Mock
    private CommandRunner commandrunner;
    @Mock
    private PId pauseQueuePId;
    @Mock
    private PiIdBuilder piIdBuilder;

    @Before
    public void before() throws IOException {
        doAnswer(new Answer<Object>() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                ((Runnable) invocation.getArguments()[0]).run();
                return null;
            }
        }).when(threadPoolTaskExecutor).execute(isA(Runnable.class));

        consoleOutput = new ConsoleOutput();
        consoleOutput.setInstanceId(I_KARATE);
        consoleOutput.setImagePlatform(ImagePlatform.linux);

        consoleOutputFile = new File("./" + I_KARATE + ".out");

        outputLines = new ArrayList<String>();
        outputLines.add(LINE1);
        outputLines.add(LINE2);
        CommandResult commandResult = new CommandResult(0, outputLines, new ArrayList<String>());

        ArrayList<String> secondOutput = new ArrayList<String>();
        secondOutput.add(SLAP);
        secondOutput.add(PUT_IT_BACK);

        secondOutput.addAll(outputLines);
        CommandResult secondCommandResult = new CommandResult(0, secondOutput, new ArrayList<String>());

        when(commandrunner.run(eq(String.format(XM_CONSOLE_COMMAND, I_KARATE)), eq(maxWaitTime), eq(true))).thenReturn(commandResult).thenReturn(secondCommandResult);
        FileUtils.writeLines(consoleOutputFile, secondOutput);

        when(receivedMessageContext.getReceivedEntity()).thenReturn(consoleOutput);

        runningInstanceHandler.setConsoleOutputFileNameFormat(TEST_OUT_FILENAME);
        runningInstanceHandler.setConsoleOutputDirectory(".");
        runningInstanceHandler.setConsoleOutputCommand(XM_CONSOLE_COMMAND);
        runningInstanceHandler.setConsoleOutputMaxWaitTime(maxWaitTime);
        when(instance.getInstanceId()).thenReturn(I_KARATE);
        when(instance.getUrl()).thenReturn(Instance.getUrl(I_KARATE));
    }

    @After
    public void after() {
        consoleOutputFile.delete();
    }

    @Test
    public void shouldCallInstanceImageManagerInNewThread() {

        // act
        runningInstanceHandler.rebootInstance(instance);

        // assert
        verify(instanceImageManager).rebootInstance(eq(instance));
    }

    @Test
    public void shouldReturnConsoleOutput() {

        // act
        runningInstanceHandler.respondWithConsoleOutput(receivedMessageContext);

        // assert
        verify(receivedMessageContext).sendResponse(eq(EntityResponseCode.OK), eq(consoleOutput));
        assertEquals(I_KARATE, consoleOutput.getInstanceId());
        assertTrue(consoleOutput.getOutput().contains(LINE1));
        assertTrue(consoleOutput.getOutput().contains(LINE2));
    }

    @Test
    public void shouldReturnConsoleOutputForWindows() {
        // setup
        consoleOutput.setImagePlatform(ImagePlatform.windows);

        // act
        runningInstanceHandler.respondWithConsoleOutput(receivedMessageContext);

        // assert
        verify(receivedMessageContext).sendResponse(eq(EntityResponseCode.OK), eq(consoleOutput));
        assertEquals(I_KARATE, consoleOutput.getInstanceId());
        assertTrue(consoleOutput.getOutput().contains(LINE1));
        assertTrue(consoleOutput.getOutput().contains(LINE2));
    }

    @Test
    public void shouldReturnConsoleOutputFromMultipleCalls() {
        // setup
        runningInstanceHandler.respondWithConsoleOutput(receivedMessageContext);
        consoleOutput.setOutput(null);

        // act
        runningInstanceHandler.respondWithConsoleOutput(receivedMessageContext);

        // assert
        verify(receivedMessageContext, times(2)).sendResponse(eq(EntityResponseCode.OK), isA(ConsoleOutput.class));
        assertEquals(I_KARATE, consoleOutput.getInstanceId());
        assertTrue(consoleOutput.getOutput().contains(LINE1));
        assertEquals(2, consoleOutput.getOutput().split(LINE2).length);
        assertTrue(consoleOutput.getOutput().contains(SLAP));
        assertTrue(consoleOutput.getOutput().contains(PUT_IT_BACK));
    }

    @Test
    public void shouldReturnErrorCodeWhenThingsPuke() {
        // setup
        ConsoleOutput mockConsoleOutput = mock(ConsoleOutput.class);
        final AtomicInteger count = new AtomicInteger(0);
        doAnswer(new Answer<Object>() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                // we throw on the first set because that is when we do a file read.
                if (count.intValue() == 0) {
                    count.addAndGet(1);
                    throw new IOException("booo");
                }
                return null;
            }
        }).when(mockConsoleOutput).setOutput(anyString());
        when(mockConsoleOutput.getInstanceId()).thenReturn(I_KARATE);
        when(mockConsoleOutput.getImagePlatform()).thenReturn(ImagePlatform.linux);

        ReceivedMessageContext errorMessageContext = mock(ReceivedMessageContext.class);
        when(errorMessageContext.getReceivedEntity()).thenReturn(mockConsoleOutput);

        // act
        runningInstanceHandler.respondWithConsoleOutput(errorMessageContext);

        // assert
        verify(errorMessageContext).sendResponse(eq(EntityResponseCode.ERROR), isA(ConsoleOutput.class));
    }

    @Test
    public void shouldPauseInstance() {
        // setup
        when(piIdBuilder.getPId(PiQueue.PAUSE_INSTANCE.getUrl())).thenReturn(pauseQueuePId);
        when(pauseQueuePId.forLocalScope(PiQueue.PAUSE_INSTANCE.getNodeScope())).thenReturn(pauseQueuePId);

        // act
        runningInstanceHandler.pauseInstance(instance);

        // assert
        verify(libvirtManager).pauseInstance(instance.getInstanceId());
        verify(taskProcessingQueueHelper).removeUrlFromQueue(pauseQueuePId, Instance.getUrl(I_KARATE));
    }

    @Test
    public void shouldUnPauseInsance() {
        // act
        runningInstanceHandler.unPauseInstance(instance);

        // assert
        verify(libvirtManager).unPauseInstance(instance.getInstanceId());
    }
}
