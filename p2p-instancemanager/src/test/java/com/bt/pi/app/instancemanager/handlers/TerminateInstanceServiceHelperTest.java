package com.bt.pi.app.instancemanager.handlers;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.isA;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.runners.MockitoJUnitRunner;
import org.mockito.stubbing.Answer;

import com.bt.pi.app.common.entities.AvailabilityZone;
import com.bt.pi.app.common.entities.AvailabilityZones;
import com.bt.pi.app.common.entities.Instance;
import com.bt.pi.app.common.id.PiIdBuilder;
import com.bt.pi.app.common.resource.PiQueue;
import com.bt.pi.core.application.MessageContextFactory;
import com.bt.pi.core.application.watcher.task.TaskProcessingQueueHelper;
import com.bt.pi.core.continuation.scattergather.ScatterGatherContinuationRunnable;
import com.bt.pi.core.continuation.scattergather.ScatterGatherContinuationRunner;
import com.bt.pi.core.id.KoalaIdFactory;
import com.bt.pi.core.id.PId;

@RunWith(MockitoJUnitRunner.class)
public class TerminateInstanceServiceHelperTest {

    private TaskProcessingQueueHelper taskProcessingQueueHelper;
    private TerminateInstanceServiceHelper terminateInstanceServiceHelper;
    private ScatterGatherContinuationRunner scatterGatherContinuationRunner;
    private Integer globalAvzCode = 345;
    @Mock
    private PId runInstanceQueueId;
    @Mock
    private PId terminateInstanceQueueId;
    @Mock
    private PId instanceNetworkManagerTeardownQueueId;
    @Mock
    private PiIdBuilder piIdBuilder;

    @Before
    public void before() {
        taskProcessingQueueHelper = mock(TaskProcessingQueueHelper.class);

        when(piIdBuilder.getGlobalAvailabilityZoneCodeFromEc2Id(isA(String.class))).thenReturn(globalAvzCode);
        when(piIdBuilder.getPId(PiQueue.RUN_INSTANCE.getUrl())).thenReturn(runInstanceQueueId);
        when(piIdBuilder.getPId(PiQueue.TERMINATE_INSTANCE.getUrl())).thenReturn(terminateInstanceQueueId);
        when(piIdBuilder.getPId(PiQueue.INSTANCE_NETWORK_MANAGER_TEARDOWN.getUrl())).thenReturn(instanceNetworkManagerTeardownQueueId);
        when(runInstanceQueueId.forGlobalAvailablityZoneCode(globalAvzCode)).thenReturn(runInstanceQueueId);
        when(terminateInstanceQueueId.forGlobalAvailablityZoneCode(globalAvzCode)).thenReturn(terminateInstanceQueueId);
        when(instanceNetworkManagerTeardownQueueId.forGlobalAvailablityZoneCode(globalAvzCode)).thenReturn(instanceNetworkManagerTeardownQueueId);

        KoalaIdFactory koalaIdFactory = mock(KoalaIdFactory.class);
        when(koalaIdFactory.getRegion()).thenReturn(1);

        scatterGatherContinuationRunner = mock(ScatterGatherContinuationRunner.class);

        MessageContextFactory messageContextFactory = mock(MessageContextFactory.class);
        AvailabilityZones zones = new AvailabilityZones();
        zones.addAvailabilityZone(new AvailabilityZone("1", 1, 10, "on"));
        // when(messageContextFactory.getAvailabilityZonesRecord()).thenReturn(zones);

        terminateInstanceServiceHelper = spy(new TerminateInstanceServiceHelper());
        terminateInstanceServiceHelper.setTaskProcessingQueueHelper(taskProcessingQueueHelper);
        terminateInstanceServiceHelper.setPiIdBuilder(piIdBuilder);
        terminateInstanceServiceHelper.setScatterGatherContinuationRunner(scatterGatherContinuationRunner);
        terminateInstanceServiceHelper.setMessageContextFactory(messageContextFactory);
        terminateInstanceServiceHelper.setKoalaIdFactory(koalaIdFactory);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testTerminateInstances() {
        // setup
        ArrayList<String> ids = new ArrayList<String>();
        ids.add("uno");
        ids.add("dos");
        ids.add("tres");
        doAnswer(new Answer<Object>() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                Collection<ScatterGatherContinuationRunnable> runnables = (Collection<ScatterGatherContinuationRunnable>) invocation.getArguments()[0];
                assertEquals(3, runnables.size());
                return null;
            }
        }).when(scatterGatherContinuationRunner).execute((Collection<ScatterGatherContinuationRunnable>) anyObject(), anyInt(), isA(TimeUnit.class));

        // act
        terminateInstanceServiceHelper.terminateInstance("bob", ids);

        // verify
        verify(taskProcessingQueueHelper, times(3)).removeUrlFromQueue(eq(runInstanceQueueId), anyString());
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testTerminateBuriedInstances() {
        // setup
        ArrayList<String> ids = new ArrayList<String>();
        ids.add("uno");
        ids.add("dos");
        ids.add("tres");
        doAnswer(new Answer<Object>() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                Collection<ScatterGatherContinuationRunnable> runnables = (Collection<ScatterGatherContinuationRunnable>) invocation.getArguments()[0];
                assertEquals(3, runnables.size());
                return null;
            }
        }).when(scatterGatherContinuationRunner).execute((Collection<ScatterGatherContinuationRunnable>) anyObject(), anyInt(), isA(TimeUnit.class));

        // act
        terminateInstanceServiceHelper.terminateBuriedInstance(ids);

        // verify
        verify(terminateInstanceServiceHelper).scatterWriter(isA(List.class), isA(TerminateBuriedInstancesContinuation.class));
    }

    @Test
    public void shouldAddTerminateInstanceTaskInQueue() {
        // setup
        List<String> instanceIds = new ArrayList<String>();
        instanceIds.add("instanceA");

        // act
        terminateInstanceServiceHelper.terminateInstance("bob", instanceIds);

        // assert
        verify(taskProcessingQueueHelper).addUrlToQueue(terminateInstanceQueueId, Instance.getUrl("instanceA"), 5);
    }

    @Test
    public void shouldAddTerminateInstanceTaskForMultipleInstancesInQueue() {
        // setup
        List<String> instanceIds = new ArrayList<String>();
        instanceIds.add("instanceA");
        instanceIds.add("instanceB");

        // act
        terminateInstanceServiceHelper.terminateInstance("bob", instanceIds);

        // assert
        for (String instanceId : instanceIds) {
            verify(taskProcessingQueueHelper).addUrlToQueue(terminateInstanceQueueId, "inst:" + instanceId, 5);
            verify(taskProcessingQueueHelper).addUrlToQueue(instanceNetworkManagerTeardownQueueId, "inst:" + instanceId, 5);
        }
    }
}
