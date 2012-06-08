package com.bt.pi.app.common.entities;

import static org.junit.Assert.assertEquals;

import java.util.HashSet;
import java.util.Set;

import org.junit.Before;
import org.junit.Test;

import com.bt.pi.app.common.entities.util.ResourceRange;
import com.bt.pi.app.common.net.utils.IpAddressUtils;
import com.bt.pi.core.application.resource.DefaultDhtResourceWatchingStrategy;
import com.bt.pi.core.application.resource.watched.WatchedResource;
import com.bt.pi.core.parser.KoalaJsonParser;

public class SubnetAllocationIndexTest {
    private SubnetAllocationIndex subnetAllocationIndex;
    private Set<ResourceRange> resourceRanges;

    @Before
    public void before() {
        resourceRanges = new HashSet<ResourceRange>();
        resourceRanges.add(new ResourceRange(10L, 15L, 2));

        subnetAllocationIndex = new SubnetAllocationIndex();
        subnetAllocationIndex.setResourceRanges(resourceRanges);
        subnetAllocationIndex.setDnsAddress("1.2.3.4");

        subnetAllocationIndex.allocate("abc:default");
    }

    @Test
    public void shouldCreateConsumerRecord() {
        // act
        SubnetAllocationRecord res = subnetAllocationIndex.addAllocationForConsumer(123L, "aaa", 16, 123);

        // assert
        assertEquals("aaa", res.getSecurityGroupId());
        assertEquals(new Long(123), res.getLastHeartbeatTimestamp());
        assertEquals(IpAddressUtils.ipToLong("255.255.255.240"), res.getSubnetMask().longValue());
    }

    @Test
    public void shouldBeAbleToRoundTripToFromJson() {
        // setup
        KoalaJsonParser koalaJsonParser = new KoalaJsonParser();

        // act
        String json = koalaJsonParser.getJson(subnetAllocationIndex);
        SubnetAllocationIndex reverse = (SubnetAllocationIndex) koalaJsonParser.getObject(json, SubnetAllocationIndex.class);

        // assert
        assertEquals(subnetAllocationIndex.getAllocationMap().size(), reverse.getAllocationMap().size());
        assertEquals(subnetAllocationIndex.getAllocationMap().get(10), reverse.getAllocationMap().get(10));
        assertEquals("1.2.3.4", reverse.getDnsAddress());
    }

    @Test
    public void ensureHardcodedResourceSchemeIsCorrect() throws Exception {
        assertEquals(ResourceSchemes.SUBNET_ALLOCATION_INDEX.toString() + ":" + "subnet-allocations", SubnetAllocationIndex.URL);
    }

    @Test
    public void shouldBeAnnotatedAsWatchedResourceWithAppropriateStrategyAndIntervalSettings() {
        // act
        WatchedResource res = SubnetAllocationIndex.class.getAnnotation(WatchedResource.class);

        // assert
        assertEquals(DefaultDhtResourceWatchingStrategy.class, res.watchingStrategy());
        assertEquals(10000, res.defaultInitialResourceRefreshIntervalMillis());
        assertEquals(86400000, res.defaultRepeatingResourceRefreshIntervalMillis());
        assertEquals("subnetAllocationIndex.subscribe.initial.wait.time.millis", res.initialResourceRefreshIntervalMillisProperty());
        assertEquals("subnetAllocationIndex.subscribe.interval.millis", res.repeatingResourceRefreshIntervalMillisProperty());
    }

    @Test
    public void testGetUrl() {
        assertEquals(SubnetAllocationIndex.URL, subnetAllocationIndex.getUrl());
    }

    @Test
    public void testUriScheme() {
        assertEquals(SubnetAllocationIndex.SCHEME, subnetAllocationIndex.getUriScheme());
    }
}
