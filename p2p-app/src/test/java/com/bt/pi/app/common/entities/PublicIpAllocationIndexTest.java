package com.bt.pi.app.common.entities;

import static com.bt.pi.app.common.net.utils.IpAddressUtils.ipToLong;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.junit.Before;
import org.junit.Test;

import com.bt.pi.app.common.entities.util.ResourceAllocationException;
import com.bt.pi.app.common.entities.util.ResourceRange;
import com.bt.pi.app.common.net.utils.IpAddressUtils;
import com.bt.pi.core.parser.KoalaJsonParser;

public class PublicIpAllocationIndexTest {
    private static final String USERNAME = "username";
    private PublicIpAllocationIndex publicIpAllocationIndex;
    private Map<Long, InstanceRecord> map = new HashMap<Long, InstanceRecord>();
    private Set<ResourceRange> resourceRanges;

    @Before
    public void before() {
        resourceRanges = new HashSet<ResourceRange>();
        resourceRanges.add(new ResourceRange(ipToLong("127.0.0.1"), ipToLong("127.0.0.5")));

        publicIpAllocationIndex = createPublicIpAllocationIndex();
        map.put(ipToLong("1.2.3.4"), new InstanceRecord("i-moo", "ownerId"));
    }

    private PublicIpAllocationIndex createPublicIpAllocationIndex() {
        PublicIpAllocationIndex tempIndex = new PublicIpAllocationIndex();
        tempIndex.setResourceRanges(resourceRanges);
        tempIndex.setAllocationMap(map);
        tempIndex.setMostRecentlyAllocatedResource(10L);
        return tempIndex;
    }

    @Test
    public void shouldStartFromMostRecentlyAllocatedPlaceInIpRange() {
        // setup
        Long lastAllocatedIpAddress = ipToLong("127.0.0.3");
        publicIpAllocationIndex.setMostRecentlyAllocatedResource(lastAllocatedIpAddress);

        // act
        String ipAddress = publicIpAllocationIndex.allocateIpAddressToInstance("bob");

        // assert
        assertEquals(lastAllocatedIpAddress + 1, ipToLong(ipAddress));
    }

    @Test
    public void testGettersAndSetters() {
        // setup

        // assert
        assertEquals(map, publicIpAllocationIndex.getAllocationMap());
        assertEquals(new Long(10), publicIpAllocationIndex.getMostRecentlyAllocatedResource());
    }

    @Test
    public void testIncrementVersion() {

        // act
        publicIpAllocationIndex.incrementVersion();

        // assert
        assertEquals(1, publicIpAllocationIndex.getVersion());
    }

    @Test
    public void testGetType() {
        // act & asssert
        assertEquals(PublicIpAllocationIndex.class.getSimpleName(), publicIpAllocationIndex.getType());
    }

    @Test
    public void shouldBeAbleToAllocateIpAddressToInstance() {
        // act
        String instanceid = "instanceId";

        // assert
        assertEquals("127.0.0.1", publicIpAllocationIndex.allocateIpAddressToInstance(instanceid));
    }

    @Test
    public void shouldBeAbleToAllocateIpAddressToUser() {
        // act
        String res = publicIpAllocationIndex.allocateElasticIpAddressToUser(USERNAME);

        // assert
        assertEquals("127.0.0.1", res);
    }

    @Test
    public void testSerialization() {
        // setup
        KoalaJsonParser parser = new KoalaJsonParser();

        // act
        PublicIpAllocationIndex reverse = (PublicIpAllocationIndex) parser.getObject(parser.getJson(publicIpAllocationIndex), PublicIpAllocationIndex.class);

        // assert
        assertEquals(publicIpAllocationIndex, reverse);
    }

    @Test
    public void testEquals() {
        assertEquals(createPublicIpAllocationIndex(), createPublicIpAllocationIndex());
    }

    @Test
    public void testHashCode() {
        assertEquals(createPublicIpAllocationIndex().hashCode(), createPublicIpAllocationIndex().hashCode());
    }

    @Test
    public void testAllocationOfNewRecordForInstanceRecord() {
        // act
        InstanceRecord res = publicIpAllocationIndex.addAllocationForConsumer(1234L, "i-123", 1, 123);

        // assert
        assertEquals("i-123", res.getInstanceId());
        assertEquals(new Long(123), res.getLastHeartbeatTimestamp());
    }

    @Test
    public void testAllocationOfNewRecordForNonInstanceRecord() {
        // act
        InstanceRecord res = publicIpAllocationIndex.addAllocationForConsumer(1234L, "aaa", 1, 123);

        // assert
        assertEquals("aaa", res.getOwnerId());
        assertEquals(new Long(123), res.getLastHeartbeatTimestamp());
    }

    @Test(expected = PublicIpAddressAllocationException.class)
    public void shouldFailToAssignElasticAddressForUnknownAddress() {
        // act
        publicIpAllocationIndex.assignElasticIpAddressToInstance("3.3.3.3", "i-xyz", "ownerId");
    }

