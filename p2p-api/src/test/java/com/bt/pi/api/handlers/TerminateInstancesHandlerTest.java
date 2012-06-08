package com.bt.pi.api.handlers;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.springframework.ws.transport.context.TransportContext;

import com.amazonaws.ec2.doc.x20081201.TerminateInstancesDocument;
import com.amazonaws.ec2.doc.x20081201.TerminateInstancesInfoType;
import com.amazonaws.ec2.doc.x20081201.TerminateInstancesItemType;
import com.amazonaws.ec2.doc.x20081201.TerminateInstancesResponseDocument;
import com.amazonaws.ec2.doc.x20081201.TerminateInstancesType;
import com.bt.pi.api.service.InstancesService;
import com.bt.pi.app.common.entities.InstanceState;
import com.bt.pi.app.instancemanager.handlers.InstanceStateTransition;

public class TerminateInstancesHandlerTest extends AbstractHandlerTest {
    private static final String INSTANCE_ID = "i-123";
    private TerminateInstancesHandler terminateInstancesHandler;
    private TerminateInstancesDocument requestDocument;
    private TerminateInstancesType addNewTerminateInstances;
    private InstancesService instancesService;

    @Before
    @SuppressWarnings("unchecked")
    public void setUp() throws Exception {
        super.before();
        this.terminateInstancesHandler = new TerminateInstancesHandler() {
            @Override
            protected TransportContext getTransportContext() {
                return transportContext;
            }
        };

        requestDocument = TerminateInstancesDocument.Factory.newInstance();
        addNewTerminateInstances = requestDocument.addNewTerminateInstances();
        TerminateInstancesInfoType addNewInstancesSet = addNewTerminateInstances.addNewInstancesSet();
        TerminateInstancesItemType addNewItem = addNewInstancesSet.addNewItem();
        addNewItem.setInstanceId(INSTANCE_ID);
        instancesService = mock(InstancesService.class);
        List<String> instanceIds = new ArrayList<String>();
        instanceIds.add(INSTANCE_ID);

        Map<String, InstanceStateTransition> instanceTransistions = new HashMap<String, InstanceStateTransition>();
        instanceTransistions.put(INSTANCE_ID, new InstanceStateTransition(InstanceState.RUNNING, InstanceState.SHUTTING_DOWN));
        when(instancesService.terminateInstances(anyString(), (Collection<String>) anyObject())).thenReturn(instanceTransistions);
        terminateInstancesHandler.setInstancesService(instancesService);
    }

    @Test
    public void testTerminateInstancesGood() {
        // setup

        // act
        TerminateInstancesResponseDocument result = this.terminateInstancesHandler.terminateInstances(requestDocument);

        // assert
        assertEquals(INSTANCE_ID, result.getTerminateInstancesResponse().getInstancesSet().getItemArray(0).getInstanceId());
        assertEquals(InstanceState.RUNNING.getDisplayName(), result.getTerminateInstancesResponse().getInstancesSet().getItemArray(0).getPreviousState().getName());
        assertEquals(InstanceState.RUNNING.getCode(), result.getTerminateInstancesResponse().getInstancesSet().getItemArray(0).getPreviousState().getCode());
        assertEquals(InstanceState.SHUTTING_DOWN.getDisplayName(), result.getTerminateInstancesResponse().getInstancesSet().getItemArray(0).getShutdownState().getName());
        assertEquals(InstanceState.SHUTTING_DOWN.getCode(), result.getTerminateInstancesResponse().getInstancesSet().getItemArray(0).getShutdownState().getCode());
    }
}
