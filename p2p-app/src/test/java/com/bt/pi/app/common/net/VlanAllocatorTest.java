package com.bt.pi.app.common.net;

import static org.junit.Assert.assertEquals;
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

import com.bt.pi.app.common.entities.VlanAllocationIndex;
import com.bt.pi.app.common.entities.VlanAllocationRecord;
import com.bt.pi.app.common.entities.util.ResourceRange;
import com.bt.pi.app.common.id.PiIdBuilder;
import com.bt.pi.core.continuation.UpdateResolvingContinuation;
import com.bt.pi.core.dht.DhtClientFactory;
import com.bt.pi.core.dht.DhtWriter;
import com.bt.pi.core.id.KoalaIdFactory;
import com.bt.pi.core.id.PId;
import com.bt.pi.core.parser.KoalaPiEntityFactory;
import com.bt.pi.core.testing.TestFriendlyContinuation;
import com.bt.pi.core.testing.UpdateResolvingContinuationAnswer;

public class VlanAllocatorTest {
    private VlanAllocator vlanAllocator;
    private PiIdBuilder piIdBuilder;
    private UpdateResolvingContinuationAnswer vlanAllocAnswer;
    private DhtClientFactory dhtClientFactory;
    private HashSet<ResourceRange> vlanRanges;
    private VlanAllocationIndex vlanAllocationIndex;
    private TestFriendlyContinuation<Long> testFriendlyContinuation;
    private DhtWriter dhtWriter;
    private PId vlanAllocationsIndexId;
    private KoalaIdFactory koalaIdFactory;

    @SuppressWarnings("unchecked")
    @Before
    public void before() {
        koalaIdFactory = new KoalaIdFactory(99, 99);
        koalaIdFactory.setKoalaPiEntityFactory(new KoalaPiEntityFactory());
        piIdBuilder = new PiIdBuilder();
        piIdBuilder.setKoalaIdFactory(koalaIdFactory);

        vlanRanges = new HashSet<ResourceRange>();
        vlanRanges.add(new ResourceRange(10L, 15L));
        vlanAllocationIndex = new VlanAllocationIndex();
        vlanAllocationIndex.getAllocationMap().put(10L, new VlanAllocationRecord("bozo:default"));
        vlanAllocationIndex.getAllocationMap().put(11L, new VlanAllocationRecord("nutter:default"));
        vlanAllocationIndex.getAllocationMap().put(12L, new VlanAllocationRecord("abc:default"));
        vlanAllocationIndex.setResourceRanges(vlanRanges);

        vlanAllocationsIndexId = piIdBuilder.getPId(VlanAllocationIndex.URL).forLocalRegion();

        vlanAllocAnswer = new UpdateResolvingContinuationAnswer(vlanAllocationIndex);

        dhtWriter = mock(DhtWriter.class);
        doAnswer(vlanAllocAnswer).when(dhtWriter).update(eq(vlanAllocationsIndexId), isA(UpdateResolvingContinuation.class));
        dhtClientFactory = mock(DhtClientFactory.class);
        when(dhtClientFactory.createWriter()).thenReturn(dhtWriter);

        vlanAllocator = new VlanAllocator();
        vlanAllocator.setPiIdBuilder(piIdBuilder);
        vlanAllocator.setDhtClientFactory(dhtClientFactory);

        testFriendlyContinuation = new TestFriendlyContinuation<Long>();
    }

    @Test
    public void shouldAllocateVlan() throws InterruptedException {
        // act
        vlanAllocator.allocateVlanInLocalRegion("frodo:default", testFriendlyContinuation);

        // assert
        assertTrue(testFriendlyContinuation.completedLatch.await(5, TimeUnit.SECONDS));
        assertEquals(new Long(13), testFriendlyContinuation.lastResult);
        assertEquals(4, ((VlanAllocationIndex) vlanAllocAnswer.getResult()).getAllocationMap().size());
        assertEquals("frodo:default", ((VlanAllocationIndex) vlanAllocAnswer.getResult()).getAllocationMap().get(13L).getSecurityGroupId());
    }

    @Test
    public void shouldUseExistingVlanAllocation() throws InterruptedException {
        // act
        vlanAllocator.allocateVlanInLocalRegion("bozo:default", testFriendlyContinuation);

        // assert
        assertTrue(testFriendlyContinuation.completedLatch.await(5, TimeUnit.SECONDS));
        assertEquals(new Long(10), testFriendlyContinuation.lastResult);
        assertEquals(3, ((VlanAllocationIndex) vlanAllocAnswer.getResult()).getAllocationMap().size());
        assertEquals("bozo:default", ((VlanAllocationIndex) vlanAllocAnswer.getResult()).getAllocationMap().get(10L).getSecurityGroupId());
    }

    @Test
    public void shouldGiveNullWhenAllocationFails() throws InterruptedException {
        // setup
        vlanRanges.clear();

        // act
        vlanAllocator.allocateVlanInLocalRegion("bozo:default", testFriendlyContinuation);

        // assert
        assertTrue(testFriendlyContinuation.completedLatch.await(5, TimeUnit.SECONDS));
        assertEquals(null, testFriendlyContinuation.lastResult);
    }

    @Test
    public void shouldReleaseAllocatedVlan() {
        // act
        vlanAllocator.releaseVlanInLocalRegion("bozo:default");

        // assert
        assertEquals(2, ((VlanAllocationIndex) vlanAllocAnswer.getResult()).getAllocationMap().size());
        assertEquals(null, ((VlanAllocationIndex) vlanAllocAnswer.getResult()).getAllocationMap().get(10L));
    }
}
