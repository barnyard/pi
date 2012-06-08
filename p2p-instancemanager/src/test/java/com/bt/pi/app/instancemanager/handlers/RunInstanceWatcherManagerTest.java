package com.bt.pi.app.instancemanager.handlers;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.isA;
import static org.mockito.Mockito.verify;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import com.bt.pi.app.common.resource.PiQueue;
import com.bt.pi.core.application.watcher.WatcherApplication;
import com.bt.pi.core.application.watcher.service.WatcherService;
import com.bt.pi.core.application.watcher.task.TaskProcessingQueueWatcher;
import com.bt.pi.core.application.watcher.task.TaskProcessingQueueWatcherProperties;
import com.bt.pi.core.dht.DhtClientFactory;
import com.bt.pi.core.id.KoalaIdFactory;

@RunWith(PowerMockRunner.class)
@PrepareForTest(RunInstanceWatcherManager.class)
@PowerMockIgnore( { "org.apache.commons.logging.*", "org.apache.log4j.*" })
public class RunInstanceWatcherManagerTest {
    private RunInstanceWatcherManager runInstanceWatcherManager = new RunInstanceWatcherManager();;
    @Mock
    private WatcherService watcherService;
    @Mock
    private KoalaIdFactory koalaIdFactory;
    @Mock
    private DhtClientFactory dhtClientFactory;
    @Mock
    private RunInstanceTaskProcessingQueueContinuation runInstanceTaskProcessingQueueContinuation;
    @Mock
    private RunInstanceTaskProcessingExhaustedInstanceContinuation exhaustedInstanceContinuation;
    @Mock
    private TaskProcessingQueueWatcher taskProcessingQueueWatcher;
    @Mock
    private WatcherApplication watcherApplication;

    private String localNodeId = "23";
    private long initialIntervalMillis = 34;
    private long repeatingIntervalMillis = 987;
    private long staleMillis = 222;

    @Before
    public void setUp() throws Exception {
        this.runInstanceWatcherManager.setInitialQueueWatcherIntervalMillis(initialIntervalMillis);
        this.runInstanceWatcherManager.setRepeatingQueueWatcherIntervalMillis(repeatingIntervalMillis);
        this.runInstanceWatcherManager.setStaleQueueItemMillis((int) staleMillis);
        this.runInstanceWatcherManager.setDhtClientFactory(dhtClientFactory);
        this.runInstanceWatcherManager.setKoalaIdFactory(koalaIdFactory);
        this.runInstanceWatcherManager.setWatcherService(watcherService);
        this.runInstanceWatcherManager.setRunInstanceTaskProcessingQueueContinuation(runInstanceTaskProcessingQueueContinuation);
        this.runInstanceWatcherManager.setRunInstanceTaskProcessingExhaustedInstanceContinuation(exhaustedInstanceContinuation);
        this.runInstanceWatcherManager.setWatcherApplication(watcherApplication);
    }

    @Test
    public void testBecomeActive() throws Exception {
        // setup
        PowerMockito.whenNew(TaskProcessingQueueWatcher.class).withArguments(PiQueue.RUN_INSTANCE.getPiLocation(), koalaIdFactory, dhtClientFactory, staleMillis, 5, runInstanceTaskProcessingQueueContinuation, exhaustedInstanceContinuation,
                watcherApplication).thenReturn(taskProcessingQueueWatcher);

        // act
        this.runInstanceWatcherManager.createTaskProcessingQueueWatcher(localNodeId);

        // assert
        PowerMockito.verifyNew(TaskProcessingQueueWatcher.class).withArguments(PiQueue.RUN_INSTANCE.getPiLocation(), koalaIdFactory, dhtClientFactory, staleMillis, 5, runInstanceTaskProcessingQueueContinuation, exhaustedInstanceContinuation,
                watcherApplication);
        verify(this.watcherService).replaceTask(eq(RunInstanceWatcherManager.RUN_INSTANCE_QUEUE_WATCHER), isA(Runnable.class), eq(initialIntervalMillis), eq(repeatingIntervalMillis));
    }

    @Test
    public void testTaskProcessingQueueWatcherProperties() {
        // act
        TaskProcessingQueueWatcherProperties annotation = runInstanceWatcherManager.getClass().getAnnotation(TaskProcessingQueueWatcherProperties.class);

        // assert
        assertEquals(30 * 60 * 1000, annotation.staleQueueItemMillis());
        assertEquals(270 * 1000, annotation.initialQueueWatcherIntervalMillis());
        assertEquals(270 * 1000, annotation.repeatingQueueWatcherIntervalMillis());
    }
}
