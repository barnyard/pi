package com.bt.pi.app.management;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.isA;
import static org.mockito.Matchers.isNull;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.Before;
import org.junit.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import com.bt.pi.app.common.entities.AvailabilityZone;
import com.bt.pi.app.common.entities.AvailabilityZones;
import com.bt.pi.app.common.entities.DuplicateAvailabilityZoneException;
import com.bt.pi.app.common.entities.DuplicateRegionException;
import com.bt.pi.app.common.entities.Region;
import com.bt.pi.app.common.entities.Regions;
import com.bt.pi.app.common.id.PiIdBuilder;
import com.bt.pi.core.continuation.UpdateResolver;
import com.bt.pi.core.dht.BlockingDhtWriter;
import com.bt.pi.core.dht.DhtClientFactory;
import com.bt.pi.core.entity.PiEntity;
import com.bt.pi.core.id.PId;

public class PiCloudManagementServiceTest {

    private Regions regions;
    private PiCloudManagementService cloudManagementService;
    private Regions savedRegions;
    private Region region;
    private AvailabilityZones availabilityZones;
    private AvailabilityZones savedAzs;
    private AvailabilityZone availabilityZone;

    @SuppressWarnings("unchecked")
    @Before
    public void before() {
        region = new Region("ben", 0, "hello", null);
        regions = new Regions();

        availabilityZone = new AvailabilityZone("jon", 6, 0, "availabile");
        availabilityZones = new AvailabilityZones();

        PId regionsId = mock(PId.class);
        PId avsId = mock(PId.class);

        PiIdBuilder piIdBuilder = mock(PiIdBuilder.class);
        when(piIdBuilder.getRegionsId()).thenReturn(regionsId);
        when(piIdBuilder.getAvailabilityZonesId()).thenReturn(avsId);

        BlockingDhtWriter blockingDhtWriter = mock(BlockingDhtWriter.class);
        doAnswer(new Answer<Object>() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                savedRegions = ((UpdateResolver<Regions>) invocation.getArguments()[2]).update(regions, null);
                return null;
            }
        }).when(blockingDhtWriter).update(eq(regionsId), (PiEntity) isNull(), isA(UpdateResolver.class));

        doAnswer(new Answer<Object>() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                savedAzs = ((UpdateResolver<AvailabilityZones>) invocation.getArguments()[2]).update(availabilityZones, null);
                return null;
            }
        }).when(blockingDhtWriter).update(eq(avsId), (PiEntity) isNull(), isA(UpdateResolver.class));

        DhtClientFactory dhtClientFactory = mock(DhtClientFactory.class);
        when(dhtClientFactory.createBlockingWriter()).thenReturn(blockingDhtWriter);

        cloudManagementService = new PiCloudManagementService();
        cloudManagementService.setDhtClientFactory(dhtClientFactory);
        cloudManagementService.setPiIdBuilder(piIdBuilder);
    }

    @Test
    public void shouldAddRegionsToRegions() {
        // setup

        // act
        cloudManagementService.addRegion(region.getRegionName(), region.getRegionCode(), region.getRegionEndpoint(), region.getPisssEndpoint());

        // verify
        assertTrue(regions.getRegions().containsKey(region.getRegionName()));
        assertEquals(region, regions.getRegion(region.getRegionName()));
        assertEquals(regions, savedRegions);
    }

    @Test(expected = DuplicateRegionException.class)
    public void shouldNotAddDuplicateRegionsToRegions() {
        // setup
        regions.addRegion(region);

        // act
        cloudManagementService.addRegion(region.getRegionName(), region.getRegionCode(), region.getRegionEndpoint(), region.getPisssEndpoint());
    }

    @Test
    public void shouldAddAvailabilityZoneToAvailabilityZonesList() {
        // setup

        // act
        cloudManagementService.addAvailabilityZone(availabilityZone.getAvailabilityZoneName(), availabilityZone.getAvailabilityZoneCodeWithinRegion(), availabilityZone.getRegionCode(), availabilityZone.getStatus());

        // verify
        assertTrue(availabilityZones.getAvailabilityZones().containsKey(availabilityZone.getAvailabilityZoneName()));
        assertEquals(availabilityZone, availabilityZones.getAvailabilityZoneByName(availabilityZone.getAvailabilityZoneName()));
        assertEquals(availabilityZones, savedAzs);

    }

    @Test(expected = DuplicateAvailabilityZoneException.class)
    public void shouldNotAddAvailabilityZoneToAvailabiityZones() {
        // setup
        availabilityZones.addAvailabilityZone(availabilityZone);

        // act
        cloudManagementService.addAvailabilityZone(availabilityZone.getAvailabilityZoneName(), availabilityZone.getAvailabilityZoneCodeWithinRegion(), availabilityZone.getRegionCode(), availabilityZone.getStatus());
    }
}
