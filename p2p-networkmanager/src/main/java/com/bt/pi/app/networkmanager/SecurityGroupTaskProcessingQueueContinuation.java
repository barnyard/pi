package com.bt.pi.app.networkmanager;

import javax.annotation.Resource;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.stereotype.Component;

import com.bt.pi.app.common.entities.SecurityGroup;
import com.bt.pi.app.common.id.PiIdBuilder;
import com.bt.pi.app.common.resource.PiQueue;
import com.bt.pi.core.application.MessageContext;
import com.bt.pi.core.application.MessageContextFactory;
import com.bt.pi.core.application.watcher.task.TaskProcessingQueueHelper;
import com.bt.pi.core.continuation.PiContinuation;
import com.bt.pi.core.dht.DhtClientFactory;
import com.bt.pi.core.dht.DhtReader;
import com.bt.pi.core.entity.EntityMethod;
import com.bt.pi.core.id.PId;

@Component
public class SecurityGroupTaskProcessingQueueContinuation {
    private static final Log LOG = LogFactory.getLog(SecurityGroupTaskProcessingQueueContinuation.class);

    private PiIdBuilder piIdBuilder;

    private MessageContextFactory messageContextFactory;

    private DhtClientFactory dhtClientFactory;

    private TaskProcessingQueueHelper taskProcessingQueueHelper;

    public SecurityGroupTaskProcessingQueueContinuation() {
        piIdBuilder = null;
        messageContextFactory = null;
        dhtClientFactory = null;
        taskProcessingQueueHelper = null;
    }

    @Resource
    public void setPiIdBuilder(PiIdBuilder aPiIdBuilder) {
        piIdBuilder = aPiIdBuilder;
    }

    @Resource
    public void setTaskProcessingQueueHelper(TaskProcessingQueueHelper aTaskProcessingQueueHelper) {
        taskProcessingQueueHelper = aTaskProcessingQueueHelper;
    }

    @Resource
    public void setDhtClientFactory(DhtClientFactory aDhtClientFactory) {
        dhtClientFactory = aDhtClientFactory;
    }

    @Resource(type = NetworkManagerApplication.class)
    public void setMessageContextFactory(MessageContextFactory aMessageContextFactory) {
        messageContextFactory = aMessageContextFactory;
    }

    protected void sendMessageToNetworkManager(final String uri, String nodeId, PiQueue piQueue, final EntityMethod entityMethod) {
        taskProcessingQueueHelper.setNodeIdOnUrl(piIdBuilder.getPiQueuePId(piQueue).forLocalScope(piQueue.getNodeScope()), uri, nodeId);

        PId securityGroupId = piIdBuilder.getPId(uri).forLocalRegion();

        DhtReader dhtReader = dhtClientFactory.createReader();
        dhtReader.getAsync(securityGroupId, new PiContinuation<SecurityGroup>() {
            @Override
            public void handleResult(SecurityGroup result) {
                final PId networkManagerApplicationPiId = piIdBuilder.getPId(result).forLocalAvailabilityZone();
                LOG.debug(String.format("Sending security group: %s to NetworkManagerApplication", result));
                MessageContext newMessageContext = messageContextFactory.newMessageContext();
                newMessageContext.routePiMessage(networkManagerApplicationPiId, entityMethod, result);
            }
        });
    }
}
