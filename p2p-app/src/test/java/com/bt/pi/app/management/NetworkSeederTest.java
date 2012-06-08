package com.bt.pi.app.management;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.isA;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Iterator;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.Before;
import org.junit.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import com.bt.pi.app.common.entities.PublicIpAllocationIndex;
import com.bt.pi.app.common.entities.SubnetAllocationIndex;
import com.bt.pi.app.common.entities.VlanAllocationIndex;
import com.bt.pi.app.common.entities.util.ResourceRange;
import com.bt.pi.app.common.id.PiIdBuilder;
import com.bt.pi.app.common.net.utils.IpAddressUtils;
import com.bt.pi.core.dht.BlockingDhtWriter;
import com.bt.pi.core.dht.DhtClientFactory;
import com.bt.pi.core.entity.PiEntity;
import com.bt.pi.core.id.KoalaIdFactory;
import com.bt.pi.core.id.PId;
import com.bt.pi.core.parser.KoalaPiEntityFactory;

public class NetworkSeederTest {
    private static final String DNS_ADDRESS = "147.149.2.5";
    private DhtClientFactory dhtClientFactory;
    private KoalaIdFactory koalaIdFactory;
    private PId vlanAllocationIndexId;
    private PId publicAddressesIndexId;
    private PId subnetIndexId;
    private PiIdBuilder piIdBuilder;
    private BlockingDhtWriter dhtWriter;
    private NetworkSeeder networkSeeder;
    private PiEntity writtenEntity;
    private AtomicBoolean writeSucceeded;
    private String publicIpAddressList = "1.2.3.4 5.6.7.8";

    @Before
    public void before() {
        koalaIdFactory = new KoalaIdFactory(99, 99);
        koalaIdFactory.setKoalaPiEntityFactory(new KoalaPiEntityFactory());
        piIdBuilder = new PiIdBuilder();
        piIdBuilder.setKoalaIdFactory(koalaIdFactory);

        vlanAllocationIndexId = piIdBuilder.getPId(VlanAllocationIndex.URL).forRegion(2);
        publicAddressesIndexId = piIdBuilder.getPId(PublicIpAllocationIndex.URL).forRegion(2);
        subnetIndexId = piIdBuilder.getPId(SubnetAllocationIndex.URL).forRegion(2);

        writtenEntity = null;
        writeSucceeded = new AtomicBoolean(true);

        Answer<Boolean> entityWrittenAnswer = new Answer<Boolean>() {
            @Override
            public Boolean answer(InvocationOnMock invocation) throws Throwable {
                if (writeSucceeded.get()) {
                    writtenEntity = (PiEntity) invocation.getArguments()[1];
                }
                return writeSucceeded.get();
            }
        };

        dhtWriter = mock(BlockingDhtWriter.class);
        doAnswer(entityWrittenAnswer).when(dhtWriter).writeIfAbsent(eq(subnetIndexId), isA(SubnetAllocationIndex.class));
        doAnswer(entityWrittenAnswer).when(dhtWriter).writeIfAbsent(eq(publicAddressesIndexId), isA(PublicIpAllocationIndex.class));
        doAnswer(entityWrittenAnswer).when(dhtWriter).writeIfAbsent(eq(vlanAllocationIndexId), isA(VlanAllocationIndex.class));

        dhtClientFactory = mock(DhtClientFactory.class);
        when(dhtClientFactory.createBlockingWriter()).thenReturn(dhtWriter);

        networkSeeder = new NetworkSeeder();
        networkSeeder.setPiIdBuilder(piIdBuilder);
        networkSeeder.setDhtClientFactory(dhtClientFactory);
    }

    @Test
    public void shouldCreateVlanAddressesIndexEntry() {
        // act
        boolean res = networkSeeder.createVlanAllocationIndex("10 11", 2);

        // assert

        VlanAllocationIndex index = (VlanAllocationIndex) writtenEntity;
        assertEquals(2, index.getResourceRanges().size());
        assertEquals(new Long(NetworkSeeder.DEFAULT_INACTIVE_RESOURCE_CONSUMER_TIMEOUT_SEC), index.getInactiveResourceConsumerTimeoutSec());
        assertTrue(res);
    }

