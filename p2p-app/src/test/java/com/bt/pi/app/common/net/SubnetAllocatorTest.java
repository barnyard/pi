package com.bt.pi.app.common.net;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.isA;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.HashSet;
import java.util.concurrent.TimeUnit;

import org.junit.Before;
import org.junit.Test;

import com.bt.pi.app.common.entities.SubnetAllocationIndex;
import com.bt.pi.app.common.entities.SubnetAllocationRecord;
import com.bt.pi.app.common.entities.util.ResourceRange;
import com.bt.pi.app.common.id.PiIdBuilder;
import com.bt.pi.app.common.net.utils.IpAddressUtils;
import com.bt.pi.core.continuation.UpdateResolvingContinuation;
import com.bt.pi.core.dht.DhtClientFactory;
import com.bt.pi.core.dht.DhtWriter;
import com.bt.pi.core.id.KoalaIdFactory;
import com.bt.pi.core.id.PId;
import com.bt.pi.core.parser.KoalaPiEntityFactory;
import com.bt.pi.core.testing.TestFriendlyContinuation;
import com.bt.pi.core.testing.UpdateResolvingContinuationAnswer;

public class SubnetAllocatorTest {
    private SubnetAllocator subnetAllocator;
    private PiIdBuilder piIdBuilder;
    private UpdateResolvingContinuationAnswer subnetAllocAnswer;
    private DhtClientFactory dhtClientFactory;
    private HashSet<ResourceRange> subnetRanges;
    private SubnetAllocationIndex subnetAllocationIndex;
    private TestFriendlyContinuation<SubnetAllocationResult> testFriendlyContinuation;
    private DhtWriter dhtWriter;
    private PId subnetAllocationsIndexId;
    private KoalaIdFactory koalaIdFactory;

    @SuppressWarnings("unchecked")
    @Before
    public void before() {
        koalaIdFactory = new KoalaIdFactory(99, 99);
        koalaIdFactory.setKoalaPiEntityFactory(new KoalaPiEntityFactory());
        piIdBuilder = new PiIdBuilder();
        piIdBuilder.setKoalaIdFactory(koalaIdFactory);

        subnetRanges = new HashSet<ResourceRange>();
        subnetRanges.add(new ResourceRange(IpAddressUtils.ipToLong("10.0.0.0"), IpAddressUtils.ipToLong("10.0.255.255"), 16));
        subnetAllocationIndex = new SubnetAllocationIndex();
        subnetAllocationIndex.getAllocationMap().put(IpAddressUtils.ipToLong("10.0.0.0"), new SubnetAllocationRecord("bozo:default", IpAddressUtils.netSizeToNetmask(16)));
        subnetAllocationIndex.getAllocationMap().put(IpAddressUtils.ipToLong("10.0.0.16"), new SubnetAllocationRecord("nutter:default", IpAddressUtils.netSizeToNetmask(16)));
        subnetAllocationIndex.getAllocationMap().put(IpAddressUtils.ipToLong("10.0.0.32"), new SubnetAllocationRecord("abc:default", IpAddressUtils.netSizeToNetmask(16)));
        subnetAllocationIndex.setResourceRanges(subnetRanges);

        subnetAllocationsIndexId = piIdBuilder.getPId(SubnetAllocationIndex.URL).forLocalRegion();

        subnetAllocAnswer = new UpdateResolvingContinuationAnswer(subnetAllocationIndex);

        dhtWriter = mock(DhtWriter.class);
        doAnswer(subnetAllocAnswer).when(dhtWriter).update(eq(subnetAllocationsIndexId), isA(UpdateResolvingContinuation.class));
        dhtClientFactory = mock(DhtClientFactory.class);
        when(dhtClientFactory.createWriter()).thenReturn(dhtWriter);

        subnetAllocator = new SubnetAllocator();
        subnetAllocator.setPiIdBuilder(piIdBuilder);
        subnetAllocator.setDhtClientFactory(dhtClientFactory);

        testFriendlyContinuation = new TestFriendlyContinuation<SubnetAllocationResult>();
    }

    @Test
    public void shouldAllocateSubnet() throws InterruptedException {
        // act
        subnetAllocator.allocateSubnetInLocalRegion("frodo:default", testFriendlyContinuation);

        // assert
        assertTrue(testFriendlyContinuation.completedLatch.await(5, TimeUnit.SECONDS));
        assertEquals(IpAddressUtils.ipToLong("10.0.0.48"), testFriendlyContinuation.lastResult.getSubnetBaseAddress());
        assertEquals(IpAddressUtils.ipToLong("255.255.255.240"), testFriendlyContinuation.lastResult.getSubnetMask());
        assertEquals(4, ((SubnetAllocationIndex) subnetAllocAnswer.getResult()).getAllocationMap().size());
        assertEquals("frodo:default", ((SubnetAllocationIndex) subnetAllocAnswer.getResult()).getAllocationMap().get(IpAddressUtils.ipToLong("10.0.0.48")).getSecurityGroupId());
    }

    @Test
    public void shouldUseExistingVlanAllocation() throws InterruptedException {
        // act
        subnetAllocator.allocateSubnetInLocalRegion("bozo:default", testFriendlyContinuation);

        // assert
        assertTrue(testFriendlyContinuation.completedLatch.await(5, TimeUnit.SECONDS));
        assertEquals(IpAddressUtils.ipToLong("10.0.0.0"), testFriendlyContinuation.lastResult.getSubnetBaseAddress());
        assertEquals(IpAddressUtils.ipToLong("255.255.255.240"), testFriendlyContinuation.lastResult.getSubnetMask());
        assertEquals(3, ((SubnetAllocationIndex) subnetAllocAnswer.getResult()).getAllocationMap().size());
        assertEquals("bozo:default", ((SubnetAllocationIndex) subnetAllocAnswer.getResult()).getAllocationMap().get(IpAddressUtils.ipToLong("10.0.0.0")).getSecurityGroupId());
    }

    @Test
    public void shouldGiveNullWhenAllocationFails() throws InterruptedException {
        // setup
        subnetRanges.clear();

        // act
        subnetAllocator.allocateSubnetInLocalRegion("bozo:default", testFriendlyContinuation);

        // assert
        assertTrue(testFriendlyContinuation.completedLatch.await(5, TimeUnit.SECONDS));
        assertNull(testFriendlyContinuation.lastResult);
    }

    @Test
    public void shouldReleaseAllocatedSubnet() {
        // act
        subnetAllocator.releaseSubnetInLocalRegion("bozo:default");

        // assert
        assertEquals(2, ((SubnetAllocationIndex) subnetAllocAnswer.getResult()).getAllocationMap().size());
        assertEquals(null, ((SubnetAllocationIndex) subnetAllocAnswer.getResult()).getAllocationMap().get(IpAddressUtils.ipToLong("10.0.0.0")));
    }
}
