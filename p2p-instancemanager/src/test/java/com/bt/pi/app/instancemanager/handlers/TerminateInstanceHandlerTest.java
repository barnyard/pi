package com.bt.pi.app.instancemanager.handlers;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.isA;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.runners.MockitoJUnitRunner;
import org.mockito.stubbing.Answer;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import com.bt.pi.app.common.entities.Instance;
import com.bt.pi.app.common.entities.InstanceState;
import com.bt.pi.app.common.entities.User;
import com.bt.pi.app.common.entities.Volume;
import com.bt.pi.app.common.id.PiIdBuilder;
import com.bt.pi.app.common.resource.PiQueue;
import com.bt.pi.app.instancemanager.images.InstanceImageManager;
import com.bt.pi.app.networkmanager.net.VirtualNetworkBuilder;
import com.bt.pi.core.application.watcher.task.TaskProcessingQueueHelper;
import com.bt.pi.core.continuation.UpdateResolvingContinuation;
import com.bt.pi.core.continuation.UpdateResolvingPiContinuation;
import com.bt.pi.core.dht.DhtClientFactory;
import com.bt.pi.core.dht.DhtWriter;
import com.bt.pi.core.entity.PiEntity;
import com.bt.pi.core.id.PId;

@RunWith(MockitoJUnitRunner.class)
public class TerminateInstanceHandlerTest {
    @InjectMocks
    private TerminateInstanceHandler terminateInstanceHandler = new TerminateInstanceHandler();
    private static final String INSTANCE_ID = "i-123";
    private Instance instance;
    @Mock
    private InstanceImageManager instanceImageManager;
    @Mock
    private DhtClientFactory dhtClientFactory;
    @Mock
    private DhtWriter dhtWriter;
    @Mock
    private ThreadPoolTaskExecutor taskExecutor;
    @Mock
    private VirtualNetworkBuilder virtualNetworkBuilder;
    @Mock
    private TerminateInstanceEventListener terminateInstanceEventListener;
    private CountDownLatch latch;
    private Volume volume;
    @Mock
    private PiIdBuilder piIdBuilder;
    @Mock
    private PId instancePId;
    @Mock
    private PId volumePId;
    @Mock
    private TaskProcessingQueueHelper taskProcessingQueueHelper;
    @Mock
    private PId terminateInstanceQueuePId;
    @Mock
    private PId removeInstanceFromUserEntityQueuePId;
    private User user;
    private String userId = "fred";
    @Mock
    private PId userPId;

