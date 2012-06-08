/* (c) British Telecommunications plc, 2010, All Rights Reserved */
package com.bt.pi.ops.website.reporting;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.isA;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.when;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.runners.MockitoJUnitRunner;
import org.mockito.stubbing.Answer;

import com.bt.pi.app.instancemanager.reporting.InstanceReportEntityCollection;
import com.bt.pi.app.instancemanager.reporting.ZombieInstanceReportEntityCollection;
import com.bt.pi.core.application.MessageContext;
import com.bt.pi.core.application.activation.SuperNodeApplicationCheckPoints;
import com.bt.pi.core.application.reporter.ReportingApplication;
import com.bt.pi.core.dht.BlockingContinuationBase;
import com.bt.pi.core.dht.cache.BlockingDhtCache;
import com.bt.pi.core.entity.EntityMethod;
import com.bt.pi.core.id.KoalaIdFactory;
import com.bt.pi.core.id.PId;
import com.bt.pi.ops.website.OpsWebsiteApplicationManager;

@RunWith(MockitoJUnitRunner.class)
public class InstancesRetrieverTest {
	private String superNodeIdString = "supernodeId";
	private int region = 5;
	private int availabilityZone = 10;

	@Mock
	private MessageContext messageContext;
	@Mock
	private InstanceReportEntityCollection runningInstanceReportEntityCollection;
	@Mock
	private ZombieInstanceReportEntityCollection zombieInstanceReportEntityCollection;
	@Mock
	private PId id;
	@Mock
	private PId superNodeId;
	@Mock
	private SuperNodeApplicationCheckPoints superNodeApplicationCheckPoints;
	@Mock
	private BlockingDhtCache blockingDhtCache;
	@Mock
	private KoalaIdFactory koalaIdFactory;
	@Mock
	private OpsWebsiteApplicationManager opsWebsiteApplicationManager;

	@InjectMocks
	private InstancesRetriever instancesRetriever = new InstancesRetriever();

	@SuppressWarnings("unchecked")
	@Before
	public void doBefore() {
		when(superNodeApplicationCheckPoints.getRandomSuperNodeCheckPoint(ReportingApplication.APPLICATION_NAME, region, availabilityZone)).thenReturn(superNodeIdString);

		when(koalaIdFactory.buildPId(SuperNodeApplicationCheckPoints.URL)).thenReturn(id);
		when(koalaIdFactory.buildPIdFromHexString(superNodeIdString)).thenReturn(superNodeId);
		when(koalaIdFactory.getRegion()).thenReturn(region);
		when(koalaIdFactory.getAvailabilityZoneWithinRegion()).thenReturn(availabilityZone);

		when(blockingDhtCache.get(id)).thenReturn(superNodeApplicationCheckPoints);

		when(opsWebsiteApplicationManager.newMessageContext()).thenReturn(messageContext);

		doAnswer(new Answer<Object>() {
			@Override
			public Object answer(InvocationOnMock invocation) throws Throwable {
				((BlockingContinuationBase<InstanceReportEntityCollection>) invocation.getArguments()[4]).receiveResult(runningInstanceReportEntityCollection);
				return null;
			}
		}).when(messageContext).routePiMessageToApplication(eq(superNodeId), eq(EntityMethod.GET), isA(InstanceReportEntityCollection.class), eq(ReportingApplication.APPLICATION_NAME),
				isA(BlockingContinuationBase.class));
		doAnswer(new Answer<Object>() {
			@Override
			public Object answer(InvocationOnMock invocation) throws Throwable {
				((BlockingContinuationBase<ZombieInstanceReportEntityCollection>) invocation.getArguments()[4]).receiveResult(zombieInstanceReportEntityCollection);
				return null;
			}
		}).when(messageContext).routePiMessageToApplication(eq(superNodeId), eq(EntityMethod.GET), isA(ZombieInstanceReportEntityCollection.class), eq(ReportingApplication.APPLICATION_NAME),
				isA(BlockingContinuationBase.class));
	}

	@Test
	public void itShouldRetrieveRunningInstances() {
		// act
		InstanceReportEntityCollection result = instancesRetriever.getAllRunningInstances(region, availabilityZone);

		// assert
		assertThat(result, equalTo(runningInstanceReportEntityCollection));
	}

	@Test
	public void itShouldRetrieveZombieInstances() {
		// act
		ZombieInstanceReportEntityCollection result = instancesRetriever.getAllZombieInstances(region, availabilityZone);

		// assert
		assertThat(result, equalTo(zombieInstanceReportEntityCollection));
	}
}