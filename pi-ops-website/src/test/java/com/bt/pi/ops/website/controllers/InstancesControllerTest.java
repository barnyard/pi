package com.bt.pi.ops.website.controllers;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.Map;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import com.bt.pi.app.common.entities.Instance;
import com.bt.pi.app.common.entities.InstanceRecord;
import com.bt.pi.app.common.entities.PublicIpAllocationIndex;
import com.bt.pi.app.common.entities.Region;
import com.bt.pi.app.common.entities.Regions;
import com.bt.pi.app.common.id.PiIdBuilder;
import com.bt.pi.app.common.net.utils.IpAddressUtils;
import com.bt.pi.app.instancemanager.handlers.PauseInstanceServiceHelper;
import com.bt.pi.core.dht.BlockingDhtReader;
import com.bt.pi.core.dht.DhtClientFactory;
import com.bt.pi.core.id.PId;

@RunWith(MockitoJUnitRunner.class)
public class InstancesControllerTest {
	@Mock
	private PiIdBuilder piIdBuilder;
	@Mock
	private DhtClientFactory dhtClientFactory;
	@Mock
	private PId instancePastryId;
	@Mock
	private BlockingDhtReader dhtReader;
	@Mock
	private PId nodePastryId;
	@Mock
	private PId publicIpAllocationIndexId;
	@Mock
	private PId regionsId;
	@Mock
	private PauseInstanceServiceHelper pauseInstanceServiceHelper;

	@InjectMocks
	private InstancesController instancesController = new InstancesController();

	private String instanceId = "i-000abc";
	private final Instance testInstance = new Instance();
	private PublicIpAllocationIndex publicIpAllocationIndex = new PublicIpAllocationIndex();
	private InstanceRecord instanceRecord = new InstanceRecord(instanceId, null);
	private String ipAddress = "127.0.1.2";

	@Before
	public void setup() {
		testInstance.setInstanceId("i-000abc");
		testInstance.setNodeId("nodeId");

		when(piIdBuilder.getPIdForEc2AvailabilityZone(Instance.getUrl(instanceId))).thenReturn(instancePastryId);
		when(piIdBuilder.getNodeIdFromNodeId(testInstance.getNodeId())).thenReturn(nodePastryId);
		when(piIdBuilder.getPId(PublicIpAllocationIndex.URL)).thenReturn(publicIpAllocationIndexId);
		when(piIdBuilder.getRegionsId()).thenReturn(regionsId);

		Map<Long, InstanceRecord> allocationMap = new HashMap<Long, InstanceRecord>();
		allocationMap.put(IpAddressUtils.ipToLong(ipAddress), instanceRecord);
		publicIpAllocationIndex.setAllocationMap(allocationMap);

		Regions regions = new Regions();
		regions.addRegion(new Region("", 0, null, null));

		when(dhtReader.get(publicIpAllocationIndexId)).thenReturn(publicIpAllocationIndex);
		when(dhtReader.get(instancePastryId)).thenReturn(testInstance);
		when(dhtReader.get(regionsId)).thenReturn(regions);

		when(publicIpAllocationIndexId.forRegion(0)).thenReturn(publicIpAllocationIndexId);
		when(dhtClientFactory.createBlockingReader()).thenReturn(dhtReader);
	}

	@Test
	public void shouldReturn404IfInstanceDoesntExist() {
		// setup
		when(dhtReader.get(instancePastryId)).thenReturn(null);

		// act
		Response pauseResponse = instancesController.pauseInstance("i-notthere");
		Response unPauseResponse = instancesController.unPauseInstance("i-notthere");

		// assert
		assertNotNull(pauseResponse);
		assertEquals(Status.NOT_FOUND.getStatusCode(), pauseResponse.getStatus());

		assertNotNull(unPauseResponse);
		assertEquals(Status.NOT_FOUND.getStatusCode(), unPauseResponse.getStatus());
	}

	@Test
	public void shouldPauseInstance() throws InterruptedException {
		// setup
		when(dhtReader.get(instancePastryId)).thenReturn(testInstance);

		// act
		Response response = instancesController.pauseInstance(testInstance.getInstanceId());

		// assert
		assertNotNull(response);
		verify(pauseInstanceServiceHelper).pauseInstance(testInstance);
	}

	@Test
	public void shouldUnPauseInstance() throws InterruptedException {
		// setup
		when(dhtReader.get(instancePastryId)).thenReturn(testInstance);

		// act
		Response response = instancesController.unPauseInstance(testInstance.getInstanceId());

		// assert
		assertNotNull(response);
		verify(pauseInstanceServiceHelper).unPauseInstance(testInstance);
	}

	@Test
	public void shouldPauseInstanceAfterLookingUpByIpAddress() throws Exception {
		// act
		Response response = instancesController.pauseInstanceByIpAddress(ipAddress);

		// assert
		assertNotNull(response);
		verify(pauseInstanceServiceHelper).pauseInstance(testInstance);
	}

	@Test
	public void shouldUnPauseInstanceAfterLookingUpByIpAddress() throws Exception {
		// act
		Response response = instancesController.unPauseInstanceByIpAddress(ipAddress);

		// assert
		assertNotNull(response);
		verify(pauseInstanceServiceHelper).unPauseInstance(testInstance);
	}
}