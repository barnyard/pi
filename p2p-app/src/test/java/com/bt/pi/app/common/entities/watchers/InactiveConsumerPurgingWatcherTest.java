package com.bt.pi.app.common.entities.watchers;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.isA;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.HashMap;

import org.junit.Before;
import org.junit.Test;

import com.bt.pi.app.common.entities.AllocatableResourceIndexBase;
import com.bt.pi.app.common.entities.HeartbeatTimestampResource;
import com.bt.pi.core.continuation.UpdateResolvingContinuation;
import com.bt.pi.core.dht.DhtClientFactory;
import com.bt.pi.core.dht.DhtWriter;
import com.bt.pi.core.id.PId;
import com.bt.pi.core.testing.UpdateResolvingContinuationAnswer;

public class InactiveConsumerPurgingWatcherTest {
    private InactiveConsumerPurgingWatcher<HeartbeatTimestampResource> inactiveConsumerPurgingWatcher;
    private DhtClientFactory dhtClientFactory;
    private DhtWriter dhtWriter;
    private UpdateResolvingContinuationAnswer updateResolvingContinuationAnswer;
    private PId id;
    private AllocatableResourceIndexBase<HeartbeatTimestampResource> index;
    private HashMap<Long, HeartbeatTimestampResource> allocationMap;
    private HeartbeatTimestampResource heartbeatEntity1;
    private HeartbeatTimestampResource heartbeatEntity2;
    private long now;

    @SuppressWarnings("unchecked")
    @Before
    public void before() {
        id = mock(PId.class);
        now = System.currentTimeMillis();

        heartbeatEntity1 = mock(HeartbeatTimestampResource.class);
        when(heartbeatEntity1.getLastHeartbeatTimestamp()).thenReturn(System.currentTimeMillis() - 60000);
        heartbeatEntity2 = mock(HeartbeatTimestampResource.class);
        when(heartbeatEntity2.getLastHeartbeatTimestamp()).thenReturn(System.currentTimeMillis() + 60000);

        allocationMap = new HashMap<Long, HeartbeatTimestampResource>();
        allocationMap.put(1L, heartbeatEntity1);
        allocationMap.put(2L, heartbeatEntity2);

        index = mock(AllocatableResourceIndexBase.class);
        when(index.getCurrentAllocations()).thenReturn(allocationMap);
        when(index.getInactiveResourceConsumerTimeoutSec()).thenReturn(1L);

        updateResolvingContinuationAnswer = new UpdateResolvingContinuationAnswer(index);

        dhtWriter = mock(DhtWriter.class);
        doAnswer(updateResolvingContinuationAnswer).when(dhtWriter).update(eq(id), isA(UpdateResolvingContinuation.class));

        dhtClientFactory = mock(DhtClientFactory.class);
        when(dhtClientFactory.createWriter()).thenReturn(dhtWriter);

        inactiveConsumerPurgingWatcher = new InactiveConsumerPurgingWatcher<HeartbeatTimestampResource>(dhtClientFactory, id) {
            @Override
            protected long getNow() {
                return now;
            }
        };
    }

    @Test
    public void shouldPurgeInactiveConsumers() {
        // act
        inactiveConsumerPurgingWatcher.run();

        // assert
        assertEquals(1, allocationMap.size());
        assertEquals(null, allocationMap.get(1L));
        assertEquals(heartbeatEntity2, allocationMap.get(2L));
    }

    @Test
    public void shouldDoNothingIfRecordHasNullInactiveInterval() {
        // setup
        when(index.getInactiveResourceConsumerTimeoutSec()).thenReturn(null);

        // act
        inactiveConsumerPurgingWatcher.run();

        // assert
        assertEquals(2, allocationMap.size());
        assertEquals(null, updateResolvingContinuationAnswer.getResult());
    }

    @Test
    public void shouldTimestampConsumersWithNullRecord() {
        // setup
        when(heartbeatEntity1.getLastHeartbeatTimestamp()).thenReturn(null);

        // act
        inactiveConsumerPurgingWatcher.run();

        // assert
        assertEquals(2, allocationMap.size());
        verify(heartbeatEntity1).heartbeat();
    }

    @Test
    public void shouldNotRemoveInactiveConsumerIfInactiveInternalTimeOutIsSetToMinusOne() {
        // setup
        when(index.getInactiveResourceConsumerTimeoutSec()).thenReturn(-1L);

        // act
        inactiveConsumerPurgingWatcher.run();

        // assert
        assertEquals(2, allocationMap.size());
    }
}
