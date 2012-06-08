package com.bt.pi.app.networkmanager.handlers;

import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.isA;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.junit.Before;
import org.junit.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import rice.Continuation;

import com.bt.pi.app.common.entities.SecurityGroup;
import com.bt.pi.app.common.id.PiIdBuilder;
import com.bt.pi.app.networkmanager.iptables.IpTablesManager;
import com.bt.pi.core.application.resource.ConsumedDhtResourceRegistry;
import com.bt.pi.core.entity.EntityMethod;
import com.bt.pi.core.id.PId;

public class SecurityGroupNetworkUpdateHandlerTest {
    private SecurityGroupNetworkUpdateHandler securityGroupNetworkUpdateHandler;
    private ConsumedDhtResourceRegistry consumedDhtResourceRegistry;
    private IpTablesManager ipTablesManager;
    private PiIdBuilder piIdBuilder;
    private SecurityGroup securityGroup;
    private PId securityGroupId;
    private CountDownLatch latch;

    @Before
    public void setup() {
        latch = new CountDownLatch(1);

        securityGroup = new SecurityGroup("owner", "groupName");
        securityGroupId = mock(PId.class);

        piIdBuilder = mock(PiIdBuilder.class);
        when(piIdBuilder.getPId(securityGroup)).thenReturn(securityGroupId);
        when(securityGroupId.forLocalRegion()).thenReturn(securityGroupId);

        consumedDhtResourceRegistry = mock(ConsumedDhtResourceRegistry.class);
        ipTablesManager = mock(IpTablesManager.class);

        securityGroupNetworkUpdateHandler = new SecurityGroupNetworkUpdateHandler();
        securityGroupNetworkUpdateHandler.setConsumedDhtResourceRegistry(consumedDhtResourceRegistry);
        securityGroupNetworkUpdateHandler.setIpTablesManager(ipTablesManager);
        securityGroupNetworkUpdateHandler.setPiIdBuilder(piIdBuilder);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testHandleWhereSharedResourceManagerRefreshWorks() throws Exception {
        // setup
        doAnswer(new Answer<Object>() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                ((Continuation) invocation.getArguments()[1]).receiveResult(securityGroup);
                latch.countDown();
                return null;
            }
        }).when(consumedDhtResourceRegistry).refresh(eq(securityGroupId), isA(Continuation.class));

        // act
        securityGroupNetworkUpdateHandler.handle(EntityMethod.UPDATE, securityGroup);

        // assert
        assertTrue(latch.await(1, TimeUnit.SECONDS));
        verify(ipTablesManager).refreshIpTables();
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testHandleWhereSharedResourceManagerRefreshFails() throws Exception {
        // setup
        doAnswer(new Answer<Object>() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                ((Continuation) invocation.getArguments()[1]).receiveException(new RuntimeException());
                latch.countDown();
                return null;
            }
        }).when(consumedDhtResourceRegistry).refresh(eq(securityGroupId), isA(Continuation.class));

        // act
        securityGroupNetworkUpdateHandler.handle(EntityMethod.UPDATE, securityGroup);

        // assert
        assertTrue(latch.await(1, TimeUnit.SECONDS));
        verify(ipTablesManager).refreshIpTables();
    }

    @Test
    public void shouldRemoveNetworksFromSecurityGroupAndRemoveAllConsumersOnDelete() {
        // act
        securityGroupNetworkUpdateHandler.handle(EntityMethod.DELETE, securityGroup);

        // assert
        verify(consumedDhtResourceRegistry).clearResource(securityGroupId);
        verify(ipTablesManager).refreshIpTables();
    }
}
