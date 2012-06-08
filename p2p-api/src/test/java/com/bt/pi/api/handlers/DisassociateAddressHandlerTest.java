package com.bt.pi.api.handlers;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.mock;

import org.junit.Before;
import org.junit.Test;
import org.springframework.ws.transport.context.TransportContext;

import com.amazonaws.ec2.doc.x20081201.DisassociateAddressDocument;
import com.amazonaws.ec2.doc.x20081201.DisassociateAddressResponseDocument;
import com.amazonaws.ec2.doc.x20081201.DisassociateAddressType;
import com.bt.pi.api.handlers.DisassociateAddressHandler;
import com.bt.pi.api.service.ElasticIpAddressesService;

public class DisassociateAddressHandlerTest extends AbstractHandlerTest {

	private DisassociateAddressHandler disassociateAddressHandler;
	private DisassociateAddressDocument requestDocument;
	private DisassociateAddressType addNewDisassociateAddress;
	private ElasticIpAddressesService elasticIpAddressesService;

	@Before
	public void setUp() throws Exception {
		super.before();
		this.disassociateAddressHandler = new DisassociateAddressHandler(){
			@Override
			protected TransportContext getTransportContext() {
				return transportContext;
			}
		};
		requestDocument = DisassociateAddressDocument.Factory.newInstance();
		addNewDisassociateAddress = requestDocument.addNewDisassociateAddress();
		elasticIpAddressesService = mock(ElasticIpAddressesService.class);
		when(elasticIpAddressesService.disassociateAddress("userid", "10.249.162.100")).thenReturn(true);
		disassociateAddressHandler.setElasticIpAddressesService(elasticIpAddressesService);
	}

	@Test
	public void testDisassociateAddressGood() {
		// setup
		addNewDisassociateAddress.setPublicIp("10.249.162.100");
		
		// act
		DisassociateAddressResponseDocument result = this.disassociateAddressHandler.disassociateAddress(requestDocument);
		
		// assert
		assertEquals(true, result.getDisassociateAddressResponse().getReturn());
	}

	@Test
	public void testDisassociateAddressBad() {
		// setup
		
		// act
		DisassociateAddressResponseDocument result = this.disassociateAddressHandler.disassociateAddress(requestDocument);
		
		// assert
		assertEquals(false, result.getDisassociateAddressResponse().getReturn());
	}
}
