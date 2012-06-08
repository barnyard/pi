package com.bt.pi.app.instancemanager.handlers;

import javax.annotation.Resource;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.stereotype.Component;

import com.bt.pi.app.common.entities.Instance;
import com.bt.pi.app.common.entities.InstanceState;
import com.bt.pi.app.common.entities.User;
import com.bt.pi.app.common.resource.PiQueue;
import com.bt.pi.app.instancemanager.images.InstanceImageManager;
import com.bt.pi.app.networkmanager.net.VirtualNetworkBuilder;
import com.bt.pi.core.continuation.UpdateResolvingPiContinuation;
import com.bt.pi.core.dht.DhtWriter;
import com.bt.pi.core.id.PId;

@Component
public class TerminateInstanceHandler extends AbstractHandler {
    public static final int REMOVE_INSTANCE_FROM_USER_ENTITY_TASK_RETRIES = 5;
    private static final Log LOG = LogFactory.getLog(TerminateInstanceHandler.class);
    @Resource
    private InstanceImageManager instanceImageManager;
    @Resource
    private VirtualNetworkBuilder virtualNetworkBuilder;
    @Resource
    private TerminateInstanceEventListener terminateInstanceEventListener;

    public TerminateInstanceHandler() {
        super();
        instanceImageManager = null;
        virtualNetworkBuilder = null;
        terminateInstanceEventListener = null;
    }

    public void terminateInstance(final Instance instance) {
        LOG.debug(String.format("terminateInstance(%s)", instance));

        Thread thread = getTaskExecutor().createThread(new Runnable() {
            @Override
            public void run() {
                try {
                    LOG.debug(String.format("Running instance termination task for %s", instance.getInstanceId()));
                    stopNetworkIfNeeded(instance);
                    updateInstanceToTerminated(instance);
                    instanceImageManager.stopInstance(instance);
                    terminateInstanceOnUser(instance);
                    addTaskToRemoveInstanceFromUser(instance);
                    removeTerminateInstanceTaskFromQueue(instance);
                    terminateInstanceEventListener.instanceTerminated(instance);
                } catch (Throwable t) {
                    LOG.error(t.getMessage(), t);
                }
            }

            private void removeTerminateInstanceTaskFromQueue(final Instance instance) {
                PId terminateInstanceQueueId = getPiIdBuilder().getPiQueuePId(PiQueue.TERMINATE_INSTANCE).forLocalScope(PiQueue.TERMINATE_INSTANCE.getNodeScope());
                getTaskProcessingQueueHelper().removeUrlFromQueue(terminateInstanceQueueId, instance.getUrl());
            }

            private void addTaskToRemoveInstanceFromUser(final Instance instance) {
                LOG.debug(String.format("Adding task to remove instance %s from the user to the queues", instance.getInstanceId()));
                getTaskProcessingQueueHelper().addUrlToQueue(getPiIdBuilder().getPiQueuePId(PiQueue.REMOVE_INSTANCE_FROM_USER).forLocalScope(PiQueue.REMOVE_INSTANCE_FROM_USER.getNodeScope()), instance.getUrl(),
                        REMOVE_INSTANCE_FROM_USER_ENTITY_TASK_RETRIES);
            }

        });
        thread.setPriority(Thread.MIN_PRIORITY);
        thread.start();
    }

    private void stopNetworkIfNeeded(Instance instance) {
        LOG.debug(String.format("stopNetwork(%s)", instance));
        virtualNetworkBuilder.tearDownVirtualNetworkForInstance(instance.getVlanId(), instance.getInstanceId());
    }

    private void terminateInstanceOnUser(final Instance instance) {
        LOG.debug(String.format("terminateInstanceOnUser(%s)", instance.getInstanceId()));
        final String userid = instance.getUserId();
        if (null == userid)
            return;
        DhtWriter dhtWriter = getDhtClientFactory().createWriter();
        PId userPastryId = getPiIdBuilder().getPId(User.getUrl(userid));
        dhtWriter.update(userPastryId, new UpdateResolvingPiContinuation<User>() {
            @Override
            public User update(User existingEntity, User requestedEntity) {
                boolean updated = existingEntity.terminateInstance(instance.getInstanceId());
                if (updated)
                    return existingEntity;
                return null;
            }

            @Override
            public void handleResult(User result) {
                if (result != null)
                    LOG.debug(String.format("User: %s updated after termination.", userid));
            }
        });
    }

    private void updateInstanceToTerminated(Instance instance) {
        DhtWriter dhtWriter = getDhtClientFactory().createWriter();
        PId instancePastryId = getPiIdBuilder().getPIdForEc2AvailabilityZone(instance);
        dhtWriter.update(instancePastryId, new UpdateResolvingPiContinuation<Instance>() {
            @Override
            public void handleResult(Instance result) {
                LOG.debug(String.format("Instance: %s updated after termination.", result));
            }

            @Override
            public Instance update(Instance existingEntity, Instance requestedEntity) {
                existingEntity.setState(InstanceState.TERMINATED);
                return existingEntity;
            }
        });
    }
}