    @Test(expected = PublicIpAddressAllocationException.class)
    public void shouldFailToAssignElasticAddressForUserWhoDoesNotOwnThatAddress() {
        // setup
        map.put(ipToLong("5.6.7.8"), new InstanceRecord(null, "ownerId"));

        // act
        publicIpAllocationIndex.assignElasticIpAddressToInstance("5.6.7.8", "i-xyz", "someone-else");
    }

    @Test(expected = PublicIpAddressAllocationException.class)
    public void shouldFailToAssignElasticAddressWhenAddressAlreadyAssignedToAnotherInstance() {
        // setup
        map.put(ipToLong("5.6.7.8"), new InstanceRecord("i-abc", "ownerId"));

        // act
        publicIpAllocationIndex.assignElasticIpAddressToInstance("5.6.7.8", "i-xyz", "ownerId");
    }

    @Test(expected = PublicIpAddressAllocationException.class)
    public void shouldFailToAssignElasticAddressWhenInstanceHasAnotherElasticAddress() {
        // setup
        map.put(ipToLong("5.6.7.8"), new InstanceRecord("i-abc", "ownerId"));

        // act
        publicIpAllocationIndex.assignElasticIpAddressToInstance("5.6.7.8", "i-moo", "ownerId");
    }

    @Test
    public void shouldAssignElasticAddressToInstanceAndReturnReleasedAddressWhenInstanceHasNoElasticAddress() {
        // setup
        map.put(ipToLong("5.6.7.8"), new InstanceRecord(null, "ownerId"));

        // act
        String res = publicIpAllocationIndex.assignElasticIpAddressToInstance("5.6.7.8", "i-boo", "ownerId");

        // assert
        assertEquals(null, res);
        assertEquals("i-boo", map.get(ipToLong("5.6.7.8")).getInstanceId());
        assertEquals("ownerId", map.get(ipToLong("5.6.7.8")).getOwnerId());
        assertEquals("i-moo", map.get(ipToLong("1.2.3.4")).getInstanceId());
        assertEquals("ownerId", map.get(ipToLong("1.2.3.4")).getOwnerId());
    }

    @Test
    public void shouldAssignElasticAddressToInstanceAndReturnReleasedAddressWhenInstanceHasDifferentElasticAddress() {
        // setup
        map.put(ipToLong("5.6.7.8"), new InstanceRecord(null, "ownerId"));

        // act
        String res = publicIpAllocationIndex.assignElasticIpAddressToInstance("5.6.7.8", "i-moo", "ownerId");

        // assert
        assertEquals("1.2.3.4", res);
        assertEquals("i-moo", map.get(ipToLong("5.6.7.8")).getInstanceId());
        assertEquals("ownerId", map.get(ipToLong("5.6.7.8")).getOwnerId());
        assertNull(map.get(ipToLong("1.2.3.4")).getInstanceId());
        assertEquals("ownerId", map.get(ipToLong("1.2.3.4")).getOwnerId());
    }

    @Test
    public void shouldAssignElasticAddressToInstanceAndReturnReleasedAddressWhenInstanceHasSameElasticAddress() {
        // setup
        map.put(ipToLong("5.6.7.8"), new InstanceRecord("i-boo", "ownerId"));

        // act
        String res = publicIpAllocationIndex.assignElasticIpAddressToInstance("5.6.7.8", "i-boo", "ownerId");

        // assert
        assertEquals(null, res);
        assertEquals("i-boo", map.get(ipToLong("5.6.7.8")).getInstanceId());
        assertEquals("ownerId", map.get(ipToLong("5.6.7.8")).getOwnerId());
        assertEquals("i-moo", map.get(ipToLong("1.2.3.4")).getInstanceId());
        assertEquals("ownerId", map.get(ipToLong("1.2.3.4")).getOwnerId());
    }

    @Test
    public void shouldAssignElasticAddressToInstanceAndReturnReleasedAddressWhenInstanceHasDynamicAddress() {
        // setup
        map.get(ipToLong("1.2.3.4")).setOwnerId(null);
        map.put(ipToLong("5.6.7.8"), new InstanceRecord(null, "ownerId"));

        // act
        String res = publicIpAllocationIndex.assignElasticIpAddressToInstance("5.6.7.8", "i-moo", "ownerId");

        // assert
        assertEquals("1.2.3.4", res);
        assertEquals("i-moo", map.get(ipToLong("5.6.7.8")).getInstanceId());
        assertEquals("ownerId", map.get(ipToLong("5.6.7.8")).getOwnerId());
        assertNull(map.get(ipToLong("1.2.3.4")));
    }

    @Test(expected = PublicIpAddressAllocationException.class)
    public void shouldFailToUnassignElasticAddressForUnknownAddress() {
        // act
        publicIpAllocationIndex.unassignElasticIpAddressFromInstance("3.3.3.3", "ownerId");
    }

    @Test(expected = PublicIpAddressAllocationException.class)
    public void shouldFailToUnassignElasticAddressForUserWhoDoesNotOwnThatAddress() {
        // setup
        map.put(ipToLong("5.6.7.8"), new InstanceRecord("i-xyz", "ownerId"));

        // act
        publicIpAllocationIndex.unassignElasticIpAddressFromInstance("5.6.7.8", "someone-else");
    }

