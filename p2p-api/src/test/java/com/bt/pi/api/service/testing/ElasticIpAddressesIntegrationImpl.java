package com.bt.pi.api.service.testing;

import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;

import com.bt.pi.api.service.ElasticIpAddressesService;
import com.bt.pi.app.common.entities.InstanceRecord;

public class ElasticIpAddressesIntegrationImpl implements ElasticIpAddressesService {

    public String allocateAddress(String ownerId) {
        return "1.2.3.4";
    }

    public boolean associateAddress(String ownerId, String publicIpAddress, String instanceId) {
        if ("1.2.3.4".equals(publicIpAddress) && "i-123".equals(instanceId))
            return true;
        return false;
    }

    public SortedMap<String, InstanceRecord> describeAddresses(String string, List<String> addresses) {
        SortedMap<String, InstanceRecord> addressesRecords = new TreeMap<String, InstanceRecord>();
        addressesRecords.put("1.1.1.1", new InstanceRecord("i-001", "userid"));
        addressesRecords.put("2.2.2.2", new InstanceRecord("i-002", "userid"));
        return addressesRecords;
    }

    public boolean disassociateAddress(String ownerId, String publicIpAddress) {
        if (publicIpAddress.equals("10.249.162.100"))
            return true;
        return false;
    }

    public boolean releaseAddress(String ownerId, String publicIpAddress) {
        if (publicIpAddress.equals("10.249.162.999"))
            return false;
        return true;
    }

}
