package com.bt.pi.ops.website.controllers;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.when;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import com.bt.pi.app.common.entities.AvailabilityZone;
import com.bt.pi.app.common.entities.AvailabilityZones;
import com.bt.pi.app.common.entities.InstanceTypes;
import com.bt.pi.app.common.entities.Region;
import com.bt.pi.app.common.entities.Regions;
import com.bt.pi.app.common.entities.ResourceSchemes;
import com.bt.pi.app.common.id.PiIdBuilder;
import com.bt.pi.core.dht.BlockingDhtReader;
import com.bt.pi.core.dht.DhtClientFactory;
import com.bt.pi.core.dht.cache.BlockingDhtCache;
import com.bt.pi.core.entity.PiEntity;
import com.bt.pi.core.id.KoalaIdFactory;
import com.bt.pi.core.id.PId;
import com.bt.pi.core.parser.KoalaJsonParser;

@RunWith(MockitoJUnitRunner.class)
public class DhtRecordControllerTest {
	private static final int REGION_CODE = 1;
	private static final int GLOBAL_AVAILABILITY_ZONE_CODE = 2;
	private static final String REGION_NAME = "region-name";
	private static final String AVAILABILITY_ZONE_NAME = "availability-zone-name";
	private static final String DUMMY_JSON = "{\"a\":\"b\"}";
	@InjectMocks
	private DhtRecordController dhtRecordController = new DhtRecordController();
	@Mock
	BlockingDhtReader dhtReader;
	@Mock
	DhtClientFactory dhtClientFactory;
	@Mock
	KoalaIdFactory koalaIdFactory;
	@Mock
	KoalaJsonParser koalaJsonParser;
	@Mock
	PiEntity dummyPiEntity;
	@Mock
	PiIdBuilder piIdBuilder;
	@Mock
	PId pid;
	@Mock
	PId regionsId;
	@Mock
	Regions regions;
	@Mock
	Region region;
	@Mock
	PId availabilityZonesId;
	@Mock
	AvailabilityZones availabilityZones;
	@Mock
	AvailabilityZone availabilityZone;
	@Mock
	BlockingDhtCache blockingDhtCache;

	@Before
	public void before() {
		when(dhtClientFactory.createBlockingReader()).thenReturn(dhtReader);
		when(dhtReader.get(pid)).thenReturn(dummyPiEntity);
		when(koalaJsonParser.getJson(dummyPiEntity)).thenReturn(DUMMY_JSON);

		when(piIdBuilder.getRegionsId()).thenReturn(regionsId);
		when(piIdBuilder.getAvailabilityZonesId()).thenReturn(availabilityZonesId);

		when(regions.getRegion(REGION_NAME)).thenReturn(region);
		when(availabilityZones.getAvailabilityZoneByName(AVAILABILITY_ZONE_NAME)).thenReturn(availabilityZone);

		when(region.getRegionCode()).thenReturn(REGION_CODE);
		when(availabilityZone.getGlobalAvailabilityZoneCode()).thenReturn(GLOBAL_AVAILABILITY_ZONE_CODE);

		when(blockingDhtCache.get(availabilityZonesId)).thenReturn(availabilityZones);
		when(blockingDhtCache.get(regionsId)).thenReturn(regions);
	}

	@Test
	public void getGlobalDhtRecord() {
		// setup
		when(koalaIdFactory.buildPId("user:abc")).thenReturn(pid);

		// act
		String res = dhtRecordController.getGlobalDhtRecord("user", "abc");

		// assert
		assertEquals(DUMMY_JSON, res);
	}

	@Test
	public void getGlobalDhtRecordMixedCaseScheme() {
		// setup
		when(koalaIdFactory.buildPId(ResourceSchemes.BUCKET_META_DATA.toString() + ":abc")).thenReturn(pid);

		// act
		String res = dhtRecordController.getGlobalDhtRecord(ResourceSchemes.BUCKET_META_DATA.toString(), "abc");

		// assert
		assertEquals(DUMMY_JSON, res);
	}

	@Test
	public void getDhtRecordForRegion() {
		// setup
		when(koalaIdFactory.buildPId("sg:abc")).thenReturn(pid);
		when(pid.forRegion(REGION_CODE)).thenReturn(pid);

		// act
		String res = dhtRecordController.getDhtRecordForRegion(REGION_NAME, "sg", "abc");

		// assert
		assertEquals(DUMMY_JSON, res);
	}

	@Test
	public void getDhtRecordForRegionMixedCaseSchema() {
		// setup
		when(koalaIdFactory.buildPId(ResourceSchemes.BUCKET_META_DATA.toString() + ":abc")).thenReturn(pid);
		when(pid.forRegion(REGION_CODE)).thenReturn(pid);

		// act
		String res = dhtRecordController.getDhtRecordForRegion(REGION_NAME, ResourceSchemes.BUCKET_META_DATA.toString(), "abc");

		// assert
		assertEquals(DUMMY_JSON, res);
	}

