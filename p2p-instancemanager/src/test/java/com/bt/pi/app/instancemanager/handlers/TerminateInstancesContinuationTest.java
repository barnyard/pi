package com.bt.pi.app.instancemanager.handlers;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;

import com.bt.pi.app.common.entities.Instance;
import com.bt.pi.app.common.entities.InstanceState;
import com.bt.pi.app.common.entities.SecurityGroup;
import com.bt.pi.app.common.id.PiIdBuilder;
import com.bt.pi.app.networkmanager.NetworkManagerApplication;
import com.bt.pi.core.application.MessageContext;
import com.bt.pi.core.application.MessageContextFactory;
import com.bt.pi.core.entity.EntityMethod;
import com.bt.pi.core.id.PId;

public class TerminateInstancesContinuationTest {

    private static final String INSTANCE_ID = "cool";
    private TerminateInstancesContinuation terminateInstancesContinuation;
    private MessageContextFactory messageContextFactory;
    private Map<String, InstanceStateTransition> instancesMap;
    private PiIdBuilder piIdBuilder;
    private PId securityGroupId;
    private PId instanceNodeId;
    private Instance instance;
    private MessageContext messageContext;

    @Before
    public void before() {
        securityGroupId = mock(PId.class);
        instanceNodeId = mock(PId.class);// "nodeid");

        instance = mock(Instance.class);
        when(instance.getInstanceId()).thenReturn(INSTANCE_ID);
        when(instance.getUserId()).thenReturn("user");
        when(instance.getSecurityGroupName()).thenReturn("default");

        piIdBuilder = mock(PiIdBuilder.class);
        when(piIdBuilder.getPId(SecurityGroup.getUrl(instance.getUserId(), instance.getSecurityGroupName()))).thenReturn(securityGroupId);
        when(piIdBuilder.getGlobalAvailabilityZoneCodeFromEc2Id(eq(INSTANCE_ID))).thenReturn(5432);
        when(piIdBuilder.getNodeIdFromNodeId(anyString())).thenReturn(instanceNodeId);
        when(securityGroupId.forGlobalAvailablityZoneCode(5432)).thenReturn(securityGroupId);
        messageContext = mock(MessageContext.class);

        messageContextFactory = mock(MessageContextFactory.class);
        when(messageContextFactory.newMessageContext()).thenReturn(messageContext);

        instancesMap = new HashMap<String, InstanceStateTransition>();
        terminateInstancesContinuation = new TerminateInstancesContinuation(piIdBuilder, messageContextFactory, instancesMap);
    }

    @Test
    public void testUpdateUpdatesInstanceToShuttingDown() {
        // setup
        when(instance.getState()).thenReturn(InstanceState.RUNNING);
        when(instance.getNodeId()).thenReturn("nodeId");

        // act
        terminateInstancesContinuation.update(instance, null);

        // verify
        verify(instance).setState(eq(InstanceState.SHUTTING_DOWN));
        assertEquals(InstanceState.RUNNING, instancesMap.get(INSTANCE_ID).getPreviousState());
        assertEquals(InstanceState.RUNNING, instancesMap.get(INSTANCE_ID).getNextState());
    }

    @Test
    public void testUpdateUpdatesInstanceAlreadyTermindated() {
        // setup
        when(instance.getState()).thenReturn(InstanceState.TERMINATED);
        when(instance.getNodeId()).thenReturn("nodeId");

        // act
        terminateInstancesContinuation.update(instance, null);

        // verify
        verify(instance, never()).setState((InstanceState) anyObject());
        assertEquals(InstanceState.TERMINATED, instancesMap.get(INSTANCE_ID).getPreviousState());
        assertEquals(InstanceState.TERMINATED, instancesMap.get(INSTANCE_ID).getNextState());
    }

    @Test
    public void testUpdateUpdatesInstanceIsYetToBeProcessed() {
        // setup
        when(instance.getState()).thenReturn(InstanceState.PENDING);

        // act
        terminateInstancesContinuation.update(instance, null);

        // verify
        verify(instance).setState(InstanceState.TERMINATED);
        assertEquals(InstanceState.PENDING, instancesMap.get(INSTANCE_ID).getPreviousState());
        assertEquals(InstanceState.PENDING, instancesMap.get(INSTANCE_ID).getNextState());
    }

    @Test
    public void testHandleResult() {
        // setup
        instancesMap.put(INSTANCE_ID, new InstanceStateTransition());
        when(instance.getState()).thenReturn(InstanceState.TERMINATED);
        when(instance.getNodeId()).thenReturn("nodeid");

        // act
        terminateInstancesContinuation.handleResult(instance);

        // verify
        verify(messageContext).routePiMessageToApplication(eq(securityGroupId), eq(EntityMethod.DELETE), eq(instance), eq(NetworkManagerApplication.APPLICATION_NAME));
        verify(messageContext).routePiMessageToApplication(eq(instanceNodeId), eq(EntityMethod.DELETE), eq(instance), eq(InstanceManagerApplication.APPLICATION_NAME));
        assertEquals(InstanceState.TERMINATED, instancesMap.get(INSTANCE_ID).getNextState());
    }

    @Test
    public void testHandleResultNodeIdIsNull() {
        // setup
        instancesMap.put(INSTANCE_ID, new InstanceStateTransition());
        when(instance.getState()).thenReturn(InstanceState.TERMINATED);

        // act
        terminateInstancesContinuation.handleResult(instance);

        // verify
        verify(messageContext).routePiMessageToApplication(eq(securityGroupId), eq(EntityMethod.DELETE), eq(instance), eq(NetworkManagerApplication.APPLICATION_NAME));
        assertEquals(InstanceState.TERMINATED, instancesMap.get(INSTANCE_ID).getNextState());
    }
}
