package com.bt.pi.app.common.net;

import java.util.concurrent.atomic.AtomicLong;

import javax.annotation.Resource;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.stereotype.Component;

import rice.Continuation;

import com.bt.pi.app.common.entities.VlanAllocationIndex;
import com.bt.pi.app.common.entities.util.ResourceAllocation;
import com.bt.pi.app.common.entities.util.ResourceAllocationException;
import com.bt.pi.app.common.id.PiIdBuilder;
import com.bt.pi.core.continuation.UpdateResolvingPiContinuation;
import com.bt.pi.core.dht.DhtClientFactory;
import com.bt.pi.core.dht.DhtWriter;
import com.bt.pi.core.id.PId;

@Component
public class VlanAllocator {
    private static final Log LOG = LogFactory.getLog(VlanAllocator.class);
    private static final String DELEGATING_HANDLING_OF_EXCEPTION_S_S_TO_CALLING_CONTINUATION = "Delegating handling of exception %s (%s) to calling continuation";
    private PiIdBuilder piIdBuilder;
    private DhtClientFactory dhtClientFactory;

    public VlanAllocator() {
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

    public void allocateVlanInLocalRegion(final String securityGroupId, final Continuation<Long, Exception> resContinuation) {
        LOG.debug(String.format("allocateVlanInLocalRegion(%s, %s)", securityGroupId, resContinuation));
        PId vlanIndexId = piIdBuilder.getPId(VlanAllocationIndex.URL).forLocalRegion();
        final AtomicLong allocatedVlanId = new AtomicLong(0);
        DhtWriter dhtWriterVlan = this.dhtClientFactory.createWriter();
        dhtWriterVlan.update(vlanIndexId, new UpdateResolvingPiContinuation<VlanAllocationIndex>() {
            @Override
            public VlanAllocationIndex update(VlanAllocationIndex existing, VlanAllocationIndex mine) {
                try {
                    ResourceAllocation resourceAllocation = existing.allocate(securityGroupId);
                    allocatedVlanId.set(resourceAllocation.getAllocatedResource());
                    LOG.debug(String.format("Provisionally allocated vlan id %d to group %s", allocatedVlanId.get(), securityGroupId));
                    return existing;
                } catch (ResourceAllocationException e) {
                    LOG.error(e.getMessage(), e);
                    return null;
                }
            }

            @Override
            public void handleException(Exception exception) {
                LOG.debug(String.format(DELEGATING_HANDLING_OF_EXCEPTION_S_S_TO_CALLING_CONTINUATION, exception.getClass().getName(), exception.getMessage()));
                resContinuation.receiveException(exception);
            }

            @Override
            public void handleResult(VlanAllocationIndex result) {
                if (result == null || allocatedVlanId.get() == 0) {
                    LOG.info(String.format("Abandoning vlan setup for group %s as no vlan was assigned", securityGroupId));
                    resContinuation.receiveResult(null);
                    return;
                }
                resContinuation.receiveResult(allocatedVlanId.get());
            }
        });
    }

    public void releaseVlanInLocalRegion(final String securityGroupId) {
        LOG.debug(String.format("releaseVlanInLocalRegion(%s)", securityGroupId));
        PId vlanIndexId = piIdBuilder.getPId(VlanAllocationIndex.URL).forLocalRegion();

        DhtWriter dhtWriterVlan = this.dhtClientFactory.createWriter();
        dhtWriterVlan.update(vlanIndexId, new UpdateResolvingPiContinuation<VlanAllocationIndex>() {
            @Override
            public VlanAllocationIndex update(VlanAllocationIndex existing, VlanAllocationIndex requested) {
                existing.freeResourceFor(securityGroupId);
                LOG.debug(String.format("Freed vlan for group %s", securityGroupId));
                return existing;
            }

            @Override
            public void handleResult(VlanAllocationIndex result) {
                LOG.debug(String.format("Released vlan for sec group %s - wrote index rec %s", securityGroupId, result));
            }
        });
    }
}
