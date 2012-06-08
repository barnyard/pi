package com.bt.pi.app.instancemanager.handlers;

import javax.annotation.Resource;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.stereotype.Component;

import com.bt.pi.app.common.entities.Instance;
import com.bt.pi.app.common.entities.InstanceAction;
import com.bt.pi.app.common.id.PiIdBuilder;
import com.bt.pi.app.common.resource.PiQueue;
import com.bt.pi.core.application.MessageContext;
import com.bt.pi.core.application.MessageContextFactory;
import com.bt.pi.core.application.watcher.task.TaskProcessingQueueContinuation;
import com.bt.pi.core.application.watcher.task.TaskProcessingQueueHelper;
import com.bt.pi.core.continuation.PiContinuation;
import com.bt.pi.core.dht.DhtClientFactory;
import com.bt.pi.core.entity.EntityMethod;
import com.bt.pi.core.id.PId;

@Component
public class PauseInstanceTaskProcessingQueueContinuation implements TaskProcessingQueueContinuation {
    private static final Log LOG = LogFactory.getLog(PauseInstanceTaskProcessingQueueContinuation.class);
    @Resource
    private PiIdBuilder piIdBuilder;
    @Resource
    private TaskProcessingQueueHelper taskProcessingQueueHelper;
    @Resource
    private DhtClientFactory dhtClientFactory;
    @Resource(type = InstanceManagerApplication.class)
    private MessageContextFactory messageContextFactory;

    public PauseInstanceTaskProcessingQueueContinuation() {
        taskProcessingQueueHelper = null;
        piIdBuilder = null;
        dhtClientFactory = null;
        messageContextFactory = null;
    }

    @Override
    public void receiveResult(final String uri, String nodeId) {
        LOG.debug(String.format("Received queue item for terminate instance: Url: %s, NodeId: %s", uri, nodeId));

        PId terminateInstanceQueueId = piIdBuilder.getPiQueuePId(PiQueue.PAUSE_INSTANCE).forLocalScope(PiQueue.PAUSE_INSTANCE.getNodeScope());
        taskProcessingQueueHelper.setNodeIdOnUrl(terminateInstanceQueueId, uri, nodeId);

        final PId instancePastryId = piIdBuilder.getPIdForEc2AvailabilityZone(uri);

        dhtClientFactory.createReader().getAsync(instancePastryId, new PiContinuation<Instance>() {
            @Override
            public void handleResult(Instance result) {
                if (null == result) {
                    LOG.warn("Unable to get the instance:" + uri);
                    return;
                }

                LOG.debug(String.format("Sending instance: %s to InstanceManagerApplication: %s", result.getInstanceId(), result.getNodeId()));
                MessageContext instanceMessageContext = messageContextFactory.newMessageContext();
                result.setActionRequired(InstanceAction.PAUSE);
                instanceMessageContext.routePiMessageToApplication(instancePastryId, EntityMethod.UPDATE, result, InstanceManagerApplication.APPLICATION_NAME);
            }
        });
    }
}
