package com.bt.pi.app.instancemanager.handlers;

import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.isA;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.runners.MockitoJUnitRunner;
import org.mockito.stubbing.Answer;

import com.bt.pi.app.common.entities.Instance;
import com.bt.pi.app.common.entities.InstanceState;
import com.bt.pi.app.common.id.PiIdBuilder;
import com.bt.pi.app.common.resource.PiQueue;
import com.bt.pi.app.common.resource.PiTopics;
import com.bt.pi.core.application.PubSubMessageContext;
import com.bt.pi.core.application.watcher.task.TaskProcessingQueueHelper;
import com.bt.pi.core.continuation.UpdateResolvingPiContinuation;
import com.bt.pi.core.dht.DhtClientFactory;
import com.bt.pi.core.dht.DhtWriter;
import com.bt.pi.core.entity.EntityMethod;
import com.bt.pi.core.id.PId;

@RunWith(MockitoJUnitRunner.class)
public class RunInstanceTaskProcessingQueueContinuationTest {
    @InjectMocks
    private RunInstanceTaskProcessingQueueContinuation runInstanceTaskProcessingQueueContinuation = new RunInstanceTaskProcessingQueueContinuation();
    private String nodeId = "nodeId";
    private String url = "url";
    @Mock
    private DhtClientFactory dhtClientFactory;
    @Mock
    private DhtWriter dhtWriter;
    @Mock
    private PId instancePastryId;
    @Mock
    private Instance instance;
    private String instanceId = "instanceId";
    @Mock
    private InstanceManagerApplication instanceManagerApplication;
    @Mock
    private PubSubMessageContext pubSubMessageContext;
    @Mock
    private TaskProcessingQueueHelper taskProcessingQueueHelper;
    @Mock
    PiIdBuilder piIdBuilder;
    @Mock
    private PId runInstanceQueueId;

    @SuppressWarnings("unchecked")
    @Before
    public void setUp() throws Exception {
        when(this.dhtClientFactory.createWriter()).thenReturn(this.dhtWriter);
        when(this.piIdBuilder.getPIdForEc2AvailabilityZone(url)).thenReturn(instancePastryId);
        when(piIdBuilder.getPiQueuePId(PiQueue.RUN_INSTANCE)).thenReturn(runInstanceQueueId);
        when(runInstanceQueueId.forLocalScope(PiQueue.RUN_INSTANCE.getNodeScope())).thenReturn(runInstanceQueueId);
        doAnswer(new Answer<Object>() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                UpdateResolvingPiContinuation<Instance> continuation = (UpdateResolvingPiContinuation<Instance>) invocation.getArguments()[1];
                Instance updateResult = continuation.update(instance, null);
                continuation.handleResult(updateResult);
                return null;
            }
        }).when(this.dhtWriter).update(eq(instancePastryId), isA(UpdateResolvingPiContinuation.class));

        when(this.instanceManagerApplication.newLocalPubSubMessageContext(PiTopics.RUN_INSTANCE)).thenReturn(pubSubMessageContext);
        when(instance.getInstanceId()).thenReturn(instanceId);
    }

    @Test
    public void testReceiveResult() {
        // setup
        when(instance.getState()).thenReturn(InstanceState.PENDING);

        // act
        this.runInstanceTaskProcessingQueueContinuation.receiveResult(url, nodeId);

        // assert
        verify(pubSubMessageContext).randomAnycast(EntityMethod.CREATE, instance);
        verify(instance).setNodeId(null);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void shouldDoNothingIfUnableToGetInstance() {
        // setup
        doAnswer(new Answer<Object>() {

            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                UpdateResolvingPiContinuation<Instance> continuation = (UpdateResolvingPiContinuation<Instance>) invocation.getArguments()[1];
                continuation.update(null, null);
                return null;
            }
        }).when(this.dhtWriter).update(eq(instancePastryId), isA(UpdateResolvingPiContinuation.class));

        // act
        this.runInstanceTaskProcessingQueueContinuation.receiveResult(url, nodeId);

        // assert
        verify(this.taskProcessingQueueHelper, never()).removeUrlFromQueue(eq(runInstanceQueueId), anyString());
        verify(pubSubMessageContext, never()).randomAnycast(EntityMethod.CREATE, instance);
    }

    @Test
    public void testReceiveResultWithTerminatedInstance() {
        // setup
        when(instance.getState()).thenReturn(InstanceState.TERMINATED);

        // act
        this.runInstanceTaskProcessingQueueContinuation.receiveResult(url, nodeId);

        // assert
        verify(this.taskProcessingQueueHelper).removeUrlFromQueue(runInstanceQueueId, instance.getUrl());
        verify(pubSubMessageContext, never()).randomAnycast(EntityMethod.CREATE, instance);
    }
}
