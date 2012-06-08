package com.bt.pi.app.common.entities;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.jackson.annotate.JsonIgnore;

import com.bt.pi.app.common.entities.util.ResourceAllocationException;
import com.bt.pi.app.common.net.utils.IpAddressUtils;
import com.bt.pi.core.application.resource.DefaultDhtResourceWatchingStrategy;
import com.bt.pi.core.application.resource.watched.WatchedResource;
import com.bt.pi.core.entity.Backupable;
import com.bt.pi.core.entity.EntityScope;
import com.bt.pi.core.scope.NodeScope;

@Backupable
@EntityScope(scope = NodeScope.REGION)
@WatchedResource(watchingStrategy = DefaultDhtResourceWatchingStrategy.class)
public class PublicIpAllocationIndex extends AllocatableResourceIndexBase<InstanceRecord> {
    public static final int NUMBER_OF_BACKUPS = 4;
    public static final String URL = ResourceSchemes.PUBLIC_IP_ADDRESS_INDEX + ":" + "public-ip-allocations";
    private static final String IP_ADDRESS_S_NOT_FOUND_FOR_USER_S = "IP address %s not found for user %s";
    private static final String INSTANCE_ID_PREFIX = "i-";
    private static final Log LOG = LogFactory.getLog(PublicIpAllocationIndex.class);
    private Map<Long, InstanceRecord> ipAddressAllocationMap;

    public PublicIpAllocationIndex() {
        ipAddressAllocationMap = new HashMap<Long, InstanceRecord>();
    }

    @JsonIgnore
    @Override
    public Map<Long, InstanceRecord> getCurrentAllocations() {
        return ipAddressAllocationMap;
    }

    public String allocateIpAddressToInstance(String instanceId) {
        Long resLong = super.allocate(instanceId).getAllocatedResource();
        String ipAddress = IpAddressUtils.longToIp(resLong);
        return ipAddress;
    }

    public String allocateElasticIpAddressToUser(String username) {
        LOG.debug(String.format("allocateElasticIpAddressToUser(%s)", username));
        Long resLong = super.allocate(username, false).getAllocatedResource();
        String ipAddress = IpAddressUtils.longToIp(resLong);
        return ipAddress;
    }

    public String assignElasticIpAddressToInstance(String ipAddress, String instanceId, String ownerId) {
        LOG.debug(String.format("assignElasticIpAddressToInstance(%s, %s, %s)", ipAddress, instanceId, ownerId));
        Long ipAsLong = IpAddressUtils.ipToLong(ipAddress);
        InstanceRecord existingAllocationForAddress = getCurrentAllocations().get(ipAsLong);
        if (existingAllocationForAddress == null || !ownerId.equals(existingAllocationForAddress.getOwnerId()))
            throw new PublicIpAddressAllocationException(String.format(IP_ADDRESS_S_NOT_FOUND_FOR_USER_S, ipAddress, ownerId));

        if (existingAllocationForAddress.getInstanceId() != null && !instanceId.equals(existingAllocationForAddress.getInstanceId()))
            throw new PublicIpAddressAllocationException(String.format("IP address %s is currently assigned to instance %s", ipAddress, existingAllocationForAddress.getInstanceId()));

        Entry<Long, InstanceRecord> existingAllocationForInstance = getExistingAddressAllocationForInstanceExcluding(instanceId, ipAsLong);
        if (existingAllocationForInstance != null) {
            LOG.info(String.format("Assignment of elastic ip to %s is releasing assignment of %s (is elastic? %s) to that instance", instanceId, IpAddressUtils.longToIp(existingAllocationForInstance.getKey()), existingAllocationForInstance
                    .getValue().getOwnerId() != null));
            if (existingAllocationForInstance.getValue().getOwnerId() != null)
                existingAllocationForInstance.getValue().setInstanceId(null);
            else
                getAllocationMap().remove(existingAllocationForInstance.getKey());
        }

        existingAllocationForAddress.setInstanceId(instanceId);

        if (existingAllocationForInstance != null)
            return IpAddressUtils.longToIp(existingAllocationForInstance.getKey());
        else
            return null;
    }

    public Entry<Long, InstanceRecord> getExistingAddressAllocationForInstance(String instanceId) {
        return getExistingAddressAllocationForInstanceExcluding(instanceId, null);
    }

    private Entry<Long, InstanceRecord> getExistingAddressAllocationForInstanceExcluding(String instanceId, Long addressToExclude) {
        LOG.debug(String.format("getExistingAddressAllocationForInstanceExcluding(%s, %s)", instanceId, addressToExclude));
        Entry<Long, InstanceRecord> res = null;
        for (Entry<Long, InstanceRecord> allocation : getCurrentAllocations().entrySet()) {
            if ((addressToExclude == null || !allocation.getKey().equals(addressToExclude)) && instanceId.equals(allocation.getValue().getInstanceId())) {
                if (res != null)
                    LOG.error(String.format("Got more than one owning address for instance %s in public ip index record!!", instanceId));

                res = allocation;
                LOG.debug(String.format("Found existing assignment of instance %s to addr %s", instanceId, IpAddressUtils.longToIp(allocation.getKey())));
            }
        }
        return res;
    }

