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

import com.bt.pi.core.application.MessageContext;
import com.bt.pi.core.application.activation.SuperNodeApplicationCheckPoints;
import com.bt.pi.core.application.health.entity.HeartbeatEntityCollection;
import com.bt.pi.core.application.reporter.ReportingApplication;
import com.bt.pi.core.dht.BlockingContinuationBase;
import com.bt.pi.core.dht.cache.BlockingDhtCache;
import com.bt.pi.core.entity.EntityMethod;
import com.bt.pi.core.id.KoalaIdFactory;
import com.bt.pi.core.id.PId;
import com.bt.pi.ops.website.OpsWebsiteApplicationManager;

@RunWith(MockitoJUnitRunner.class)
public class HeartbeatRetrieverTest {
	private String superNodeIdString = "supernodeId";
	private int region = 5;
	private int availabilityZone = 10;

	@Mock
	private MessageContext messageContext;
	@Mock
	private HeartbeatEntityCollection heartbeatEntityCollection;
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
	private HeartbeatRetriever heartbeatRetriever = new HeartbeatRetriever();

	@SuppressWarnings("unchecked")
	@Before
	public void doBefore() {
		when(superNodeApplicationCheckPoints.getRandomSuperNodeCheckPoint(ReportingApplication.APPLICATION_NAME, region, availabilityZone)).thenReturn(superNodeIdString);

		when(koalaIdFactory.buildPId(SuperNodeApplicationCheckPoints.URL)).thenReturn(id);
		when(koalaIdFactory.buildPIdFromHexString(superNodeIdString)).thenReturn(superNodeId);

		when(blockingDhtCache.get(id)).thenReturn(superNodeApplicationCheckPoints);

		when(opsWebsiteApplicationManager.newMessageContext()).thenReturn(messageContext);

		doAnswer(new Answer<Object>() {
			@Override
			public Object answer(InvocationOnMock invocation) throws Throwable {
				((BlockingContinuationBase<HeartbeatEntityCollection>) invocation.getArguments()[4]).receiveResult(heartbeatEntityCollection);
				return null;
			}
		}).when(messageContext).routePiMessageToApplication(eq(superNodeId), eq(EntityMethod.GET), isA(HeartbeatEntityCollection.class), eq(ReportingApplication.APPLICATION_NAME),
				isA(BlockingContinuationBase.class));
	}

	@Test
	public void itShouldRetrieveHeartbeats() {
		// act
		HeartbeatEntityCollection result = heartbeatRetriever.getAllHeartbeats(region, availabilityZone);

		// assert
		assertThat(result, equalTo(heartbeatEntityCollection));
	}
}