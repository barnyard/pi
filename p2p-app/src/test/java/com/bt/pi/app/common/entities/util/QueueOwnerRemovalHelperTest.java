package com.bt.pi.app.common.entities.util;

import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.isA;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import com.bt.pi.core.continuation.UpdateResolvingPiContinuation;
import com.bt.pi.core.dht.DhtClientFactory;
import com.bt.pi.core.dht.DhtWriter;
import com.bt.pi.core.entity.PiLocation;
import com.bt.pi.core.entity.TaskProcessingQueue;
import com.bt.pi.core.id.KoalaIdFactory;
import com.bt.pi.core.id.PId;
import com.bt.pi.core.parser.KoalaPiEntityFactory;
import com.bt.pi.core.scope.NodeScope;

public class QueueOwnerRemovalHelperTest {
    QueueOwnerRemovalHelper queueOwnerRemovalHelper;
    DhtClientFactory dhtClientFactory;
    KoalaIdFactory koalaIdFactory;
    DhtWriter dhtWriter;
    List<PiLocation> queueLocations;
    TaskProcessingQueue queue;
    QueueOwnerRemovalContinuation continuation;
    String OwnerId = "nodeId";

    @SuppressWarnings("unchecked")
    @Before
    public void before() {

        queue = mock(TaskProcessingQueue.class);

        queueLocations = new ArrayList<PiLocation>();

        koalaIdFactory = new KoalaIdFactory(255, 255);
        koalaIdFactory.setKoalaPiEntityFactory(new KoalaPiEntityFactory());

        queueLocations.add(new PiLocation("queue1", NodeScope.REGION));
        queueLocations.add(new PiLocation("queue2", NodeScope.REGION));
        queueLocations.add(new PiLocation("queue3", NodeScope.REGION));
        queueLocations.add(new PiLocation("queue4", NodeScope.REGION));

        dhtWriter = mock(DhtWriter.class);
        doAnswer(new Answer<Object>() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                ((UpdateResolvingPiContinuation) invocation.getArguments()[1]).update(queue, null);
                return null;
            }
        }).when(dhtWriter).update(isA(PId.class), isA(UpdateResolvingPiContinuation.class));

        continuation = new QueueOwnerRemovalContinuation(OwnerId) {

            @Override
            public void handleResult(TaskProcessingQueue result) {
            }
        };

        dhtClientFactory = mock(DhtClientFactory.class);
        when(dhtClientFactory.createWriter()).thenReturn(dhtWriter);

        queueOwnerRemovalHelper = new QueueOwnerRemovalHelper();
        queueOwnerRemovalHelper.setKoalaIdFactory(koalaIdFactory);
        queueOwnerRemovalHelper.setDhtClientFactory(dhtClientFactory);
    }

    @Test
    public void shouldRemoveOwnerFromAllQueues() {
        // act
        queueOwnerRemovalHelper.removeNodeIdFromAllQueues(queueLocations, continuation);

        // assert
        verify(queue, times(4)).removeOwnerFromAllTasks(eq(OwnerId));
    }

    @Test
    public void shouldNotRemoveOwnerWhenOwnerIsNull() {
        // setup
        continuation = new QueueOwnerRemovalContinuation(null) {
            @Override
            public void handleResult(TaskProcessingQueue result) {
            }
        };

        // act
        queueOwnerRemovalHelper.removeNodeIdFromAllQueues(queueLocations, continuation);

        // assert
        verify(queue, never()).removeOwnerFromAllTasks(anyString());
    }

    @Test
    public void shouldNotRemoveOwnerWhenLocationsAreNull() {
        // act
        queueOwnerRemovalHelper.removeNodeIdFromAllQueues(null, continuation);

        // assert
        verify(queue, never()).removeOwnerFromAllTasks(anyString());
    }

}
