package com.bt.pi.api.handlers;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.Before;
import org.junit.Test;
import org.springframework.ws.transport.context.TransportContext;

import com.amazonaws.ec2.doc.x20081201.CreateKeyPairDocument;
import com.amazonaws.ec2.doc.x20081201.CreateKeyPairResponseDocument;
import com.amazonaws.ec2.doc.x20081201.CreateKeyPairType;
import com.bt.pi.api.handlers.CreateKeyPairHandler;
import com.bt.pi.api.service.KeyPairsService;
import com.bt.pi.app.common.entities.KeyPair;

public class CreateKeyPairHandlerTest extends AbstractHandlerTest {

	private CreateKeyPairHandler createKeyPairHandler;
	private String keyName = "myKey";
	private KeyPairsService keyPairsService;

	@Before
	public void setUp() throws Exception {
		super.before();
		this.createKeyPairHandler = new CreateKeyPairHandler(){
			@Override
			protected TransportContext getTransportContext() {
				return transportContext;
			}
		};
		keyPairsService = mock(KeyPairsService.class);
		KeyPair keyPair = new KeyPair(keyName, keyName + " fingerprint", keyName + " some key material");
		when(keyPairsService.createKeyPair("userid", keyName)).thenReturn(keyPair);
		createKeyPairHandler.setKeyPairsService(keyPairsService);
	}

	@Test
	public void testCreateKeyPair() {
		// setup
		CreateKeyPairDocument requestDocument = CreateKeyPairDocument.Factory.newInstance();
		CreateKeyPairType addNewCreateKeyPair = requestDocument.addNewCreateKeyPair();
		addNewCreateKeyPair.setKeyName(keyName);
		
		// act
		CreateKeyPairResponseDocument result = this.createKeyPairHandler.createKeyPair(requestDocument);
		
		// assert
		assertEquals(keyName, result.getCreateKeyPairResponse().getKeyName());
		assertEquals(keyName + " fingerprint", result.getCreateKeyPairResponse().getKeyFingerprint());
		assertEquals(keyName + " some key material", result.getCreateKeyPairResponse().getKeyMaterial());
		
	}
}
