package com.bt.pi.app.common.entities;

import static org.junit.Assert.assertEquals;

import java.util.HashSet;
import java.util.Set;

import org.junit.Before;
import org.junit.Test;

import com.bt.pi.app.common.entities.util.AllocatableResourceContainer;
import com.bt.pi.app.common.entities.util.ResourceAllocation;
import com.bt.pi.app.common.entities.util.ResourceAllocationException;
import com.bt.pi.app.common.entities.util.ResourceRange;
import com.bt.pi.app.common.entities.util.RoundRobinAllocator;

public class RoundRobinAllocatorTest {
    private Set<Long> allocatableResources;
    private Set<ResourceRange> allocatableResourceRanges;
    private Set<Long> resourcesInUse;
    private RoundRobinAllocator roundRobinAllocator;
    private AllocatableResourceContainer allocatableResourceContainer;
    private Long mostRecentlyAllocatedResource;

    @Before
    public void before() {
        allocatableResources = new HashSet<Long>();
        allocatableResources.add(10L);
        allocatableResources.add(11L);
        allocatableResources.add(12L);
        allocatableResources.add(13L);
        allocatableResources.add(14L);
        allocatableResources.add(15L);

        resourcesInUse = new HashSet<Long>();
        resourcesInUse.add(10L);
        resourcesInUse.add(11L);
        resourcesInUse.add(12L);

        allocatableResourceContainer = new AllocatableResourceContainer() {
            @Override
            public void setMostRecentlyAllocatedResource(Long mostRecentlyAllocatedResource) {
                RoundRobinAllocatorTest.this.mostRecentlyAllocatedResource = mostRecentlyAllocatedResource;
            }

            @Override
            public Long getMostRecentlyAllocatedResource() {
                return RoundRobinAllocatorTest.this.mostRecentlyAllocatedResource;
            }
        };

        allocatableResourceRanges = new HashSet<ResourceRange>();
        allocatableResourceRanges.add(new ResourceRange(10L, 15L));

        roundRobinAllocator = new RoundRobinAllocator();
    }

    @Test
    public void shouldGetNextAvailableVlan() {
        // act
        ResourceAllocation res = roundRobinAllocator.getNextAvailableResource(allocatableResources, resourcesInUse, allocatableResourceContainer);

        // assert
        assertEquals(13, res.getAllocatedResource());
        assertEquals(1, res.getAllocatedStepSize());
        assertEquals(new Long(13), mostRecentlyAllocatedResource);
    }

    @Test(expected = ResourceAllocationException.class)
    public void shouldThrowWhenNullAllocatableResourcesGiven() {
        // act
        roundRobinAllocator.getNextAvailableResource(null, resourcesInUse, allocatableResourceContainer);
    }

    @Test(expected = ResourceAllocationException.class)
    public void shouldThrowWhenNoAllocatableResourcesGiven() {
        // act
        roundRobinAllocator.getNextAvailableResource(new HashSet<Long>(), resourcesInUse, allocatableResourceContainer);
    }

    @Test
    public void shouldGetFirstAvailableResourceWhenNoneInUse() {
        // act
        ResourceAllocation res = roundRobinAllocator.getNextAvailableResource(allocatableResources, null, allocatableResourceContainer);

        // assert
        assertEquals(10, res.getAllocatedResource());
        assertEquals(1, res.getAllocatedStepSize());
        assertEquals(new Long(10), mostRecentlyAllocatedResource);
    }

    @Test(expected = ResourceAllocationException.class)
    public void shouldFailToGetNextAvailableResourceWhenNoResourceAvailable() {
        // setup
        allocatableResources = new HashSet<Long>();
        allocatableResources.add(10L);
        allocatableResources.add(11L);
        allocatableResources.add(12L);

        // act
        roundRobinAllocator.getNextAvailableResource(allocatableResources, resourcesInUse, allocatableResourceContainer);
    }

