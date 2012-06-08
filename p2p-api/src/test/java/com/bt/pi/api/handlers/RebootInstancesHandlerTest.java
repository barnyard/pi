package com.bt.pi.api.handlers;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.springframework.ws.transport.context.TransportContext;

import com.amazonaws.ec2.doc.x20081201.RebootInstancesDocument;
import com.amazonaws.ec2.doc.x20081201.RebootInstancesResponseDocument;
import com.amazonaws.ec2.doc.x20081201.RebootInstancesType;
import com.bt.pi.api.handlers.RebootInstancesHandler;
import com.bt.pi.api.service.InstancesService;

public class RebootInstancesHandlerTest extends AbstractHandlerTest {

	private RebootInstancesHandler rebootInstancesHandler;
	private RebootInstancesDocument requestDocument;
	private RebootInstancesType addNewRebootInstances;
	private String instanceId;
	private InstancesService instancesService;

	@Before
	public void setUp() throws Exception {
		super.before();
		this.rebootInstancesHandler = new RebootInstancesHandler() {
			@Override
			protected TransportContext getTransportContext() {
				return transportContext;
			}
		};
		requestDocument = RebootInstancesDocument.Factory.newInstance();
		addNewRebootInstances = requestDocument.addNewRebootInstances();
		instanceId = "i-123";
		addNewRebootInstances.addNewInstancesSet().addNewItem().setInstanceId(instanceId);
		instancesService = mock(InstancesService.class);
		List<String> instanceIds = new ArrayList<String>();
		instanceIds.add("i-123");
		when(instancesService.rebootInstances("userid", instanceIds)).thenReturn(true);
		rebootInstancesHandler.setInstancesService(instancesService);
	}

	@Test
	public void testRebootInstancesGood() {
		// setup
		
		// act
		RebootInstancesResponseDocument result = this.rebootInstancesHandler.rebootInstances(requestDocument);
		
		// assert
		assertEquals(true, result.getRebootInstancesResponse().getReturn());
	}

	@Test
	public void testRebootInstancesBad() {
		// setup
		addNewRebootInstances.getInstancesSet().getItemArray(0).setInstanceId("i-2222");
		
		// act
		RebootInstancesResponseDocument result = this.rebootInstancesHandler.rebootInstances(requestDocument);
		
		// assert
		assertEquals(false, result.getRebootInstancesResponse().getReturn());
	}
}