    @Test
    public void shouldUnassignElasticAddressWhenAddressNotAssignedToInstance() {
        // setup
        map.put(ipToLong("5.6.7.8"), new InstanceRecord(null, "ownerId"));

        // act
        String res = publicIpAllocationIndex.unassignElasticIpAddressFromInstance("5.6.7.8", "ownerId");

        // assert
        assertNull(res);
    }

    @Test
    public void shouldUnassignElasticAddressWhenAddressAssignedToInstance() {
        // setup
        map.put(ipToLong("5.6.7.8"), new InstanceRecord("i-baah", "ownerId"));

        // act
        String res = publicIpAllocationIndex.unassignElasticIpAddressFromInstance("5.6.7.8", "ownerId");

        // assert
        assertEquals("i-baah", res);
    }

    @Test
    public void shouldReleaseExistingElasticIpAddrForInstance() {
        // act
        Set<Long> res = this.publicIpAllocationIndex.freeResourceFor("i-moo");

        // assert
        assertEquals(1, res.size());
        assertEquals(new Long(ipToLong("1.2.3.4")), res.iterator().next());
        assertEquals(null, publicIpAllocationIndex.getAllocationMap().get(ipToLong("1.2.3.4")).getInstanceId());
    }

    @Test
    public void shouldAllocateElasticIpForUser() {
        // act
        String res = publicIpAllocationIndex.allocateElasticIpAddressToUser(USERNAME);

        // assert
        assertEquals("127.0.0.1", res);
    }

    @Test
    public void shouldAllocateElasticIpForUserWhenUserAlreadyHasAllocations() {
        // setup
        publicIpAllocationIndex.allocateElasticIpAddressToUser(USERNAME);

        // act
        String res = publicIpAllocationIndex.allocateElasticIpAddressToUser(USERNAME);

        // assert
        assertEquals("127.0.0.2", res);
    }

    @Test(expected = ResourceAllocationException.class)
    public void shouldNotAllowReleaseOfExistingElasticIpAddrForUser() {
        // act
        this.publicIpAllocationIndex.freeResourceFor("user");
    }

    @Test
    public void shouldReleaseExistingElasticIpAddrForUserWhenIpAssignedToInstance() {
        // act
        boolean res = this.publicIpAllocationIndex.releaseElasticIpAddressForUser("1.2.3.4", "ownerId");

        // assert
        assertTrue(res);
        assertEquals(null, publicIpAllocationIndex.getAllocationMap().get(ipToLong("1.2.3.4")).getOwnerId());
        assertEquals("i-moo", publicIpAllocationIndex.getAllocationMap().get(ipToLong("1.2.3.4")).getInstanceId());
    }

    @Test
    public void shouldReleaseExistingElasticIpAddrForUserWhenIpNotAssignedToInstance() {
        // setup
        publicIpAllocationIndex.getAllocationMap().get(ipToLong("1.2.3.4")).setInstanceId(null);

        // act
        boolean res = this.publicIpAllocationIndex.releaseElasticIpAddressForUser("1.2.3.4", "ownerId");

        // assert
        assertTrue(res);
        assertEquals(null, publicIpAllocationIndex.getAllocationMap().get(ipToLong("1.2.3.4")));
    }

    @Test
    public void shouldDoNothingWhenReleasingElasticIpForUnknownOwner() {
        // act
        boolean res = this.publicIpAllocationIndex.releaseElasticIpAddressForUser("1.2.3.4", "unknown");

        // assert
        assertFalse(res);
        assertEquals(1, publicIpAllocationIndex.getAllocationMap().size());
    }

    @Test
    public void shouldReleaseExistingDynamicIpAddrForInstance() {
        // setup
        publicIpAllocationIndex.getAllocationMap().get(ipToLong("1.2.3.4")).setOwnerId(null);

        // act
        Set<Long> res = this.publicIpAllocationIndex.freeResourceFor("i-moo");

        // assert
        assertEquals(1, res.size());
        assertEquals(new Long(ipToLong("1.2.3.4")), res.iterator().next());
        assertEquals(null, publicIpAllocationIndex.getAllocationMap().get(ipToLong("1.2.3.4")));
    }

    @Test
    public void shouldDoNothingForUnknownInstanceAllocation() {
        // act
        Set<Long> res = this.publicIpAllocationIndex.freeResourceFor("i-unknown");

        // assert
        assertEquals(0, res.size());
        assertEquals(1, publicIpAllocationIndex.getAllocationMap().size());
    }

    @Test
    public void shouldRemoveDupesForDynamicAddress() {
        // setup
        publicIpAllocationIndex.getAllocationMap().get(ipToLong("1.2.3.4")).setOwnerId(null);
        this.publicIpAllocationIndex.getAllocationMap().put(IpAddressUtils.ipToLong("5.6.7.8"), new InstanceRecord("i-moo", null));

        // act
        this.publicIpAllocationIndex.freeResourceFor("i-moo");

        // assert
        assertEquals(0, publicIpAllocationIndex.getAllocationMap().size());
    }
}
