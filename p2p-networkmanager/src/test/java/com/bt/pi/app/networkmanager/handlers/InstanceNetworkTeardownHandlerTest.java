package com.bt.pi.app.networkmanager.handlers;

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
import com.bt.pi.app.networkmanager.addressing.PublicIpAddressManager;
import com.bt.pi.app.networkmanager.net.NetworkManager;
import com.bt.pi.core.application.ReceivedMessageContext;
import com.bt.pi.core.application.watcher.task.TaskProcessingQueueHelper;
import com.bt.pi.core.continuation.GenericContinuation;
import com.bt.pi.core.id.PId;
import com.bt.pi.core.testing.GenericContinuationAnswer;

@RunWith(MockitoJUnitRunner.class)
public class InstanceNetworkTeardownHandlerTest {
    private Instance instance;

    @Mock
    private NetworkManager networkManager;
    @Mock
    private PublicIpAddressManager addressManager;
    @Mock
    private ReceivedMessageContext messageContext;
    @Mock
    private PiIdBuilder piIdBuilder;
    @Mock
    private TaskProcessingQueueHelper taskProcessingQueueHelper;
    @Mock
    private PId queuePId;

    @InjectMocks
    private InstanceNetworkTeardownHandler instanceNetworkTeardownHandler = new InstanceNetworkTeardownHandler();

    @Before
    public void before() {
        instance = new Instance("i-123", "userId", "default");

        when(piIdBuilder.getPiQueuePId(PiQueue.INSTANCE_NETWORK_MANAGER_TEARDOWN)).thenReturn(queuePId);
        when(queuePId.forLocalScope(PiQueue.INSTANCE_NETWORK_MANAGER_TEARDOWN.getNodeScope())).thenReturn(queuePId);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void shouldReleaseNetworkAndPublicIpAddress() {
        // setup
        GenericContinuationAnswer<Boolean> answer = new GenericContinuationAnswer<Boolean>(true);
        doAnswer(answer).when(addressManager).releasePublicIpAddressForInstance(eq("i-123"), eq("userId:default"), isA(GenericContinuation.class));

        // act
        instanceNetworkTeardownHandler.handle(instance, messageContext);

        // assert
        verify(networkManager).releaseNetworkForInstance("userId", "default", "i-123");
    }

    @SuppressWarnings("unchecked")
    @Test
    public void shouldRemoveTaskFromQueueOncePublicIpAddressIsReleased() {
        // setup
        GenericContinuationAnswer<Boolean> answer = new GenericContinuationAnswer<Boolean>(true);
        doAnswer(answer).when(addressManager).releasePublicIpAddressForInstance(eq("i-123"), eq("userId:default"), isA(GenericContinuation.class));

        // act
        instanceNetworkTeardownHandler.handle(instance, messageContext);

        // assert
        verify(taskProcessingQueueHelper).removeUrlFromQueue(queuePId, instance.getUrl());
    }

    @SuppressWarnings("unchecked")
    @Test
    public void shouldReleaseNetworkEvenWhenPublicIpAddressReleaseFails() {
        // setup
        GenericContinuationAnswer<Boolean> answer = new GenericContinuationAnswer<Boolean>(false);
        doAnswer(answer).when(addressManager).releasePublicIpAddressForInstance(eq("i-123"), eq("userId:default"), isA(GenericContinuation.class));

        // act
        instanceNetworkTeardownHandler.handle(instance, messageContext);

        // assert
        verify(networkManager).releaseNetworkForInstance("userId", "default", "i-123");
    }
}
