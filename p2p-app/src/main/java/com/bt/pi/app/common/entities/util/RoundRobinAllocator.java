package com.bt.pi.app.common.entities.util;

import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class RoundRobinAllocator {
    private static final String NO_ALLOCATABLE_RESOURCE_RANGES_GIVEN = "No allocatable resource ranges given";
    private static final Log LOG = LogFactory.getLog(RoundRobinAllocator.class);

    public RoundRobinAllocator() {
    }

    public ResourceAllocation getNextAvailableResource(Set<Long> allocatableResources, Set<Long> resourcesInUse, AllocatableResourceContainer resourceContainer) {
        Set<ResourceRange> ranges = new TreeSet<ResourceRange>();

        if (allocatableResources == null || allocatableResources.isEmpty())
            throw new ResourceAllocationException(NO_ALLOCATABLE_RESOURCE_RANGES_GIVEN);

        for (Long l : allocatableResources)
            ranges.add(new ResourceRange(l, l));
        return getNextAvailableResourceByRange(ranges, resourcesInUse, resourceContainer);
    }

    public ResourceAllocation getNextAvailableResourceByRange(Set<ResourceRange> allocatableResources, Set<Long> resourcesInUse, AllocatableResourceContainer resourceContainer) {
        LOG.debug(String.format("getNextAvailableResource(%s, %s, %s)", allocatableResources, resourcesInUse, resourceContainer));

        if (allocatableResources == null || allocatableResources.isEmpty())
            throw new ResourceAllocationException(NO_ALLOCATABLE_RESOURCE_RANGES_GIVEN);

        SortedSet<ResourceRange> sortedAllocatableResources;
        if (allocatableResources instanceof SortedSet<?>)
            sortedAllocatableResources = (SortedSet<ResourceRange>) allocatableResources;
        else
            sortedAllocatableResources = new TreeSet<ResourceRange>(allocatableResources);

        Long minAcrossAllRanges = sortedAllocatableResources.first().getMin();
        Long maxAcrossAllRanges = sortedAllocatableResources.last().getMax();
        for (ResourceRange rr : sortedAllocatableResources)
            if (rr.getMax() > maxAcrossAllRanges)
                maxAcrossAllRanges = rr.getMax();
        LOG.debug(String.format("Min across all ranges is %d, max is %d", minAcrossAllRanges, maxAcrossAllRanges));

        Long mostRecentlyAllocated = resourceContainer.getMostRecentlyAllocatedResource();
        LOG.debug(String.format("Most recently allocated is %d", mostRecentlyAllocated));

        Long positionToStartFrom = getPositionToStartFrom(minAcrossAllRanges, maxAcrossAllRanges, mostRecentlyAllocated);
        LOG.debug(String.format("Starting from %d", positionToStartFrom));

        ResourceAllocation resourceAllocation = searchRanges(sortedAllocatableResources, resourcesInUse, positionToStartFrom, maxAcrossAllRanges);
        if (resourceAllocation == null)
            resourceAllocation = searchRanges(sortedAllocatableResources, resourcesInUse, minAcrossAllRanges, positionToStartFrom - 1);

        if (resourceAllocation != null) {
            // record what we will be using.
            resourceContainer.setMostRecentlyAllocatedResource(resourceAllocation.getAllocatedResource());
        } else {
            throw new ResourceAllocationException(String.format("No resource available from the set: %s", allocatableResources));
        }

        LOG.debug(String.format("Allocated resource %s", resourceAllocation));
        return resourceAllocation;
    }

    protected ResourceAllocation searchRanges(SortedSet<ResourceRange> sortedAllocatableResources, Set<Long> resourcesInUse, Long minSearchIndex, Long maxSearchIndex) {
        if (minSearchIndex > maxSearchIndex)
            return null;

        for (ResourceRange currentResourceRange : sortedAllocatableResources) {
            if (currentResourceRange.getMax() < minSearchIndex)
                continue;
            if (currentResourceRange.getMin() > maxSearchIndex)
                continue;

            LOG.debug(String.format("Looking for available resource in range %d - %d", currentResourceRange.getMin(), currentResourceRange.getMax()));
            for (Long currentResource : currentResourceRange) {
                if (currentResource >= minSearchIndex && currentResource <= maxSearchIndex && (resourcesInUse == null || !resourcesInUse.contains(currentResource))) {
                    return new ResourceAllocation(currentResource, currentResourceRange.getAllocationStepSize());
                }
            }
        }
        return null;
    }

    private Long getPositionToStartFrom(Long minAllowedResource, Long maxAllowedResource, Long mostRecentlyAllocated) {
        Long positionToStartFrom = null;
        if (mostRecentlyAllocated != null)
            positionToStartFrom = mostRecentlyAllocated + 1;

        // loop go back to the start;
        if (positionToStartFrom == null || positionToStartFrom > maxAllowedResource) {
            positionToStartFrom = minAllowedResource;
        }
        return positionToStartFrom;
    }
}
