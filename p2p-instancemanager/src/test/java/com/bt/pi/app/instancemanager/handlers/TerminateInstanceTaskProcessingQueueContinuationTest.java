package com.bt.pi.app.instancemanager.handlers;

import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.isA;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import com.bt.pi.app.common.entities.Instance;
import com.bt.pi.app.common.id.PiIdBuilder;
import com.bt.pi.app.common.resource.PiQueue;
import com.bt.pi.app.instancemanager.handlers.InstanceManagerApplication;
import com.bt.pi.app.instancemanager.handlers.TerminateInstanceTaskProcessingQueueContinuation;
import com.bt.pi.core.application.MessageContext;
import com.bt.pi.core.application.MessageContextFactory;
import com.bt.pi.core.application.watcher.task.TaskProcessingQueueHelper;
import com.bt.pi.core.continuation.PiContinuation;
import com.bt.pi.core.dht.DhtClientFactory;
import com.bt.pi.core.dht.DhtReader;
import com.bt.pi.core.entity.EntityMethod;
import com.bt.pi.core.id.PId;
import com.bt.pi.core.testing.GenericContinuationAnswer;

@RunWith(MockitoJUnitRunner.class)
public class TerminateInstanceTaskProcessingQueueContinuationTest {

    @Mock
    PiIdBuilder piIdBuilder;
    @Mock
    MessageContextFactory messageContextFactory;
    @Mock
    DhtClientFactory dhtClientFactory;
    @Mock
    TaskProcessingQueueHelper taskProcessingQueueHelper;
    @Mock
    MessageContext messageContext;
    @Mock
    PId instanceId;
    @Mock
    Instance resultInstance;
    @Mock
    DhtReader dhtReader;
    @Mock
    private PId terminateInstanceQueueId;

    @InjectMocks
    TerminateInstanceTaskProcessingQueueContinuation c = new TerminateInstanceTaskProcessingQueueContinuation();

    String uri = "uri";
    String nodeId = "nodeId";

    @Before
    public void before() {
        when(piIdBuilder.getPIdForEc2AvailabilityZone(uri)).thenReturn(instanceId);
        when(piIdBuilder.getPiQueuePId(PiQueue.TERMINATE_INSTANCE)).thenReturn(terminateInstanceQueueId);
        when(terminateInstanceQueueId.forLocalScope(PiQueue.TERMINATE_INSTANCE.getNodeScope())).thenReturn(terminateInstanceQueueId);
        when(messageContextFactory.newMessageContext()).thenReturn(messageContext);
        when(dhtClientFactory.createReader()).thenReturn(dhtReader);
        when(resultInstance.getInstanceId()).thenReturn("instanceA");
        when(resultInstance.getNodeId()).thenReturn("nodeA");
    }

    @Test
    public void shouldAddNodeIfOnQueueItem() {
        // act
        c.receiveResult(uri, nodeId);

        // assert
        verify(taskProcessingQueueHelper).setNodeIdOnUrl(terminateInstanceQueueId, uri, nodeId);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void shouldSendMessageToTheNodeRunningTheInstance() {
        // setup

        GenericContinuationAnswer<Instance> gca = new GenericContinuationAnswer<Instance>(resultInstance);
        doAnswer(gca).when(dhtReader).getAsync(eq(instanceId), isA(PiContinuation.class));

        // act
        c.receiveResult(uri, nodeId);

        // assert
        verify(messageContext).routePiMessageToApplication(isA(PId.class), eq(EntityMethod.DELETE), eq(resultInstance), eq(InstanceManagerApplication.APPLICATION_NAME));
    }
}
