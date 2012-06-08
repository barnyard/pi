/* (c) British Telecommunications plc, 2009, All Rights Reserved */
package com.bt.pi.api.service;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import javax.annotation.Resource;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.stereotype.Component;

import com.bt.pi.app.common.entities.Instance;
import com.bt.pi.app.common.entities.InstanceState;
import com.bt.pi.app.common.entities.NetworkRule;
import com.bt.pi.app.common.entities.OwnerIdGroupNamePair;
import com.bt.pi.app.common.entities.SecurityGroup;
import com.bt.pi.app.common.entities.User;
import com.bt.pi.app.common.resource.PiQueue;
import com.bt.pi.app.common.resource.PiTopics;
import com.bt.pi.core.application.PubSubMessageContext;
import com.bt.pi.core.application.watcher.task.TaskProcessingQueueContinuation;
import com.bt.pi.core.continuation.UpdateResolver;
import com.bt.pi.core.dht.BlockingDhtReader;
import com.bt.pi.core.dht.BlockingDhtWriter;
import com.bt.pi.core.entity.EntityMethod;
import com.bt.pi.core.entity.PiEntity;
import com.bt.pi.core.id.PId;

@Component
public class SecurityGroupServiceImpl extends ServiceBaseImpl implements SecurityGroupService {
    private static final int TEN = 10;
    private static final int SECURITY_GROUP_DELETE_RETRIES = 5;

    private static final Log LOG = LogFactory.getLog(SecurityGroupServiceImpl.class);
    private SecurityGroupServiceHelper securityGroupServiceHelper;

    public SecurityGroupServiceImpl() {
        super();
        securityGroupServiceHelper = null;
    }

    @Override
    public boolean authoriseIngress(String ownerId, String groupName, List<NetworkRule> networkRules) {
        return securityGroupServiceHelper.updateNetworkRules(ownerId, groupName, networkRules, true);
    }

    @Override
    public boolean revokeIngress(String ownerId, String groupName, List<NetworkRule> networkRules) {
        return securityGroupServiceHelper.updateNetworkRules(ownerId, groupName, networkRules, false);
    }

    @Override
    public boolean createSecurityGroup(final String ownerId, final String groupName, final String groupDescription) {
        LOG.debug(String.format("createSecurityGroup(%s, %s, %s)", ownerId, groupName, groupDescription));
        // create new security group entity
        SecurityGroup securityGroup = new SecurityGroup();
        securityGroup.setDescription(groupDescription);
        securityGroup.setOwnerIdGroupNamePair(new OwnerIdGroupNamePair(ownerId, groupName));

        // get Pi id
        PId id = getPiIdBuilder().getPId(SecurityGroup.getUrl(ownerId, groupName)).forLocalRegion();
        // write to Pi
        BlockingDhtWriter blockingDhtWriter = getDhtClientFactory().createBlockingWriter();
        blockingDhtWriter.update(id, securityGroup, new UpdateResolver<SecurityGroup>() {
            @Override
            public SecurityGroup update(SecurityGroup previousEntity, SecurityGroup newEntity) {
                if (previousEntity == null || previousEntity.isDeleted())
                    return newEntity;
                LOG.warn("Creating new security group, but already exists. Returning existing group.");
                return previousEntity;
            }
        });
        // check what was written to dht
        PiEntity writtenEntity = blockingDhtWriter.getValueWritten();
        if (writtenEntity == null) {
            LOG.warn(String.format("Failed to create security group as no record was written"));
            return false;
        }

        getUserService().addSecurityGroupToUser(ownerId, groupName);

        return true;
    }

    @Override
    public boolean deleteSecurityGroup(String userId, final String groupName) {
        LOG.debug(String.format("deleteSecurityGroup(%s, %s)", userId, groupName));
        if (StringUtils.isBlank(groupName) || groupName.equalsIgnoreCase("default")) {
            return false;
        }

        User user = getUserManagementService().getUser(userId);
        Set<String> securityGroups = user.getSecurityGroupIds();

        if (!securityGroups.contains(groupName))
            return false;

        final PId securityGroupId = getPiIdBuilder().getPId(SecurityGroup.getUrl(userId, groupName)).forLocalRegion();
        BlockingDhtReader reader = getDhtClientFactory().createBlockingReader();
        SecurityGroup groupToDelete = (SecurityGroup) reader.get(securityGroupId);
        if (null == groupToDelete) {
            return removeSecurityGroup(groupName, user, null);
        }

        if (groupToDelete.getInstances() != null && groupToDelete.getInstances().size() > 0 && !allInstancesAreTerminated(groupToDelete)) {
            throw new IllegalStateException(String.format("Security group %s still has non-terminated instances", SecurityGroup.getUrl(userId, groupName)));
        }

        PId removeSecGroupQueueId = getPiIdBuilder().getPiQueuePId(PiQueue.REMOVE_SECURITY_GROUP).forLocalScope(PiQueue.REMOVE_SECURITY_GROUP.getNodeScope());
        final CountDownLatch latch = new CountDownLatch(1);
        getTaskProcessingQueueHelper().addUrlToQueue(removeSecGroupQueueId, groupToDelete.getUrl(), SECURITY_GROUP_DELETE_RETRIES, new TaskProcessingQueueContinuation() {
            @Override
            public void receiveResult(String uri, String nodeId) {
                latch.countDown();
            }
        });

        // seems like we might as well still do the subsequent work but at least we now get visibility of
        // failure to add to the queue
        try {
            if (!latch.await(TEN, TimeUnit.SECONDS)) {
                LOG.warn("failed to add delete task to task processing queue");
            }
        } catch (InterruptedException e) {
            LOG.warn("latch await interrupted", e);
        }

        SecurityGroup securityGroupWritten = updateSecurityGroup(groupName, securityGroupId);
        return removeSecurityGroup(groupName, user, securityGroupWritten);
    }

