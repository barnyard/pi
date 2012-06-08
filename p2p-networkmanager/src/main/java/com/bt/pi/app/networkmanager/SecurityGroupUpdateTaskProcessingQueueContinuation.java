package com.bt.pi.app.networkmanager;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.stereotype.Component;

import com.bt.pi.app.common.resource.PiQueue;
import com.bt.pi.core.application.watcher.task.TaskProcessingQueueContinuation;
import com.bt.pi.core.entity.EntityMethod;

@Component
public class SecurityGroupUpdateTaskProcessingQueueContinuation extends SecurityGroupTaskProcessingQueueContinuation implements TaskProcessingQueueContinuation {
    private static final Log LOG = LogFactory.getLog(SecurityGroupUpdateTaskProcessingQueueContinuation.class);

    public SecurityGroupUpdateTaskProcessingQueueContinuation() {
        // TODO Auto-generated constructor stub
    }

    @Override
    public void receiveResult(String uri, String nodeId) {
        LOG.info(String.format("Received security group update request %s as node %s", uri, nodeId));
        sendMessageToNetworkManager(uri, nodeId, PiQueue.UPDATE_SECURITY_GROUP, EntityMethod.UPDATE);
    }
}
