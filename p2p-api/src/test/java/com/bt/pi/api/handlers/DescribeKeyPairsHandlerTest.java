package com.bt.pi.api.handlers;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.springframework.ws.transport.context.TransportContext;

import com.amazonaws.ec2.doc.x20081201.DescribeKeyPairsDocument;
import com.amazonaws.ec2.doc.x20081201.DescribeKeyPairsInfoType;
import com.amazonaws.ec2.doc.x20081201.DescribeKeyPairsResponseDocument;
import com.amazonaws.ec2.doc.x20081201.DescribeKeyPairsType;
import com.bt.pi.api.handlers.DescribeKeyPairsHandler;
import com.bt.pi.api.service.KeyPairsService;
import com.bt.pi.app.common.entities.KeyPair;

public class DescribeKeyPairsHandlerTest extends AbstractHandlerTest {

	private DescribeKeyPairsHandler describeKeyPairsHandler;
	private String keyName1 = "myKey1";
	private String keyName2 = "myKey2";
	private KeyPairsService keyPairsService;

	@Before
	public void setUp() throws Exception {
		super.before();
		this.describeKeyPairsHandler = new DescribeKeyPairsHandler(){
			@Override
			protected TransportContext getTransportContext() {
				return transportContext;
			}
		};
		keyPairsService = mock(KeyPairsService.class);
		List<String> keyNames = new ArrayList<String>();
		keyNames.add(keyName1);
		keyNames.add(keyName2);
		List<KeyPair> keyPairs = new ArrayList<KeyPair>();
		keyPairs.add(new KeyPair("myKey1", "myKey1 fingerprint", ""));
		keyPairs.add(new KeyPair("myKey2", "myKey2 fingerprint", ""));
		when(keyPairsService.describeKeyPairs("userid", keyNames)).thenReturn(keyPairs);
		describeKeyPairsHandler.setKeyPairsService(keyPairsService);
	}

	@Test
	public void testDescribeKeyPairs() {
		// setup
		DescribeKeyPairsDocument requestDocument = DescribeKeyPairsDocument.Factory.newInstance();
		DescribeKeyPairsType addNewDescribeKeyPairs = requestDocument.addNewDescribeKeyPairs();
		DescribeKeyPairsInfoType addNewKeySet = addNewDescribeKeyPairs.addNewKeySet();
		addNewKeySet.addNewItem().setKeyName(keyName1);
		addNewKeySet.addNewItem().setKeyName(keyName2);
		
		// act
		DescribeKeyPairsResponseDocument result = this.describeKeyPairsHandler.describeKeyPairs(requestDocument);
		
		// assert
		assertEquals(keyName1, result.getDescribeKeyPairsResponse().getKeySet().getItemArray(0).getKeyName());
		assertEquals(keyName1 + " fingerprint", result.getDescribeKeyPairsResponse().getKeySet().getItemArray(0).getKeyFingerprint());
		assertEquals(keyName2, result.getDescribeKeyPairsResponse().getKeySet().getItemArray(1).getKeyName());
		assertEquals(keyName2 + " fingerprint", result.getDescribeKeyPairsResponse().getKeySet().getItemArray(1).getKeyFingerprint());
	}
}
