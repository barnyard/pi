/* (c) British Telecommunications plc, 2010, All Rights Reserved */
package com.bt.pi.ops.website.reporting;

import javax.annotation.Resource;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.stereotype.Component;

import com.bt.pi.core.application.MessageContext;
import com.bt.pi.core.application.activation.SuperNodeApplicationCheckPoints;
import com.bt.pi.core.application.health.entity.LogMessageEntityCollection;
import com.bt.pi.core.application.reporter.ReportingApplication;
import com.bt.pi.core.dht.BlockingContinuationBase;
import com.bt.pi.core.dht.cache.BlockingDhtCache;
import com.bt.pi.core.entity.EntityMethod;
import com.bt.pi.core.id.KoalaIdFactory;
import com.bt.pi.ops.website.OpsWebsiteApplicationManager;

@Component
public class LogMessageRetriever {
	private static final Log LOG = LogFactory.getLog(LogMessageRetriever.class);

	private static final int BLOCKING_CONTINUATION_TIMEOUT_SECONDS = 30;

	@Resource
	private KoalaIdFactory koalaIdFactory;
	@Resource(name = "generalBlockingCache")
	private BlockingDhtCache blockingDhtCache;
	@Resource
	private OpsWebsiteApplicationManager opsWebsiteApplicationManager;

	public LogMessageRetriever() {
		blockingDhtCache = null;
		koalaIdFactory = null;
		opsWebsiteApplicationManager = null;
	}

	public LogMessageEntityCollection getAllLogMessages(int region, int availabilityZone) {
		LOG.debug(String.format("getAllLogMessages(%d)", availabilityZone));

		String superNodeApplicationId = getSuperNodeApplicationId(region, availabilityZone);
		LogMessageEntityCollectionBlockingContinuation blockingContinuation = new LogMessageEntityCollectionBlockingContinuation(BLOCKING_CONTINUATION_TIMEOUT_SECONDS);
		MessageContext messageContext = opsWebsiteApplicationManager.newMessageContext();
		messageContext.routePiMessageToApplication(koalaIdFactory.buildPIdFromHexString(superNodeApplicationId), EntityMethod.GET, new LogMessageEntityCollection(),
				ReportingApplication.APPLICATION_NAME, blockingContinuation);

		LogMessageEntityCollection result = blockingContinuation.blockUntilComplete();
		return result;
	}

	private String getSuperNodeApplicationId(int region, int availabilityZone) {
		SuperNodeApplicationCheckPoints superNodeApplicationCheckPoints = (SuperNodeApplicationCheckPoints) blockingDhtCache.get(koalaIdFactory.buildPId(SuperNodeApplicationCheckPoints.URL));
		return superNodeApplicationCheckPoints.getRandomSuperNodeCheckPoint(ReportingApplication.APPLICATION_NAME, koalaIdFactory.getRegion(), koalaIdFactory.getAvailabilityZoneWithinRegion());
	}

	private static final class LogMessageEntityCollectionBlockingContinuation extends BlockingContinuationBase<LogMessageEntityCollection> {
		private LogMessageEntityCollectionBlockingContinuation(int aBlockingTimeoutSecs) {
			super(aBlockingTimeoutSecs);
		}
	}
}