package com.bt.pi.app.instancemanager.handlers;

import javax.annotation.Resource;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.stereotype.Component;

import com.bt.pi.app.common.entities.Instance;
import com.bt.pi.app.common.entities.InstanceState;
import com.bt.pi.app.common.id.PiIdBuilder;
import com.bt.pi.app.common.resource.PiQueue;
import com.bt.pi.app.common.resource.PiTopics;
import com.bt.pi.core.application.PubSubMessageContext;
import com.bt.pi.core.application.watcher.task.TaskProcessingQueueContinuation;
import com.bt.pi.core.application.watcher.task.TaskProcessingQueueHelper;
import com.bt.pi.core.continuation.UpdateResolvingPiContinuation;
import com.bt.pi.core.dht.DhtClientFactory;
import com.bt.pi.core.dht.DhtWriter;
import com.bt.pi.core.entity.EntityMethod;
import com.bt.pi.core.id.PId;

@Component
public class RunInstanceTaskProcessingQueueContinuation implements TaskProcessingQueueContinuation {
    private static final Log LOG = LogFactory.getLog(RunInstanceTaskProcessingQueueContinuation.class);
    @Resource
    private DhtClientFactory dhtClientFactory;
    @Resource
    private InstanceManagerApplication instanceManagerApplication;
    @Resource
    private TaskProcessingQueueHelper taskProcessingQueueHelper;
    @Resource
    private PiIdBuilder piIdBuilder;

    public RunInstanceTaskProcessingQueueContinuation() {
        this.dhtClientFactory = null;
        this.instanceManagerApplication = null;
        this.taskProcessingQueueHelper = null;
        this.piIdBuilder = null;
    }

    @Override
    public void receiveResult(final String url, final String nodeId) {
        LOG.debug(String.format("receiveResult(%s, %s)", url, nodeId));
        PId id = piIdBuilder.getPIdForEc2AvailabilityZone(url);

        DhtWriter writer = dhtClientFactory.createWriter();
        writer.update(id, new UpdateResolvingPiContinuation<Instance>() {
            @Override
            public Instance update(Instance instance, Instance requestedEntity) {
                if (null == instance) {
                    LOG.warn(String.format("Unable to get instance: %s.", url));
                    return null;
                }
                if (instance.getState() != InstanceState.PENDING) {
                    LOG.warn(String.format("Removing instance %s from queue %s as instance state is not pending.", instance, PiQueue.RUN_INSTANCE));
                    PId runInstanceQueueId = piIdBuilder.getPiQueuePId(PiQueue.RUN_INSTANCE).forLocalScope(PiQueue.RUN_INSTANCE.getNodeScope());
                    taskProcessingQueueHelper.removeUrlFromQueue(runInstanceQueueId, instance.getUrl());
                    return null;
                }

                instance.setNodeId(null);
                return instance;
            }

            @Override
            public void handleResult(Instance instance) {
                if (instance != null) {
                    LOG.debug("Sending anycast message for instance:" + instance.getInstanceId());
                    PubSubMessageContext messageContext = instanceManagerApplication.newLocalPubSubMessageContext(PiTopics.RUN_INSTANCE);
                    messageContext.randomAnycast(EntityMethod.CREATE, instance);
                }
            }
        });
    }
}
