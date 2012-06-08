package com.bt.pi.app.common.entities;

import java.util.HashMap;
import java.util.Map;

import org.codehaus.jackson.annotate.JsonIgnore;

import com.bt.pi.core.entity.Backupable;
import com.bt.pi.core.entity.EntityScope;
import com.bt.pi.core.scope.NodeScope;

@Backupable
@EntityScope(scope = NodeScope.REGION)
public class VlanAllocationIndex extends AllocatableResourceIndexBase<VlanAllocationRecord> {
    public static final String SCHEME = "idx";
    public static final String URL = SCHEME + ":" + "vlan-allocations";
    private Map<Long, VlanAllocationRecord> vlanAllocationMap;

    public VlanAllocationIndex() {
        super();
        setMostRecentlyAllocatedResource(null);
        vlanAllocationMap = new HashMap<Long, VlanAllocationRecord>();
    }

    public Map<Long, VlanAllocationRecord> getAllocationMap() {
        return vlanAllocationMap;
    }

    @Override
    @JsonIgnore
    public Map<Long, VlanAllocationRecord> getCurrentAllocations() {
        return vlanAllocationMap;
    }

    @Override
    protected VlanAllocationRecord addAllocationForConsumer(Long allocatedResource, String consumerId, int stepSize, long creationTimestamp) {
        VlanAllocationRecord rec = new VlanAllocationRecord(consumerId);
        rec.setLastHeartbeatTimestamp(creationTimestamp);
        this.vlanAllocationMap.put(allocatedResource, rec);
        return rec;
    }

    @Override
    public boolean releaseResourceAllocationForConsumer(Long allocatedResource, String consumerId) {
        return vlanAllocationMap.remove(allocatedResource) != null;
    }

    @Override
    public String getUrl() {
        return URL;
    }

    @Override
    protected int getExistingAllocationStepSize(Long allocatedResource, VlanAllocationRecord allocationRecord) {
        return 1;
    }

    @Override
    public String getUriScheme() {
        return SCHEME;
    }
}
