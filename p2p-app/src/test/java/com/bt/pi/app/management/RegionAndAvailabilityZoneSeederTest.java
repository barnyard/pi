package com.bt.pi.app.management;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.isA;
import static org.mockito.Matchers.isNull;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.Before;
import org.junit.Test;

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
import com.bt.pi.core.id.PId;
import com.bt.pi.core.testing.UpdateResolverAnswer;

public class RegionAndAvailabilityZoneSeederTest {
    private PId regionsId;
    private PId availabilityZonesId;
    private Regions regions;
    private AvailabilityZones availabilityZones;

    private PiIdBuilder piIdBuilder;
    private DhtClientFactory dhtClientFactory;
    private BlockingDhtWriter dhtWriter;
    private RegionAndAvailabilityZoneSeeder regionAndAvailabilityZoneSeeder;
    private UpdateResolverAnswer regionsAnswer;
    private UpdateResolverAnswer availabilityZonesAnswer;

    @SuppressWarnings("unchecked")
    @Before
    public void setup() {
        regions = new Regions();
        regions.addRegion(new Region("USA", 1, "usa.com", "pisss.usa.com"));
        regions.addRegion(new Region("UK", 2, "uk.com", "pisss.uk.com"));

        availabilityZones = new AvailabilityZones();
        availabilityZones.addAvailabilityZone(new AvailabilityZone("c1", 1, 1, "UP"));
        availabilityZones.addAvailabilityZone(new AvailabilityZone("c2", 23, 2, "UP"));
        availabilityZones.addAvailabilityZone(new AvailabilityZone("c3", 135, 3, "UP"));

        regionsId = mock(PId.class);
        availabilityZonesId = mock(PId.class);

        piIdBuilder = mock(PiIdBuilder.class);
        when(piIdBuilder.getRegionsId()).thenReturn(regionsId);
        when(piIdBuilder.getAvailabilityZonesId()).thenReturn(availabilityZonesId);

        regionsAnswer = new UpdateResolverAnswer(null);
        availabilityZonesAnswer = new UpdateResolverAnswer(null);

        dhtWriter = mock(BlockingDhtWriter.class);
        when(dhtWriter.writeIfAbsent(regionsId, regions)).thenReturn(true);
        when(dhtWriter.writeIfAbsent(availabilityZonesId, availabilityZones)).thenReturn(true);
        doAnswer(regionsAnswer).when(dhtWriter).update(eq(regionsId), (Regions) isNull(), isA(UpdateResolver.class));
        doAnswer(availabilityZonesAnswer).when(dhtWriter).update(eq(availabilityZonesId), (AvailabilityZones) isNull(), isA(UpdateResolver.class));

        dhtClientFactory = mock(DhtClientFactory.class);
        when(dhtClientFactory.createBlockingWriter()).thenReturn(dhtWriter);

        regionAndAvailabilityZoneSeeder = new RegionAndAvailabilityZoneSeeder();
        regionAndAvailabilityZoneSeeder.setPiIdBuilder(piIdBuilder);
        regionAndAvailabilityZoneSeeder.setDhtClientFactory(dhtClientFactory);
    }

    @Test
    public void shouldConfigureRegions() throws Exception {
        // act
        boolean result = regionAndAvailabilityZoneSeeder.configureRegions("USA;UK", "1;2", "usa.com;uk.com", "pisss.usa.com;pisss.uk.com");

        // assert
        assertThat(result, is(true));
    }

    @Test
    public void shouldAddRegion() throws Exception {
        // setup
        when(dhtWriter.getValueWritten()).thenReturn(regions);

        // act
        boolean result = regionAndAvailabilityZoneSeeder.addRegion("USA", "1", "usa.com", "pisss.usa.com");

        // assert
        assertThat(result, is(true));
        assertEquals(1, ((Regions) regionsAnswer.getResult()).getRegion("USA").getRegionCode());
        assertEquals(1, ((Regions) regionsAnswer.getResult()).getRegions().size());
    }

    @SuppressWarnings("unchecked")
    @Test(expected = DuplicateRegionException.class)
    public void shouldFailToAddRegionWhenItAlreadyExists() throws Exception {
        // setup
        regionsAnswer = new UpdateResolverAnswer(regions);
        doAnswer(regionsAnswer).when(dhtWriter).update(eq(regionsId), (Regions) isNull(), isA(UpdateResolver.class));

        // act
        regionAndAvailabilityZoneSeeder.addRegion("USA", "1", "usa.com", "pisss.usa.com");
    }

