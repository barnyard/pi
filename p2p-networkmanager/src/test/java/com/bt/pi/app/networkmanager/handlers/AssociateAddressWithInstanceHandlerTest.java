package com.bt.pi.app.networkmanager.handlers;

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

import com.bt.pi.app.common.entities.PublicIpAddress;
import com.bt.pi.app.common.id.PiIdBuilder;
import com.bt.pi.app.common.resource.PiQueue;
import com.bt.pi.app.networkmanager.addressing.PublicIpAddressManager;
import com.bt.pi.core.application.watcher.task.TaskProcessingQueueHelper;
import com.bt.pi.core.continuation.GenericContinuation;
import com.bt.pi.core.id.PId;
import com.bt.pi.core.testing.GenericContinuationAnswer;

@RunWith(MockitoJUnitRunner.class)
public class AssociateAddressWithInstanceHandlerTest {
    private static final String OWNER_ID = "user-id";
    private static final String INSTANCE_ID = "i-abc";
    private static final String PUB_IP_ADDR = "1.2.3.4";
    private static final String SEC_GROUP_NAME = "default";
    private PublicIpAddress addr;
    private GenericContinuationAnswer<Boolean> answer;
    @Mock
    PublicIpAddressManager addressManager;
    @Mock
    TaskProcessingQueueHelper taskProcessingQueueHelper;
    @Mock
    PiIdBuilder piIdBuilder;
    @InjectMocks
    private AssociateAddressWithInstanceHandler associateAddressHandler = new AssociateAddressWithInstanceHandler();
    @Mock
    private PId queueId;

    @SuppressWarnings("unchecked")
    @Before
    public void before() {
        addr = new PublicIpAddress();
        addr.setInstanceId(INSTANCE_ID);
        addr.setIpAddress(PUB_IP_ADDR);
        addr.setSecurityGroupName(SEC_GROUP_NAME);
        addr.setOwnerId(OWNER_ID);

        answer = new GenericContinuationAnswer<Boolean>(true);
        doAnswer(answer).when(addressManager).associatePublicIpAddressWithInstance(eq(PUB_IP_ADDR), eq(INSTANCE_ID), eq(OWNER_ID + ":" + SEC_GROUP_NAME), isA(GenericContinuation.class));

        when(piIdBuilder.getPId(PiQueue.ASSOCIATE_ADDRESS.getUrl())).thenReturn(queueId);
        when(queueId.forLocalScope(PiQueue.ASSOCIATE_ADDRESS.getNodeScope())).thenReturn(queueId);
    }

    @Test
    public void shouldDelegateAssociationToAddressManagerAndRemoveTaskFromQueueOnSuccess() {
        // act
        associateAddressHandler.handle(addr);

        // assert
        verify(taskProcessingQueueHelper).removeUrlFromQueue(queueId, "addr:" + PUB_IP_ADDR + ";sg=" + OWNER_ID + ":" + SEC_GROUP_NAME + ";inst=" + INSTANCE_ID);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void shouldNotRemoveTaskFromQueueOnFailure() {
        // setup
        answer = new GenericContinuationAnswer<Boolean>(false);
        doAnswer(answer).when(addressManager).associatePublicIpAddressWithInstance(eq(PUB_IP_ADDR), eq(INSTANCE_ID), eq(OWNER_ID + ":" + SEC_GROUP_NAME), isA(GenericContinuation.class));

        // act
        associateAddressHandler.handle(addr);

        // assert
        verify(taskProcessingQueueHelper, never()).removeUrlFromQueue(isA(PId.class), anyString());
    }
}
