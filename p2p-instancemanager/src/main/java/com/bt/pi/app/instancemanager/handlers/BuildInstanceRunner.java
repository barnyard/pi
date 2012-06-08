package com.bt.pi.app.instancemanager.handlers;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.bt.pi.app.common.entities.Instance;
import com.bt.pi.app.common.entities.InstanceState;
import com.bt.pi.app.common.id.PiIdBuilder;
import com.bt.pi.app.common.images.platform.ImagePlatform;
import com.bt.pi.app.common.resource.PiQueue;
import com.bt.pi.app.instancemanager.images.PlatformBuilder;
import com.bt.pi.core.application.resource.ConsumedDhtResourceRegistry;
import com.bt.pi.core.application.watcher.task.TaskProcessingQueueHelper;
import com.bt.pi.core.continuation.UpdateResolvingPiContinuation;
import com.bt.pi.core.id.PId;
import com.bt.pi.core.util.SerialExecutorRunnable;

public class BuildInstanceRunner extends SerialExecutorRunnable {
    private static final Log LOG = LogFactory.getLog(BuildInstanceRunner.class);
    private Instance instance;
    private String key;
    private PiIdBuilder piIdBuilder;
    private PlatformBuilder platformBuilder;
    private SystemResourceState systemResourceState;

    private ConsumedDhtResourceRegistry consumedDhtResourceRegistry;
    private TaskProcessingQueueHelper taskProcessingQueueHelper;

    public BuildInstanceRunner(ConsumedDhtResourceRegistry aConsumedDhtResourceRegistry, Instance anInstance, String aKey, PiIdBuilder aPiBuilder, PlatformBuilder aPlatformBuilder, SystemResourceState aSystemResourceState,
            TaskProcessingQueueHelper aTaskProcessingQueueHelper) {
        super(PiQueue.RUN_INSTANCE.getUrl(), anInstance.getUrl());

        consumedDhtResourceRegistry = aConsumedDhtResourceRegistry;
        instance = anInstance;
        key = aKey;
        piIdBuilder = aPiBuilder;
        platformBuilder = aPlatformBuilder;
        systemResourceState = aSystemResourceState;

        taskProcessingQueueHelper = aTaskProcessingQueueHelper;
    }

    protected ConsumedDhtResourceRegistry getConsumedDhtResourceRegistry() {
        return consumedDhtResourceRegistry;
    }

    protected String getKey() {
        return key;
    }

    @Override
    public void run() {
        try {
            LOG.debug("Building platform for instance: " + instance);
            platformBuilder.build(instance, key);
            // set instance as running.
            updateInstanceStateAndPlatform(InstanceState.RUNNING, instance.getPlatform());
            systemResourceState.unreserveResources(instance.getInstanceId());
            PId queueId = piIdBuilder.getPiQueuePId(PiQueue.RUN_INSTANCE).forLocalScope(PiQueue.RUN_INSTANCE.getNodeScope());
            taskProcessingQueueHelper.removeUrlFromQueue(queueId, Instance.getUrl(instance.getInstanceId()));
        } catch (Throwable t) {
            LOG.error(t.getMessage(), t);
            consumedDhtResourceRegistry.deregisterConsumer(piIdBuilder.getPIdForEc2AvailabilityZone(instance), instance.getInstanceId());
            // leave for Task Processing Queue
        }
    }

    private void updateInstanceStateAndPlatform(final InstanceState instanceState, final ImagePlatform imagePlatform) {
        consumedDhtResourceRegistry.update(piIdBuilder.getPIdForEc2AvailabilityZone(instance), new UpdateResolvingPiContinuation<Instance>() {
            @Override
            public Instance update(Instance existingEntity, Instance requestedEntity) {
                LOG.info(String.format("Updating instance state to: %s and instance platform to: %s", instanceState, imagePlatform));
                existingEntity.setState(instanceState);
                existingEntity.setPlatform(imagePlatform);

                if (instanceState.equals(InstanceState.RUNNING)) {
                    existingEntity.setLaunchTime(System.currentTimeMillis());
                }
                return existingEntity;
            }

            @Override
            public void handleResult(Instance result) {
                LOG.debug("Instance was set to " + instanceState + ". Instance: " + result);
            }
        });
    }
}
