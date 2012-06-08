package com.bt.pi.app.instancemanager.handlers;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import com.bt.pi.app.common.entities.Instance;
import com.bt.pi.app.common.entities.User;
import com.bt.pi.app.common.id.PiIdBuilder;
import com.bt.pi.app.common.resource.PiQueue;
import com.bt.pi.app.instancemanager.handlers.RemoveInstanceFromUserTaskProcessingQueueContinuation;
import com.bt.pi.core.application.watcher.task.TaskProcessingQueueHelper;
import com.bt.pi.core.dht.DhtClientFactory;
import com.bt.pi.core.dht.DhtReader;
import com.bt.pi.core.id.PId;

@RunWith(MockitoJUnitRunner.class)
public class RemoveInstanceFromUserTaskProcessingQueueContinuationTest {

    @InjectMocks
    RemoveInstanceFromUserTaskProcessingQueueContinuation continuation = new RemoveInstanceFromUserTaskProcessingQueueContinuation();
    @Mock
    DhtClientFactory dhtClientFactory;
    @Mock
    PiIdBuilder piIdBuilder;
    @Mock
    TaskProcessingQueueHelper taskProcessingQueueHelper;
    @Mock
    PId instancePastryId;
    @Mock
    PId removeInstanceFromUserQueuePastryId;
    @Mock
    DhtReader dhtReader;

    String uri = "uri";
    String nodeId = "nodeId";

    private Instance instance;
    private User user;

    private String userId = "userId";
    private String instanceId = "i-1234";

    @Before
    public void setup() {
        when(piIdBuilder.getPIdForEc2AvailabilityZone(uri)).thenReturn(instancePastryId);
        when(piIdBuilder.getPiQueuePId(PiQueue.REMOVE_INSTANCE_FROM_USER)).thenReturn(removeInstanceFromUserQueuePastryId);
        when(removeInstanceFromUserQueuePastryId.forLocalScope(PiQueue.REMOVE_INSTANCE_FROM_USER.getNodeScope())).thenReturn(removeInstanceFromUserQueuePastryId);
        when(dhtClientFactory.createReader()).thenReturn(dhtReader);

        instance = new Instance();
        user = new User();
    }

    @Test
    public void shouldAddNodeIdOnUrl() {
        // setup

        // act
        continuation.receiveResult(uri, nodeId);

        // assert
        verify(taskProcessingQueueHelper).setNodeIdOnUrl(removeInstanceFromUserQueuePastryId, uri, nodeId);
    }

    @Test
    public void shouldGetUserFromInstance() {
        // setup
        instance.setUserId(userId);

        // act
        String ownerId = continuation.getOwnerId(instance);

        // assert
        assertEquals(userId, ownerId);
    }

    @Test
    public void shouldRemoveInstanceIdFromUser() {
        // setup
        instance.setInstanceId(instanceId);
        user.addTerminatedInstance(instanceId);

        // act
        boolean removeResourceFromUser = continuation.removeResourceFromUser(user, instance);

        // assert
        assertThat(user.getTerminatedInstanceIds().contains(instanceId), is(false));
        assertThat(removeResourceFromUser, is(true));
    }

    @Test
    public void shouldGetInstanceId() {
        // setup
        instance.setInstanceId(instanceId);

        // act
        String actualInstanceId = continuation.getResourceId(instance);

        // assert
        assertEquals(instanceId, actualInstanceId);
    }

    @Test
    public void shouldNotSetTheInstanceStateAsItIsAlreadyTerminated() {
        // setup

        // act
        boolean statusToBuried = continuation.setResourceStatusToBuried(instance);

        assertThat(statusToBuried, is(false));
    }
}
