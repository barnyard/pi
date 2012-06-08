/* (c) British Telecommunications plc, 2009, All Rights Reserved */
package com.bt.pi.api.service;

import java.util.List;
import java.util.Map.Entry;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.annotation.Resource;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.stereotype.Component;

import com.bt.pi.app.common.entities.InstanceRecord;
import com.bt.pi.app.common.entities.PublicIpAddress;
import com.bt.pi.app.common.entities.PublicIpAddressAllocationException;
import com.bt.pi.app.common.entities.PublicIpAllocationIndex;
import com.bt.pi.app.common.id.PiIdBuilder;
import com.bt.pi.app.common.net.utils.IpAddressUtils;
import com.bt.pi.app.common.resource.PiQueue;
import com.bt.pi.core.application.watcher.task.TaskProcessingQueueContinuation;
import com.bt.pi.core.continuation.UpdateResolver;
import com.bt.pi.core.dht.BlockingDhtReader;
import com.bt.pi.core.dht.BlockingDhtWriter;
import com.bt.pi.core.dht.DhtClientFactory;
import com.bt.pi.core.id.PId;
import com.bt.pi.core.util.MutableString;

@Component
public class ElasticIpAddressesServiceImpl extends ServiceBaseImpl implements ElasticIpAddressesService {
    private static final Log LOG = LogFactory.getLog(ElasticIpAddressesServiceImpl.class);
    @Resource
    private DhtClientFactory dhtClientFactory;
    @Resource
    private PiIdBuilder piIdBuilder;
    private ElasticIpAddressOperationHelper elasticIpAddressOperationHelper;

    public ElasticIpAddressesServiceImpl() {
        dhtClientFactory = null;
        piIdBuilder = null;
        elasticIpAddressOperationHelper = null;
    }

    @Resource
    public void setElasticIpAddressOperationHelper(ElasticIpAddressOperationHelper anElasticIpAddressOperationHelper) {
        this.elasticIpAddressOperationHelper = anElasticIpAddressOperationHelper;
    }

    public String allocateAddress(final String ownerId) {
        LOG.info(String.format("allocateAddress(%s)", ownerId));

        PId id = piIdBuilder.getPId(PublicIpAllocationIndex.URL).forLocalRegion();

        final StringBuffer allocatedAddressBuffer = new StringBuffer();
        BlockingDhtWriter blockingDhtWriter = dhtClientFactory.createBlockingWriter();
        blockingDhtWriter.update(id, null, new UpdateResolver<PublicIpAllocationIndex>() {
            @Override
            public PublicIpAllocationIndex update(PublicIpAllocationIndex existingEntity, PublicIpAllocationIndex requestedEntity) {
                LOG.debug(String.format("update public ip addr index %s", existingEntity));

                String allocatedAddress = existingEntity.allocateElasticIpAddressToUser(ownerId);
                LOG.debug(String.format("Provisionally allocated ip addr %s as an elastic ip", allocatedAddress));
                if (allocatedAddressBuffer.length() > 0)
                    allocatedAddressBuffer.delete(0, allocatedAddressBuffer.length());
                allocatedAddressBuffer.append(allocatedAddress);
                return existingEntity;
            }
        });

        if (blockingDhtWriter.getValueWritten() != null) {
            LOG.debug(String.format("Allocated elastic address %s to user %s", allocatedAddressBuffer.toString(), ownerId));
            return allocatedAddressBuffer.toString();
        } else {
            LOG.warn(String.format("Did not allocate elastic address to user %s", ownerId));
            return null;
        }
    }

    public boolean associateAddress(final String ownerId, final String publicIpAddress, final String instanceId) {
        LOG.info(String.format("associateAddress(%s, %s)", ownerId, publicIpAddress));

        elasticIpAddressOperationHelper.validateInstanceForUser(ownerId, instanceId);

        PId publicIpIndexRecordId = piIdBuilder.getPId(PublicIpAllocationIndex.URL).forLocalRegion();
        BlockingDhtWriter dhtWriterForPublicIpIndex = dhtClientFactory.createBlockingWriter();
        try {
            dhtWriterForPublicIpIndex.update(publicIpIndexRecordId, null, new UpdateResolver<PublicIpAllocationIndex>() {
                @Override
                public PublicIpAllocationIndex update(PublicIpAllocationIndex existingEntity, PublicIpAllocationIndex requestedEntity) {
                    String releasedAddress = existingEntity.assignElasticIpAddressToInstance(publicIpAddress, instanceId, ownerId);
                    LOG.info(String.format("Provisionally released address %s by associating %s with %s", releasedAddress, publicIpAddress, instanceId));
                    return existingEntity;
                }
            });
        } catch (PublicIpAddressAllocationException e) {
            throw new IllegalArgumentException(e.getMessage(), e);
        }

        String securityGroupName = elasticIpAddressOperationHelper.writePublicIpToInstanceRecord(publicIpAddress, instanceId);
        final PublicIpAddress publicIpAddressEntity = new PublicIpAddress(publicIpAddress, instanceId, ownerId, securityGroupName);

        elasticIpAddressOperationHelper.enqueueTask(publicIpAddressEntity, PiQueue.ASSOCIATE_ADDRESS, new TaskProcessingQueueContinuation() {
            @Override
            public void receiveResult(String uri, String nodeId) {
                elasticIpAddressOperationHelper.sendAssociateElasticIpAddressRequestToSecurityGroupNode(publicIpAddressEntity);
            }
        });

        return true;
    }