    @SuppressWarnings("unchecked")
    @Before
    public void setUp() throws Exception {
        setupInstance();
        setupUser();

        when(piIdBuilder.getPIdForEc2AvailabilityZone(isA(Instance.class))).thenReturn(instancePId);
        when(piIdBuilder.getPIdForEc2AvailabilityZone(isA(String.class))).thenReturn(volumePId);
        when(piIdBuilder.getPiQueuePId(PiQueue.TERMINATE_INSTANCE)).thenReturn(terminateInstanceQueuePId);
        when(piIdBuilder.getPId(User.getUrl(userId))).thenReturn(userPId);
        when(terminateInstanceQueuePId.forLocalScope(PiQueue.TERMINATE_INSTANCE.getNodeScope())).thenReturn(terminateInstanceQueuePId);

        when(piIdBuilder.getPiQueuePId(PiQueue.REMOVE_INSTANCE_FROM_USER)).thenReturn(removeInstanceFromUserEntityQueuePId);
        when(removeInstanceFromUserEntityQueuePId.forLocalScope(PiQueue.REMOVE_INSTANCE_FROM_USER.getNodeScope())).thenReturn(removeInstanceFromUserEntityQueuePId);

        doAnswer(new Answer<Object>() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                Runnable r = (Runnable) invocation.getArguments()[0];
                return new Thread(r);
            }
        }).when(taskExecutor).createThread(isA(Runnable.class));

        latch = new CountDownLatch(1);

        doAnswer(new Answer<Object>() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                latch.countDown();
                return null;
            }
        }).when(terminateInstanceEventListener).instanceTerminated(isA(Instance.class));

        doAnswer(new Answer<Object>() {
            @Override
            public PiEntity answer(InvocationOnMock invocation) throws Throwable {
                UpdateResolvingPiContinuation continuation = (UpdateResolvingPiContinuation) invocation.getArguments()[1];
                continuation.update(instance, null);
                continuation.handleResult(instance);
                return null;
            }
        }).when(dhtWriter).update(eq(instancePId), isA(UpdateResolvingContinuation.class));

        doAnswer(new Answer<Object>() {
            @Override
            public PiEntity answer(InvocationOnMock invocation) throws Throwable {
                UpdateResolvingPiContinuation continuation = (UpdateResolvingPiContinuation) invocation.getArguments()[1];
                continuation.update(user, null);
                continuation.handleResult(user);
                return null;
            }
        }).when(dhtWriter).update(eq(userPId), isA(UpdateResolvingPiContinuation.class));

        doAnswer(new Answer<Object>() {
            @Override
            public PiEntity answer(InvocationOnMock invocation) throws Throwable {
                UpdateResolvingPiContinuation continuation = (UpdateResolvingPiContinuation) invocation.getArguments()[1];
                continuation.update(volume, null);
                continuation.handleResult(volume);
                return null;
            }
        }).when(dhtWriter).update(eq(volumePId), isA(UpdateResolvingContinuation.class));

        when(dhtClientFactory.createWriter()).thenReturn(dhtWriter);
    }

    private void setupUser() {
        this.user = new User();
        user.addInstance(INSTANCE_ID);
    }

    private void setupInstance() {
        instance = new Instance();
        instance.setInstanceId(INSTANCE_ID);
        instance.setVlanId(1234);
        instance.setSecurityGroupName("securityGroupName");
        instance.setUserId(userId);
    }

    @Test
    public void shouldStopInstance() throws Exception {
        // act
        terminateInstanceHandler.terminateInstance(instance);

        // assert
        assertTrue(latch.await(2000, TimeUnit.MILLISECONDS));
        verify(instanceImageManager).stopInstance(instance);
    }

    @Test
    public void shouldDestroyInstance() throws Exception {
        // act
        terminateInstanceHandler.terminateInstance(instance);

        // assert
        assertTrue(latch.await(2000, TimeUnit.MILLISECONDS));
        verify(instanceImageManager).stopInstance(instance);
    }

    @Test
    public void shouldUpdateDhtWithInstanceTerminated() throws Exception {
        // act
        terminateInstanceHandler.terminateInstance(instance);

        // assert
        assertTrue(latch.await(2000, TimeUnit.MILLISECONDS));
        assertEquals(InstanceState.TERMINATED, instance.getState());
    }

    @Test
    public void shouldStopNetworkWhenInstanceTerminated() throws Exception {
        // act
        terminateInstanceHandler.terminateInstance(instance);

        // assert
        assertTrue(latch.await(2000, TimeUnit.MILLISECONDS));
        verify(virtualNetworkBuilder).tearDownVirtualNetworkForInstance(instance.getVlanId(), instance.getInstanceId());
    }

    @Test
    public void shouldNotifyListenerWhenInstanceTerminated() throws Exception {
        // act
        terminateInstanceHandler.terminateInstance(instance);

        // assert
        assertTrue(latch.await(2000, TimeUnit.MILLISECONDS));
        verify(terminateInstanceEventListener).instanceTerminated(instance);
    }

    @Test
    public void shouldRemoveTaskItemFromQueueAfterTerminateInstance() throws Exception {
        // setup

        // act
        terminateInstanceHandler.terminateInstance(instance);

        // assert
        assertTrue(latch.await(2000, TimeUnit.MILLISECONDS));
        verify(taskProcessingQueueHelper).removeUrlFromQueue(terminateInstanceQueuePId, instance.getUrl());
    }

    @Test
    public void shouldAddTaskItemToRemoveInstanceFromUserEntity() throws InterruptedException {
        // setup

        // act
        terminateInstanceHandler.terminateInstance(instance);

        // assert
        assertTrue(latch.await(2000, TimeUnit.MILLISECONDS));
        verify(taskProcessingQueueHelper).addUrlToQueue(removeInstanceFromUserEntityQueuePId, instance.getUrl(), 5);
    }

    @Test
    public void shouldMarkInstanceAsTerminatedInUserEntity() throws InterruptedException {
        // setup

        // act
        terminateInstanceHandler.terminateInstance(instance);

        // assert
        assertTrue(latch.await(2000, TimeUnit.MILLISECONDS));
        assertFalse(user.getImageIds().contains(INSTANCE_ID));
        assertTrue(user.getTerminatedInstanceIds().contains(INSTANCE_ID));
    }
}
