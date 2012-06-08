package com.bt.pi.api.handlers;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.Before;
import org.junit.Test;
import org.springframework.ws.transport.context.TransportContext;

import com.amazonaws.ec2.doc.x20081201.DeleteKeyPairDocument;
import com.amazonaws.ec2.doc.x20081201.DeleteKeyPairResponseDocument;
import com.amazonaws.ec2.doc.x20081201.DeleteKeyPairType;
import com.bt.pi.api.handlers.DeleteKeyPairHandler;
import com.bt.pi.api.service.KeyPairsService;

public class DeleteKeyPairHandlerTest extends AbstractHandlerTest {

	private DeleteKeyPairHandler deleteKeyPairHandler;
	private String keyName = "myKey";
	private KeyPairsService keyPairsService;

	@Before
	public void setUp() throws Exception {
		super.before();
		this.deleteKeyPairHandler = new DeleteKeyPairHandler(){
			@Override
			protected TransportContext getTransportContext() {
				return transportContext;
			}
		};
		keyPairsService = mock(KeyPairsService.class);
		when(keyPairsService.deleteKeyPair("userid", keyName)).thenReturn(true);
		deleteKeyPairHandler.setKeyPairsService(keyPairsService);
	}

	@Test
	public void testDeleteKeyPair() {
		// setup
		DeleteKeyPairDocument requestDocument = DeleteKeyPairDocument.Factory.newInstance();
		DeleteKeyPairType addNewDeleteKeyPair = requestDocument.addNewDeleteKeyPair();
		addNewDeleteKeyPair.setKeyName(keyName);
		
		// act
		DeleteKeyPairResponseDocument result = this.deleteKeyPairHandler.deleteKeyPair(requestDocument);
		
		// assert
		assertEquals(true, result.getDeleteKeyPairResponse().getReturn());
	}
}
