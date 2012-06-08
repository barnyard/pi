/* (c) British Telecommunications plc, 2009, All Rights Reserved */
package com.bt.pi.app.common.entities;

import java.util.HashMap;
import java.util.Map;

import org.codehaus.jackson.annotate.JsonIgnore;

import com.bt.pi.app.common.net.utils.IpAddressUtils;
import com.bt.pi.core.application.resource.DefaultDhtResourceWatchingStrategy;
import com.bt.pi.core.application.resource.watched.WatchedResource;
import com.bt.pi.core.entity.Backupable;
import com.bt.pi.core.entity.EntityScope;
import com.bt.pi.core.scope.NodeScope;

@Backupable
@EntityScope(scope = NodeScope.REGION)
@WatchedResource(watchingStrategy = DefaultDhtResourceWatchingStrategy.class, defaultInitialResourceRefreshIntervalMillis = SubnetAllocationIndex.DEFAULT_INITIAL_INTERVAL_MILLIS, defaultRepeatingResourceRefreshIntervalMillis = SubnetAllocationIndex.DEFAULT_REPEATING_INTERVAL_MILLIS, initialResourceRefreshIntervalMillisProperty = SubnetAllocationIndex.INITIAL_INTERVAL_MILLIS_PROPERTY, repeatingResourceRefreshIntervalMillisProperty = SubnetAllocationIndex.REPEATING_INTERVAL_MILLIS_PROPERTY)
public class SubnetAllocationIndex extends AllocatableResourceIndexBase<SubnetAllocationRecord> {
    public static final String SCHEME = "idx";
    public static final String URL = SCHEME + ":" + "subnet-allocations";
    protected static final long DEFAULT_INITIAL_INTERVAL_MILLIS = 10000;
    protected static final long DEFAULT_REPEATING_INTERVAL_MILLIS = 86400000;
    protected static final String INITIAL_INTERVAL_MILLIS_PROPERTY = "subnetAllocationIndex.subscribe.initial.wait.time.millis";
    protected static final String REPEATING_INTERVAL_MILLIS_PROPERTY = "subnetAllocationIndex.subscribe.interval.millis";
    private Map<Long, SubnetAllocationRecord> subnetAllocationMap;
    private String dnsAddress;

    public SubnetAllocationIndex() {
        super();
        subnetAllocationMap = new HashMap<Long, SubnetAllocationRecord>();
        dnsAddress = null;
    }

    public String getDnsAddress() {
        return dnsAddress;
    }

    public void setDnsAddress(String aDnsAddress) {
        this.dnsAddress = aDnsAddress;
    }

    public Map<Long, SubnetAllocationRecord> getAllocationMap() {
        return subnetAllocationMap;
    }

    @JsonIgnore
    @Override
    public Map<Long, SubnetAllocationRecord> getCurrentAllocations() {
        return subnetAllocationMap;
    }

    @Override
    protected SubnetAllocationRecord addAllocationForConsumer(Long allocatedResource, String consumerId, int addressesInSubnet, long creationTimestamp) {
        SubnetAllocationRecord subnetAllocationRecord = new SubnetAllocationRecord();
        subnetAllocationRecord.setSecurityGroupId(consumerId);
        subnetAllocationRecord.setSubnetMask(IpAddressUtils.netSizeToNetmask(addressesInSubnet));
        subnetAllocationRecord.setLastHeartbeatTimestamp(creationTimestamp);

        this.subnetAllocationMap.put(allocatedResource, subnetAllocationRecord);
        return subnetAllocationRecord;
    }

    @Override
    public boolean releaseResourceAllocationForConsumer(Long allocatedResource, String consumerId) {
        return subnetAllocationMap.remove(allocatedResource) != null;
    }

    @Override
    public String getUrl() {
        return URL;
    }

    @Override
    protected int getExistingAllocationStepSize(Long allocatedResource, SubnetAllocationRecord allocationRecord) {
        long res = IpAddressUtils.netmaskToNetSize(allocationRecord.getSubnetMask());
        if (res > Integer.MAX_VALUE)
            throw new RuntimeException("Allocation size for subnet out of range!");
        return (int) res;
    }

    @Override
    public String getUriScheme() {
        return SCHEME;
    }
}
