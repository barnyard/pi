/* (c) British Telecommunications plc, 2010, All Rights Reserved */
package com.bt.pi.ops.website.reporting;

import javax.annotation.Resource;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.stereotype.Component;

import com.bt.pi.core.application.MessageContext;
import com.bt.pi.core.application.activation.SuperNodeApplicationCheckPoints;
import com.bt.pi.core.application.health.entity.HeartbeatEntityCollection;
import com.bt.pi.core.application.reporter.ReportingApplication;
import com.bt.pi.core.dht.BlockingContinuationBase;
import com.bt.pi.core.dht.cache.BlockingDhtCache;
import com.bt.pi.core.entity.EntityMethod;
import com.bt.pi.core.id.KoalaIdFactory;
import com.bt.pi.ops.website.OpsWebsiteApplicationManager;

@Component
public class HeartbeatRetriever {
	private static final Log LOG = LogFactory.getLog(HeartbeatRetriever.class);

	private static final int BLOCKING_CONTINUATION_TIMEOUT_SECONDS = 30;

	@Resource
	private KoalaIdFactory koalaIdFactory;
	@Resource(name = "generalBlockingCache")
	private BlockingDhtCache blockingDhtCache;
	@Resource
	private OpsWebsiteApplicationManager opsWebsiteApplicationManager;

	public HeartbeatRetriever() {
		blockingDhtCache = null;
		koalaIdFactory = null;
		opsWebsiteApplicationManager = null;
	}

	public HeartbeatEntityCollection getAllHeartbeats(int region, int availabilityZone) {
		LOG.debug(String.format("getAllHeartbeats(%s,%s)", region, availabilityZone));

		String superNodeApplicationId = getSuperNodeApplicationId(region, availabilityZone);
		HeartbeatEntityCollectionBlockingContinuation blockingContinuation = new HeartbeatEntityCollectionBlockingContinuation(BLOCKING_CONTINUATION_TIMEOUT_SECONDS);
		MessageContext messageContext = opsWebsiteApplicationManager.newMessageContext();
		messageContext.routePiMessageToApplication(koalaIdFactory.buildPIdFromHexString(superNodeApplicationId), EntityMethod.GET, new HeartbeatEntityCollection(),
				ReportingApplication.APPLICATION_NAME, blockingContinuation);

		return blockingContinuation.blockUntilComplete();
	}

	private String getSuperNodeApplicationId(int region, int availabilityZone) {
		SuperNodeApplicationCheckPoints superNodeApplicationCheckPoints = (SuperNodeApplicationCheckPoints) blockingDhtCache.get(koalaIdFactory.buildPId(SuperNodeApplicationCheckPoints.URL));
		return superNodeApplicationCheckPoints.getRandomSuperNodeCheckPoint(ReportingApplication.APPLICATION_NAME, region, availabilityZone);
	}

	private static final class HeartbeatEntityCollectionBlockingContinuation extends BlockingContinuationBase<HeartbeatEntityCollection> {
		private HeartbeatEntityCollectionBlockingContinuation(int aBlockingTimeoutSecs) {
			super(aBlockingTimeoutSecs);
		}
	}
}