package com.bt.pi.app.common.entities;

import java.util.Map;
import java.util.Set;

import com.bt.pi.app.common.entities.util.ResourceAllocation;
import com.bt.pi.core.application.resource.leased.LeasedResourceAllocationRecord;
import com.bt.pi.core.entity.PiEntity;

public interface AllocatableResourceIndex<T> extends PiEntity, LeasedResourceAllocationRecord<Long> {

    Map<Long, T> getCurrentAllocations();

    ResourceAllocation allocate(String consumerId);

    ResourceAllocation allocate(String consumerId, boolean reuseExisting);

    Set<Long> freeResourceFor(String consumerId);

    Long getInactiveResourceConsumerTimeoutSec();

}