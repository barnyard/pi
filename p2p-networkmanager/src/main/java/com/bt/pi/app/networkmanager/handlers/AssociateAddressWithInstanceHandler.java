package com.bt.pi.app.networkmanager.handlers;

import javax.annotation.Resource;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.stereotype.Component;

import com.bt.pi.app.common.entities.PublicIpAddress;
import com.bt.pi.app.common.entities.SecurityGroup;
import com.bt.pi.app.common.id.PiIdBuilder;
import com.bt.pi.app.common.resource.PiQueue;
import com.bt.pi.app.networkmanager.addressing.PublicIpAddressManager;
import com.bt.pi.core.application.watcher.task.TaskProcessingQueueHelper;
import com.bt.pi.core.continuation.GenericContinuation;

@Component
public class AssociateAddressWithInstanceHandler {
    private static final Log LOG = LogFactory.getLog(AssociateAddressWithInstanceHandler.class);
    private PublicIpAddressManager publicIpAddressManager;
    private TaskProcessingQueueHelper taskProcessingQueueHelper;
    private PiIdBuilder piIdBuilder;

    public AssociateAddressWithInstanceHandler() {
        publicIpAddressManager = null;
        taskProcessingQueueHelper = null;
        piIdBuilder = null;
    }

    @Resource
    public void setPiIdBuilder(PiIdBuilder aPiIdBuilder) {
        this.piIdBuilder = aPiIdBuilder;
    }

    @Resource
    public void setPublicIpAddressManager(PublicIpAddressManager anAddressManager) {
        this.publicIpAddressManager = anAddressManager;
    }

    @Resource
    public void setTaskProcessingQueueHelper(TaskProcessingQueueHelper aTaskProcessingQueueHelper) {
        this.taskProcessingQueueHelper = aTaskProcessingQueueHelper;
    }

    public void handle(final PublicIpAddress addr) {
        LOG.debug(String.format("handle(%s)", addr));
        publicIpAddressManager.associatePublicIpAddressWithInstance(addr.getIpAddress(), addr.getInstanceId(), String.format(SecurityGroup.SEC_GROUP_ID_FORMAT_STRING, addr.getOwnerId(), addr.getSecurityGroupName()),
                new GenericContinuation<Boolean>() {
                    @Override
                    public void handleResult(Boolean result) {
                        LOG.debug(String.format("Association of %s to %s returned %s", addr.getIpAddress(), addr.getInstanceId(), result));
                        if (result) {
                            String taskUri = QueueTaskUriHelper.getUriForAssociateAddress(addr);
                            LOG.info(String.format("Removing address association task (%s to %s) with uri %s from queue after success", addr.getIpAddress(), addr.getInstanceId(), taskUri));
                            PiQueue queue = PiQueue.ASSOCIATE_ADDRESS;
                            taskProcessingQueueHelper.removeUrlFromQueue(piIdBuilder.getPId(queue.getUrl()).forLocalScope(queue.getNodeScope()), taskUri);
                        } else {
                            LOG.info(String.format("Leaving failed address association task (%s to %s) on queue for reprocessing", addr.getIpAddress(), addr.getInstanceId()));
                        }
                    }
                });
    }
}
