package com.bt.pi.api.service.testing;

import java.util.ArrayList;
import java.util.List;

import com.bt.pi.api.service.KeyPairsService;
import com.bt.pi.app.common.entities.KeyPair;

public class KeyPairsServiceIntegrationImpl implements KeyPairsService {

	public KeyPair createKeyPair(String ownerId, String keyName) {
		return new KeyPair(keyName, keyName + " fingerprint", keyName + " some key material");
	}

	public boolean deleteKeyPair(String ownerId, String keyName) {
		return true;
	}

	public List<KeyPair> describeKeyPairs(String string, List<String> keyNames) {
		List<KeyPair> keyPairs = new ArrayList<KeyPair>();
		for (String keyName : keyNames) {
			keyPairs.add(new KeyPair(keyName, keyName + " fingerprint", ""));
		}
		return keyPairs;
	}

}