    @Test
    public void shouldGetNextAvailableResourceWhenRollingOver() {
        // setup
        allocatableResources = new HashSet<Long>();
        allocatableResources.add(5L);
        allocatableResources.add(11L);
        allocatableResources.add(12L);

        this.mostRecentlyAllocatedResource = 12L;

        // act
        ResourceAllocation res = roundRobinAllocator.getNextAvailableResource(allocatableResources, resourcesInUse, allocatableResourceContainer);

        // assert
        assertEquals(5, res.getAllocatedResource());
        assertEquals(1, res.getAllocatedStepSize());
        assertEquals(new Long(5), mostRecentlyAllocatedResource);
    }

    @Test
    public void shouldGetNextHighestAvailableResourceWhenResourceAllocationNonContiguous() {
        // setup
        resourcesInUse.remove(12L);
        resourcesInUse.add(13L);

        // act
        ResourceAllocation res = roundRobinAllocator.getNextAvailableResource(allocatableResources, resourcesInUse, allocatableResourceContainer);

        // assert
        assertEquals(12, res.getAllocatedResource());
        assertEquals(1, res.getAllocatedStepSize());
        assertEquals(new Long(12), mostRecentlyAllocatedResource);
    }

    @Test
    public void shouldNotRepeatedlyAllocateTheSameVlanId() {
        // setup
        roundRobinAllocator.getNextAvailableResource(allocatableResources, resourcesInUse, allocatableResourceContainer);

        // act
        ResourceAllocation res = roundRobinAllocator.getNextAvailableResource(allocatableResources, resourcesInUse, allocatableResourceContainer);

        // assert
        assertEquals(14, res.getAllocatedResource());
        assertEquals(1, res.getAllocatedStepSize());
        assertEquals(new Long(14), mostRecentlyAllocatedResource);
    }

    @Test
    public void shouldAllocateAboveMostRecentlyAllocated() {
        // setup
        mostRecentlyAllocatedResource = 13L;

        // act
        ResourceAllocation res = roundRobinAllocator.getNextAvailableResource(allocatableResources, resourcesInUse, allocatableResourceContainer);

        // assert
        assertEquals(14, res.getAllocatedResource());
        assertEquals(1, res.getAllocatedStepSize());
        assertEquals(new Long(14), mostRecentlyAllocatedResource);
    }

    @Test
    public void shouldNotCycleWhenAllocatingVlansAtTheTopOfTheRange() {
        // setup
        allocatableResources.remove(15L);
        resourcesInUse.remove(10L);

        ResourceAllocation res1 = roundRobinAllocator.getNextAvailableResource(allocatableResources, resourcesInUse, allocatableResourceContainer);
        resourcesInUse.add(res1.getAllocatedResource());

        ResourceAllocation res2 = roundRobinAllocator.getNextAvailableResource(allocatableResources, resourcesInUse, allocatableResourceContainer);
        resourcesInUse.add(res2.getAllocatedResource());

        resourcesInUse.remove(10L);

        // act
        ResourceAllocation res3 = roundRobinAllocator.getNextAvailableResource(allocatableResources, resourcesInUse, allocatableResourceContainer);
        ResourceAllocation res4 = roundRobinAllocator.getNextAvailableResource(allocatableResources, resourcesInUse, allocatableResourceContainer);

        // assert
        assertEquals(10, res1.getAllocatedResource());
        assertEquals(13, res2.getAllocatedResource());
        assertEquals(14, res3.getAllocatedResource());
        assertEquals(10, res4.getAllocatedResource());

        assertEquals(1, res1.getAllocatedStepSize());
        assertEquals(1, res2.getAllocatedStepSize());
        assertEquals(1, res3.getAllocatedStepSize());
        assertEquals(1, res4.getAllocatedStepSize());
    }

    @Test
    public void shouldNotRepeatedlyAllocateTheSameVlanIdWithRollover() {
        // setup
        roundRobinAllocator.getNextAvailableResource(allocatableResources, resourcesInUse, allocatableResourceContainer);
        roundRobinAllocator.getNextAvailableResource(allocatableResources, resourcesInUse, allocatableResourceContainer);
        roundRobinAllocator.getNextAvailableResource(allocatableResources, resourcesInUse, allocatableResourceContainer);

        // act
        ResourceAllocation res = roundRobinAllocator.getNextAvailableResource(allocatableResources, resourcesInUse, allocatableResourceContainer);

        // assert
        assertEquals(13, res.getAllocatedResource());
        assertEquals(1, res.getAllocatedStepSize());
    }

