/* (c) British Telecommunications plc, 2009, All Rights Reserved */
package com.bt.pi.app.networkmanager.addressing;

import java.util.HashSet;
import java.util.Set;

import javax.annotation.Resource;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.stereotype.Component;

import rice.Continuation;

import com.bt.pi.app.common.entities.InstanceRecord;
import com.bt.pi.app.common.entities.PublicIpAllocationIndex;
import com.bt.pi.app.common.entities.watchers.securitygroup.InstanceAddressManager;
import com.bt.pi.app.common.id.PiIdBuilder;
import com.bt.pi.app.common.net.utils.IpAddressUtils;
import com.bt.pi.core.continuation.GenericContinuation;
import com.bt.pi.core.continuation.PiContinuation;
import com.bt.pi.core.continuation.UpdateResolvingPiContinuation;
import com.bt.pi.core.dht.DhtClientFactory;
import com.bt.pi.core.dht.DhtReader;
import com.bt.pi.core.dht.DhtWriter;
import com.bt.pi.core.id.PId;

@Component
public class PublicIpAddressManager implements InstanceAddressManager {
    private static final Log LOG = LogFactory.getLog(PublicIpAddressManager.class);
    private DhtClientFactory dhtClientFactory;
    private PiIdBuilder piIdBuilder;
    private AddressAssignmentExecutor addressAssignmentExecutor;

    public PublicIpAddressManager() {
        super();
        this.dhtClientFactory = null;
        this.piIdBuilder = null;
        this.addressAssignmentExecutor = null;
    }

    @Resource
    public void setPiIdBuilder(PiIdBuilder aPiIdBuilder) {
        piIdBuilder = aPiIdBuilder;
    }

    @Resource
    public void setAddressAssignmentExecutor(AddressAssignmentExecutor anAddressAssignmentExecutor) {
        this.addressAssignmentExecutor = anAddressAssignmentExecutor;
    }

    @Resource
    public void setDhtClientFactory(DhtClientFactory dhtFactory) {
        dhtClientFactory = dhtFactory;
    }

    @Override
    public void allocatePublicIpAddressForInstance(final String instanceId, final Continuation<String, Exception> resultContinuation) {
        LOG.debug(String.format("allocatePublicIpAddressForInstance(%s,  %s)", instanceId, resultContinuation));
        final StringBuilder allocatedPublicIpAddress = new StringBuilder();
        int globalAvzCodeForInstance = piIdBuilder.getGlobalAvailabilityZoneCodeFromEc2Id(instanceId);
        PId publicIpIndexId = piIdBuilder.getPId(PublicIpAllocationIndex.URL).forRegion(PId.getRegionCodeFromGlobalAvailabilityZoneCode(globalAvzCodeForInstance));
        DhtWriter writer = dhtClientFactory.createWriter();

        writer.update(publicIpIndexId, new UpdateResolvingPiContinuation<PublicIpAllocationIndex>() {
            @Override
            public PublicIpAllocationIndex update(PublicIpAllocationIndex existingEntity, PublicIpAllocationIndex requestedEntity) {
                if (existingEntity == null) {
                    LOG.warn(String.format("No Public ip address record read!"));
                    return null;
                }

                String allocated;
                try {
                    allocated = existingEntity.allocateIpAddressToInstance(instanceId);
                } catch (RuntimeException e) {
                    LOG.error(String.format("Unable to allocate new address: %s", e.getMessage()), e);
                    return null;
                }

                LOG.debug(String.format("Provisionally allocated public ip address %s to %s", allocated, instanceId));
                allocatedPublicIpAddress.delete(0, allocatedPublicIpAddress.length());
                allocatedPublicIpAddress.append(allocated);
                return existingEntity;
            };

            @Override
            public void handleException(Exception exception) {
                LOG.debug(String.format("Delegating handling of exception %s (%s( to calling continuation", exception.getClass().getName(), exception.getMessage()));
                resultContinuation.receiveException(exception);
            }

            @Override
            public void handleResult(PublicIpAllocationIndex result) {
                LOG.debug(String.format("Allocation of public address for instance %s wrote address record %s", instanceId, result));
                if (result == null || allocatedPublicIpAddress.length() <= 0) {
                    LOG.info(String.format("No public ip address allocated for instance %s", instanceId));
                    resultContinuation.receiveResult(null);
                    return;
                }

                LOG.info(String.format("Allocated address %s for instance %s - now assigning it", allocatedPublicIpAddress.toString(), instanceId));
                addressAssignmentExecutor.assignPublicIpAddressToInstance(allocatedPublicIpAddress.toString(), instanceId, new GenericContinuation<Boolean>() {
                    @Override
                    public void handleResult(Boolean result) {
                        LOG.debug(String.format("Result for assignment of %s to %s: %s", allocatedPublicIpAddress.toString(), instanceId, result));
                        if (result)
                            resultContinuation.receiveResult(allocatedPublicIpAddress.toString());
                        else
                            resultContinuation.receiveResult(null);
                    }
                });
            }
        });
    }

