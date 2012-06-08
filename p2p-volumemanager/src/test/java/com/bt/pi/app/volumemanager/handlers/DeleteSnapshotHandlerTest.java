package com.bt.pi.app.volumemanager.handlers;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;
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
import org.mockito.invocation.InvocationOnMock;
import org.mockito.runners.MockitoJUnitRunner;
import org.mockito.stubbing.Answer;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import com.bt.pi.app.common.entities.Snapshot;
import com.bt.pi.app.common.entities.SnapshotState;
import com.bt.pi.app.common.entities.User;
import com.bt.pi.app.common.id.PiIdBuilder;
import com.bt.pi.app.common.resource.PiQueue;
import com.bt.pi.core.application.watcher.task.TaskProcessingQueueContinuation;
import com.bt.pi.core.application.watcher.task.TaskProcessingQueueHelper;
import com.bt.pi.core.continuation.UpdateResolvingContinuation;
import com.bt.pi.core.dht.DhtClientFactory;
import com.bt.pi.core.dht.DhtWriter;
import com.bt.pi.core.id.PId;
import com.bt.pi.core.util.common.CommandRunner;

@RunWith(MockitoJUnitRunner.class)
public class DeleteSnapshotHandlerTest {
    private Snapshot snapshot;
    @Mock
    private PId pid;
    @Mock
    private PId queueId;
    @Mock
    private PId removeSnapshotFromUserQueueId;
    @Mock
    private PId userId;
    @Mock
    private User user;
    @Mock
    private PiIdBuilder piIdBuilder;
    @Mock
    private DhtWriter dhtWriter;
    @Mock
    private DhtClientFactory dhtClientFactory;
    @Mock
    private ThreadPoolTaskExecutor taskExecutor;
    @Mock
    private TaskProcessingQueueHelper taskProcessingQueueHelper;
    @Mock
    private CommandRunner commandRunner;
    private String nodeId = "1234";

    @InjectMocks
    private DeleteSnapshotHandler deleteHandler = new DeleteSnapshotHandler();

    @SuppressWarnings("unchecked")
    @Before
    public void setup() {
        snapshot = new Snapshot("snapshotid", "volumeid", SnapshotState.COMPLETE, 1234, 1234, "snapshot", "user");
        user = new User("user", "", "");
        user.getSnapshotIds().add("snapshotid");

        when(piIdBuilder.getPId(snapshot)).thenReturn(pid);
        when(piIdBuilder.getPiQueuePId(PiQueue.DELETE_SNAPSHOT)).thenReturn(queueId);
        when(piIdBuilder.getPiQueuePId(PiQueue.REMOVE_SNAPSHOT_FROM_USER)).thenReturn(removeSnapshotFromUserQueueId);
        when(piIdBuilder.getPId(User.getUrl("user"))).thenReturn(userId);
        when(pid.forGlobalAvailablityZoneCode(0)).thenReturn(pid);
        when(queueId.forLocalScope(PiQueue.DELETE_SNAPSHOT.getNodeScope())).thenReturn(queueId);
        when(removeSnapshotFromUserQueueId.forLocalScope(PiQueue.REMOVE_SNAPSHOT_FROM_USER.getNodeScope())).thenReturn(removeSnapshotFromUserQueueId);

        when(dhtClientFactory.createWriter()).thenReturn(dhtWriter);
        doAnswer(new Answer<Object>() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                UpdateResolvingContinuation<Snapshot, Exception> continuation = (UpdateResolvingContinuation<Snapshot, Exception>) invocation.getArguments()[1];
                continuation.update(snapshot, snapshot);
                continuation.receiveResult(snapshot);

                return null;
            }
        }).when(dhtWriter).update(eq(pid), isA(UpdateResolvingContinuation.class));
        doAnswer(new Answer<Object>() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                UpdateResolvingContinuation<User, Exception> continuation = (UpdateResolvingContinuation<User, Exception>) invocation.getArguments()[1];
                continuation.update(user, user);
                continuation.receiveResult(user);
                return null;
            }
        }).when(dhtWriter).update(eq(userId), isA(UpdateResolvingContinuation.class));

        doAnswer(new Answer<Object>() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                Runnable r = (Runnable) invocation.getArguments()[0];
                return new Thread(r);
            }
        }).when(taskExecutor).createThread(isA(Runnable.class));

        doAnswer(new Answer<Object>() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                TaskProcessingQueueContinuation continuation = (TaskProcessingQueueContinuation) invocation.getArguments()[3];
                continuation.receiveResult(snapshot.getUrl(), nodeId);
                return null;
            }
        }).when(taskProcessingQueueHelper).setNodeIdOnUrl(eq(queueId), eq(snapshot.getUrl()), eq(nodeId), isA(TaskProcessingQueueContinuation.class));
    }

    @Test
    public void testDeleteSnapshot() throws Exception {

        // act
        deleteHandler.deleteSnapshot(snapshot, nodeId);
        Thread.sleep(1000);

        // assert
        assertThat(snapshot.getStatus(), equalTo(SnapshotState.DELETED));
        verify(commandRunner).runNicely("rm var/snapshots/snapshotid");
        verify(taskProcessingQueueHelper).removeUrlFromQueue(queueId, snapshot.getUrl());
        verify(taskProcessingQueueHelper).addUrlToQueue(removeSnapshotFromUserQueueId, snapshot.getUrl(), 5);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testDeleteSnapshotNotInCompleteState() throws Exception {
        // setup
        snapshot.setStatus(SnapshotState.PENDING);

        // act
        deleteHandler.deleteSnapshot(snapshot, nodeId);
    }
}
