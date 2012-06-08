package com.bt.pi.ops.website.controllers;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.Matchers.isA;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.test.util.ReflectionTestUtils;

import com.bt.pi.app.common.entities.AvailabilityZone;
import com.bt.pi.app.common.entities.AvailabilityZones;
import com.bt.pi.app.common.entities.InstanceTypeConfiguration;
import com.bt.pi.app.common.entities.InstanceTypes;
import com.bt.pi.app.common.id.PiIdBuilder;
import com.bt.pi.app.instancemanager.health.AvailableResourcesHeartBeatFiller;
import com.bt.pi.core.application.health.entity.HeartbeatEntity;
import com.bt.pi.core.application.health.entity.HeartbeatEntityCollection;
import com.bt.pi.core.dht.cache.BlockingDhtCache;
import com.bt.pi.core.id.PId;
import com.bt.pi.core.parser.KoalaJsonParser;
import com.bt.pi.ops.website.entities.AvailableResources;
import com.bt.pi.ops.website.reporting.HeartbeatRetriever;

@RunWith(MockitoJUnitRunner.class)
public class AvailableResourcesControllerTest {
	private static final String MEDIUM_INSTANCE = "medium";
	private static final String SMALL_INSTANCE = "small";
	private static final String AVAILABILITY_ZONE_NAME = "availabilityZone";

	@InjectMocks
	private AvailableResourcesController availableResourcesController = new AvailableResourcesController();
	@Mock
	private PiIdBuilder piIdBuilder;
	@Mock
	private HeartbeatRetriever heartbeatRetriever;
	@Mock
	private BlockingDhtCache blockingDhtCache;
	@Mock
	private PId availabilityZonesId;
	@Mock
	private PId instanceTypesId;
	private KoalaJsonParser koalaJsonParser = new KoalaJsonParser();
	@Mock
	private AvailabilityZones availabilityZones;
	@Mock
	private AvailabilityZone availabilityZone;
	private InstanceTypes instanceTypes;

	@Before
	public void setUp() throws Exception {
		ReflectionTestUtils.setField(availableResourcesController, "koalaJsonParser", koalaJsonParser);
		HeartbeatEntityCollection heartbeatEntityCollection = setupHeartBeatEntityCollection();
		InstanceTypes instanceTypes = setupInstanceTypes();

		when(heartbeatRetriever.getAllHeartbeats(isA(Integer.class), isA(Integer.class))).thenReturn(heartbeatEntityCollection);

		when(piIdBuilder.getAvailabilityZonesId()).thenReturn(availabilityZonesId);
		when(piIdBuilder.getPId(InstanceTypes.URL_STRING)).thenReturn(instanceTypesId);
		when(blockingDhtCache.get(instanceTypesId)).thenReturn(instanceTypes);
		when(blockingDhtCache.get(availabilityZonesId)).thenReturn(availabilityZones);
		when(availabilityZones.getAvailabilityZoneByName(AVAILABILITY_ZONE_NAME)).thenReturn(availabilityZone);
	}

	private InstanceTypes setupInstanceTypes() {
		instanceTypes = new InstanceTypes();
		InstanceTypeConfiguration config1 = new InstanceTypeConfiguration();
		config1.setDiskSizeInGB(10);
		config1.setMemorySizeInMB(1 * 1024);
		config1.setNumCores(1);
		config1.setInstanceType(SMALL_INSTANCE);
		InstanceTypeConfiguration config2 = new InstanceTypeConfiguration();
		config2.setDiskSizeInGB(20);
		config2.setMemorySizeInMB(2 * 1024);
		config2.setNumCores(2);
		config2.setInstanceType(MEDIUM_INSTANCE);
		Map<String, InstanceTypeConfiguration> instancesMap = new HashMap<String, InstanceTypeConfiguration>();
		instancesMap.put(SMALL_INSTANCE, config1);
		instancesMap.put(MEDIUM_INSTANCE, config2);
		instanceTypes.setInstanceTypes(instancesMap);
		return instanceTypes;
	}

