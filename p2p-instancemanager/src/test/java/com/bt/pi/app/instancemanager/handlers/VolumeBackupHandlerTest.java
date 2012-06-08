package com.bt.pi.app.instancemanager.handlers;

import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.argThat;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.isA;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentMatcher;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.runners.MockitoJUnitRunner;
import org.mockito.stubbing.Answer;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import rice.Continuation;

import com.bt.pi.app.common.entities.BlockDeviceMapping;
import com.bt.pi.app.common.entities.Instance;
import com.bt.pi.app.instancemanager.handlers.DetachVolumeHandler;
import com.bt.pi.app.instancemanager.handlers.VolumeBackupHandler;
import com.bt.pi.core.application.resource.ConsumedDhtResourceRegistry;
import com.bt.pi.core.cli.commands.CommandExecutionException;
import com.bt.pi.core.util.common.CommandRunner;

@RunWith(MockitoJUnitRunner.class)
public class VolumeBackupHandlerTest {
    @Mock
    private DetachVolumeHandler detachVolumeHandler;
    @Mock
    private ConsumedDhtResourceRegistry consumedDhtResourceRegistry;
    @Mock
    private ThreadPoolTaskExecutor taskExecutor;
    @Mock
    private CommandRunner commandRunner;
    @Mock
    private File mockFile;

    private Instance instanceA;
    private Instance instanceB;

    @InjectMocks
    VolumeBackupHandler volumeBackupHandler = new VolumeBackupHandler() {
        @Override
        protected File createTempFile(BlockDeviceMapping blockDevice) throws IOException {
            return mockFile;
        };

        @Override
        protected boolean existsFile(String localVolumeFilename) {
            return true;
        };
    };

    @Before
    public void before() throws Exception {
        volumeBackupHandler.setNfsVolumesDirectory("./build");
        volumeBackupHandler.setLocalVolumesDirectory("build");

        instanceA = new Instance();
        instanceA.setInstanceId("instanceA");
        instanceA.setBlockDeviceMappings(Arrays.asList(new BlockDeviceMapping("vol-1A")));

        instanceB = new Instance();
        instanceB.setInstanceId("instanceB");
        instanceB.setBlockDeviceMappings(Arrays.asList(new BlockDeviceMapping("vol-2A"), new BlockDeviceMapping("vol-2B")));

        when(consumedDhtResourceRegistry.getByType(Instance.class)).thenReturn(Arrays.asList(instanceA, instanceB));

        when(detachVolumeHandler.acquireLock(isA(String.class), eq(1L), eq(TimeUnit.MILLISECONDS))).thenReturn(true);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void shouldStartBackupForAllRegisteredInstances() throws InterruptedException, IOException {
        // setup
        when(mockFile.getPath()).thenReturn("./build/vol-tmp");
        final CountDownLatch latch = new CountDownLatch(1);
        doAnswer(new Answer<Object>() {

            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                Runnable r = (Runnable) invocation.getArguments()[0];
                r.run();
                latch.countDown();
                return null;
            }
        }).when(taskExecutor).execute(isA(Runnable.class));

        Continuation mockContinuation = mock(Continuation.class);

        // act
        volumeBackupHandler.startBackup(mockContinuation);

        latch.await(5, TimeUnit.SECONDS);
        // assert
        verify(taskExecutor).execute(isA(Runnable.class));
        verify(commandRunner, times(6)).runNicely(argThat(new ArgumentMatcher<String>() {

            @Override
            public boolean matches(Object argument) {
                String command = (String) argument;
                boolean copyMatcher = Pattern.compile("cp\\s.*\\/vol-.*\\s.\\/build\\/vol-tmp").matcher(command).matches();
                boolean moveMatcher = Pattern.compile("mv\\s.*vol-tmp\\s.*vol-.*").matcher(command).matches();
                assertTrue(copyMatcher || moveMatcher);
                return copyMatcher || moveMatcher;
            }

        }));
        verify(mockContinuation).receiveResult(null);
    }

    @Test
    public void shouldNotCopyVolumeIfUnableToCreateTempFile() throws InterruptedException {
        // setup
        VolumeBackupHandler h = new VolumeBackupHandler() {
            @Override
            protected File createTempFile(BlockDeviceMapping blockDevice) throws IOException {
                throw new IOException();
            }
        };
        h.setThreadPoolTaskExecutor(taskExecutor);
        h.setConsumedDhtResourceRegistry(consumedDhtResourceRegistry);
        when(consumedDhtResourceRegistry.getByType(Instance.class)).thenReturn(Arrays.asList(instanceA));

        final CountDownLatch latch = new CountDownLatch(1);
        doAnswer(new Answer<Object>() {

            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                Runnable r = (Runnable) invocation.getArguments()[0];
                r.run();
                latch.countDown();
                return null;
            }
        }).when(taskExecutor).execute(isA(Runnable.class));

        // act
        h.startBackup(null);
        latch.await(5, TimeUnit.SECONDS);

        // assert
        verify(commandRunner, never()).runNicely(anyString());
    }

    @Test
    public void shouldRemoveTempFileIfUnableToCompleteTheBackup() throws InterruptedException {
        // setup
        when(commandRunner.runNicely(anyString())).thenThrow(new CommandExecutionException());
        when(mockFile.getPath()).thenReturn("./build/vol-tmp");
        when(mockFile.exists()).thenReturn(true);

        final CountDownLatch latch = new CountDownLatch(1);
        doAnswer(new Answer<Object>() {

            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                Runnable r = (Runnable) invocation.getArguments()[0];
                r.run();
                latch.countDown();
                return null;
            }
        }).when(taskExecutor).execute(isA(Runnable.class));

        // act
        volumeBackupHandler.startBackup(null);
        latch.await(5, TimeUnit.SECONDS);

        // assert
        verify(mockFile, times(3)).delete();
    }

}