    public boolean disassociateAddress(final String ownerId, final String publicIpAddress) {
        LOG.info(String.format("disassociateAddress(%s, %s)", ownerId, publicIpAddress));
        PId publicIpIndexRecordId = piIdBuilder.getPId(PublicIpAllocationIndex.URL).forLocalRegion();
        final MutableString instanceId = new MutableString();
        BlockingDhtWriter dhtWriterForPublicIpIndex = dhtClientFactory.createBlockingWriter();
        try {
            dhtWriterForPublicIpIndex.update(publicIpIndexRecordId, null, new UpdateResolver<PublicIpAllocationIndex>() {
                @Override
                public PublicIpAllocationIndex update(PublicIpAllocationIndex existingEntity, PublicIpAllocationIndex requestedEntity) {
                    String instanceIdAffected = existingEntity.unassignElasticIpAddressFromInstance(publicIpAddress, ownerId);
                    instanceId.set(instanceIdAffected);
                    LOG.info(String.format("Provisionally disassociated address %s from instance %s for user %s", publicIpAddress, instanceId, ownerId));
                    return existingEntity;
                }
            });
        } catch (PublicIpAddressAllocationException e) {
            throw new IllegalArgumentException(e.getMessage(), e);
        }

        if (null != instanceId.get()) {
            String securityGroupName = elasticIpAddressOperationHelper.writePublicIpToInstanceRecord(null, instanceId.get());
            final PublicIpAddress publicIpAddressEntity = new PublicIpAddress(publicIpAddress, instanceId.get(), ownerId, securityGroupName);
            elasticIpAddressOperationHelper.enqueueTask(publicIpAddressEntity, PiQueue.DISASSOCIATE_ADDRESS, new TaskProcessingQueueContinuation() {
                @Override
                public void receiveResult(String uri, String nodeId) {
                    elasticIpAddressOperationHelper.sendDisassociateElasticIpAddressRequestToSecurityGroupNode(publicIpAddressEntity);
                }
            });
        }

        return true;
    }

    public SortedMap<String, InstanceRecord> describeAddresses(String ownerId, List<String> addresses) {
        LOG.info(String.format("describeAddresses(%s, %s)", ownerId, addresses));

        PId id = piIdBuilder.getPId(PublicIpAllocationIndex.URL).forLocalRegion();

        final SortedMap<String, InstanceRecord> res = new TreeMap<String, InstanceRecord>();
        BlockingDhtReader blockingDhtReader = dhtClientFactory.createBlockingReader();
        PublicIpAllocationIndex publicIpAllocationIndex = (PublicIpAllocationIndex) blockingDhtReader.get(id);
        for (Entry<Long, InstanceRecord> entry : publicIpAllocationIndex.getCurrentAllocations().entrySet()) {
            InstanceRecord allocation = entry.getValue();
            LOG.debug(String.format("Checking record %s against %s", allocation, ownerId));
            if (allocation.getOwnerId() != null && allocation.getOwnerId().equals(ownerId)) {
                String currentIpAddress = IpAddressUtils.longToIp(entry.getKey());
                if (addresses == null || addresses.isEmpty() || addresses.contains(currentIpAddress)) {
                    res.put(currentIpAddress, allocation);
                }
            }
        }

        LOG.debug(String.format("Returning addrs %s for user %s", res, ownerId));
        return res;
    }

    public boolean releaseAddress(final String ownerId, final String publicIpAddress) {
        LOG.info(String.format("releaseAddress(%s, %s)", ownerId, publicIpAddress));

        PId id = piIdBuilder.getPId(PublicIpAllocationIndex.URL).forLocalRegion();

        final AtomicBoolean wasReleased = new AtomicBoolean(false);
        BlockingDhtWriter blockingDhtWriter = dhtClientFactory.createBlockingWriter();
        blockingDhtWriter.update(id, null, new UpdateResolver<PublicIpAllocationIndex>() {
            @Override
            public PublicIpAllocationIndex update(PublicIpAllocationIndex existingEntity, PublicIpAllocationIndex requestedEntity) {
                LOG.debug(String.format("Updating public ip addr index %s for release of addr %s", existingEntity, publicIpAddress));

                boolean released = existingEntity.releaseElasticIpAddressForUser(publicIpAddress, ownerId);
                wasReleased.set(released);
                if (released) {
                    return existingEntity;
                } else {
                    LOG.debug(String.format("Failed to release address %s for user %s", ownerId, publicIpAddress));
                    return null;
                }
            }

        });

        if (wasReleased.get()) {
            LOG.debug(String.format("Released elastic address %s for user %s", publicIpAddress, ownerId));
            return true;
        }

        LOG.warn(String.format("Did not release elastic address %s for user %s", publicIpAddress, ownerId));
        return false;
    }
}