    public void associatePublicIpAddressWithInstance(final String publicIpAddress, final String instanceId, final String securityGroupId, final GenericContinuation<Boolean> resultContinuation) {
        LOG.debug(String.format("associatePublicIpAddressWithInstance(%s, %s, %s)", publicIpAddress, instanceId, securityGroupId));
        DhtReader dhtReader = dhtClientFactory.createReader();
        int globalAvzCodeForInstance = piIdBuilder.getGlobalAvailabilityZoneCodeFromEc2Id(instanceId);
        PId publicIpIndexId = piIdBuilder.getPId(PublicIpAllocationIndex.URL).forRegion(PId.getRegionCodeFromGlobalAvailabilityZoneCode(globalAvzCodeForInstance));

        dhtReader.getAsync(publicIpIndexId, new PiContinuation<PublicIpAllocationIndex>() {
            @Override
            public void handleResult(PublicIpAllocationIndex result) {
                LOG.debug(String.format("Checking to see if allocation of public address %s for instance %s is still valid in record %s", publicIpAddress, instanceId, result));
                if (result == null) {
                    LOG.info(String.format("No public ip address record found, abandoning assignment of addr %s to %s", publicIpAddress, instanceId));
                    resultContinuation.receiveResult(false);
                    return;
                }

                long addrAsLong = IpAddressUtils.ipToLong(publicIpAddress);
                InstanceRecord currentAllocation = result.getCurrentAllocations().get(addrAsLong);
                if (currentAllocation == null || instanceId == null || !instanceId.equals(currentAllocation.getInstanceId())) {
                    LOG.info(String.format("Abandoning assignment of addr %s to %s as the public ip addr index says this address is currently assigned to %s", publicIpAddress, instanceId, currentAllocation == null ? null : currentAllocation
                            .getInstanceId()));
                    resultContinuation.receiveResult(false);
                    return;
                }

                LOG.debug(String.format("Unassigning existing address from instance %s to make way for association of %s", instanceId, publicIpAddress));
                addressAssignmentExecutor.unassignPublicIpAddressFromInstance(instanceId, securityGroupId, new GenericContinuation<Boolean>() {
                    @Override
                    public void handleResult(Boolean result) {
                        LOG.debug(String.format("Address unassignment during association with %s returned %s", instanceId, result));
                        if (!result) {
                            resultContinuation.receiveResult(false);
                            return;
                        }

                        addressAssignmentExecutor.assignPublicIpAddressToInstance(publicIpAddress, instanceId, new GenericContinuation<Boolean>() {
                            @Override
                            public void handleResult(Boolean result) {
                                LOG.debug(String.format("Address assignment of %s during association with %s returned %s", publicIpAddress, instanceId, result));
                                resultContinuation.receiveResult(result);
                            }
                        });
                    }
                });
            }
        });
    }

    public void disassociatePublicIpAddressFromInstance(final String publicIpAddress, final String instanceId, final String securityGroupId, final GenericContinuation<String> resultContinuation) {
        LOG.debug(String.format("disassociatePublicIpAddressWithInstance(%s, %s, %s)", publicIpAddress, instanceId, securityGroupId));
        addressAssignmentExecutor.unassignPublicIpAddressFromInstance(instanceId, securityGroupId, new GenericContinuation<Boolean>() {
            @Override
            public void handleResult(Boolean result) {
                LOG.debug(String.format("Address unassignment during disassociation with %s returned %s", instanceId, result));
                if (!result) {
                    resultContinuation.receiveResult(null);
                    return;
                }

                allocatePublicIpAddressForInstance(instanceId, new GenericContinuation<String>() {
                    @Override
                    public void handleResult(String result) {
                        LOG.debug(String.format("Address assignment of %s during disassociation from %s returned %s", publicIpAddress, instanceId, result));
                        resultContinuation.receiveResult(result);
                    }
                });
            }
        });
    }

    @Override
    public void releasePublicIpAddressForInstance(final String instanceId, final String securityGroupId, final GenericContinuation<Boolean> resultContinuation) {
        LOG.debug(String.format("releasePublicIpAddressForInstance(%s, %s)", instanceId, securityGroupId));
        final Set<Long> removedResources = new HashSet<Long>();
        int globalAvzCodeForInstance = piIdBuilder.getGlobalAvailabilityZoneCodeFromEc2Id(instanceId);
        PId publicIpIndexId = piIdBuilder.getPId(PublicIpAllocationIndex.URL).forRegion(PId.getRegionCodeFromGlobalAvailabilityZoneCode(globalAvzCodeForInstance));
        DhtWriter writer = dhtClientFactory.createWriter();

        writer.update(publicIpIndexId, new UpdateResolvingPiContinuation<PublicIpAllocationIndex>() {
            @Override
            public PublicIpAllocationIndex update(PublicIpAllocationIndex existingEntity, PublicIpAllocationIndex requestedEntity) {
                LOG.debug(String.format("update(%s, %s)", existingEntity, requestedEntity));
                Set<Long> provisionallyRemovedResources = existingEntity.freeResourceFor(instanceId);
                removedResources.clear();
                removedResources.addAll(provisionallyRemovedResources);
                return existingEntity;
            }

            @Override
            public void handleResult(PublicIpAllocationIndex result) {
                LOG.debug(String.format("Release of public address for instance %s updated public address record %s", instanceId, result));
                if (result == null) {
                    resultContinuation.receiveResult(false);
                    return;
                }

                LOG.debug(String.format("Addresses released: %s", removedResources));
                addressAssignmentExecutor.unassignPublicIpAddressFromInstance(instanceId, securityGroupId, new GenericContinuation<Boolean>() {
                    @Override
                    public void handleResult(Boolean result) {
                        LOG.debug(String.format("Unassign of public ip for %s result: %s", instanceId, result));
                        resultContinuation.receiveResult(result);
                    }
                });
            }
        });
    }
}