    @Test
    public void shouldGetFirstAvailableResourceWhenOnlyOneSpecifiedByRange() {
        // setup
        allocatableResourceRanges = new HashSet<ResourceRange>();
        allocatableResourceRanges.add(new ResourceRange(10L, 10L));
        resourcesInUse.remove(10L);

        // act
        ResourceAllocation res = roundRobinAllocator.getNextAvailableResourceByRange(allocatableResourceRanges, resourcesInUse, allocatableResourceContainer);

        // assert
        assertEquals(10, res.getAllocatedResource());
        assertEquals(1, res.getAllocatedStepSize());
    }

    @Test(expected = ResourceAllocationException.class)
    public void shouldNotGetAnAllocationWhenOnlyOneResourceSpecifiedByRangeAndStepSizeIsMoreThanOne() {
        // setup
        allocatableResourceRanges = new HashSet<ResourceRange>();
        allocatableResourceRanges.add(new ResourceRange(10L, 10L, 3));
        resourcesInUse.remove(10L);

        // act
        roundRobinAllocator.getNextAvailableResourceByRange(allocatableResourceRanges, resourcesInUse, allocatableResourceContainer);
    }

    @Test
    public void shouldGetResourceAcrossTwoRanges() {
        // setup
        allocatableResourceRanges = new HashSet<ResourceRange>();
        allocatableResourceRanges.add(new ResourceRange(10L, 11L));
        allocatableResourceRanges.add(new ResourceRange(12L, 13L));

        // act
        ResourceAllocation res = roundRobinAllocator.getNextAvailableResourceByRange(allocatableResourceRanges, resourcesInUse, allocatableResourceContainer);

        // assert
        assertEquals(13, res.getAllocatedResource());
        assertEquals(1, res.getAllocatedStepSize());
    }

    @Test
    public void shouldGetResourceAcrossTwoRangesWithDifferentSizes() {
        // setup
        allocatableResourceRanges = new HashSet<ResourceRange>();
        allocatableResourceRanges.add(new ResourceRange(10L, 11L));
        allocatableResourceRanges.add(new ResourceRange(13L, 15L, 3));

        // act
        ResourceAllocation res = roundRobinAllocator.getNextAvailableResourceByRange(allocatableResourceRanges, resourcesInUse, allocatableResourceContainer);

        // assert
        assertEquals(13, res.getAllocatedResource());
        assertEquals(3, res.getAllocatedStepSize());
    }

    @Test
    public void shouldGetResourceAcrossTwoRangesWithAGap() {
        // setup
        allocatableResourceRanges = new HashSet<ResourceRange>();
        allocatableResourceRanges.add(new ResourceRange(10L, 11L));
        allocatableResourceRanges.add(new ResourceRange(14L, 15L));

        // act
        ResourceAllocation res = roundRobinAllocator.getNextAvailableResourceByRange(allocatableResourceRanges, resourcesInUse, allocatableResourceContainer);

        // assert
        assertEquals(14, res.getAllocatedResource());
        assertEquals(1, res.getAllocatedStepSize());
    }

    @Test
    public void shouldGetResourceAcrossTwoOverlappingRanges() {
        // setup
        allocatableResourceRanges = new HashSet<ResourceRange>();
        allocatableResourceRanges.add(new ResourceRange(10L, 13L));
        allocatableResourceRanges.add(new ResourceRange(12L, 15L));

        // act
        ResourceAllocation res = roundRobinAllocator.getNextAvailableResourceByRange(allocatableResourceRanges, resourcesInUse, allocatableResourceContainer);

        // assert
        assertEquals(13, res.getAllocatedResource());
        assertEquals(1, res.getAllocatedStepSize());
    }

