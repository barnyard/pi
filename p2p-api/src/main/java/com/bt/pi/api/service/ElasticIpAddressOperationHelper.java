package com.bt.pi.api.service;

import java.util.Locale;

import javax.annotation.Resource;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.stereotype.Component;

import com.bt.pi.app.common.entities.Instance;
import com.bt.pi.app.common.entities.InstanceState;
import com.bt.pi.app.common.entities.PublicIpAddress;
import com.bt.pi.app.common.entities.SecurityGroup;
import com.bt.pi.app.common.entities.User;
import com.bt.pi.app.common.resource.PiQueue;
import com.bt.pi.app.networkmanager.NetworkManagerApplication;
import com.bt.pi.app.networkmanager.handlers.QueueTaskUriHelper;
import com.bt.pi.core.application.MessageContext;
import com.bt.pi.core.application.MessageContextFactory;
import com.bt.pi.core.application.watcher.task.TaskProcessingQueueContinuation;
import com.bt.pi.core.continuation.UpdateResolver;
import com.bt.pi.core.dht.BlockingDhtWriter;
import com.bt.pi.core.entity.EntityMethod;
import com.bt.pi.core.id.PId;
import com.bt.pi.core.util.MutableString;

@Component
public class ElasticIpAddressOperationHelper extends ServiceHelperBase {
    private static final Log LOG = LogFactory.getLog(ElasticIpAddressOperationHelper.class);
    private static final int MAX_QUEUE_RETRIES = 5;
    private MessageContextFactory messageContextFactory;

    public ElasticIpAddressOperationHelper() {
        messageContextFactory = null;
    }

    @Resource(type = ApiApplicationManager.class)
    public void setMessageContextFactory(MessageContextFactory aMessageContextFactory) {
        this.messageContextFactory = aMessageContextFactory;
    }

    protected String writePublicIpToInstanceRecord(final String publicIpAddress, final String instanceId) {
        LOG.debug(String.format("writePublicIpToInstanceRecord(%s, %s)", publicIpAddress, instanceId));
        PId instanceRecordId = getPiIdBuilder().getPIdForEc2AvailabilityZone(Instance.getUrl(instanceId));
        BlockingDhtWriter dhtWriterForInstance = getDhtClientFactory().createBlockingWriter();
        final MutableString securityGroupName = new MutableString();
        dhtWriterForInstance.update(instanceRecordId, null, new UpdateResolver<Instance>() {
            @Override
            public Instance update(Instance existingEntity, Instance requestedEntity) {
                LOG.debug(String.format("Provisionally setting public ip addr of instance %s to %s", instanceId, publicIpAddress));
                securityGroupName.set(existingEntity.getSecurityGroupName());

                existingEntity.setPublicIpAddress(publicIpAddress);
                return existingEntity;
            }
        });

        if (dhtWriterForInstance.getValueWritten() == null)
            throw new NotFoundException(String.format("Failed to update instance %s", instanceId));

        return securityGroupName.get();
    }

    protected void enqueueTask(PublicIpAddress publicIpAddressEntity, PiQueue queue, TaskProcessingQueueContinuation continuation) {
        LOG.debug(String.format("enqueueTask(%s, %s)", publicIpAddressEntity, queue));
        String taskUri = QueueTaskUriHelper.getUriForAssociateAddress(publicIpAddressEntity);
        LOG.debug(String.format("Adding elastic ip address task to queue for addr %s to instance %s, with uri %s", publicIpAddressEntity.getIpAddress(), publicIpAddressEntity.getInstanceId(), taskUri));
        int globalAvzCode = getPiIdBuilder().getGlobalAvailabilityZoneCodeFromEc2Id(publicIpAddressEntity.getInstanceId());
        int regionCode = PId.getRegionCodeFromGlobalAvailabilityZoneCode(globalAvzCode);
        PId queueId = getPiIdBuilder().getPId(queue.getUrl()).forRegion(regionCode);
        getTaskProcessingQueueHelper().addUrlToQueue(queueId, taskUri, MAX_QUEUE_RETRIES, continuation);
    }

    protected void sendElasticIpAddressRequestToNetworkManagerNode(PublicIpAddress publicIpAddressEntity, EntityMethod entityMethod) {
        LOG.debug(String.format("sendElasticIpAddressRequestToNetworkManagerNode(%s, %s)", publicIpAddressEntity, entityMethod));

        String secGroupUri = SecurityGroup.getUrl(publicIpAddressEntity.getOwnerId(), publicIpAddressEntity.getSecurityGroupName());
        int publicAddressInstanceGlobalAvzCode = getPiIdBuilder().getGlobalAvailabilityZoneCodeFromEc2Id(publicIpAddressEntity.getInstanceId());
        PId networkManagerAppId = getPiIdBuilder().getPId(secGroupUri).forGlobalAvailablityZoneCode(publicAddressInstanceGlobalAvzCode);
        MessageContext messageContext = messageContextFactory.newMessageContext();
        messageContext.routePiMessageToApplication(networkManagerAppId, entityMethod, publicIpAddressEntity, NetworkManagerApplication.APPLICATION_NAME);
    }

    protected void sendAssociateElasticIpAddressRequestToSecurityGroupNode(PublicIpAddress publicIpAddressEntity) {
        sendElasticIpAddressRequestToNetworkManagerNode(publicIpAddressEntity, EntityMethod.CREATE);
    }

    protected void sendDisassociateElasticIpAddressRequestToSecurityGroupNode(PublicIpAddress publicIpAddressEntity) {
        sendElasticIpAddressRequestToNetworkManagerNode(publicIpAddressEntity, EntityMethod.DELETE);
    }

    protected void validateInstanceForUser(String ownerId, String instanceId) {
        PId userRecordId = getPiIdBuilder().getPId(User.getUrl(ownerId));
        User userRecord = getBlockingDhtCache().getReadThrough(userRecordId);
        PId instancePastryId = getPiIdBuilder().getPIdForEc2AvailabilityZone(Instance.getUrl(instanceId));

        Instance instance = getBlockingDhtCache().getReadThrough(instancePastryId);
        if (userRecord == null)
            throw new UserNotFoundException(String.format("User %s not found", ownerId));

        if (instance == null)
            throw new NotFoundException(String.format("Instance %s not found", instanceId));

        if (!userRecord.hasInstance(instanceId))
            throw new NotFoundException(String.format("Instance %s not found for user %s", instanceId, ownerId));

        if (!instance.getState().equals(InstanceState.RUNNING))
            throw new IllegalArgumentException(String.format("Invalid instance state %s for instance %s, should be running", instance.getState().toString().toLowerCase(Locale.getDefault()), instanceId));
    }
}
