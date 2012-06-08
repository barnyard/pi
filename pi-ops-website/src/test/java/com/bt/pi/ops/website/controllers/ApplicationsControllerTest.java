package com.bt.pi.ops.website.controllers;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertTrue;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Collection;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.test.util.ReflectionTestUtils;

import com.bt.pi.app.common.AbstractManagedAddressingPiApplication;
import com.bt.pi.app.common.entities.Region;
import com.bt.pi.app.common.entities.Regions;
import com.bt.pi.app.common.id.PiIdBuilder;
import com.bt.pi.core.application.activation.ApplicationRecord;
import com.bt.pi.core.application.activation.RegionScopedApplicationRecord;
import com.bt.pi.core.application.activation.SharedRecordConditionalApplicationActivator;
import com.bt.pi.core.dht.BlockingDhtReader;
import com.bt.pi.core.dht.BlockingDhtWriter;
import com.bt.pi.core.dht.DhtClientFactory;
import com.bt.pi.core.dht.cache.BlockingDhtCache;
import com.bt.pi.core.id.KoalaIdFactory;
import com.bt.pi.core.id.PId;
import com.bt.pi.core.parser.KoalaJsonParser;
import com.bt.pi.core.scope.NodeScope;
import com.bt.pi.ops.website.OpsWebsiteApplicationManager;

@RunWith(MockitoJUnitRunner.class)
public class ApplicationsControllerTest {

	private static final String MOCK_APP_1_NAME = "MOCK_APP_1";
	private static final String MOCK_APP_2_NAME = "MOCK_APP_2";

	@InjectMocks
	private ApplicationsController applicationsController = new ApplicationsController();

	@Mock
	private OpsWebsiteApplicationManager opsWebsiteApplicationManager;

	@Mock
	private PiIdBuilder piIdBuilder;

	@Mock
	private KoalaIdFactory koalaIdFactory;

	@Mock
	private DhtClientFactory dhtClientFactory;

	@Mock
	private BlockingDhtReader blockingDhtReader;

	@Mock
	private BlockingDhtWriter blockingDhtWriter;

	@Mock
	private BlockingDhtCache blockingDhtCache;

	@Mock
	private AbstractManagedAddressingPiApplication mockApplication1;

	@Mock
	private PId mockApplication1PId;

	@Mock
	private ApplicationRecord applicationRecord1;

	@Mock
	private AbstractManagedAddressingPiApplication mockApplication2;

	@Mock
	private PId mockApplication2PId;
	@Mock
	private ApplicationRecord applicationRecord2;
	@Mock
	private SharedRecordConditionalApplicationActivator mockSharedRecordConditionalApplicationActivator;

	@Mock
	private PId mockRegionsId;

	private Regions regions;

	private Region region1 = new Region("region1", 1, "aRegionEndpoint", "aPisssEndpoint");
	private Region region2 = new Region("region2", 2, "aRegionEndpoint", "aPisssEndpoint");

	@org.junit.Before
	public void setUp() {
		regions = new Regions();
		regions.addRegion(region1);
		regions.addRegion(region2);
		when(mockApplication1.getApplicationName()).thenReturn(MOCK_APP_1_NAME);
		when(mockApplication2.getApplicationName()).thenReturn(MOCK_APP_2_NAME);
		Collection<AbstractManagedAddressingPiApplication> mockApplications = new ArrayList<AbstractManagedAddressingPiApplication>();
		mockApplications.add(mockApplication2);
		mockApplications.add(mockApplication1);
		when(opsWebsiteApplicationManager.getManageableSharedResources()).thenReturn(mockApplications);

		ReflectionTestUtils.setField(applicationsController, "koalaJsonParser", new KoalaJsonParser());

		when(piIdBuilder.getRegionsId()).thenReturn(mockRegionsId);
		when(blockingDhtCache.get(mockRegionsId)).thenReturn(regions);
		when(mockSharedRecordConditionalApplicationActivator.getActivationScope()).thenReturn(NodeScope.REGION);
		when(mockApplication1.getApplicationActivator()).thenReturn(mockSharedRecordConditionalApplicationActivator);
		when(mockApplication2.getApplicationActivator()).thenReturn(mockSharedRecordConditionalApplicationActivator);
		when(mockApplication1PId.forRegion(anyInt())).thenReturn(mockApplication1PId);
		when(mockApplication2PId.forRegion(anyInt())).thenReturn(mockApplication2PId);
		when(koalaIdFactory.buildPId(RegionScopedApplicationRecord.getUrl(MOCK_APP_1_NAME))).thenReturn(mockApplication1PId);
		when(koalaIdFactory.buildPId(RegionScopedApplicationRecord.getUrl(MOCK_APP_2_NAME))).thenReturn(mockApplication2PId);
		when(dhtClientFactory.createBlockingReader()).thenReturn(blockingDhtReader);
		when(dhtClientFactory.createBlockingWriter()).thenReturn(blockingDhtWriter);
		when(blockingDhtReader.get(mockApplication1PId)).thenReturn(applicationRecord1);
		when(blockingDhtReader.get(mockApplication2PId)).thenReturn(applicationRecord2);
		when(applicationRecord1.getApplicationName()).thenReturn(MOCK_APP_1_NAME);
		when(applicationRecord2.getApplicationName()).thenReturn(MOCK_APP_2_NAME);
		when(applicationRecord1.getType()).thenReturn(RegionScopedApplicationRecord.TYPE);
		when(applicationRecord2.getType()).thenReturn(RegionScopedApplicationRecord.TYPE);

	}

	@Test
	public void shouldGetFullApplicationRecordList() {

		// act
		String result = applicationsController.getApplicationRecordList();

		// assert
		assertTrue("result should contain app 1", result.indexOf(MOCK_APP_1_NAME) > 0);
		assertTrue("result should contain app 2", result.indexOf(MOCK_APP_2_NAME) > 0);
	}

	@Test
	public void shouldDeActivateApplication() {

		// setup
		String nodeId = "NODE_ID";
		PId mockNodeId = mock(PId.class);
		when(piIdBuilder.getNodeIdFromNodeId(nodeId)).thenReturn(mockNodeId);
		when(mockNodeId.getRegion()).thenReturn(region1.getRegionCode());

		// act
		String result = applicationsController.deActivateApplication(MOCK_APP_1_NAME, nodeId);

		// assert
		assertEquals(result, "OK");
	}
}
