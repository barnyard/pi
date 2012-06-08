package com.bt.pi.ops.website.controllers;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import javax.annotation.Resource;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.core.task.TaskExecutor;
import org.springframework.stereotype.Component;

import com.bt.pi.api.service.InstancesService;
import com.bt.pi.app.common.entities.Instance;
import com.bt.pi.app.common.entities.InstanceActivityState;
import com.bt.pi.app.common.entities.InstanceState;
import com.bt.pi.app.common.entities.User;
import com.bt.pi.app.instancemanager.handlers.PauseInstanceServiceHelper;
import com.bt.pi.core.continuation.UpdateResolvingPiContinuation;
import com.bt.pi.core.continuation.scattergather.ScatterGatherContinuationRunnable;
import com.bt.pi.core.continuation.scattergather.UpdateResolvingPiScatterGatherContinuation;
import com.bt.pi.core.entity.PiEntity;
import com.bt.pi.core.id.PId;
import com.bt.pi.ops.website.entities.ReadOnlyInstance;
import com.sun.jersey.api.NotFoundException;
import com.sun.jersey.api.view.Viewable;

@Component
@Path("/users/{username}/instancevalidation/{pid}")
public class UsersInstanceValidationController extends UsersController {
	private static final String PID = "pid";
	private static final String VALIDATE = "validate_";
	private static final String USERNAME = "username";
	private static final Log LOG = LogFactory.getLog(UsersInstanceValidationController.class);
	private static final long TWO = 2;
	@Resource
	private InstancesService instancesService;
	@Resource
	private TaskExecutor taskExecutor;
	@Resource
	private PauseInstanceServiceHelper pauseInstanceServiceHelper;

	public UsersInstanceValidationController() {
		instancesService = null;
	}

	@POST
	@Consumes(MediaType.APPLICATION_FORM_URLENCODED)
	public Viewable validateUserInstances(MultivaluedMap<String, String> formParams, @PathParam(USERNAME) String username, @PathParam(PID) String pid) {
		LOG.debug(String.format("validateUserInstances(%s, %s, %s)", formParams, username, pid));

		// for each form param named validate_<instanceId> re-validate instance and un pause it
		List<String> instanceIdsToValidateFromRequest = new ArrayList<String>();
		validatePid(username, pid);
		for (Entry<String, List<String>> entry : formParams.entrySet()) {
			if (entry.getKey().startsWith(VALIDATE) && Boolean.valueOf(formParams.getFirst(entry.getKey()))) {
				instanceIdsToValidateFromRequest.add(entry.getKey().substring(VALIDATE.length()));
			}
		}
		LOG.debug(instanceIdsToValidateFromRequest);

		// match request to the users instances
		List<PId> ids = new ArrayList<PId>();
		if (instanceIdsToValidateFromRequest.size() > 0) {
			Collection<ReadOnlyInstance> instances = getInstances(username);
			LOG.debug(instances);
			for (ReadOnlyInstance readOnlyInstance : instances) {
				if (instanceIdsToValidateFromRequest.contains(readOnlyInstance.getInstanceId())) {
					int globalAvailabilityZoneCode = getPiIdBuilder().getGlobalAvailabilityZoneCodeFromEc2Id(readOnlyInstance.getInstanceId());
					ids.add(getPiIdBuilder().getPId(Instance.getUrl(readOnlyInstance.getInstanceId())).forGlobalAvailablityZoneCode(globalAvailabilityZoneCode));
				}
			}
		}

		scatterWriter(ids, new InstanceActivityStateResetContinuation(this));

		return getUserWithInstancesForValidation(username, pid);
	}

	private static class InstanceActivityStateResetContinuation extends UpdateResolvingPiContinuation<Instance> {
		private UsersInstanceValidationController controller;
		private InstanceActivityState oldInstanceActivityState;

		public InstanceActivityStateResetContinuation(UsersInstanceValidationController usersInstanceValidationController) {
			this.controller = usersInstanceValidationController;
		}

		@Override
		public Instance update(Instance existingEntity, Instance requestedEntity) {
			if (null == existingEntity) {
				return null;
			}
			// always set the status to force the timestamp update
			oldInstanceActivityState = existingEntity.getInstanceActivityState();
			existingEntity.setInstanceActivityState(InstanceActivityState.GREEN);
			return existingEntity;
		}

		@Override
		public void handleResult(final Instance result) {
			if (null != result) {
				LOG.debug(String.format("Instance %s activity state updated to %s", result.getInstanceId(), result.getInstanceActivityState()));
				controller.taskExecutor.execute(new Runnable() {
					@Override
					public void run() {
						if (InstanceActivityState.RED.equals(oldInstanceActivityState)) {
							LOG.debug(String.format("un-pausing instance %s", result.getInstanceId()));
							controller.pauseInstanceServiceHelper.unPauseInstance(result);
						}
					}
				});
			}
		}
	}

	private <T extends PiEntity> void scatterWriter(final List<PId> ids, final UpdateResolvingPiContinuation<T> continuation) {
		LOG.debug(String.format("scatterWriter(%s, %s)", ids, continuation));
		List<ScatterGatherContinuationRunnable> runnables = new ArrayList<ScatterGatherContinuationRunnable>();
		for (final PId id : ids) {
			LOG.debug("adding " + id.toStringFull());
			final UpdateResolvingPiScatterGatherContinuation<T> updateResolvingPiScatterGatherContinuation = new UpdateResolvingPiScatterGatherContinuation<T>(continuation);
			runnables.add(new ScatterGatherContinuationRunnable(updateResolvingPiScatterGatherContinuation) {
				@Override
				public void run() {
					getDhtClientFactory().createWriter().update(id, updateResolvingPiScatterGatherContinuation);
				}
			});
		}
		getScatterGatherContinuationRunner().execute(runnables, TWO, TimeUnit.MINUTES);
	}

	@SuppressWarnings("unchecked")
	@GET
	public Viewable getUserWithInstancesForValidation(@PathParam(USERNAME) String username, @PathParam(PID) String pid) {
		LOG.debug(String.format("getUserWithInstancesForValidation(%s, %s)", username, pid));
		validatePid(username, pid);
		Viewable result = getViewable(getUser(username), "user_instances_validation");
		Collection<ReadOnlyInstance> instances = getInstances(username);
		Map<String, Object> model = (Map<String, Object>) result.getModel();
		model.put("instances", instances);
		model.put(PID, pid);
		return result;
	}

	private void validatePid(String username, String pid) {
		LOG.debug(String.format("validatePid(%s, %s)", username, pid));
		PId userPid = getPiIdBuilder().getPId(User.getUrl(username));
		if (!pid.equals(userPid.toStringFull()))
			throw new NotFoundException();
	}

	private Collection<ReadOnlyInstance> getInstances(String username) {
		LOG.debug(String.format("getInstances(%s)", username));
		Map<String, Set<Instance>> describeInstancesMap = this.instancesService.describeInstances(username, Collections.<String> emptyList());
		LOG.debug(describeInstancesMap);
		Collection<ReadOnlyInstance> result = new ArrayList<ReadOnlyInstance>();
		for (Set<Instance> set : describeInstancesMap.values())
			for (Instance instance : set)
				if (InstanceState.RUNNING.equals(instance.getState()))
					result.add(new ReadOnlyInstance(instance));
		return result;
	}
}
