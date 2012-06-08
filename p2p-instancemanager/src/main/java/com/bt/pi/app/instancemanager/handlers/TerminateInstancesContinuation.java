/* (c) British Telecommunications plc, 2009, All Rights Reserved */
package com.bt.pi.app.instancemanager.handlers;

import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.bt.pi.app.common.entities.Instance;
import com.bt.pi.app.common.entities.InstanceState;
import com.bt.pi.app.common.entities.SecurityGroup;
import com.bt.pi.app.common.id.PiIdBuilder;
import com.bt.pi.app.networkmanager.NetworkManagerApplication;
import com.bt.pi.core.application.MessageContext;
import com.bt.pi.core.application.MessageContextFactory;
import com.bt.pi.core.continuation.UpdateResolvingPiContinuation;
import com.bt.pi.core.entity.EntityMethod;
import com.bt.pi.core.id.PId;

public class TerminateInstancesContinuation extends UpdateResolvingPiContinuation<Instance> {
    private static final Log LOG = LogFactory.getLog(TerminateInstancesContinuation.class);
    private PiIdBuilder piIdBuilder;
    private MessageContextFactory messageContextFactory;
    private Map<String, InstanceStateTransition> instanceStatusMap;

    public TerminateInstancesContinuation(PiIdBuilder aPiIdBuilder, MessageContextFactory aMessageContextFactory, Map<String, InstanceStateTransition> anInstanceStatusMap) {
        piIdBuilder = aPiIdBuilder;
        messageContextFactory = aMessageContextFactory;
        instanceStatusMap = anInstanceStatusMap;
    }

    @Override
    public Instance update(Instance existingEntity, Instance requestedEntity) {
        LOG.debug(String.format("update(Existing Instance - %s,Requested Instance - %s)", existingEntity, requestedEntity));
        InstanceStateTransition transition = new InstanceStateTransition();
        Instance entityToReturn = null;
        if (existingEntity != null) {
            transition.setPreviousState(existingEntity.getState());
            // we set this here in case we can't write the state want.
            // this way the user doesn't get back a null. :D.
            transition.setNextState(existingEntity.getState());
            instanceStatusMap.put(existingEntity.getInstanceId(), transition);
            if (existingEntity.getNodeId() != null && existingEntity.getState() != InstanceState.SHUTTING_DOWN && existingEntity.getState() != InstanceState.TERMINATED) {
                existingEntity.setState(InstanceState.SHUTTING_DOWN);
                entityToReturn = existingEntity;
            } else if (existingEntity.getState() != InstanceState.TERMINATED) {
                LOG.info(String.format("Instance %s is provisionally being terminated before an Instance Manager has claimed it by stamping a node id onto it", existingEntity.getInstanceId()));
                existingEntity.setState(InstanceState.TERMINATED);
                entityToReturn = existingEntity;
            }
        } else {
            LOG.warn(String.format("Expected non-null instance"));
        }
        return entityToReturn;
    }

    @Override
    public void handleResult(Instance result) {
        LOG.debug(String.format("handleResult(PiEntity - %s)", result));
        if (result != null) {
            if (result.getNodeId() != null) {
                LOG.debug(String.format("Sending termination message for inst %s to node %s", result.getInstanceId(), result.getNodeId()));
                PId instanceRecordId = piIdBuilder.getNodeIdFromNodeId(result.getNodeId());
                MessageContext instanceMessageContext = messageContextFactory.newMessageContext();
                instanceMessageContext.routePiMessageToApplication(instanceRecordId, EntityMethod.DELETE, result, InstanceManagerApplication.APPLICATION_NAME);
            } else {
                LOG.debug(String.format("Null node id detected, not sending termination message for %s to instance manager", result.getInstanceId()));
            }

            int instanceGlobalAvzCode = piIdBuilder.getGlobalAvailabilityZoneCodeFromEc2Id(result.getInstanceId());
            PId securityGroupId = piIdBuilder.getPId(SecurityGroup.getUrl(result.getUserId(), result.getSecurityGroupName())).forGlobalAvailablityZoneCode(instanceGlobalAvzCode);
            LOG.debug(String.format("Sending termination message for inst %s to network app node %s for sec group %s:%s", result.getInstanceId(), result.getUserId(), securityGroupId.toStringFull(), result.getSecurityGroupName()));
            MessageContext securityGroupMessageContext = messageContextFactory.newMessageContext();
            securityGroupMessageContext.routePiMessageToApplication(securityGroupId, EntityMethod.DELETE, result, NetworkManagerApplication.APPLICATION_NAME);
            InstanceStateTransition transition = instanceStatusMap.get(result.getInstanceId());
            if (transition != null) {
                transition.setNextState(result.getState());
            }
        } else {
            LOG.warn("Expected a non-null instance record");
        }
    }
}
