package com.bt.pi.app.networkmanager.handlers;

import javax.annotation.Resource;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.stereotype.Component;

import com.bt.pi.app.common.entities.Instance;
import com.bt.pi.app.common.entities.PublicIpAddress;
import com.bt.pi.app.common.entities.SecurityGroup;
import com.bt.pi.app.common.id.PiIdBuilder;
import com.bt.pi.app.common.resource.PiQueue;
import com.bt.pi.app.networkmanager.addressing.PublicIpAddressManager;
import com.bt.pi.core.application.watcher.task.TaskProcessingQueueHelper;
import com.bt.pi.core.continuation.GenericContinuation;
import com.bt.pi.core.continuation.UpdateResolvingPiContinuation;
import com.bt.pi.core.dht.DhtClientFactory;
import com.bt.pi.core.dht.DhtWriter;
import com.bt.pi.core.id.PId;

@Component
public class DisassociateAddressFromInstanceHandler {
    private static final Log LOG = LogFactory.getLog(DisassociateAddressFromInstanceHandler.class);
    private PublicIpAddressManager publicIpAddressManager;
    private TaskProcessingQueueHelper taskProcessingQueueHelper;
    private DhtClientFactory dhtClientFactory;
    private PiIdBuilder piIdBuilder;

    public DisassociateAddressFromInstanceHandler() {
        publicIpAddressManager = null;
        taskProcessingQueueHelper = null;
        dhtClientFactory = null;
        piIdBuilder = null;
    }

    @Resource
    public void setPublicIpAddressManager(PublicIpAddressManager aPublicIpAddressManager) {
        this.publicIpAddressManager = aPublicIpAddressManager;
    }

    @Resource
    public void setTaskProcessingQueueHelper(TaskProcessingQueueHelper aTaskProcessingQueueHelper) {
        this.taskProcessingQueueHelper = aTaskProcessingQueueHelper;
    }

    @Resource
    public void setDhtClientFactory(DhtClientFactory aDhtClientFactory) {
        this.dhtClientFactory = aDhtClientFactory;
    }

    @Resource
    public void setPiIdBuilder(PiIdBuilder aPiIdBuilder) {
        this.piIdBuilder = aPiIdBuilder;
    }

    public void handle(final PublicIpAddress addr) {
        LOG.debug(String.format("handle(%s)", addr));
        publicIpAddressManager.disassociatePublicIpAddressFromInstance(addr.getIpAddress(), addr.getInstanceId(), String.format(SecurityGroup.SEC_GROUP_ID_FORMAT_STRING, addr.getOwnerId(), addr.getSecurityGroupName()),
                new GenericContinuation<String>() {
                    @Override
                    public void handleResult(String result) {
                        LOG.debug(String.format("Disssociation of %s from %s returned %s", addr.getIpAddress(), addr.getInstanceId(), result));
                        if (result != null) {
                            persistNewPublicIpAddressOnInstanceRecord(addr, result);
                        } else {
                            LOG.info(String.format("Leaving failed address disassociation task (%s to %s) on queue for reprocessing", addr.getIpAddress(), addr.getInstanceId()));
                        }
                    }
                });
    }

    private void persistNewPublicIpAddressOnInstanceRecord(final PublicIpAddress addr, final String newPublicIpAddress) {
        LOG.debug(String.format("persistNewPublicIpAddressOnInstanceRecord(%s, %s)", addr, newPublicIpAddress));
        PId instanceDhtId = piIdBuilder.getPIdForEc2AvailabilityZone(Instance.getUrl(addr.getInstanceId()));
        DhtWriter writer = dhtClientFactory.createWriter();
        writer.update(instanceDhtId, new UpdateResolvingPiContinuation<Instance>() {
            @Override
            public Instance update(Instance existingEntity, Instance requestedEntity) {
                existingEntity.setPublicIpAddress(newPublicIpAddress);
                return existingEntity;
            }

            @Override
            public void handleResult(Instance result) {
                LOG.debug(String.format("Write of instance %s after disassociation of %s wrote %s", addr.getInstanceId(), addr.getIpAddress(), result));
                String taskUri = QueueTaskUriHelper.getUriForDisassociateAddress(addr);
                LOG.info(String.format("Removing address disassociation task (%s to %s) with uri %s from queue after success", addr.getIpAddress(), addr.getInstanceId(), taskUri));
                taskProcessingQueueHelper.removeUrlFromQueue(piIdBuilder.getPiQueuePId(PiQueue.DISASSOCIATE_ADDRESS).forLocalScope(PiQueue.DISASSOCIATE_ADDRESS.getNodeScope()), taskUri);
            }
        });
    }
}