    @Test
    public void shouldGetResourceAcrossTwoOverlappingDifferentSteppedRanges() {
        // setup
        allocatableResourceRanges = new HashSet<ResourceRange>();
        allocatableResourceRanges.add(new ResourceRange(10L, 13L, 3));
        allocatableResourceRanges.add(new ResourceRange(12L, 15L, 2));

        // act
        ResourceAllocation res = roundRobinAllocator.getNextAvailableResourceByRange(allocatableResourceRanges, resourcesInUse, allocatableResourceContainer);

        // assert
        assertEquals(14, res.getAllocatedResource());
        assertEquals(2, res.getAllocatedStepSize());
    }

    @Test
    public void shouldGetResourceAcrossTwoRangesWithWraparound() {
        // setup
        resourcesInUse.add(13L);
        resourcesInUse.add(14L);
        resourcesInUse.remove(11L);

        allocatableResourceRanges = new HashSet<ResourceRange>();
        allocatableResourceRanges.add(new ResourceRange(10L, 11L));
        allocatableResourceRanges.add(new ResourceRange(12L, 14L));

        // act
        ResourceAllocation res = roundRobinAllocator.getNextAvailableResourceByRange(allocatableResourceRanges, resourcesInUse, allocatableResourceContainer);

        // assert
        assertEquals(11, res.getAllocatedResource());
        assertEquals(1, res.getAllocatedStepSize());
    }

    @Test
    public void shouldGetResourceAcrossTwoAllocationStepRangesWithWraparound() {
        // setup
        resourcesInUse.add(13L);
        resourcesInUse.remove(11L);

        allocatableResourceRanges = new HashSet<ResourceRange>();
        allocatableResourceRanges.add(new ResourceRange(10L, 11L));
        allocatableResourceRanges.add(new ResourceRange(12L, 14L, 2));

        // act
        ResourceAllocation res = roundRobinAllocator.getNextAvailableResourceByRange(allocatableResourceRanges, resourcesInUse, allocatableResourceContainer);

        // assert
        assertEquals(11, res.getAllocatedResource());
        assertEquals(1, res.getAllocatedStepSize());
    }

    @Test
    public void shouldGetResourceAcrossTwoRangesFirstNestedWithinSecond() {
        // setup
        resourcesInUse.remove(12L);

        allocatableResourceRanges = new HashSet<ResourceRange>();
        allocatableResourceRanges.add(new ResourceRange(13L, 14L));
        allocatableResourceRanges.add(new ResourceRange(10L, 15L));

        // act
        ResourceAllocation res = roundRobinAllocator.getNextAvailableResourceByRange(allocatableResourceRanges, resourcesInUse, allocatableResourceContainer);

        // assert
        assertEquals(12, res.getAllocatedResource());
        assertEquals(1, res.getAllocatedStepSize());
    }

    @Test
    public void shouldGetResourceWithNegativeIndex() {
        // setup
        resourcesInUse = new HashSet<Long>();
        resourcesInUse.add(-3L);
        resourcesInUse.add(-2L);

        allocatableResourceRanges = new HashSet<ResourceRange>();
        allocatableResourceRanges.add(new ResourceRange(-3L, -1L));

        // act
        ResourceAllocation res = roundRobinAllocator.getNextAvailableResourceByRange(allocatableResourceRanges, resourcesInUse, allocatableResourceContainer);

        // assert
        assertEquals(-1, res.getAllocatedResource());
        assertEquals(1, res.getAllocatedStepSize());
    }

    @Test
    public void shouldGetResourceAcrossSomeNonSensicalRangesSecond() {
        // setup
        allocatableResourceRanges = new HashSet<ResourceRange>();
        allocatableResourceRanges.add(new ResourceRange(12L, 1000L));
        allocatableResourceRanges.add(new ResourceRange(0L, -1L));
        allocatableResourceRanges.add(new ResourceRange(10L, 1L));
        allocatableResourceRanges.add(new ResourceRange(11L, 11L));
        allocatableResourceRanges.add(new ResourceRange(39L, 53L));
        allocatableResourceRanges.add(new ResourceRange(-1L, -2L));

        // act
        ResourceAllocation res = roundRobinAllocator.getNextAvailableResourceByRange(allocatableResourceRanges, resourcesInUse, allocatableResourceContainer);

        // assert
        assertEquals(13, res.getAllocatedResource());
        assertEquals(1, res.getAllocatedStepSize());
    }

