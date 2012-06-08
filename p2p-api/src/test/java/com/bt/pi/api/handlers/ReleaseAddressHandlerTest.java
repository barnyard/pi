package com.bt.pi.api.handlers;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.Before;
import org.junit.Test;
import org.springframework.ws.transport.context.TransportContext;

import com.amazonaws.ec2.doc.x20081201.ReleaseAddressDocument;
import com.amazonaws.ec2.doc.x20081201.ReleaseAddressResponseDocument;
import com.amazonaws.ec2.doc.x20081201.ReleaseAddressType;
import com.bt.pi.api.handlers.ReleaseAddressHandler;
import com.bt.pi.api.service.ElasticIpAddressesService;

public class ReleaseAddressHandlerTest extends AbstractHandlerTest {
	private ReleaseAddressHandler releaseAddressHandler;
	private ElasticIpAddressesService elasticIpAddressesService;

	@Before
	public void setUp() throws Exception {
		super.before();
		this.releaseAddressHandler = new ReleaseAddressHandler(){
			@Override
			protected TransportContext getTransportContext() {
				return transportContext;
			}
		};
		elasticIpAddressesService = mock(ElasticIpAddressesService.class);
		when(elasticIpAddressesService.releaseAddress("userid", "10.249.162.999")).thenReturn(false);
		when(elasticIpAddressesService.releaseAddress("userid", "1.2.3.4")).thenReturn(true);
		releaseAddressHandler.setElasticIpAddressesService(elasticIpAddressesService);
	}

	@Test
	public void testAllocateAddressGood() {
		// setup
		ReleaseAddressDocument requestDocument = ReleaseAddressDocument.Factory.newInstance();
		ReleaseAddressType addNewReleaseAddress = requestDocument.addNewReleaseAddress();
		addNewReleaseAddress.setPublicIp("1.2.3.4");
		
		// act
		ReleaseAddressResponseDocument result = this.releaseAddressHandler.releaseAddress(requestDocument);
		
		// assert
		assertEquals(true, result.getReleaseAddressResponse().getReturn());
	}

	@Test
	public void testAllocateAddressBad() {
		// setup
		ReleaseAddressDocument requestDocument = ReleaseAddressDocument.Factory.newInstance();
		ReleaseAddressType addNewReleaseAddress = requestDocument.addNewReleaseAddress();
		addNewReleaseAddress.setPublicIp("10.249.162.999");
		
		// act
		ReleaseAddressResponseDocument result = this.releaseAddressHandler.releaseAddress(requestDocument);
		
		// assert
		assertEquals(false, result.getReleaseAddressResponse().getReturn());
	}
}
