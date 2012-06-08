package com.bt.pi.ops.website.controllers;

import java.util.HashMap;
import java.util.Map;

import javax.annotation.Resource;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.security.authentication.encoding.PasswordEncoder;

import com.bt.pi.app.common.entities.Instance;
import com.bt.pi.app.common.entities.InstanceAction;
import com.bt.pi.app.common.id.PiIdBuilder;
import com.bt.pi.app.instancemanager.handlers.InstanceManagerApplication;
import com.bt.pi.app.instancemanager.handlers.PauseInstanceServiceHelper;
import com.bt.pi.core.continuation.scattergather.ScatterGatherContinuationRunner;
import com.bt.pi.core.dht.BlockingDhtReader;
import com.bt.pi.core.dht.DhtClientFactory;
import com.bt.pi.core.dht.cache.BlockingDhtCache;
import com.bt.pi.core.entity.EntityMethod;
import com.bt.pi.core.id.KoalaIdFactory;
import com.bt.pi.core.id.PId;
import com.bt.pi.core.parser.KoalaJsonParser;
import com.bt.pi.ops.website.OpsWebsiteApplicationManager;
import com.sun.jersey.api.view.Viewable;

public abstract class ControllerBase {
	private static final String UNABLE_TO_FIND_INSTANCE = "Unable to find instance:";
	private static final String SENDING_INSTANCE_S_MESSAGE_TO_INSTANCE_MANAGER_APPLICATION_ON_NODE_ID_S = "Sending instance (%s) message to InstanceManagerApplication on NodeId (%s)";
	private static final Log LOG = LogFactory.getLog(ControllerBase.class);
	@Resource
	private PasswordEncoder passwordEncoder;
	@Resource
	private PiIdBuilder piIdBuilder;
	@Resource
	private OpsWebsiteApplicationManager opsWebsiteApplicationManager;
	@Resource
	private DhtClientFactory dhtClientFactory;
	@Resource(name = "generalBlockingCache")
	private BlockingDhtCache blockingDhtCache;
	@Resource
	private KoalaJsonParser koalaJsonParser;
	@Resource
	private KoalaIdFactory koalaIdFactory;
	@Resource
	private ScatterGatherContinuationRunner scatterGatherContinuationRunner;
	@Resource
	private PauseInstanceServiceHelper pauseInstanceServiceHelper;

	public ControllerBase() {
		passwordEncoder = null;
		blockingDhtCache = null;
		koalaJsonParser = null;
		koalaIdFactory = null;
		scatterGatherContinuationRunner = null;
		pauseInstanceServiceHelper = null;
	}

	protected PiIdBuilder getPiIdBuilder() {
		return piIdBuilder;
	}

	protected OpsWebsiteApplicationManager getOpsWebsiteApplicationManager() {
		return opsWebsiteApplicationManager;
	}

	protected DhtClientFactory getDhtClientFactory() {
		return dhtClientFactory;
	}

	protected Viewable buildModelAndView(String templateName, String modelKey, Object modelValue) {
		Map<String, Object> model = new HashMap<String, Object>();
		model.put(modelKey, modelValue);

		return new Viewable(templateName, model);
	}

	protected void checkNotNullOrEmpty(String... argsToValidate) {
		for (String arg : argsToValidate) {
			if (null == arg || arg.isEmpty()) {
				throw new IllegalArgumentException("Empty argument");
			}
		}
	}

	protected String getEncodedPassword(String clearTextPassword) {
		return passwordEncoder.encodePassword(clearTextPassword, null);
	}

	protected BlockingDhtCache getBlockingDhtCache() {
		return blockingDhtCache;
	}

	protected KoalaJsonParser getKoalaJsonParser() {
		return koalaJsonParser;
	}

	protected KoalaIdFactory getKoalaIdFactory() {
		return koalaIdFactory;
	}

	protected ScatterGatherContinuationRunner getScatterGatherContinuationRunner() {
		return scatterGatherContinuationRunner;
	}

	protected Response pauseInstanceUsingHelper(String instanceId) {
		LOG.debug(String.format("pauseInstanceUsingHelper(%s)", instanceId));
		Instance instanceToPause = readInstance(instanceId);
		if (instanceToPause == null) {
			LOG.warn(UNABLE_TO_FIND_INSTANCE + instanceId);
			return Response.status(Status.NOT_FOUND).build();
		}
		pauseInstanceServiceHelper.pauseInstance(instanceToPause);
		return Response.ok().build();
	}

	protected Response unpauseInstanceUsingHelper(String instanceId) {
		LOG.debug(String.format("unpauseInstanceUsingHelper(%s)", instanceId));
		Instance instanceToUnpause = readInstance(instanceId);
		if (instanceToUnpause == null) {
			LOG.warn(UNABLE_TO_FIND_INSTANCE + instanceId);
			return Response.status(Status.NOT_FOUND).build();
		}
		pauseInstanceServiceHelper.unPauseInstance(instanceToUnpause);
		return Response.ok().build();
	}

	private Instance readInstance(String instanceId) {
		final PId instancePastryId = getPiIdBuilder().getPIdForEc2AvailabilityZone(Instance.getUrl(instanceId));
		BlockingDhtReader blockingReader = getDhtClientFactory().createBlockingReader();
		return (Instance) blockingReader.get(instancePastryId);
	}

	protected void sendMessageToInstanceManager(final String instanceId, Instance result, InstanceAction instanceAction) {
		LOG.debug(String.format("sendMessageToInstanceManager(%s, %s, %s)", instanceId, result, instanceAction));
		PId nodePastryId = getPiIdBuilder().getNodeIdFromNodeId(result.getNodeId());
		result.setActionRequired(instanceAction);
		LOG.debug(String.format(SENDING_INSTANCE_S_MESSAGE_TO_INSTANCE_MANAGER_APPLICATION_ON_NODE_ID_S, result.getInstanceId(), result.getHostname()));
		getOpsWebsiteApplicationManager().newMessageContext().routePiMessageToApplication(nodePastryId, EntityMethod.UPDATE, result, InstanceManagerApplication.APPLICATION_NAME);
	}
}