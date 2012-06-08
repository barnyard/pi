/* (c) British Telecommunications plc, 2010, All Rights Reserved */
package com.bt.pi.ops.website.reporting;

import javax.annotation.Resource;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.stereotype.Component;

import com.bt.pi.app.instancemanager.reporting.InstanceReportEntityCollection;
import com.bt.pi.app.instancemanager.reporting.ZombieInstanceReportEntityCollection;
import com.bt.pi.core.application.MessageContext;
import com.bt.pi.core.application.activation.SuperNodeApplicationCheckPoints;
import com.bt.pi.core.application.reporter.ReportingApplication;
import com.bt.pi.core.dht.BlockingContinuationBase;
import com.bt.pi.core.dht.cache.BlockingDhtCache;
import com.bt.pi.core.entity.EntityMethod;
import com.bt.pi.core.id.KoalaIdFactory;
import com.bt.pi.ops.website.OpsWebsiteApplicationManager;

@Component
public class InstancesRetriever {
	private static final Log LOG = LogFactory.getLog(InstancesRetriever.class);

	private static final int BLOCKING_CONTINUATION_TIMEOUT_SECONDS = 30;

	@Resource
	private KoalaIdFactory koalaIdFactory;
	@Resource(name = "generalBlockingCache")
	private BlockingDhtCache blockingDhtCache;
	@Resource
	private OpsWebsiteApplicationManager opsWebsiteApplicationManager;

	public InstancesRetriever() {
		blockingDhtCache = null;
		koalaIdFactory = null;
		opsWebsiteApplicationManager = null;
	}

	public InstanceReportEntityCollection getAllRunningInstances(int region, int availabilityZone) {
		LOG.debug(String.format("getAllRunningInstances(%d, %d)", region, availabilityZone));

		String superNodeApplicationId = getSuperNodeApplicationId(region, availabilityZone);
		LOG.debug(String.format("superNodeApplicationId = %s", superNodeApplicationId));
		RunningInstanceReportEntityCollectionBlockingContinuation blockingContinuation = new RunningInstanceReportEntityCollectionBlockingContinuation(BLOCKING_CONTINUATION_TIMEOUT_SECONDS);
		MessageContext messageContext = opsWebsiteApplicationManager.newMessageContext();
		messageContext.routePiMessageToApplication(koalaIdFactory.buildPIdFromHexString(superNodeApplicationId), EntityMethod.GET, new InstanceReportEntityCollection(),
				ReportingApplication.APPLICATION_NAME, blockingContinuation);

		return blockingContinuation.blockUntilComplete();
	}

	public ZombieInstanceReportEntityCollection getAllZombieInstances(int region, int availabilityZone) {
		LOG.debug(String.format("getAllZombieInstances(%d, %d)", region, availabilityZone));

		String superNodeApplicationId = getSuperNodeApplicationId(region, availabilityZone);
		ZombieInstanceReportEntityCollectionBlockingContinuation blockingContinuation = new ZombieInstanceReportEntityCollectionBlockingContinuation(BLOCKING_CONTINUATION_TIMEOUT_SECONDS);
		MessageContext messageContext = opsWebsiteApplicationManager.newMessageContext();
		messageContext.routePiMessageToApplication(koalaIdFactory.buildPIdFromHexString(superNodeApplicationId), EntityMethod.GET, new ZombieInstanceReportEntityCollection(),
				ReportingApplication.APPLICATION_NAME, blockingContinuation);

		return blockingContinuation.blockUntilComplete();
	}

	private String getSuperNodeApplicationId(int region, int availabilityZone) {
		LOG.debug(String.format("getSuperNodeApplicationId(%d, %d)", region, availabilityZone));
		SuperNodeApplicationCheckPoints superNodeApplicationCheckPoints = (SuperNodeApplicationCheckPoints) blockingDhtCache.get(koalaIdFactory.buildPId(SuperNodeApplicationCheckPoints.URL));
		return superNodeApplicationCheckPoints.getRandomSuperNodeCheckPoint(ReportingApplication.APPLICATION_NAME, region, availabilityZone);
	}

	private static final class RunningInstanceReportEntityCollectionBlockingContinuation extends BlockingContinuationBase<InstanceReportEntityCollection> {
		private RunningInstanceReportEntityCollectionBlockingContinuation(int aBlockingTimeoutSecs) {
			super(aBlockingTimeoutSecs);
		}
	}

	private static final class ZombieInstanceReportEntityCollectionBlockingContinuation extends BlockingContinuationBase<ZombieInstanceReportEntityCollection> {
		private ZombieInstanceReportEntityCollectionBlockingContinuation(int aBlockingTimeoutSecs) {
			super(aBlockingTimeoutSecs);
		}
	}
}