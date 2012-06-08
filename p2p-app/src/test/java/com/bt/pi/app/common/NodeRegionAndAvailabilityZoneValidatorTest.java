package com.bt.pi.app.common;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.isA;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.apache.log4j.Logger;
import org.junit.Before;
import org.junit.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import com.bt.pi.app.common.NodeRegionAndAvailabilityZoneValidator.RegionAndAvailabilityZoneContinuation;
import com.bt.pi.app.common.entities.AvailabilityZone;
import com.bt.pi.app.common.entities.AvailabilityZones;
import com.bt.pi.app.common.entities.Instance;
import com.bt.pi.app.common.entities.Region;
import com.bt.pi.app.common.entities.Regions;
import com.bt.pi.app.common.id.PiIdBuilder;
import com.bt.pi.core.dht.DhtClientFactory;
import com.bt.pi.core.dht.DhtReader;
import com.bt.pi.core.id.KoalaIdFactory;
import com.bt.pi.core.id.PId;
import com.bt.pi.core.node.NodeStartedEvent;
import com.bt.pi.core.testing.UnitTestAppender;

public class NodeRegionAndAvailabilityZoneValidatorTest {
    private int regionCode = 23;
    private String regionName = "Jordan Basketball";

    private int availabilityZoneCode = 45;
    private String availabilityZoneName = "Jordan Baseball";

    private Regions regions;
    private AvailabilityZones availabilityZones;
    private PId regionsId;
    private PId availabilityZonesId;
    private DhtReader dhtReader;
    private DhtClientFactory dhtClientFactory;
    private KoalaIdFactory koalaIdFactory;
    private PiIdBuilder piIdBuilder;
    private UnitTestAppender unitTestAppender;
    private NodeStartedEvent nodeStartedEvent;

    private NodeRegionAndAvailabilityZoneValidator nodeRegionAndAvailabilityZoneValidator;

    @Before
    public void before() {
        regions = new Regions();
        regions.addRegion(new Region(regionName, regionCode, "", ""));

        availabilityZones = new AvailabilityZones();
        availabilityZones.addAvailabilityZone(new AvailabilityZone(availabilityZoneName, availabilityZoneCode, regionCode, ""));

        regionsId = mock(PId.class);
        availabilityZonesId = mock(PId.class);

        dhtReader = mock(DhtReader.class);

        dhtClientFactory = mock(DhtClientFactory.class);
        when(dhtClientFactory.createReader()).thenReturn(dhtReader);

        koalaIdFactory = mock(KoalaIdFactory.class);
        when(koalaIdFactory.getRegion()).thenReturn(regionCode);
        when(koalaIdFactory.getAvailabilityZoneWithinRegion()).thenReturn(availabilityZoneCode);

        piIdBuilder = mock(PiIdBuilder.class);
        when(piIdBuilder.getRegionsId()).thenReturn(regionsId);
        when(piIdBuilder.getAvailabilityZonesId()).thenReturn(availabilityZonesId);

        nodeRegionAndAvailabilityZoneValidator = new NodeRegionAndAvailabilityZoneValidator();
        nodeRegionAndAvailabilityZoneValidator.setDhtClientFactory(dhtClientFactory);
        nodeRegionAndAvailabilityZoneValidator.setKoalaIdFactory(koalaIdFactory);
        nodeRegionAndAvailabilityZoneValidator.setPiIdBuilder(piIdBuilder);
    }

    @Before
    public void setupLoggingAppender() {
        unitTestAppender = new UnitTestAppender();
        Logger.getLogger(NodeRegionAndAvailabilityZoneValidator.class).addAppender(unitTestAppender);
    }

    @Before
    public void setupNodeStartedEvent() {
        nodeStartedEvent = new NodeStartedEvent(this);
    }

    @SuppressWarnings("unchecked")
    private void setupDhtReader(final Exception e) {
        doAnswer(new Answer<Object>() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                PId id = (PId) invocation.getArguments()[0];

                if (id.equals(regionsId)) {
                    RegionAndAvailabilityZoneContinuation<Regions> continuation = (RegionAndAvailabilityZoneContinuation<Regions>) invocation.getArguments()[1];
                    if (e == null)
                        continuation.receiveResult(regions);
                    else
                        continuation.receiveException(e);
                } else if (id.equals(availabilityZonesId)) {
                    RegionAndAvailabilityZoneContinuation<AvailabilityZones> continuation = (RegionAndAvailabilityZoneContinuation<AvailabilityZones>) invocation.getArguments()[1];
                    if (e == null)
                        continuation.receiveResult(availabilityZones);
                    else
                        continuation.receiveException(e);
                }
                return null;
            }
        }).when(dhtReader).getAsync(isA(PId.class), isA(RegionAndAvailabilityZoneContinuation.class));
    }

    @Test
    public void testHappyPath() throws Exception {
        // setup
        setupDhtReader(null);

        // act
        nodeRegionAndAvailabilityZoneValidator.onApplicationEvent(nodeStartedEvent);

        // assert
        assertThat(unitTestAppender.getMessages().contains(String.format("Node has started in region %s, code %d", regionName, regionCode)), is(true));
        assertThat(unitTestAppender.getMessages().contains(String.format("Node has started in availability zone %s, code %d", availabilityZoneName, availabilityZoneCode)), is(true));
    }

    @Test
    public void testRegionNotFound() throws Exception {
        // setup
        regions.getRegions().remove(regionName);
        setupDhtReader(null);

        // act
        nodeRegionAndAvailabilityZoneValidator.onApplicationEvent(nodeStartedEvent);

        // assert
        assertThat(unitTestAppender.getMessages().contains(String.format("Node has started with region code %d, which does not exist in the dht!", regionCode)), is(true));
        assertThat(unitTestAppender.getMessages().contains(String.format("Node has started in availability zone %s, code %d", availabilityZoneName, availabilityZoneCode)), is(true));
    }

    @Test
    public void testAvailabilityZoneNotFound() throws Exception {
        // setup
        availabilityZones.getAvailabilityZones().remove(availabilityZoneName);
        setupDhtReader(null);

        // act
        nodeRegionAndAvailabilityZoneValidator.onApplicationEvent(nodeStartedEvent);

        // assert
        assertThat(unitTestAppender.getMessages().contains(String.format("Node has started in region %s, code %d", regionName, regionCode)), is(true));
        assertThat(unitTestAppender.getMessages().contains(String.format("Node has started with availability zone code %d, which does not exist in the dht!", availabilityZoneCode)), is(true));
    }

    @Test
    public void testWithExceptionOnRead() throws Exception {
        // setup
        setupDhtReader(new RuntimeException());

        // act
        nodeRegionAndAvailabilityZoneValidator.onApplicationEvent(nodeStartedEvent);

        // assert
        assertThat(unitTestAppender.getMessages().contains("Error retrieving regions entity from the dht"), is(true));
        assertThat(unitTestAppender.getMessages().contains("Error retrieving availability zones entity from the dht"), is(true));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testUnknownType() throws Exception {
        // setup
        final Instance instance = new Instance();
        doAnswer(new Answer<Object>() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                ((RegionAndAvailabilityZoneContinuation<Instance>) invocation.getArguments()[1]).receiveResult(instance);
                return null;
            }
        }).when(dhtReader).getAsync(isA(PId.class), isA(RegionAndAvailabilityZoneContinuation.class));

        // act
        nodeRegionAndAvailabilityZoneValidator.onApplicationEvent(nodeStartedEvent);

        // assert
        assertThat(unitTestAppender.getMessages().contains(String.format("Unknown object type: %s, throw it away", instance.getClass())), is(true));
    }
}
