package com.bt.pi.app.common.entities;

import static org.junit.Assert.assertEquals;

import java.util.HashSet;
import java.util.Set;

import org.junit.Before;
import org.junit.Test;

import com.bt.pi.app.common.entities.util.ResourceAllocation;
import com.bt.pi.app.common.entities.util.ResourceRange;
import com.bt.pi.core.parser.KoalaJsonParser;

public class VlanAllocationIndexTest {
    private VlanAllocationIndex vlanAllocationIndex;
    private Set<ResourceRange> vlanRanges;

    @Before
    public void before() {
        vlanRanges = new HashSet<ResourceRange>();
        vlanRanges.add(new ResourceRange(10L, 15L));

        vlanAllocationIndex = new VlanAllocationIndex();
        vlanAllocationIndex.getAllocationMap().put(10L, new VlanAllocationRecord("moo"));
        vlanAllocationIndex.setResourceRanges(vlanRanges);
    }

    @Test
    public void shouldBeAbleToRoundTripToFromJson() {
        // setup
        KoalaJsonParser koalaJsonParser = new KoalaJsonParser();

        // act
        String json = koalaJsonParser.getJson(vlanAllocationIndex);
        VlanAllocationIndex reverse = (VlanAllocationIndex) koalaJsonParser.getObject(json, VlanAllocationIndex.class);

        // assert
        assertEquals(vlanAllocationIndex.getAllocationMap().size(), reverse.getAllocationMap().size());
        assertEquals(vlanAllocationIndex.getAllocationMap().get(10), reverse.getAllocationMap().get(10));
    }

    @Test
    public void shouldCreateNewRecord() {
        // act
        VlanAllocationRecord res = vlanAllocationIndex.addAllocationForConsumer(111L, "aaa", 1, 123);

        // assert
        assertEquals("aaa", res.getSecurityGroupId());
        assertEquals(new Long(123), res.getLastHeartbeatTimestamp());
    }

    @Test
    public void shouldReturnExistingVlanWhenPresent() {
        // act
        ResourceAllocation res = this.vlanAllocationIndex.allocate("moo");

        // assert
        assertEquals(10, res.getAllocatedResource());
        assertEquals(1, res.getAllocatedStepSize());
    }

    @Test
    public void shouldNotReturnExistingVlanWhenPresentIfNotWithinCorrectRange() {
        // setup
        vlanRanges = new HashSet<ResourceRange>();
        vlanRanges.add(new ResourceRange(12L, 15L));
        vlanAllocationIndex.setResourceRanges(vlanRanges);

        // act
        ResourceAllocation res = this.vlanAllocationIndex.allocate("moo");

        // assert
        assertEquals(12, res.getAllocatedResource());
        assertEquals(1, res.getAllocatedStepSize());
    }

    @Test
    public void shouldReturnNextAvailableVlanWhenNotPresent() {
        // act
        ResourceAllocation res = this.vlanAllocationIndex.allocate("baa");

        // assert
        assertEquals(11, res.getAllocatedResource());
        assertEquals(1, res.getAllocatedStepSize());
    }

    @Test
    public void shouldRemoveVlan() {
        // act
        this.vlanAllocationIndex.freeResourceFor("moo");

        // assert
        assertEquals(0, vlanAllocationIndex.getAllocationMap().size());
    }

    @Test
    public void shouldDoNothingForUnknownVlan() {
        // act
        this.vlanAllocationIndex.freeResourceFor("baa");

        // assert
        assertEquals(1, vlanAllocationIndex.getAllocationMap().size());
    }

    @Test
    public void shouldRemoveDupesForVlan() {
        // setup
        this.vlanAllocationIndex.getAllocationMap().put(11L, new VlanAllocationRecord("moo"));

        // act
        this.vlanAllocationIndex.freeResourceFor("moo");

        // assert
        assertEquals(0, vlanAllocationIndex.getAllocationMap().size());
    }

    @Test
    public void ensureHardcodedResourceSchemeIsCorrect() throws Exception {
        assertEquals(ResourceSchemes.VLAN_ALLOCATION_INDEX.toString() + ":" + "vlan-allocations", VlanAllocationIndex.URL);
    }
}
