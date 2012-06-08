package com.bt.pi.api.handlers;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.Before;
import org.junit.Test;
import org.springframework.ws.transport.context.TransportContext;

import com.amazonaws.ec2.doc.x20081201.AssociateAddressDocument;
import com.amazonaws.ec2.doc.x20081201.AssociateAddressResponseDocument;
import com.amazonaws.ec2.doc.x20081201.AssociateAddressType;
import com.bt.pi.api.handlers.AssociateAddressHandler;
import com.bt.pi.api.service.ElasticIpAddressesService;

public class AssociateAddressHandlerTest extends AbstractHandlerTest {

	private AssociateAddressHandler associateAddressHandler;
	private ElasticIpAddressesService elasticIpAddressesService;
	
	@Before
	public void setUp() throws Exception {
		super.before();
		this.associateAddressHandler = new AssociateAddressHandler(){
			@Override
			protected TransportContext getTransportContext() {
				return transportContext;
			}
		};
		elasticIpAddressesService = mock(ElasticIpAddressesService.class);
		when(elasticIpAddressesService.associateAddress("userid", "1.2.3.4", "i-123")).thenReturn(true);
		associateAddressHandler.setElasticIpAddressesService(elasticIpAddressesService);
	}

	@Test
	public void testAllocateAddressGood() {
		// setup
		AssociateAddressDocument requestDocument = AssociateAddressDocument.Factory.newInstance();
		AssociateAddressType addNewAssociateAddress = requestDocument.addNewAssociateAddress();
		addNewAssociateAddress.setPublicIp("1.2.3.4");
		addNewAssociateAddress.setInstanceId("i-123");
		
		// act
		AssociateAddressResponseDocument result = this.associateAddressHandler.associateAddress(requestDocument);
		
		// assert
		assertEquals(true, result.getAssociateAddressResponse().getReturn());
	}

	@Test
	public void testAllocateAddressBad() {
		// setup
		AssociateAddressDocument requestDocument = AssociateAddressDocument.Factory.newInstance();
		requestDocument.addNewAssociateAddress();
		
		// act
		AssociateAddressResponseDocument result = this.associateAddressHandler.associateAddress(requestDocument);
		
		// assert
		assertEquals(false, result.getAssociateAddressResponse().getReturn());
	}
}