    public String unassignElasticIpAddressFromInstance(String ipAddress, String ownerId) {
        LOG.debug(String.format("unassignElasticIpAddressFromInstance(%s, %s)", ipAddress, ownerId));
        Long ipAsLong = IpAddressUtils.ipToLong(ipAddress);
        InstanceRecord existingAllocationForAddress = getCurrentAllocations().get(ipAsLong);
        if (existingAllocationForAddress == null || !ownerId.equals(existingAllocationForAddress.getOwnerId()))
            throw new PublicIpAddressAllocationException(String.format(IP_ADDRESS_S_NOT_FOUND_FOR_USER_S, ipAddress, ownerId));

        String existingInstance = existingAllocationForAddress.getInstanceId();
        existingAllocationForAddress.setInstanceId(null);
        LOG.info(String.format("Unassignment of elastic ip address %s released address from instance %s", ipAddress, existingInstance));
        return existingInstance;
    }

    public boolean releaseElasticIpAddressForUser(String ipAddress, String ownerId) {
        return releaseResourceAllocationForConsumer(IpAddressUtils.ipToLong(ipAddress), ownerId);
    }

    @Override
    protected InstanceRecord addAllocationForConsumer(Long allocatedResource, String consumerId, int stepSize, long creationTimestamp) {
        LOG.debug(String.format("addAllocationForConsumer(%s, %s, %s, %s)", allocatedResource, consumerId, stepSize, creationTimestamp));
        InstanceRecord instanceRecord = new InstanceRecord();
        instanceRecord.setLastHeartbeatTimestamp(creationTimestamp);
        if (consumerId != null && consumerId.startsWith(INSTANCE_ID_PREFIX)) {
            LOG.debug(String.format("Consumer id %s starts with 'i-' - assuming this is an instance", consumerId));
            instanceRecord.setInstanceId(consumerId);
        } else {
            instanceRecord.setOwnerId(consumerId);
        }

        ipAddressAllocationMap.put(allocatedResource, instanceRecord);
        return instanceRecord;
    }

    @JsonIgnore
    @Override
    protected boolean releaseResourceAllocationForConsumer(Long allocatedResource, String consumerId) {
        InstanceRecord currentAssignment = ipAddressAllocationMap.get(allocatedResource);
        if (currentAssignment == null) {
            LOG.info(String.format("Address %s is currently not assigned anything (including %s) - nothing to release", IpAddressUtils.longToIp(allocatedResource), consumerId));
            return false;
        }

        if (consumerId.startsWith(INSTANCE_ID_PREFIX)) {
            return unassignAddressFromInstance(allocatedResource, consumerId, currentAssignment);
        } else {
            return unassignAddressFromOwner(allocatedResource, consumerId, currentAssignment);
        }
    }

    private boolean unassignAddressFromOwner(Long allocatedResource, String consumerId, InstanceRecord currentAssignment) {
        String currentOwner = currentAssignment.getOwnerId();
        if (currentOwner == null) {
            LOG.info(String.format("Address %s is not owned by anyone (inc %s) - nothing to release", IpAddressUtils.longToIp(allocatedResource), consumerId));
            return false;
        }
        if (!(currentOwner.equals(consumerId))) {
            LOG.warn(String.format("Address %s is owned by %s rather than %s - won't release", IpAddressUtils.longToIp(allocatedResource), currentOwner, consumerId));
            return false;
        }
        String currentInstanceId = currentAssignment.getInstanceId();
        if (currentInstanceId != null) {
            LOG.debug(String.format("Unassigning elastic address %s for user %s, BUT leaving it assigned to instance %s as a dynamic address", IpAddressUtils.longToIp(allocatedResource), consumerId, currentInstanceId));
            currentAssignment.setOwnerId(null);
            return true;
        }
        LOG.debug(String.format("Removing assignment of addr %s to owner %s", IpAddressUtils.longToIp(allocatedResource), consumerId));
        return ipAddressAllocationMap.remove(allocatedResource) != null;
    }

    private boolean unassignAddressFromInstance(Long allocatedResource, String consumerId, InstanceRecord currentAssignment) {
        String currentInstanceId = currentAssignment.getInstanceId();
        if (currentInstanceId == null || !currentInstanceId.equals(consumerId)) {
            LOG.warn(String.format("Address %s is currently assigned to instance %s rather than %s - won't unassign", IpAddressUtils.longToIp(allocatedResource), currentInstanceId, consumerId));
            return false;
        }
        if (currentAssignment.getOwnerId() != null) {
            LOG.debug(String.format("Unassigning elastic address %s for instance %s", IpAddressUtils.longToIp(allocatedResource), consumerId));
            currentAssignment.setInstanceId(null);
            return true;
        }
        LOG.debug(String.format("Removing assignment of addr %s to instance %s", IpAddressUtils.longToIp(allocatedResource), consumerId));
        return ipAddressAllocationMap.remove(allocatedResource) != null;
    }

    @Override
    public Set<Long> freeResourceFor(String consumerId) {
        if (consumerId != null && !consumerId.startsWith(INSTANCE_ID_PREFIX))
            throw new ResourceAllocationException(String.format("Cannot release public addresses for non-instance resource %s", consumerId));
        return super.freeResourceFor(consumerId);
    }

    public Map<Long, InstanceRecord> getAllocationMap() {
        return ipAddressAllocationMap;
    }

    public void setAllocationMap(Map<Long, InstanceRecord> map) {
        this.ipAddressAllocationMap = map;
    }

    @Override
    public String getUrl() {
        return PublicIpAllocationIndex.URL;
    }

    @Override
    protected int getExistingAllocationStepSize(Long allocatedResource, InstanceRecord allocationRecord) {
        return 1;
    }

    @Override
    public String getUriScheme() {
        return ResourceSchemes.PUBLIC_IP_ADDRESS_INDEX.toString();
    }
}