    private SecurityGroup updateSecurityGroup(final String groupName, final PId securityGroupId) {
        final BlockingDhtWriter writer = getDhtClientFactory().createBlockingWriter();
        writer.update(securityGroupId, null, new UpdateResolver<SecurityGroup>() {
            @Override
            public SecurityGroup update(SecurityGroup existingEntity, SecurityGroup requestedEntity) {
                existingEntity.removeNetworkRules();
                existingEntity.setDeleted(true);
                return existingEntity;
            }
        });

        SecurityGroup securityGroupWritten = (SecurityGroup) writer.getValueWritten();
        if (securityGroupWritten == null)
            throw new IllegalStateException(String.format("SecurityGroup %s cannot be deleted as it is still in use", groupName));
        return securityGroupWritten;
    }

    private boolean allInstancesAreTerminated(SecurityGroup groupToDelete) {
        LOG.debug(String.format("allInstancesAreTerminated(%s)", groupToDelete));
        Set<String> instanceIds = groupToDelete.getInstances().keySet();

        for (String instanceId : instanceIds) {
            PId instancePiId = securityGroupServiceHelper.getInstancePiId(instanceId);

            Instance instance = (Instance) getDhtClientFactory().createBlockingReader().get(instancePiId);
            if (!instance.getState().equals(InstanceState.TERMINATED)) {
                return false;
            }
        }

        return true;
    }

    private boolean removeSecurityGroup(final String groupName, User user, SecurityGroup groupToDelete) {
        LOG.debug(String.format("removeSecurityGroup(%s, %s, %s)", groupName, user, groupToDelete));

        // remove the group from the user
        getUserService().removeSecurityGroupFromUser(user.getUsername(), groupName);

        // send message to delete
        if (null != groupToDelete) {
            PubSubMessageContext ctxt = getApiApplicationManager().newLocalPubSubMessageContext(PiTopics.NETWORK_MANAGERS_IN_REGION);
            ctxt.publishContent(EntityMethod.DELETE, groupToDelete);
        }

        // delete user from group
        User writtenUser = getUserManagementService().getUser(user.getUsername());
        return !writtenUser.getSecurityGroupIds().contains(groupName);
    }

    @Override
    public List<SecurityGroup> describeSecurityGroups(String ownerId, List<String> securityGroupNames) {
        LOG.debug(String.format("describeSecurityGroups(%s, %s)", ownerId, securityGroupNames));

        List<SecurityGroup> securityGroups = new ArrayList<SecurityGroup>();

        User user = getUserManagementService().getUser(ownerId);
        Set<String> userSecurityGroups = user.getSecurityGroupIds();

        if (null != securityGroupNames && securityGroupNames.size() > 0) {
            for (String securityGroupName : securityGroupNames) {
                if (userSecurityGroups.contains(securityGroupName)) {
                    securityGroups.add(getSecurityGroupFromDht(ownerId, securityGroupName));
                }
            }
        } else {
            Iterator<String> iterator = userSecurityGroups.iterator();
            while (iterator.hasNext()) {
                String securityGroupName = iterator.next();
                securityGroups.add(getSecurityGroupFromDht(ownerId, securityGroupName));
            }
        }

        return securityGroups;
    }

    private SecurityGroup getSecurityGroupFromDht(String ownerId, String securityGroupName) {
        PId securityGroupPastryId = getPiIdBuilder().getPId(SecurityGroup.getUrl(ownerId, securityGroupName)).forLocalRegion();
        return (SecurityGroup) getDhtClientFactory().createBlockingReader().get(securityGroupPastryId);
    }

    @Resource
    public void setSecurityGroupServiceHelper(SecurityGroupServiceHelper securityGroupServiceHelper2) {
        this.securityGroupServiceHelper = securityGroupServiceHelper2;
    }
}
