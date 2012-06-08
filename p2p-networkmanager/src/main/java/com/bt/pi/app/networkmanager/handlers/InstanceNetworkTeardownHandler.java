/* (c) British Telecommunications plc, 2009, All Rights Reserved */
package com.bt.pi.app.networkmanager.handlers;

import javax.annotation.Resource;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.stereotype.Component;

import com.bt.pi.app.common.entities.Instance;
import com.bt.pi.app.common.entities.SecurityGroup;
import com.bt.pi.app.common.id.PiIdBuilder;
import com.bt.pi.app.common.resource.PiQueue;
import com.bt.pi.app.networkmanager.addressing.PublicIpAddressManager;
import com.bt.pi.app.networkmanager.net.NetworkManager;
import com.bt.pi.core.application.ReceivedMessageContext;
import com.bt.pi.core.application.watcher.task.TaskProcessingQueueHelper;
import com.bt.pi.core.continuation.GenericContinuation;
import com.bt.pi.core.id.PId;

@Component
public class InstanceNetworkTeardownHandler {
    private static final Log LOG = LogFactory.getLog(InstanceNetworkTeardownHandler.class);

    @Resource
    private NetworkManager networkManager;
    @Resource
    private PublicIpAddressManager publicIpAddressManager;
    @Resource
    private PiIdBuilder piIdBuilder;
    @Resource
    private TaskProcessingQueueHelper taskProcessingQueueHelper;

    public InstanceNetworkTeardownHandler() {
        networkManager = null;
        publicIpAddressManager = null;
        piIdBuilder = null;
        taskProcessingQueueHelper = null;
    }

    public void handle(final Instance aInstance, final ReceivedMessageContext receivedMessageContext) {
        LOG.debug(String.format("handle(%s)", aInstance));

        publicIpAddressManager.releasePublicIpAddressForInstance(aInstance.getInstanceId(), String.format(SecurityGroup.SEC_GROUP_ID_FORMAT_STRING, aInstance.getUserId(), aInstance.getSecurityGroupName()), new GenericContinuation<Boolean>() {
            @Override
            public void handleResult(Boolean result) {
                LOG.debug(String.format("Result of public address release as part of instance teardown for %s: %s", aInstance.getInstanceId(), result));
                networkManager.releaseNetworkForInstance(aInstance.getUserId(), aInstance.getSecurityGroupName(), aInstance.getInstanceId());
                removeTerminateInstanceTaskFromQueue(aInstance);
            }

            private void removeTerminateInstanceTaskFromQueue(final Instance instance) {
                PId terminateInstanceQueueId = piIdBuilder.getPiQueuePId(PiQueue.INSTANCE_NETWORK_MANAGER_TEARDOWN).forLocalScope(PiQueue.INSTANCE_NETWORK_MANAGER_TEARDOWN.getNodeScope());
                taskProcessingQueueHelper.removeUrlFromQueue(terminateInstanceQueueId, instance.getUrl());
            }
        });
    }
}
