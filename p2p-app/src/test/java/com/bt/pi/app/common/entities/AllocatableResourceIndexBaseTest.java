package com.bt.pi.app.common.entities;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.HashSet;
import java.util.Random;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.Before;
import org.junit.Test;

import com.bt.pi.app.common.entities.util.ResourceAllocation;
import com.bt.pi.app.common.entities.util.ResourceRange;
import com.bt.pi.core.parser.KoalaJsonParser;

public class AllocatableResourceIndexBaseTest {
    private AllocatableResourceIndexBase<DummyHeartbeatTimestampEntity> allocatableResourceIndexBase;
    private Set<ResourceRange> resourceRanges;

    @Before
    public void before() {
        resourceRanges = new HashSet<ResourceRange>();
        resourceRanges.add(new ResourceRange(10L, 15L));

        allocatableResourceIndexBase = new DummyAllocatableResourceIndex();
        allocatableResourceIndexBase.getCurrentAllocations().put(10L, new DummyHeartbeatTimestampEntity("moo"));
        allocatableResourceIndexBase.setResourceRanges(resourceRanges);
        allocatableResourceIndexBase.setInactiveResourceConsumerTimeoutSec(1L);
    }

    @Test
    public void shouldBeAbleToRoundTripToFromJson() {
        // setup
        KoalaJsonParser koalaJsonParser = new KoalaJsonParser();

        // act
        String json = koalaJsonParser.getJson(allocatableResourceIndexBase);
        DummyAllocatableResourceIndex reverse = (DummyAllocatableResourceIndex) koalaJsonParser.getObject(json, DummyAllocatableResourceIndex.class);

        // assert
        assertEquals(allocatableResourceIndexBase.getCurrentAllocations().size(), reverse.getAllocationMap().size());
        assertEquals(allocatableResourceIndexBase.getCurrentAllocations().get(10), reverse.getAllocationMap().get(10));
    }

    @Test
    public void shouldReturnExistingResourceWhenPresent() {
        // act
        ResourceAllocation res = this.allocatableResourceIndexBase.allocate("moo");

        // assert
        assertEquals(10, res.getAllocatedResource());
        assertEquals(1, res.getAllocatedStepSize());
    }

    @Test
    public void shouldNotReturnExistingResourceWhenPresentIfNoReuseFlagSet() {
        // act
        ResourceAllocation res = this.allocatableResourceIndexBase.allocate("moo", false);

        // assert
        assertEquals(11, res.getAllocatedResource());
        assertEquals(1, res.getAllocatedStepSize());
    }

    @Test
    public void shouldNotReturnExistingResourceWhenPresentIfNotWithinCorrectRange() {
        // setup
        resourceRanges = new HashSet<ResourceRange>();
        resourceRanges.add(new ResourceRange(12L, 15L));
        allocatableResourceIndexBase.setResourceRanges(resourceRanges);

        // act
        ResourceAllocation res = this.allocatableResourceIndexBase.allocate("moo");

        // assert
        assertEquals(12, res.getAllocatedResource());
        assertEquals(1, res.getAllocatedStepSize());
    }

    @Test
    public void shouldReturnNextAvailableResourceWhenNotPresent() {
        // act
        ResourceAllocation res = this.allocatableResourceIndexBase.allocate("baa");

        // assert
        assertEquals(11, res.getAllocatedResource());
        assertEquals(1, res.getAllocatedStepSize());
    }

    @Test
    public void shouldRemoveResource() {
        // act
        Set<Long> res = this.allocatableResourceIndexBase.freeResourceFor("moo");

        // assert
        assertEquals(0, allocatableResourceIndexBase.getCurrentAllocations().size());
        assertEquals(1, res.size());
        assertTrue(res.contains(10L));
    }

    @Test
    public void shouldDoNothingForUnknownConsumer() {
        // act
        Set<Long> res = this.allocatableResourceIndexBase.freeResourceFor("baa");

        // assert
        assertEquals(1, allocatableResourceIndexBase.getCurrentAllocations().size());
        assertEquals(0, res.size());
    }

    @Test
    public void shouldRemoveDupesForResource() {
        // setup
        this.allocatableResourceIndexBase.getCurrentAllocations().put(11L, new DummyHeartbeatTimestampEntity("moo"));

        // act
        Set<Long> res = this.allocatableResourceIndexBase.freeResourceFor("moo");

        // assert
        assertEquals(0, allocatableResourceIndexBase.getCurrentAllocations().size());
        assertEquals(2, res.size());
        assertTrue(res.contains(10L));
        assertTrue(res.contains(11L));
    }

    @Test
    public void shouldHeartbeat() {
        // act
        boolean res = allocatableResourceIndexBase.heartbeat(10L, "moo");

        // assert
        assertTrue(res);
    }

    @Test
    public void shouldNotHeartbeatWhenResourceNotAllocatedToGivenConsumer() {
        // act
        boolean res = allocatableResourceIndexBase.heartbeat(10L, "baa");

        // assert
        assertFalse(res);
    }

    @Test
    public void shouldNotHeartbeatWhenResourceNotInAllocationMap() {
        // act
        boolean res = allocatableResourceIndexBase.heartbeat(11L, "moo");

        // assert
        assertFalse(res);
    }

    @Test
    public void shouldBeAbleToDoToStringForLargeRecordInReasonableTime() {
        // setup
        Log LOG = LogFactory.getLog(AllocatableResourceIndexBaseTest.class);
        Random random = new Random();
        for (int i = 0; i < 100000; i++)
            allocatableResourceIndexBase.addAllocationForConsumer(random.nextLong(), "consumer", 1, System.currentTimeMillis());

        // act
        long timestampBefore = System.currentTimeMillis();
        String res = allocatableResourceIndexBase.toString();
        LOG.info(res);
        long timestampAfter = System.currentTimeMillis();

        // assert
        assertTrue(timestampAfter - timestampBefore < 1000);
    }
}