    @SuppressWarnings("unchecked")
    @Test
    public void shouldAddRegionWhenRegionsRecordAlreadyExists() throws Exception {
        // setup
        regionsAnswer = new UpdateResolverAnswer(regions);
        when(dhtWriter.getValueWritten()).thenReturn(regions);
        doAnswer(regionsAnswer).when(dhtWriter).update(eq(regionsId), (Regions) isNull(), isA(UpdateResolver.class));

        // act
        boolean result = regionAndAvailabilityZoneSeeder.addRegion("OZ", "3", "oz.com", "pisss.oz.com");

        // assert
        assertThat(result, is(true));
        assertEquals(3, ((Regions) regionsAnswer.getResult()).getRegion("OZ").getRegionCode());
        assertEquals(3, ((Regions) regionsAnswer.getResult()).getRegions().size());
    }

    @Test
    public void shouldAddAvailabilityZone() throws Exception {
        // setup
        when(dhtWriter.getValueWritten()).thenReturn(availabilityZones);

        // act
        boolean result = regionAndAvailabilityZoneSeeder.addAvailabilityZone("c4", "67", "1", "available");

        // assert
        assertThat(result, is(true));
        assertEquals(0x0100 + 67, ((AvailabilityZones) availabilityZonesAnswer.getResult()).getAvailabilityZoneByName("c4").getGlobalAvailabilityZoneCode());
        assertEquals(1, ((AvailabilityZones) availabilityZonesAnswer.getResult()).getAvailabilityZones().size());
    }

    @SuppressWarnings("unchecked")
    @Test(expected = DuplicateAvailabilityZoneException.class)
    public void shouldFailToAddAvailabilityZoneWhenItAlreadyExists() throws Exception {
        // setup
        availabilityZonesAnswer = new UpdateResolverAnswer(availabilityZones);
        doAnswer(availabilityZonesAnswer).when(dhtWriter).update(eq(availabilityZonesId), (AvailabilityZones) isNull(), isA(UpdateResolver.class));

        // act
        regionAndAvailabilityZoneSeeder.addAvailabilityZone("c1", "67", "1", "available");
    }

    @SuppressWarnings("unchecked")
    @Test
    public void shouldAddAvailabilityZoneWhenAvailabilityZonesRecordAlreadyExists() throws Exception {
        // setup
        availabilityZonesAnswer = new UpdateResolverAnswer(availabilityZones);
        when(dhtWriter.getValueWritten()).thenReturn(availabilityZones);
        doAnswer(availabilityZonesAnswer).when(dhtWriter).update(eq(availabilityZonesId), (AvailabilityZones) isNull(), isA(UpdateResolver.class));

        // act
        boolean result = regionAndAvailabilityZoneSeeder.addAvailabilityZone("c4", "67", "1", "available");

        // assert
        assertThat(result, is(true));
        assertEquals(0x0100 + 67, ((AvailabilityZones) availabilityZonesAnswer.getResult()).getAvailabilityZoneByName("c4").getGlobalAvailabilityZoneCode());
        assertEquals(4, ((AvailabilityZones) availabilityZonesAnswer.getResult()).getAvailabilityZones().size());

    }

    @Test
    public void shouldNotConfigureRegionsIfArgumentNumbersMismatch() throws Exception {
        // act
        boolean result = regionAndAvailabilityZoneSeeder.configureRegions("USA;UK", "1;2", "usa.com", "pisss.usa.com");

        // assert
        assertThat(result, is(false));
    }

    @Test
    public void shouldConfigureAvailabilityZones() throws Exception {
        // act
        boolean result = regionAndAvailabilityZoneSeeder.configureAvailabilityZones("c1;c2;c3", "1;23;135", "1;2;3", "UP;UP;UP");

        // assert
        assertThat(result, is(true));
    }

    @Test
    public void shouldNotConfigureAvailabilityZonesIfArgumentNumbersMismatch() throws Exception {
        // act
        boolean result = regionAndAvailabilityZoneSeeder.configureAvailabilityZones("c1;c2;c3", "1;23;135", "USA;UK;ASIA", "UP");

        // assert
        assertThat(result, is(false));
    }
}