	@Test(expected = IllegalArgumentException.class)
	public void failToGetDhtRegionRecordForBadRegion() {
		// act
		dhtRecordController.getDhtRecordForRegion("moo", "sg", "abc");
	}

	@Test(expected = IllegalArgumentException.class)
	public void failToGetDhtRegionRecordForBadScheme() {
		// act
		dhtRecordController.getDhtRecordForRegion(REGION_NAME, "baaad", "abc");
	}

	@Test
	public void getDhtRecordForAvailabilityZone() {
		// setup
		when(koalaIdFactory.buildPId("inst:i-123")).thenReturn(pid);
		when(pid.forGlobalAvailablityZoneCode(GLOBAL_AVAILABILITY_ZONE_CODE)).thenReturn(pid);

		// act
		String res = dhtRecordController.getDhtRecordForAvailabilityZone(AVAILABILITY_ZONE_NAME, "inst", "i-123");

		// assert
		assertEquals(DUMMY_JSON, res);
	}

	@Test
	public void getDhtRecordForAvailabilityZoneMixedCaseScheme() {
		// setup
		when(koalaIdFactory.buildPId(ResourceSchemes.BUCKET_META_DATA.toString() + ":i-123")).thenReturn(pid);
		when(pid.forGlobalAvailablityZoneCode(GLOBAL_AVAILABILITY_ZONE_CODE)).thenReturn(pid);

		// act
		String res = dhtRecordController.getDhtRecordForAvailabilityZone(AVAILABILITY_ZONE_NAME, ResourceSchemes.BUCKET_META_DATA.toString(), "i-123");

		// assert
		assertEquals(DUMMY_JSON, res);
	}

	@Test(expected = IllegalArgumentException.class)
	public void failToGetDhtAvailZoneRecordForBadAvailabilityZone() {
		// act
		dhtRecordController.getDhtRecordForAvailabilityZone("moo", "sg", "abc");
	}

	@Test(expected = IllegalArgumentException.class)
	public void failToGetDhtAvailZoneRecordForBadScheme() {
		// act
		dhtRecordController.getDhtRecordForAvailabilityZone(AVAILABILITY_ZONE_NAME, "oooooo", "abc");
	}

	@Test
	public void getLocalDhtRecordByScopeAndUri() {
		// setup
		when(koalaIdFactory.buildPId("user:abc")).thenReturn(pid);
		when(pid.forLocalAvailabilityZone()).thenReturn(pid);

		// act
		String res = dhtRecordController.getLocalDhtRecord("global", "user", "abc");

		// assert
		assertEquals(DUMMY_JSON, res);
	}

	@Test
	public void getLocalDhtRecordByScopeAndUriWithMixedCaseScheme() {
		// setup
		when(koalaIdFactory.buildPId(ResourceSchemes.BUCKET_META_DATA.toString() + ":abc")).thenReturn(pid);
		when(pid.forLocalAvailabilityZone()).thenReturn(pid);

		// act
		String res = dhtRecordController.getLocalDhtRecord("global", ResourceSchemes.BUCKET_META_DATA.toString(), "abc");

		// assert
		assertEquals(DUMMY_JSON, res);
	}

	@Test(expected = IllegalArgumentException.class)
	public void getLocalDhtRecordByScopeAndUriWithBadScope() {
		// act
		dhtRecordController.getLocalDhtRecord("bad", "user", "abc");
	}

	@Test(expected = IllegalArgumentException.class)
	public void getLocalDhtRecordByScopeAndUriWithBadScheme() {
		// act
		dhtRecordController.getLocalDhtRecord("global", "bad", "abc");
	}

	@Test(expected = IllegalArgumentException.class)
	public void getLocalDhtRecordByScopeAndUriWithBlankScope() {
		// act
		dhtRecordController.getLocalDhtRecord("", "app", "abc");
	}

	@Test(expected = IllegalArgumentException.class)
	public void getLocalDhtRecordByScopeAndUriWithBlankScheme() {
		// act
		dhtRecordController.getLocalDhtRecord("availability_zone", "", "abc");
	}

	@Test(expected = IllegalArgumentException.class)
	public void getLocalDhtRecordByScopeAndUriWithBlankUriId() {
		// act
		dhtRecordController.getLocalDhtRecord("region", "app", "");
	}

	@Test
	public void getInstanceTypes() {
		// setup
		when(piIdBuilder.getPId(InstanceTypes.URL_STRING)).thenReturn(pid);

		// act
		String res = dhtRecordController.getInstanceTypes();

		// assert
		assertEquals(DUMMY_JSON, res);
	}

	@Test
	public void getRegions() {
		// setup
		when(piIdBuilder.getRegionsId()).thenReturn(pid);

		// act
		String res = dhtRecordController.getRegions();

		// assert
		assertEquals(DUMMY_JSON, res);
	}

	@Test
	public void getAvailabilityZones() {
		// setup
		when(piIdBuilder.getAvailabilityZonesId()).thenReturn(pid);

		// act
		String res = dhtRecordController.getAvailabilityZones();

		// assert
		assertEquals(DUMMY_JSON, res);
	}
}
