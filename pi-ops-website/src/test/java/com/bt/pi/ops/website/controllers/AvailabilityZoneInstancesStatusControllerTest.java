package com.bt.pi.ops.website.controllers;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.isA;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import com.bt.pi.app.common.entities.AvailabilityZone;
import com.bt.pi.app.common.entities.AvailabilityZones;
import com.bt.pi.app.common.entities.InstanceState;
import com.bt.pi.app.common.entities.Region;
import com.bt.pi.app.common.entities.Regions;
import com.bt.pi.app.common.id.PiIdBuilder;
import com.bt.pi.app.instancemanager.handlers.InstanceStateTransition;
import com.bt.pi.app.instancemanager.handlers.TerminateInstanceServiceHelper;
import com.bt.pi.app.instancemanager.reporting.InstanceReportEntityCollection;
import com.bt.pi.app.instancemanager.reporting.ZombieInstanceReportEntityCollection;
import com.bt.pi.core.dht.cache.BlockingDhtCache;
import com.bt.pi.core.id.PId;
import com.bt.pi.core.parser.KoalaJsonParser;
import com.bt.pi.ops.website.controllers.AvailabilityZoneInstancesStatusController.TerminateZombieInstanceStatus;
import com.bt.pi.ops.website.reporting.InstancesRetriever;

@RunWith(MockitoJUnitRunner.class)
public class AvailabilityZoneInstancesStatusControllerTest {
	private String json = "json";
	private String json2 = "json2";
	private int availabilityZoneCode = 1;
	private int regionCode = 2;

	@Mock
	private PId availabilityZonesId;
	@Mock
	private PId regionsId;
	@Mock
	private InstanceReportEntityCollection instanceReportEntityCollection;
	@Mock
	private ZombieInstanceReportEntityCollection zombieInstanceReportEntityCollection;
	@Mock
	private InstancesRetriever instancesRetriever;
	@Mock
	private KoalaJsonParser koalaJsonParser;
	@Mock
	private PiIdBuilder piIdBuilder;
	@Mock
	private BlockingDhtCache blockingDhtCache;
	@Mock
	private TerminateInstanceServiceHelper terminateInstanceServiceHelper;

	@InjectMocks
	private AvailabilityZoneInstancesStatusController availabilityZoneInstancesController = new AvailabilityZoneInstancesStatusController();

	@Before
	public void setup() {
		when(piIdBuilder.getAvailabilityZonesId()).thenReturn(availabilityZonesId);
		when(piIdBuilder.getRegionsId()).thenReturn(regionsId);

		Regions regions = new Regions();
		regions.addRegion(new Region("r1", regionCode, null, null));

		AvailabilityZones availabilityZones = new AvailabilityZones();
		availabilityZones.addAvailabilityZone(new AvailabilityZone("av1", availabilityZoneCode, regionCode, null));

		when(blockingDhtCache.get(availabilityZonesId)).thenReturn(availabilityZones);
		when(blockingDhtCache.get(regionsId)).thenReturn(regions);

		when(koalaJsonParser.getJson(instanceReportEntityCollection)).thenReturn(json);
		when(koalaJsonParser.getJson(zombieInstanceReportEntityCollection)).thenReturn(json2);
		when(koalaJsonParser.getJson(isA(TerminateZombieInstanceStatus.class))).thenReturn(json);
	}

	@Test
	public void shouldGetRunningInstances() throws Exception {
		// setup
		when(instancesRetriever.getAllRunningInstances(regionCode, availabilityZoneCode)).thenReturn(instanceReportEntityCollection);

		// act
		String result = availabilityZoneInstancesController.getRunningInstances("av1");

		// assert
		assertThat(result, equalTo(json));
	}

	@Test
	public void shouldGetZombieInstances() throws Exception {
		// setup
		when(instancesRetriever.getAllZombieInstances(regionCode, availabilityZoneCode)).thenReturn(zombieInstanceReportEntityCollection);

		// act
		String result = availabilityZoneInstancesController.getZombieInstances("av1");

		// assert
		assertThat(result, equalTo(json2));
	}

	@SuppressWarnings("unchecked")
	@Test
	public void shouldTerminateZombieInstance() throws Exception {
		// setup
		Map<String, InstanceStateTransition> stateTransitions = new HashMap<String, InstanceStateTransition>();
		stateTransitions.put("ins-1", new InstanceStateTransition(InstanceState.RUNNING, InstanceState.TERMINATED));
		when(terminateInstanceServiceHelper.terminateBuriedInstance(isA(Collection.class))).thenReturn(stateTransitions);

		// act
		String result = availabilityZoneInstancesController.terminateBuriedInstance("ins-1");

		// assert
		verify(terminateInstanceServiceHelper).terminateBuriedInstance(Arrays.asList(new String[] { "ins-1" }));
		assertThat(result, equalTo(json));
	}
}
