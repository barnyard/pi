package com.bt.pi.app.common.entities.watchers.instance;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.mock;

import org.junit.Before;
import org.junit.Test;

import com.bt.pi.core.id.PId;

public class InstanceWatchingStrategyTest {
    private InstanceWatchingStrategy instanceWatchingStrategy;
    private InstanceRefreshHandler instanceRefreshHandler;
    private PId id;

    @Before
    public void before() {
        id = mock(PId.class);
        instanceWatchingStrategy = new InstanceWatchingStrategy();
        instanceWatchingStrategy.setInstanceRefreshHandler(instanceRefreshHandler);
    }

    @Test
    public void shouldReturnNullConsumerWatcher() {
        // act
        Runnable res = instanceWatchingStrategy.getConsumerWatcher(id, "consumerId");

        // assert
        assertNull(res);
    }

    @Test
    public void shouldOverrideDefaultInitialRefreshInterval() {
        // setup
        this.instanceWatchingStrategy.setInitialResourceRefreshIntervalMillis(10);

        // act
        long res = instanceWatchingStrategy.getInitialResourceRefreshIntervalMillis();

        // assert
        assertEquals(10, res);
    }

    @Test
    public void shouldOverrideDefaultRepeatingRefreshInterval() {
        // setup
        this.instanceWatchingStrategy.setRepeatingResourceRefreshIntervalMillis(20);

        // act
        long res = instanceWatchingStrategy.getRepeatingResourceRefreshIntervalMillis();

        // assert
        assertEquals(20, res);
    }
}