	private HeartbeatEntityCollection setupHeartBeatEntityCollection() {
		HeartbeatEntityCollection heartbeatEntityCollection = new HeartbeatEntityCollection();
		HeartbeatEntity entity1 = new HeartbeatEntity();
		entity1.getAvailableResources().put(AvailableResourcesHeartBeatFiller.FREE_CORES, (long) 4);
		entity1.getAvailableResources().put(AvailableResourcesHeartBeatFiller.FREE_DISK, (long) (20 * 1024));
		entity1.getAvailableResources().put(AvailableResourcesHeartBeatFiller.FREE_MEMORY, (long) (4 * 1024));
		HeartbeatEntity entity2 = new HeartbeatEntity();
		entity2.getAvailableResources().put(AvailableResourcesHeartBeatFiller.FREE_CORES, (long) 6);
		entity2.getAvailableResources().put(AvailableResourcesHeartBeatFiller.FREE_DISK, (long) (40 * 1024));
		entity2.getAvailableResources().put(AvailableResourcesHeartBeatFiller.FREE_MEMORY, (long) (2 * 1024));
		HeartbeatEntity entity3 = new HeartbeatEntity();
		entity3.getAvailableResources().put(AvailableResourcesHeartBeatFiller.FREE_CORES, (long) 1);
		entity3.getAvailableResources().put(AvailableResourcesHeartBeatFiller.FREE_DISK, (long) (1024));
		entity3.getAvailableResources().put(AvailableResourcesHeartBeatFiller.FREE_MEMORY, (long) (1024));
		heartbeatEntityCollection.setEntities(Arrays.asList(entity1, entity2, entity3, new HeartbeatEntity()));
		return heartbeatEntityCollection;
	}

	@Test
	public void shouldCalculateAvailableResources() throws Exception {
		// act
		String availableResourcesString = availableResourcesController.getAvailableResources(AVAILABILITY_ZONE_NAME);
		AvailableResources availableResources = (AvailableResources) koalaJsonParser.getObject(availableResourcesString, AvailableResources.class);

		// assert
		assertEquals(new Integer(11), availableResources.getFreeCores());
		assertEquals(new Long(61 * 1024), availableResources.getFreeDiskInMB());
		assertEquals(new Long(7 * 1024), availableResources.getFreeMemoryInMB());
	}

	@Test
	public void shouldCalculateAvailableInstances() throws Exception {
		// act
		String availableResourcesString = availableResourcesController.getAvailableResources(AVAILABILITY_ZONE_NAME);
		AvailableResources availableResources = (AvailableResources) koalaJsonParser.getObject(availableResourcesString, AvailableResources.class);

		// assert
		assertEquals(new Long(4), availableResources.getAvailableInstancesByType().get(SMALL_INSTANCE));
		assertEquals(new Long(2), availableResources.getAvailableInstancesByType().get(MEDIUM_INSTANCE));
	}

	@Test
	public void shouldIgnoreDeprecatedInstanceTypesWhenGettingAvailableResources() throws Exception {
		// setup
		InstanceTypeConfiguration deprecatedConfiguration = new InstanceTypeConfiguration();
		deprecatedConfiguration.setDiskSizeInGB(10);
		deprecatedConfiguration.setMemorySizeInMB(1000);
		deprecatedConfiguration.setNumCores(3);
		deprecatedConfiguration.setInstanceType("deprecated");
		deprecatedConfiguration.setDeprecated(true);
		instanceTypes.addInstanceType(deprecatedConfiguration);
		when(blockingDhtCache.get(instanceTypesId)).thenReturn(instanceTypes);

		// act
		String availableResourcesString = availableResourcesController.getAvailableResources(AVAILABILITY_ZONE_NAME);
		AvailableResources availableResources = (AvailableResources) koalaJsonParser.getObject(availableResourcesString, AvailableResources.class);

		// assert
		assertEquals(new Long(4), availableResources.getAvailableInstancesByType().get(SMALL_INSTANCE));
		assertEquals(new Long(2), availableResources.getAvailableInstancesByType().get(MEDIUM_INSTANCE));
		assertNull(availableResources.getAvailableInstancesByType().get("deprecated"));
	}

	@Test
	public void shouldHandleBadlySeededInstanceTypes() throws Exception {
		// setup
		InstanceTypeConfiguration badConfiguration = new InstanceTypeConfiguration();
		badConfiguration.setDiskSizeInGB(0);
		badConfiguration.setMemorySizeInMB(0);
		badConfiguration.setNumCores(0);
		badConfiguration.setInstanceType("BAD_INSTANCE");
		instanceTypes.addInstanceType(badConfiguration);
		when(blockingDhtCache.get(instanceTypesId)).thenReturn(instanceTypes);

		// act
		String availableResourcesString = availableResourcesController.getAvailableResources(AVAILABILITY_ZONE_NAME);
		AvailableResources availableResources = (AvailableResources) koalaJsonParser.getObject(availableResourcesString, AvailableResources.class);

		// assert
		assertNull(availableResources.getAvailableInstancesByType().get("BAD_INSTANCE"));
	}
}