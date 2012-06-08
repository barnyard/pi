package com.bt.pi.app.instancemanager.handlers;

import static org.mockito.Mockito.verify;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class InstanceManagerApplicationQueueWatcherInitiatorTest {
    private String nodeIdFull = "nodeId1234";

    @InjectMocks
    private InstanceManagerApplicationQueueWatcherInitiator i = new InstanceManagerApplicationQueueWatcherInitiator();

    @Mock
    private RunInstanceWatcherManager runInstanceWatcherManager;
    @Mock
    private TerminateInstanceTaskQueueWatcherInitiator terminateInstanceTaskQueueWatcherInitiator;
    @Mock
    private PauseInstanceTaskQueueWatcherInitiator pauseInstanceTaskQueueWatcherInitiator;
    @Mock
    private RemoveInstanceFromUserTaskQueueWatcherInitiator removeInstanceFromUserTaskQueueWatcherInitiator;

    @Test
    public void shouldCreateAllTaskWatchersForInstanceManagerApplication() {
        // act
        i.initialiseWatchers(nodeIdFull);

        // assert
        verify(this.runInstanceWatcherManager).createTaskProcessingQueueWatcher(nodeIdFull);
        verify(this.terminateInstanceTaskQueueWatcherInitiator).createTaskProcessingQueueWatcher(nodeIdFull);
        verify(this.pauseInstanceTaskQueueWatcherInitiator).createTaskProcessingQueueWatcher(nodeIdFull);
        verify(this.removeInstanceFromUserTaskQueueWatcherInitiator).createTaskProcessingQueueWatcher(nodeIdFull);
    }

    @Test
    public void shouldRemoveAllTaskWatchersForInstanceManagerApplication() {
        // act
        i.removeWatchers();

        // assert
        verify(this.runInstanceWatcherManager).removeTaskProcessingQueueWatcher();
        verify(this.terminateInstanceTaskQueueWatcherInitiator).removeTaskProcessingQueueWatcher();
        verify(this.pauseInstanceTaskQueueWatcherInitiator).removeTaskProcessingQueueWatcher();
        verify(this.removeInstanceFromUserTaskQueueWatcherInitiator).removeTaskProcessingQueueWatcher();
    }
}
