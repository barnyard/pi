package com.bt.pi.app.imagemanager;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.isA;
import static org.mockito.Mockito.verify;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import com.bt.pi.core.application.watcher.service.WatcherService;
import com.bt.pi.core.application.watcher.task.TaskProcessingQueueWatcher;
import com.bt.pi.core.application.watcher.task.TaskProcessingQueueWatcherProperties;

@RunWith(MockitoJUnitRunner.class)
public class ImageManagerApplicationWatcherManagerTest {
    @InjectMocks
    private ImageManagerApplicationWatcherManager imageManagerApplicationWatcherManager = new ImageManagerApplicationWatcherManager();
    @Mock
    private WatcherService watcherService;
    private String localNodeId = "23";
    private long initialIntervalMillis = 34;
    private long repeatingIntervalMillis = 987;

    @Before
    public void setUp() throws Exception {
        this.imageManagerApplicationWatcherManager.setInitialQueueWatcherIntervalMillis(initialIntervalMillis);
        this.imageManagerApplicationWatcherManager.setRepeatingQueueWatcherIntervalMillis(repeatingIntervalMillis);
        this.imageManagerApplicationWatcherManager.setStaleQueueItemMillis(222); // for Emma
    }

    @Test
    public void testBecomeActive() {
        // setup

        // act
        this.imageManagerApplicationWatcherManager.createTaskProcessingQueueWatcher(localNodeId);

        // assert
        verify(this.watcherService).replaceTask(eq(ImageManagerApplicationWatcherManager.DECRYPT_IMAGE_QUEUE_WATCHER), isA(TaskProcessingQueueWatcher.class), eq(initialIntervalMillis), eq(repeatingIntervalMillis));
    }

    @Test
    public void testTaskProcessingQueueWatcherProperties() {
        // act
        TaskProcessingQueueWatcherProperties annotation = imageManagerApplicationWatcherManager.getClass().getAnnotation(TaskProcessingQueueWatcherProperties.class);

        // assert
        assertEquals(30 * 60 * 1000, annotation.staleQueueItemMillis());
        assertEquals(260 * 1000, annotation.initialQueueWatcherIntervalMillis());
        assertEquals(260 * 1000, annotation.repeatingQueueWatcherIntervalMillis());
    }
}
