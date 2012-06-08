package com.bt.pi.app.networkmanager;

import javax.annotation.Resource;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.stereotype.Component;

import com.bt.pi.app.common.entities.Instance;
import com.bt.pi.app.common.entities.SecurityGroup;
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
public class InstanceNetworkManagerTeardownTaskProcessingQueueContinuation implements TaskProcessingQueueContinuation {
    private static final Log LOG = LogFactory.getLog(InstanceNetworkManagerTeardownTaskProcessingQueueContinuation.class);

    @Resource
    private PiIdBuilder piIdBuilder;
    @Resource
    private TaskProcessingQueueHelper taskProcessingQueueHelper;
    @Resource
    private DhtClientFactory dhtClientFactory;
    @Resource(type = NetworkManagerApplication.class)
    private MessageContextFactory messageContextFactory;

    public InstanceNetworkManagerTeardownTaskProcessingQueueContinuation() {
        piIdBuilder = null;
        taskProcessingQueueHelper = null;
        dhtClientFactory = null;
    }

    @Override
    public void receiveResult(final String uri, String nodeId) {
        LOG.info(String.format("Received queue item for network manager teardown while terminating instance. Uri: %s, nodeId: %s", uri, nodeId));

        PId instanceNetworkManagerTeardownQueueId = piIdBuilder.getPiQueuePId(PiQueue.INSTANCE_NETWORK_MANAGER_TEARDOWN).forLocalScope(PiQueue.INSTANCE_NETWORK_MANAGER_TEARDOWN.getNodeScope());
        taskProcessingQueueHelper.setNodeIdOnUrl(instanceNetworkManagerTeardownQueueId, uri, nodeId);

        final PId instancePastryId = piIdBuilder.getPIdForEc2AvailabilityZone(uri);
        dhtClientFactory.createReader().getAsync(instancePastryId, new PiContinuation<Instance>() {
            @Override
            public void handleResult(Instance result) {
                if (null == result) {
                    LOG.warn("Unable to get the instance:" + uri);
                    return;
                }

                LOG.debug(String.format("Sending instance: %s to NetworkManagerApplication: %s", result.getInstanceId(), result.getNodeId()));
                int instanceGlobalAvzCode = piIdBuilder.getGlobalAvailabilityZoneCodeFromEc2Id(result.getInstanceId());
                PId securityGroupId = piIdBuilder.getPId(SecurityGroup.getUrl(result.getUserId(), result.getSecurityGroupName())).forGlobalAvailablityZoneCode(instanceGlobalAvzCode);
                LOG.debug(String.format("Sending termination message for inst %s to network app node %s for sec group %s:%s", result.getInstanceId(), result.getUserId(), securityGroupId.toStringFull(), result.getSecurityGroupName()));
                MessageContext securityGroupMessageContext = messageContextFactory.newMessageContext();
                securityGroupMessageContext.routePiMessageToApplication(securityGroupId, EntityMethod.DELETE, result, NetworkManagerApplication.APPLICATION_NAME);
            }
        });
    }
}
