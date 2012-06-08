package com.bt.pi.app.common.entities;

import java.util.HashMap;
import java.util.Map;

import org.codehaus.jackson.annotate.JsonIgnore;

public class DummyAllocatableResourceIndex extends AllocatableResourceIndexBase<DummyHeartbeatTimestampEntity> {

    private Map<Long, DummyHeartbeatTimestampEntity> map = new HashMap<Long, DummyHeartbeatTimestampEntity>();

    public DummyAllocatableResourceIndex() {
    }

    @Override
    public String getUrl() {
        return "url:url";
    }

    public Map<Long, DummyHeartbeatTimestampEntity> getAllocationMap() {
        return map;
    }

    @JsonIgnore
    @Override
    public Map<Long, DummyHeartbeatTimestampEntity> getCurrentAllocations() {
        return map;
    }

    @Override
    protected DummyHeartbeatTimestampEntity addAllocationForConsumer(Long allocatedResource, String consumerId, int stepSize, long creationTimestamp) {
        DummyHeartbeatTimestampEntity res = new DummyHeartbeatTimestampEntity(consumerId);
        map.put(allocatedResource, res);
        return res;
    }

    @Override
    public boolean releaseResourceAllocationForConsumer(Long allocatedResource, String consumerId) {
        return map.remove(allocatedResource) != null;
    }

    @Override
    protected int getExistingAllocationStepSize(Long allocatedResource, DummyHeartbeatTimestampEntity allocationRecord) {
        return 1;
    }

    @Override
    public boolean heartbeat(Long resourceId, String consumerId) {
        return map.containsKey(resourceId) && map.get(resourceId).isConsumedBy(consumerId);
    }

    @Override
    public String getUriScheme() {
        return "url";
    }
};