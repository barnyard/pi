/* (c) British Telecommunications plc, 2009, All Rights Reserved */
package com.bt.pi.api.service;

import java.util.List;

import com.bt.pi.app.common.entities.KeyPair;

/**
 *	SSH Key Pair Service layer
 *	- create key pair
 *	- describe key pair
 *	- delete key pair 
 */
public interface KeyPairsService {

	KeyPair createKeyPair(String ownerId, String keyName);
	
	boolean deleteKeyPair(String ownerId, String keyName);

	List<KeyPair> describeKeyPairs(String ownerId, List<String> keyNames);
	
}