    @Test
    public void shouldGetResourceAcrossTwoRangesFirstNestedWithinSecondWithWraparound() {
        // setup
        resourcesInUse.add(13L);
        resourcesInUse.add(14L);
        resourcesInUse.remove(11L);

        allocatableResourceRanges = new HashSet<ResourceRange>();
        allocatableResourceRanges.add(new ResourceRange(12L, 13L));
        allocatableResourceRanges.add(new ResourceRange(10L, 14L));

        // act
        ResourceAllocation res = roundRobinAllocator.getNextAvailableResourceByRange(allocatableResourceRanges, resourcesInUse, allocatableResourceContainer);

        // assert
        assertEquals(11, res.getAllocatedResource());
        assertEquals(1, res.getAllocatedStepSize());
    }

    @Test(expected = ResourceAllocationException.class)
    public void shouldFailToGetNextAvailableResourceWhenMinExceedsMax() {
        // setup
        allocatableResourceRanges = new HashSet<ResourceRange>();
        allocatableResourceRanges.add(new ResourceRange(10L, 9L));

        // act
        roundRobinAllocator.getNextAvailableResourceByRange(allocatableResourceRanges, resourcesInUse, allocatableResourceContainer);
    }

    @Test
    public void shouldGetNextHighestAvailableResourceWhenCurrentlyAllocatedBelowRange() {
        // setup
        mostRecentlyAllocatedResource = 10L;
        resourcesInUse.remove(11L);
        resourcesInUse.remove(12L);
        allocatableResourceRanges = new HashSet<ResourceRange>();
        allocatableResourceRanges.add(new ResourceRange(12L, 15L));

        // act
        ResourceAllocation res = roundRobinAllocator.getNextAvailableResourceByRange(allocatableResourceRanges, resourcesInUse, allocatableResourceContainer);

        // assert
        assertEquals(12, res.getAllocatedResource());
        assertEquals(1, res.getAllocatedStepSize());
    }

    @Test
    public void shouldGetNextHighestAvailableResourceWhenCurrentlyAllocatedAboveRange() {
        // setup
        resourcesInUse.remove(12L);
        mostRecentlyAllocatedResource = 15L;

        allocatableResourceRanges = new HashSet<ResourceRange>();
        allocatableResourceRanges.add(new ResourceRange(12L, 14L));

        // act
        ResourceAllocation res = roundRobinAllocator.getNextAvailableResourceByRange(allocatableResourceRanges, resourcesInUse, allocatableResourceContainer);

        // assert
        assertEquals(12, res.getAllocatedResource());
        assertEquals(1, res.getAllocatedStepSize());
    }

    @Test
    public void shouldGetNextHighestAvailableResourceWhenCurrentlyAllocatedWithinRangeButStepInsufficientForAllocation() {
        // setup
        resourcesInUse.remove(12L);
        mostRecentlyAllocatedResource = 14L;

        allocatableResourceRanges = new HashSet<ResourceRange>();
        allocatableResourceRanges.add(new ResourceRange(12L, 14L, 2));

        // act
        ResourceAllocation res = roundRobinAllocator.getNextAvailableResourceByRange(allocatableResourceRanges, resourcesInUse, allocatableResourceContainer);

        // assert
        assertEquals(12, res.getAllocatedResource());
        assertEquals(2, res.getAllocatedStepSize());
    }

    @Test
    public void shouldUseMostRecentlyAllocatedAndStartAllocatingFromThere() {
        resourcesInUse.add(14L);
        mostRecentlyAllocatedResource = 14L;

        // act
        ResourceAllocation res = roundRobinAllocator.getNextAvailableResourceByRange(allocatableResourceRanges, resourcesInUse, allocatableResourceContainer);

        // assert
        assertEquals(15, res.getAllocatedResource());
    }
}