    @Test
    public void shouldCreateVlanAddressesIndexEntryWithARange() {
        // act
        boolean res = networkSeeder.createVlanAllocationIndex("10-11", 2);

        // assert
        assertEquals(1, ((VlanAllocationIndex) writtenEntity).getResourceRanges().size());
        assertEquals(new Long(10), ((VlanAllocationIndex) writtenEntity).getResourceRanges().iterator().next().getMin());
        assertEquals(new Long(11), ((VlanAllocationIndex) writtenEntity).getResourceRanges().iterator().next().getMax());
        assertTrue(res);
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldThrowWhenBadVlanId() {
        // act
        networkSeeder.createVlanAllocationIndex("1a", 2);
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldThrowWhenBadVlanIdFormat() {
        // act
        networkSeeder.createVlanAllocationIndex("12 -13", 2);
    }

    @Test
    public void shouldReturnFalseWhenVlanAlreadyExists() {
        // setup
        writeSucceeded.set(false);

        // act
        boolean res = networkSeeder.createVlanAllocationIndex("10 11", 2);

        // assert
        assertFalse(res);
        assertEquals(null, writtenEntity);
    }

    @Test
    public void shouldCreatePublicIpAddressesIndexEntry() {
        // setup
        when(dhtClientFactory.createBlockingWriter()).thenReturn(dhtWriter);

        // act
        boolean res = networkSeeder.createPublicAddressAllocationIndex(publicIpAddressList, 2);

        // assert
        PublicIpAllocationIndex index = (PublicIpAllocationIndex) writtenEntity;
        assertEquals(2, index.getResourceRanges().size());
        assertEquals(new Long(NetworkSeeder.DEFAULT_INACTIVE_RESOURCE_CONSUMER_FOR_PUBLIC_IP_ALLOCATION_INDEX_TIMEOUT_SEC), index.getInactiveResourceConsumerTimeoutSec());
        assertTrue(res);
    }

    @Test
    public void shouldCreatePublicIpAddressesIndexEntryWithARange() {
        // setup
        when(dhtClientFactory.createBlockingWriter()).thenReturn(dhtWriter);

        // act
        boolean res = networkSeeder.createPublicAddressAllocationIndex("1.2.3.4-2.3.4.5", 2);

        // assert
        assertEquals(1, ((PublicIpAllocationIndex) writtenEntity).getResourceRanges().size());
        assertEquals(new Long(16909060), ((PublicIpAllocationIndex) writtenEntity).getResourceRanges().iterator().next().getMin());
        assertEquals(new Long(33752069), ((PublicIpAllocationIndex) writtenEntity).getResourceRanges().iterator().next().getMax());
        assertTrue(res);
    }

    @Test
    public void shouldCreatePublicIpAddressesIndexEntryWithMultipleRanges() {
        // setup
        when(dhtClientFactory.createBlockingWriter()).thenReturn(dhtWriter);

        // act
        boolean res = networkSeeder.createPublicAddressAllocationIndex(" 9.8.7.6 1.2.3.4-2.3.4.5  10.12.32.34-11.12.15.16 1.0.0.1", 2);

        // assert
        assertEquals(4, ((PublicIpAllocationIndex) writtenEntity).getResourceRanges().size());
        assertTrue(res);
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldThrowWhenBadAddress() {
        // setup
        when(dhtClientFactory.createBlockingWriter()).thenReturn(dhtWriter);

        // act
        networkSeeder.createPublicAddressAllocationIndex("1.2.3.A", 2);
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldThrowWhenBadFormat() {
        // setup
        when(dhtClientFactory.createBlockingWriter()).thenReturn(dhtWriter);

        // act
        networkSeeder.createPublicAddressAllocationIndex("1.2.3.4- 5.6.7.8", 2);
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldThrowBeforeInsertingWhenBadIpAddressDetected() {
        // setup
        when(dhtClientFactory.createBlockingWriter()).thenReturn(dhtWriter);

        // act
        networkSeeder.createPublicAddressAllocationIndex("11111 1.2.3.4", 2);
    }

    @Test
    public void shouldReturnFalseWhenPublicAddressIndexAlreadyExists() {
        // setup
        writeSucceeded.set(false);

        // act
        boolean res = networkSeeder.createPublicAddressAllocationIndex(publicIpAddressList, 2);

        // assert
        assertFalse(res);
        assertEquals(null, writtenEntity);
    }

    @Test
    public void shouldCreateSubnetAddressesIndexEntry() {
        // setup
        when(dhtClientFactory.createBlockingWriter()).thenReturn(dhtWriter);

        // act
        boolean res = networkSeeder.createSubnetAllocationIndex("172.0.0.0/24;16", DNS_ADDRESS, 2);

        // assert
        assertEquals(1, ((SubnetAllocationIndex) writtenEntity).getResourceRanges().size());

        ResourceRange resRange = ((SubnetAllocationIndex) writtenEntity).getResourceRanges().iterator().next();
        assertEquals(new Long(IpAddressUtils.ipToLong("172.0.0.0")), resRange.getMin());
        assertEquals(new Long(IpAddressUtils.ipToLong("172.0.0.255")), resRange.getMax());
        assertEquals(16, resRange.getAllocationStepSize());
        assertEquals(DNS_ADDRESS, ((SubnetAllocationIndex) writtenEntity).getDnsAddress());
        assertTrue(res);
    }

    @Test
    public void shouldCreateSubnetIndexEntryWithARange() {
        // setup
        when(dhtClientFactory.createBlockingWriter()).thenReturn(dhtWriter);

        // act
        boolean res = networkSeeder.createSubnetAllocationIndex("172.0.0.0/24;16 172.20.0.0/16;32", DNS_ADDRESS, 2);

        // assert
        SubnetAllocationIndex index = (SubnetAllocationIndex) writtenEntity;
        assertEquals(2, index.getResourceRanges().size());

        SortedSet<ResourceRange> sortedSet = new TreeSet<ResourceRange>(index.getResourceRanges());
        Iterator<ResourceRange> iter = sortedSet.iterator();
        ResourceRange first = iter.next();
        ResourceRange second = iter.next();

        assertEquals(new Long(IpAddressUtils.ipToLong("172.0.0.0")), first.getMin());
        assertEquals(new Long(IpAddressUtils.ipToLong("172.0.0.255")), first.getMax());
        assertEquals(16, first.getAllocationStepSize());
        assertEquals(new Long(IpAddressUtils.ipToLong("172.20.0.0")), second.getMin());
        assertEquals(new Long(IpAddressUtils.ipToLong("172.20.255.255")), second.getMax());
        assertEquals(32, second.getAllocationStepSize());
        assertEquals(new Long(NetworkSeeder.DEFAULT_INACTIVE_RESOURCE_CONSUMER_TIMEOUT_SEC), index.getInactiveResourceConsumerTimeoutSec());
        assertTrue(res);
    }

    @Test
    public void shouldCreateSubnetAddressesIndexEntryWithMultipleRanges() {
        // setup
        when(dhtClientFactory.createBlockingWriter()).thenReturn(dhtWriter);

        // act
        boolean res = networkSeeder.createSubnetAllocationIndex(" 9.8.7.6/30;12 1.2.3.4/28;4 10.12.32.34/8;8 1.3.0.3/24;32", DNS_ADDRESS, 2);

        // assert
        assertEquals(4, ((SubnetAllocationIndex) writtenEntity).getResourceRanges().size());
        assertTrue(res);
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldThrowWhenBadNetworkAddress() {
        // setup
        when(dhtClientFactory.createBlockingWriter()).thenReturn(dhtWriter);

        // act
        networkSeeder.createSubnetAllocationIndex("1.2.3.A/4;4", DNS_ADDRESS, 2);
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldThrowWhenBadSubnetFormat() {
        // setup
        when(dhtClientFactory.createBlockingWriter()).thenReturn(dhtWriter);

        // act
        networkSeeder.createSubnetAllocationIndex("1.2.3.4/AA;32", DNS_ADDRESS, 2);
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldThrowWhenBadAddrsPerAllocation() {
        // setup
        when(dhtClientFactory.createBlockingWriter()).thenReturn(dhtWriter);

        // act
        networkSeeder.createSubnetAllocationIndex("1.2.3.4/32;BB", DNS_ADDRESS, 2);
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldThrowWhenBadDNSAddress() {
        // setup
        when(dhtClientFactory.createBlockingWriter()).thenReturn(dhtWriter);

        // act
        networkSeeder.createSubnetAllocationIndex("1.2.3.4/24;8", "BAD BAD", 2);
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldThrowBeforeInsertingWhenBadSubnetDetected() {
        // setup
        when(dhtClientFactory.createBlockingWriter()).thenReturn(dhtWriter);

        // act
        networkSeeder.createSubnetAllocationIndex("11111 1.2.3.4/32;1", DNS_ADDRESS, 2);
    }

    @Test
    public void shouldReturnFalseWhenSubnetAlreadyExists() {
        // setup
        writeSucceeded.set(false);

        // act
        boolean res = networkSeeder.createSubnetAllocationIndex("1.2.3.4/24;32", DNS_ADDRESS, 2);

        // assert
        assertFalse(res);
        assertEquals(null, writtenEntity);
    }

}
