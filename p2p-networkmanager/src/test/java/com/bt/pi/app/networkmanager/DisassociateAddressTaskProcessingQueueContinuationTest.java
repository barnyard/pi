package com.bt.pi.app.networkmanager;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.argThat;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentMatcher;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import com.bt.pi.app.common.entities.Instance;
import com.bt.pi.app.common.entities.PublicIpAddress;
import com.bt.pi.app.common.id.PiIdBuilder;
import com.bt.pi.core.application.MessageContext;
import com.bt.pi.core.application.MessageContextFactory;
import com.bt.pi.core.entity.EntityMethod;
import com.bt.pi.core.id.PId;

@RunWith(MockitoJUnitRunner.class)
public class DisassociateAddressTaskProcessingQueueContinuationTest {
    private static final String NODE_ID = "node-id";
    DisassociateAddressTaskProcessingQueueContinuation disassociateAddressTaskProcessingQueueContinuation;
    @Mock
    PiIdBuilder piIdBuilder;
    @Mock
    MessageContextFactory messageContextFactory;
    @Mock
    MessageContext messageContext;
    @Mock
    PId id;

    @Before
    public void before() {
        when(messageContextFactory.newMessageContext()).thenReturn(messageContext);

        when(piIdBuilder.getGlobalAvailabilityZoneCodeFromEc2Id("i-123")).thenReturn(123);
        when(piIdBuilder.getPId("sg:user:default")).thenReturn(id);
        when(id.forGlobalAvailablityZoneCode(123)).thenReturn(id);

        disassociateAddressTaskProcessingQueueContinuation = new DisassociateAddressTaskProcessingQueueContinuation();
        disassociateAddressTaskProcessingQueueContinuation.setMessageContextFactory(messageContextFactory);
        disassociateAddressTaskProcessingQueueContinuation.setPiIdBuilder(piIdBuilder);
    }

    @Test
    public void shouldSendMessageToNetworkManagerForSecGroupFromUri() {
        // act
        disassociateAddressTaskProcessingQueueContinuation.receiveResult("addr:1.2.3.4;sg=user:default;inst=i-123", NODE_ID);

        // assert
        verify(messageContext).routePiMessage(eq(id), eq(EntityMethod.DELETE), argThat(new ArgumentMatcher<Instance>() {
            public boolean matches(Object argument) {
                PublicIpAddress publicIp = (PublicIpAddress) argument;
                assertEquals("1.2.3.4", publicIp.getIpAddress());
                assertEquals("user", publicIp.getOwnerId());
                assertEquals("default", publicIp.getSecurityGroupName());
                assertEquals("i-123", publicIp.getInstanceId());
                return true;
            }
        }));
    }
}
