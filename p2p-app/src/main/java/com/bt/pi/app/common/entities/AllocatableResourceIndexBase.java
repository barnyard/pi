package com.bt.pi.app.common.entities;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.jackson.annotate.JsonIgnore;

import com.bt.pi.app.common.entities.util.AllocatableResourceContainer;
import com.bt.pi.app.common.entities.util.ResourceAllocation;
import com.bt.pi.app.common.entities.util.ResourceAllocationException;
import com.bt.pi.app.common.entities.util.ResourceRange;
import com.bt.pi.app.common.entities.util.RoundRobinAllocator;
import com.bt.pi.core.entity.PiEntityBase;

public abstract class AllocatableResourceIndexBase<T extends HeartbeatTimestampResource> extends PiEntityBase implements AllocatableResourceContainer, AllocatableResourceIndex<T> {
    private static final Log LOG = LogFactory.getLog(AllocatableResourceIndexBase.class);
    private Set<ResourceRange> resourceRanges;
    private Long mostRecentlyAllocatedResource;
    private RoundRobinAllocator allocator;
    private Long inactiveResourceConsumerTimeoutSec;

    public AllocatableResourceIndexBase() {
        resourceRanges = new HashSet<ResourceRange>();
        mostRecentlyAllocatedResource = null;
        allocator = new RoundRobinAllocator();
        inactiveResourceConsumerTimeoutSec = null;
    }

    @JsonIgnore
    public RoundRobinAllocator getAllocator() {
        return allocator;
    }

    @Override
    @JsonIgnore
    public abstract Map<Long, T> getCurrentAllocations();

    protected abstract T addAllocationForConsumer(Long allocatedResource, String consumerId, int stepSize, long creationTimestamp);

    protected abstract boolean releaseResourceAllocationForConsumer(Long allocatedResource, String consumerId);

    protected abstract int getExistingAllocationStepSize(Long allocatedResource, T allocationRecord);

    public Set<ResourceRange> getResourceRanges() {
        return resourceRanges;
    }

    public void setResourceRanges(Set<ResourceRange> aResourceRanges) {
        this.resourceRanges = aResourceRanges;
    }

    @Override
    public String getType() {
        return getClass().getSimpleName();
    }

    /*
     * Time out value in seconds. If the value is -1 then the resource will not be released. The default value for PublicIpAllocationIndex is set to -1
     * because we don't want it to release if not attached to an instance.
     * @see com.bt.pi.app.common.entities.AllocatableResourceIndex#getInactiveResourceConsumerTimeoutSec()
     */
    @Override
    public Long getInactiveResourceConsumerTimeoutSec() {
        return inactiveResourceConsumerTimeoutSec;
    }

    public void setInactiveResourceConsumerTimeoutSec(Long aInactiveResourceConsumerTimeoutSec) {
        this.inactiveResourceConsumerTimeoutSec = aInactiveResourceConsumerTimeoutSec;
    }

    @Override
    public Long getMostRecentlyAllocatedResource() {
        return mostRecentlyAllocatedResource;
    }

    @Override
    public void setMostRecentlyAllocatedResource(Long resourceIndex) {
        mostRecentlyAllocatedResource = resourceIndex;
    }

    @SuppressWarnings("unchecked")
    @Override
    public boolean equals(final Object other) {
        if (!(other instanceof AllocatableResourceIndexBase))
            return false;
        AllocatableResourceIndexBase castOther = (AllocatableResourceIndexBase) other;
        return new EqualsBuilder().append(getCurrentAllocations(), castOther.getCurrentAllocations()).append(mostRecentlyAllocatedResource, castOther.mostRecentlyAllocatedResource).isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder().append(getCurrentAllocations()).append(mostRecentlyAllocatedResource).toHashCode();
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this).appendSuper(super.toString()).append("allocationMap - size: ", getCurrentAllocations().size()).append("mostResentlyAllocatedResource", mostRecentlyAllocatedResource).append("allocator", allocator).toString();
    }

    protected boolean isResourceInRange(long resource) {
        for (ResourceRange rr : getResourceRanges()) {
            if (resource >= rr.getMin() && resource <= rr.getMax())
                return true;
        }
        return false;
    }

    public ResourceAllocation allocate(String consumerId) {
        return allocate(consumerId, true);
    }

    public ResourceAllocation allocate(String consumerId, boolean reuseExisting) {
        LOG.debug(String.format("Allocate resource to consumer %s, reuse existing: %s", consumerId, reuseExisting));

        Set<Long> allocatedVlans = new HashSet<Long>();
        for (Entry<Long, T> entry : getCurrentAllocations().entrySet()) {
            Long currentResource = entry.getKey();
            T currentAllocation = entry.getValue();
            if (currentAllocation != null && currentAllocation.isConsumedBy(consumerId) && reuseExisting) {
                if (isResourceInRange(currentResource)) {
                    LOG.info(String.format("Reusing existing allocation %s of resource %s", currentAllocation, currentResource));
                    int existingAllocationStepSize = getExistingAllocationStepSize(currentResource, entry.getValue());
                    return new ResourceAllocation(currentResource, existingAllocationStepSize);
                } else {
                    LOG.info(String.format("Allocated resource id for consumer %s was %s, and hence outside of the valid range - ignoring it", consumerId, currentResource));
                }
            } else {
                if (currentResource != null)
                    allocatedVlans.add(currentResource);
            }

        }
        ResourceAllocation resourceAllocation = null;
        try {
            resourceAllocation = getAllocator().getNextAvailableResourceByRange(getResourceRanges(), allocatedVlans, this);
        } catch (ResourceAllocationException e) {
            throw e;
        }

        T newAllocation = addAllocationForConsumer(resourceAllocation.getAllocatedResource(), consumerId, resourceAllocation.getAllocatedStepSize(), System.currentTimeMillis());
        LOG.info(String.format("Allocated resource %s to group %s, and got allocation %s", resourceAllocation, consumerId, newAllocation));
        return resourceAllocation;
    }

    public Set<Long> freeResourceFor(String consumerId) {
        LOG.debug(String.format("freeResourceFor(%s)", consumerId));
        List<Entry<Long, T>> resourcesToRemove = new ArrayList<Entry<Long, T>>();
        for (Entry<Long, T> entry : getCurrentAllocations().entrySet()) {
            if (entry.getValue() == null)
                continue;
            T currentAllocation = entry.getValue();
            if (currentAllocation != null && currentAllocation.isConsumedBy(consumerId)) {
                resourcesToRemove.add(entry);
            }
        }

        Set<Long> removedResources = new HashSet<Long>();
        for (Entry<Long, T> entry : resourcesToRemove) {
            LOG.info(String.format("Freeing resouce %s for consumer %s", entry.getKey(), consumerId));
            if (releaseResourceAllocationForConsumer(entry.getKey(), consumerId))
                removedResources.add(entry.getKey());
        }
        return removedResources;
    }

    @Override
    public boolean heartbeat(Long resourceId, String consumerId) {
        LOG.debug(String.format("heartbeat(%s, %s)", resourceId, consumerId));
        HeartbeatTimestampResource allocation = getCurrentAllocations().get(resourceId);
        if (allocation == null) {
            LOG.warn(String.format("No allocation found for resource %s", resourceId));
            return false;
        }

        if (!allocation.isConsumedBy(consumerId)) {
            LOG.warn(String.format("Resource %s is not currently allocated to consumer %s - current allocation is %s", resourceId, consumerId, allocation));
            return false;
        }

        allocation.heartbeat();
        return true;
    };
}
