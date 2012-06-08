package com.bt.pi.app.networkmanager.handlers;

import static org.junit.Assert.assertEquals;
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
import org.mockito.runners.MockitoJUnitRunner;
import org.mockito.stubbing.Answer;

import com.bt.pi.app.common.entities.Instance;
import com.bt.pi.app.common.entities.PublicIpAddress;
import com.bt.pi.app.common.id.PiIdBuilder;
import com.bt.pi.app.common.resource.PiQueue;
import com.bt.pi.app.networkmanager.addressing.PublicIpAddressManager;
import com.bt.pi.core.application.watcher.task.TaskProcessingQueueHelper;
import com.bt.pi.core.continuation.GenericContinuation;
import com.bt.pi.core.continuation.UpdateResolvingContinuation;
import com.bt.pi.core.dht.DhtClientFactory;
import com.bt.pi.core.dht.DhtWriter;
import com.bt.pi.core.id.PId;
import com.bt.pi.core.testing.GenericContinuationAnswer;
import com.bt.pi.core.testing.UpdateResolvingContinuationAnswer;

@RunWith(MockitoJUnitRunner.class)
public class DisassociateAddressFromInstanceHandlerTest {
    private static final String OWNER_ID = "user-id";
    private static final String INSTANCE_ID = "i-abc";
    private static final String PUB_IP_ADDR = "1.2.3.4";
    private static final String SEC_GROUP_NAME = "default";
    private PublicIpAddress addr;
    private GenericContinuationAnswer<String> newAddressAnswer;
    @SuppressWarnings("unchecked")
    private Answer instanceUpdateAnswer;
    private Instance instance;
    @InjectMocks
    private DisassociateAddressFromInstanceHandler disassociateAddressHandler = new DisassociateAddressFromInstanceHandler();;
    @Mock
    PublicIpAddressManager addressManager;
    @Mock
    TaskProcessingQueueHelper taskProcessingQueueHelper;
    @Mock
    PiIdBuilder piIdBuilder;
    @Mock
    DhtClientFactory dhtClientFactory;
    @Mock
    PId instanceRecordDhtId;
    @Mock
    DhtWriter dhtWriter;
    @Mock
    PId queueId;

    @SuppressWarnings("unchecked")
    @Before
    public void before() {
        addr = new PublicIpAddress();
        addr.setInstanceId(INSTANCE_ID);
        addr.setIpAddress(PUB_IP_ADDR);
        addr.setSecurityGroupName(SEC_GROUP_NAME);
        addr.setOwnerId(OWNER_ID);

        instance = new Instance();

        when(piIdBuilder.getPIdForEc2AvailabilityZone(Instance.getUrl(INSTANCE_ID))).thenReturn(instanceRecordDhtId);

        instanceUpdateAnswer = new UpdateResolvingContinuationAnswer(instance);
        when(dhtClientFactory.createWriter()).thenReturn(dhtWriter);
        doAnswer(instanceUpdateAnswer).when(dhtWriter).update(eq(instanceRecordDhtId), isA(UpdateResolvingContinuation.class));

        newAddressAnswer = new GenericContinuationAnswer<String>("9.8.7.6");
        doAnswer(newAddressAnswer).when(addressManager).disassociatePublicIpAddressFromInstance(eq(PUB_IP_ADDR), eq(INSTANCE_ID), eq(OWNER_ID + ":" + SEC_GROUP_NAME), isA(GenericContinuation.class));

        when(piIdBuilder.getPiQueuePId(PiQueue.DISASSOCIATE_ADDRESS)).thenReturn(queueId);
        when(queueId.forLocalScope(PiQueue.ASSOCIATE_ADDRESS.getNodeScope())).thenReturn(queueId);
    }

    @Test
    public void shouldDelegateDisassociationToAddressManagerAndRemoveTaskFromQueueOnSuccess() {
        // act
        disassociateAddressHandler.handle(addr);

        // assert
        verify(taskProcessingQueueHelper).removeUrlFromQueue(queueId, "addr:" + PUB_IP_ADDR + ";sg=" + OWNER_ID + ":" + SEC_GROUP_NAME + ";inst=" + INSTANCE_ID);
        assertEquals("9.8.7.6", instance.getPublicIpAddress());
    }

    @SuppressWarnings("unchecked")
    @Test
    public void shouldNotRemoveTaskFromQueueOnFailure() {
        // setup
        newAddressAnswer = new GenericContinuationAnswer<String>(null);
        doAnswer(newAddressAnswer).when(addressManager).disassociatePublicIpAddressFromInstance(eq(PUB_IP_ADDR), eq(INSTANCE_ID), eq(OWNER_ID + ":" + SEC_GROUP_NAME), isA(GenericContinuation.class));

        // act
        disassociateAddressHandler.handle(addr);

        // assert
        verify(taskProcessingQueueHelper, never()).removeUrlFromQueue(isA(PId.class), anyString());
    }
}
