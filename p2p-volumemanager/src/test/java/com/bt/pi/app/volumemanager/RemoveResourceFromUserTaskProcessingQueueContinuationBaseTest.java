package com.bt.pi.app.volumemanager;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.isA;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.runners.MockitoJUnitRunner;
import org.mockito.stubbing.Answer;

import rice.Continuation;

import com.bt.pi.app.common.entities.User;
import com.bt.pi.app.common.id.PiIdBuilder;
import com.bt.pi.app.common.resource.PiQueue;
import com.bt.pi.core.application.watcher.task.TaskProcessingQueueHelper;
import com.bt.pi.core.continuation.UpdateResolvingContinuation;
import com.bt.pi.core.dht.DhtClientFactory;
import com.bt.pi.core.dht.DhtReader;
import com.bt.pi.core.dht.DhtWriter;
import com.bt.pi.core.entity.PiEntity;
import com.bt.pi.core.id.PId;

@RunWith(MockitoJUnitRunner.class)
public class RemoveResourceFromUserTaskProcessingQueueContinuationBaseTest {
    @Rule
    public TestName testName = new TestName();

    private String uri = "uri";
    private String nodeId = "nodeId";
    private String ownerId = "ownerId";

    private boolean setResourceStatusToBuried = false;
    private User removeResourceFromUserInvokedWithUser = null;
    private PiEntity removeResourceFromUserInvokedWithEntity = null;

    @Mock
    private PId queueId;
    @Mock
    private PId resourcePastryId;
    @Mock
    private PId userId;

    @Mock
    private User user;
    @Mock
    private PiEntity piEntity;

    @Mock
    private PiIdBuilder piIdBuilder;
    @Mock
    private TaskProcessingQueueHelper taskProcessingQueueHelper;
    @Mock
    private DhtClientFactory dhtClientFactory;
    @Mock
    private DhtWriter dhtWriter;

    @InjectMocks
    private RemoveResourceFromUserTaskProcessingQueueContinuationBase<PiEntity> continuationBase = new RemoveResourceFromUserTaskProcessingQueueContinuationBase<PiEntity>() {
        @Override
        protected boolean setResourceStatusToBuried(PiEntity resource) {
            setResourceStatusToBuried = true;
            return !testName.getMethodName().contains("ResourceStatusIsAlreadyBuried");
        }

        @Override
        protected boolean removeResourceFromUser(User user, PiEntity resource) {
            removeResourceFromUserInvokedWithUser = user;
            removeResourceFromUserInvokedWithEntity = resource;
            return !testName.getMethodName().contains("ResourceAlreadyRemovedFromUser");
        }

        @Override
        protected String getResourceId(PiEntity resource) {
            return "resourceId";
        }

        @Override
        protected PiQueue getPiQueueForResource() {
            return PiQueue.REMOVE_SNAPSHOT_FROM_USER;
        }

        @Override
        protected String getOwnerId(PiEntity result) {
            return ownerId;
        }

        @Override
        protected PId getResourcePastryId(String uri) {
            return resourcePastryId;
        }
    };

    @SuppressWarnings("unchecked")
    @Before
    public void setup() {
        when(queueId.forLocalScope(PiQueue.REMOVE_SNAPSHOT_FROM_USER.getNodeScope())).thenReturn(queueId);

        when(piIdBuilder.getPiQueuePId(PiQueue.REMOVE_SNAPSHOT_FROM_USER)).thenReturn(queueId);
        when(piIdBuilder.getPId(User.getUrl(ownerId))).thenReturn(userId);

        DhtReader dhtReader = mock(DhtReader.class);
        doAnswer(new Answer<Object>() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                ((Continuation) invocation.getArguments()[1]).receiveResult(piEntity);
                return null;
            }
        }).when(dhtReader).getAsync(eq(resourcePastryId), isA(Continuation.class));

        doAnswer(new Answer<Object>() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                Object object = ((UpdateResolvingContinuation) invocation.getArguments()[1]).update(user, null);
                if (object != null)
                    ((UpdateResolvingContinuation) invocation.getArguments()[1]).receiveResult(user);
                return null;
            }
        }).when(dhtWriter).update(eq(userId), isA(UpdateResolvingContinuation.class));
        doAnswer(new Answer<Object>() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                Object object = ((UpdateResolvingContinuation) invocation.getArguments()[1]).update(piEntity, null);
                if (object != null)
                    ((UpdateResolvingContinuation) invocation.getArguments()[1]).receiveResult(piEntity);
                return null;
            }
        }).when(dhtWriter).update(eq(resourcePastryId), isA(UpdateResolvingContinuation.class));

        when(dhtClientFactory.createReader()).thenReturn(dhtReader);
        when(dhtClientFactory.createWriter()).thenReturn(dhtWriter);
    }

    @Test
    public void testThatNodeIdIsSetOnTaskProcessingQueueHelper() throws Exception {
        // act
        continuationBase.receiveResult(uri, nodeId);

        // assert
        verify(taskProcessingQueueHelper).setNodeIdOnUrl(queueId, uri, nodeId);
    }

    @Test
    public void testThatResourceIsRemovedFromUser() throws Exception {
        // act
        continuationBase.receiveResult(uri, nodeId);

        // assert
        assertThat(removeResourceFromUserInvokedWithUser, equalTo(user));
        assertThat(removeResourceFromUserInvokedWithEntity, equalTo(piEntity));
    }

    @Test
    public void testThatResourceStatusIsSetToBuried() throws Exception {
        // act
        continuationBase.receiveResult(uri, nodeId);

        // assert
        assertThat(setResourceStatusToBuried, is(true));
    }

    @Test
    public void testThatUrlIsRemovedFromQueue() throws Exception {
        // setup
        when(piEntity.getUrl()).thenReturn("url");

        // act
        continuationBase.receiveResult(uri, nodeId);

        // assert
        verify(taskProcessingQueueHelper).removeUrlFromQueue(queueId, "url");
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testThatResourceIsNotRemovedFromUserAndStatusIsNotBuriedIfUserNotFound() throws Exception {
        // setup
        DhtWriter dhtWriter = mock(DhtWriter.class);
        doAnswer(new Answer<Object>() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                ((UpdateResolvingContinuation) invocation.getArguments()[1]).update(null, null);
                return null;
            }
        }).when(dhtWriter).update(eq(userId), isA(UpdateResolvingContinuation.class));
        when(dhtClientFactory.createWriter()).thenReturn(dhtWriter);

        // act
        continuationBase.receiveResult(uri, nodeId);

        // assert
        assertNull(removeResourceFromUserInvokedWithUser);
        assertNull(removeResourceFromUserInvokedWithEntity);
        assertThat(setResourceStatusToBuried, is(false));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testThatResourceAlreadyRemovedFromUserDoesNotUpdateSnapshotStatus() throws Exception {
        // Assertions for this test depend on test name
        // act
        continuationBase.receiveResult(uri, nodeId);

        // assert
        verify(dhtWriter, never()).update(eq(resourcePastryId), isA(UpdateResolvingContinuation.class));
    }

    @Test
    public void testThatResourceStatusIsAlreadyBuriedDoesNotRemoveUrlFromQueue() throws Exception {
        // Assertions for this test depend on test name
        // setup
        when(piEntity.getUrl()).thenReturn("url");

        // act
        continuationBase.receiveResult(uri, nodeId);

        // assert
        verify(taskProcessingQueueHelper, never()).removeUrlFromQueue(queueId, "url");
    }
}
