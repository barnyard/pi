/* (c) British Telecommunications plc, 2009, All Rights Reserved */
package com.bt.pi.api.service;

import java.util.List;
import java.util.SortedMap;

import com.bt.pi.app.common.entities.InstanceRecord;

/**
 * Interface for all IP address based API calls - Allocate Address - Associate Address - Describe Address - Disassociate
 * Address - Release Address
 * 
 */
public interface ElasticIpAddressesService {

    String allocateAddress(String ownerId);

    boolean associateAddress(String ownerId, String publicIpAddress, String instanceId);

    SortedMap<String, InstanceRecord> describeAddresses(String ownerId, List<String> addresses);

    boolean disassociateAddress(String ownerId, String publicIpAddress);

    boolean releaseAddress(String ownerId, String publicIpAddress);

}
