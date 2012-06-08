package com.bt.pi.app.common.net;

import java.util.concurrent.atomic.AtomicLong;

import javax.annotation.Resource;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.stereotype.Component;

import rice.Continuation;

import com.bt.pi.app.common.entities.SubnetAllocationIndex;
import com.bt.pi.app.common.entities.util.ResourceAllocation;
import com.bt.pi.app.common.entities.util.ResourceAllocationException;
import com.bt.pi.app.common.id.PiIdBuilder;
import com.bt.pi.app.common.net.utils.IpAddressUtils;
import com.bt.pi.core.continuation.UpdateResolvingPiContinuation;
import com.bt.pi.core.dht.DhtClientFactory;
import com.bt.pi.core.dht.DhtWriter;
import com.bt.pi.core.id.PId;

@Component
public class SubnetAllocator {
    private static final Log LOG = LogFactory.getLog(SubnetAllocator.class);
    private static final String DELEGATING_HANDLING_OF_EXCEPTION_S_S_TO_CALLING_CONTINUATION = "Delegating handling of exception %s (%s) to calling continuation";
    private PiIdBuilder piIdBuilder;
    private DhtClientFactory dhtClientFactory;

    public SubnetAllocator() {
        this.piIdBuilder = null;
        this.dhtClientFactory = null;
    }

    @Resource
    public void setPiIdBuilder(PiIdBuilder aPiIdBuilder) {
        this.piIdBuilder = aPiIdBuilder;
    }

    @Resource
    public void setDhtClientFactory(DhtClientFactory aDhtClientFactory) {
        this.dhtClientFactory = aDhtClientFactory;
    }

    public void allocateSubnetInLocalRegion(final String securityGroupId, final Continuation<SubnetAllocationResult, Exception> resContinuation) {
        LOG.debug(String.format("allocateSubnetInLocalRegion(%s, %s)", securityGroupId, resContinuation));
        final AtomicLong allocatedSubnet = new AtomicLong(0);
        final AtomicLong allocatedNetmask = new AtomicLong(0);

        PId subnetIndexId = piIdBuilder.getPId(SubnetAllocationIndex.URL).forLocalRegion();
        DhtWriter dhtWriter = this.dhtClientFactory.createWriter();
        dhtWriter.update(subnetIndexId, new UpdateResolvingPiContinuation<SubnetAllocationIndex>() {
            @Override
            public SubnetAllocationIndex update(SubnetAllocationIndex existing, SubnetAllocationIndex mine) {
                LOG.debug(String.format("Read subnet alloc index %s", existing));
                try {
                    ResourceAllocation resourceAllocation = existing.allocate(securityGroupId);
                    allocatedSubnet.set(resourceAllocation.getAllocatedResource());
                    allocatedNetmask.set(IpAddressUtils.netSizeToNetmask(resourceAllocation.getAllocatedStepSize()));
                    LOG.debug(String.format("Provisionally allocated subnet %s and netmask %s to group %s", allocatedSubnet.get(), allocatedNetmask.get(), securityGroupId));
                    return existing;
                } catch (ResourceAllocationException e) {
                    LOG.error(e.getMessage(), e);
                    return null;
                }
            }

            @Override
            public void handleResult(SubnetAllocationIndex result) {
                if (result == null || allocatedSubnet.get() <= 0) {
                    LOG.info(String.format("Abandoning subnet setup for group %s as no subnet was assigned", securityGroupId));
                    resContinuation.receiveResult(null);
                    return;
                }
                String dnsAddress = ((SubnetAllocationIndex) result).getDnsAddress();
                SubnetAllocationResult subnetAllocationResult = new SubnetAllocationResult(allocatedSubnet.get(), allocatedNetmask.get(), dnsAddress);
                LOG.debug(String.format("Subnet allocation result: %s", subnetAllocationResult));
                resContinuation.receiveResult(subnetAllocationResult);
            }

            @Override
            public void handleException(Exception exception) {
                LOG.debug(String.format(DELEGATING_HANDLING_OF_EXCEPTION_S_S_TO_CALLING_CONTINUATION, exception.getClass().getName(), exception.getMessage()));
                resContinuation.receiveException(exception);
            }
        });
    }

    public void releaseSubnetInLocalRegion(final String securityGroupId) {
        LOG.debug(String.format("releaseSubnetInLocalRegion(%s)", securityGroupId));
        PId subnetIndexId = piIdBuilder.getPId(SubnetAllocationIndex.URL).forLocalRegion();

        DhtWriter dhtWriter = this.dhtClientFactory.createWriter();
        dhtWriter.update(subnetIndexId, new UpdateResolvingPiContinuation<SubnetAllocationIndex>() {
            @Override
            public SubnetAllocationIndex update(SubnetAllocationIndex existing, SubnetAllocationIndex requested) {
                existing.freeResourceFor(securityGroupId);
                LOG.debug(String.format("Freed subnet for group %s", securityGroupId));
                return existing;
            }

            @Override
            public void handleResult(SubnetAllocationIndex result) {
                LOG.debug(String.format("Released subnet for sec group %s - wrote index rec %s", securityGroupId, result));
            }
        });
    }
}
