package com.bt.pi.app.instancemanager.handlers;

import static org.junit.Assert.assertEquals;
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
import org.mockito.runners.MockitoJUnitRunner;

import com.bt.pi.app.common.entities.Instance;
import com.bt.pi.app.common.entities.InstanceState;
import com.bt.pi.app.common.entities.User;
import com.bt.pi.app.common.id.PiIdBuilder;
import com.bt.pi.app.common.resource.PiQueue;
import com.bt.pi.core.application.watcher.task.TaskProcessingQueueHelper;
import com.bt.pi.core.continuation.UpdateResolvingPiContinuation;
import com.bt.pi.core.dht.DhtClientFactory;
import com.bt.pi.core.dht.DhtWriter;
import com.bt.pi.core.id.PId;
import com.bt.pi.core.testing.UpdateResolvingContinuationAnswer;

@RunWith(MockitoJUnitRunner.class)
public class RunInstanceTaskProcessingExhaustedInstanceContinuationTest {
    @InjectMocks
    private RunInstanceTaskProcessingExhaustedInstanceContinuation exhaustedInstanceContinuation = new RunInstanceTaskProcessingExhaustedInstanceContinuation();
    private String instanceId = "Foo";
    private String instanceUri = "inst:" + instanceId;
    private String nodeId;
    @Mock
    private DhtClientFactory dhtClientFactory;
    @Mock
    private PiIdBuilder piIdBuilder;
    @Mock
    private DhtWriter writer;
    private Instance instance;
    private UpdateResolvingContinuationAnswer updateInstanceAnswer;
    private UpdateResolvingContinuationAnswer updateUserAnswer;
    private String userId = "fred";
    @Mock
    private User user;
    @Mock
    private PId userPId;
    @Mock
    private PId instancePId;
    @Mock
    private TaskProcessingQueueHelper taskProcessingQueueHelper;
    @Mock
    private PId queuePId;

    @SuppressWarnings("unchecked")
    @Before
    public void before() {
        nodeId = "nodeId";
        instance = new Instance();
        instance.setUserId(userId);
        instance.setInstanceId(instanceId);
        when(piIdBuilder.getPIdForEc2AvailabilityZone(eq(instanceUri))).thenReturn(instancePId);
        updateInstanceAnswer = new UpdateResolvingContinuationAnswer(instance);
        doAnswer(updateInstanceAnswer).when(writer).update(eq(instancePId), isA(UpdateResolvingPiContinuation.class));
        when(dhtClientFactory.createWriter()).thenReturn(writer);
        when(piIdBuilder.getPId(User.getUrl(userId))).thenReturn(userPId);
        updateUserAnswer = new UpdateResolvingContinuationAnswer(user);
        doAnswer(updateUserAnswer).when(writer).update(eq(userPId), isA(UpdateResolvingPiContinuation.class));
        when(piIdBuilder.getPiQueuePId(PiQueue.REMOVE_INSTANCE_FROM_USER)).thenReturn(queuePId);
        when(queuePId.forLocalScope(PiQueue.REMOVE_INSTANCE_FROM_USER.getNodeScope())).thenReturn(queuePId);
        when(user.terminateInstance(instanceId)).thenReturn(true);
    }

    @Test
    public void testReceiveResult() {
        // setup
        instance.setState(InstanceState.PENDING);

        // act
        exhaustedInstanceContinuation.receiveResult(instanceUri, nodeId);

        // assert
        assertEquals(instance, updateInstanceAnswer.getResult());
        assertEquals(InstanceState.FAILED, instance.getState());
    }

    @Test
    public void testReceiveResultDoesntTerminateRunningInstance() {
        // setup
        instance.setState(InstanceState.RUNNING);

        // act
        exhaustedInstanceContinuation.receiveResult(instanceUri, nodeId);

        // assert
        assertEquals(null, updateInstanceAnswer.getResult());
        assertEquals(InstanceState.RUNNING, instance.getState());
        verify(user, never()).terminateInstance(instanceId);
        verify(taskProcessingQueueHelper, never()).addUrlToQueue(queuePId, instanceUri, TerminateInstanceHandler.REMOVE_INSTANCE_FROM_USER_ENTITY_TASK_RETRIES);
    }

    @Test
    public void testReceiveResultDoesntWriteIfAlreadyTerminated() {
        // setup
        instance.setState(InstanceState.TERMINATED);

        // act
        exhaustedInstanceContinuation.receiveResult(instanceUri, nodeId);

        // assert
        assertEquals(null, updateInstanceAnswer.getResult());
        assertEquals(InstanceState.TERMINATED, instance.getState());
        verify(user).terminateInstance(instanceId);
        verify(taskProcessingQueueHelper).addUrlToQueue(queuePId, instanceUri, TerminateInstanceHandler.REMOVE_INSTANCE_FROM_USER_ENTITY_TASK_RETRIES);
    }

    @Test
    public void testReceiveResultDoesntWriteIfAlreadyFailed() {
        // setup
        instance.setState(InstanceState.FAILED);

        // act
        exhaustedInstanceContinuation.receiveResult(instanceUri, nodeId);

        // assert
        assertEquals(null, updateInstanceAnswer.getResult());
        assertEquals(InstanceState.FAILED, instance.getState());
        verify(user).terminateInstance(instanceId);
        verify(taskProcessingQueueHelper).addUrlToQueue(queuePId, instanceUri, TerminateInstanceHandler.REMOVE_INSTANCE_FROM_USER_ENTITY_TASK_RETRIES);
    }

    @Test
    public void testReceiveResultTerminatesInstanceInUser() {
        // setup
        instance.setState(InstanceState.PENDING);

        // act
        exhaustedInstanceContinuation.receiveResult(instanceUri, nodeId);

        // assert
        assertEquals(user, updateUserAnswer.getResult());
        verify(user).terminateInstance(instanceId);
    }

    @Test
    public void testReceiveResultAddsToRemoveInstanceFromUserQueue() {
        // setup
        instance.setState(InstanceState.PENDING);

        // act
        exhaustedInstanceContinuation.receiveResult(instanceUri, nodeId);

        // assert
        verify(taskProcessingQueueHelper).addUrlToQueue(queuePId, instanceUri, TerminateInstanceHandler.REMOVE_INSTANCE_FROM_USER_ENTITY_TASK_RETRIES);
    }
}
