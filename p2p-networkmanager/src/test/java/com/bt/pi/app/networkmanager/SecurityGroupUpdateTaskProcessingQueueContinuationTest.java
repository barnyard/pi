package com.bt.pi.app.networkmanager;

import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.isA;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import com.bt.pi.app.common.entities.SecurityGroup;
import com.bt.pi.app.common.id.PiIdBuilder;
import com.bt.pi.app.common.resource.PiQueue;
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
public class SecurityGroupUpdateTaskProcessingQueueContinuationTest {

    @Mock
    PiIdBuilder piIdBuilder;
    @Mock
    DhtClientFactory dhtClientFactory;
    @Mock
    MessageContextFactory messageContextFactory;
    @Mock
    TaskProcessingQueueHelper taskProcessingQueueHelper;
    @Mock
    DhtReader dhtReader;
    @Mock
    MessageContext messageContext;
    @Mock
    PId securityGroupId;
    @Mock
    PId networkManagerApplicationPiId;
    @Mock
    SecurityGroup securityGroup;
    @Mock
    PId queueId;

    @InjectMocks
    SecurityGroupUpdateTaskProcessingQueueContinuation s = new SecurityGroupUpdateTaskProcessingQueueContinuation();

    String uri = "securityGroupUri";
    String nodeId = "nodeId";

    @Before
    public void setUp() throws Exception {

        when(dhtClientFactory.createReader()).thenReturn(dhtReader);
        when(messageContextFactory.newMessageContext()).thenReturn(messageContext);
        when(piIdBuilder.getPiQueuePId(PiQueue.UPDATE_SECURITY_GROUP)).thenReturn(queueId);
        when(queueId.forLocalScope(PiQueue.UPDATE_SECURITY_GROUP.getNodeScope())).thenReturn(queueId);
        when(piIdBuilder.getPId(uri)).thenReturn(securityGroupId);
    }

    @Test
    public void shouldSetNodeIdOnQueueItemAfterReceivingTheTask() {
        // act
        s.receiveResult(uri, nodeId);

        // assert
        verify(taskProcessingQueueHelper, times(1)).setNodeIdOnUrl(queueId, uri, nodeId);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void shouldSendSecurityGroupToTheNetworkManagerApplication() {
        // setup

        when(securityGroupId.forLocalRegion()).thenReturn(securityGroupId);
        when(piIdBuilder.getPId(isA(SecurityGroup.class))).thenReturn(networkManagerApplicationPiId);
        when(networkManagerApplicationPiId.forLocalAvailabilityZone()).thenReturn(networkManagerApplicationPiId);

        GenericContinuationAnswer<SecurityGroup> answer = new GenericContinuationAnswer<SecurityGroup>(securityGroup);
        doAnswer(answer).when(dhtReader).getAsync(eq(securityGroupId), isA(PiContinuation.class));

        // act
        s.receiveResult(uri, nodeId);

        // assert
        verify(messageContext).routePiMessage(networkManagerApplicationPiId, EntityMethod.UPDATE, securityGroup);
    }
}
