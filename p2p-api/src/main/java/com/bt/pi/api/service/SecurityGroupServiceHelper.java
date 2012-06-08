package com.bt.pi.api.service;

import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.stereotype.Component;

import com.bt.pi.app.common.entities.Instance;
import com.bt.pi.app.common.entities.NetworkRule;
import com.bt.pi.app.common.entities.SecurityGroup;
import com.bt.pi.app.common.resource.PiQueue;
import com.bt.pi.app.common.resource.PiTopics;
import com.bt.pi.core.application.PubSubMessageContext;
import com.bt.pi.core.application.watcher.task.TaskProcessingQueueContinuation;
import com.bt.pi.core.continuation.UpdateResolver;
import com.bt.pi.core.dht.BlockingDhtWriter;
import com.bt.pi.core.entity.EntityMethod;
import com.bt.pi.core.id.PId;

@Component
public class SecurityGroupServiceHelper extends ServiceHelperBase {
    private static final Log LOG = LogFactory.getLog(SecurityGroupServiceHelper.class);
    private static final int SECURITY_GROUP_UPDATE_RETRIES = 5;

    public SecurityGroupServiceHelper() {
    }

    public PId getInstancePiId(String instanceId) {
        LOG.debug(String.format("getInstancePiId(%s)", instanceId));
        return getPiIdBuilder().getPIdForEc2AvailabilityZone(Instance.getUrl(instanceId));
    }

    public boolean updateNetworkRules(String ownerId, String groupName, List<NetworkRule> networkRules, boolean addRules) {
        String securityGroupUrl = SecurityGroup.getUrl(ownerId, groupName);
        PId securityGroupId = getPiIdBuilder().getPId(securityGroupUrl).forLocalRegion();
        BlockingDhtWriter blockingDhtWriter = getDhtClientFactory().createBlockingWriter();
        blockingDhtWriter.update(securityGroupId, null, new SecurityGroupNetworkRuleResolver(networkRules, addRules));

        final SecurityGroup securityGroup = (SecurityGroup) blockingDhtWriter.getValueWritten();
        if (securityGroup == null) {
            LOG.warn(String.format("Sec group %s not written when %s network rules", securityGroupUrl, addRules ? "adding" : "removing"));
            throw new NotFoundException(String.format("security group %s:%s not found", ownerId, groupName));
        }

        clearOutAllUnneededFieldsInSecurityGroupForNetworkRulesUpdate(securityGroup);

        // enqueue task
        PId updateSecGroupQueueId = getPiIdBuilder().getPiQueuePId(PiQueue.UPDATE_SECURITY_GROUP).forLocalScope(PiQueue.UPDATE_SECURITY_GROUP.getNodeScope());
        getTaskProcessingQueueHelper().addUrlToQueue(updateSecGroupQueueId, securityGroup.getUrl(), SECURITY_GROUP_UPDATE_RETRIES, new TaskProcessingQueueContinuation() {
            @Override
            public void receiveResult(String uri, String nodeId) {
                PubSubMessageContext ctxt = getApiApplicationManager().newLocalPubSubMessageContext(PiTopics.NETWORK_MANAGERS_IN_REGION);
                ctxt.publishContent(EntityMethod.UPDATE, securityGroup);
            }
        });

        return true;
    }

    private void clearOutAllUnneededFieldsInSecurityGroupForNetworkRulesUpdate(SecurityGroup securityGroup) {
        securityGroup.setDescription(null);
        securityGroup.setDnsAddress(null);
        securityGroup.setInstances(null);
        securityGroup.setNetmask(null);
        securityGroup.setNetworkAddress(null);
        securityGroup.setVlanId(null);
    }

    /**
     * Add the network rules to the existing security group. If security group doesn't exist, do nothing
     */
    static final class SecurityGroupNetworkRuleResolver implements UpdateResolver<SecurityGroup> {
        private List<NetworkRule> networkRules;
        private boolean shouldAdd;

        private SecurityGroupNetworkRuleResolver(List<NetworkRule> aNetworkRules, boolean shouldAddRules) {
            this.networkRules = aNetworkRules;
            this.shouldAdd = shouldAddRules;
        }

        @Override
        public SecurityGroup update(SecurityGroup previousEntity, SecurityGroup newEntity) {
            if (null == previousEntity)
                return null;

            LOG.debug(String.format("Adding new network rules %s, to security group %s", networkRules, previousEntity));
            for (NetworkRule networkRule : networkRules) {
                if (shouldAdd) {
                    previousEntity.addNetworkRule(networkRule);
                } else {
                    previousEntity.removeNetworkRule(networkRule);
                }
            }
            return previousEntity;
        }
    }
}
