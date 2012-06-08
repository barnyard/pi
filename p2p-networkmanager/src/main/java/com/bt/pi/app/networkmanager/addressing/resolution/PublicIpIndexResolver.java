package com.bt.pi.app.networkmanager.addressing.resolution;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.annotation.Resource;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.context.ApplicationListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import rice.pastry.Id;

import com.bt.pi.app.common.entities.Instance;
import com.bt.pi.app.common.entities.InstanceRecord;
import com.bt.pi.app.common.entities.InstanceState;
import com.bt.pi.app.common.entities.PublicIpAllocationIndex;
import com.bt.pi.app.common.id.PiIdBuilder;
import com.bt.pi.app.networkmanager.NetworkManagerApplication;
import com.bt.pi.core.application.KoalaPastryApplicationBase;
import com.bt.pi.core.continuation.PiContinuation;
import com.bt.pi.core.continuation.UpdateResolvingPiContinuation;
import com.bt.pi.core.continuation.scattergather.PiScatterGatherContinuation;
import com.bt.pi.core.continuation.scattergather.ScatterGatherContinuationRunnable;
import com.bt.pi.core.continuation.scattergather.ScatterGatherContinuationRunner;
import com.bt.pi.core.dht.DhtClientFactory;
import com.bt.pi.core.id.KoalaIdUtils;
import com.bt.pi.core.id.PId;
import com.bt.pi.core.node.NodeStartedEvent;
import com.bt.pi.core.scope.NodeScope;

@Component
public class PublicIpIndexResolver implements ApplicationListener<NodeStartedEvent> {
    private static final Log LOG = LogFactory.getLog(PublicIpIndexResolver.class);
    private static final int ONE_HOUR = 60 * 60 * 1000;
    private static final int TWENTY = 20;

    @Resource
    private KoalaIdUtils koalaIdUtils;
    @Resource
    private PiIdBuilder piIdBuilder;
    @Resource
    private DhtClientFactory dhtClientFactory;
    @Resource
    private ScatterGatherContinuationRunner scatterGatherContinuationRunner;

    // this way to avoid cyclic dependencies
    @Resource(type = NetworkManagerApplication.class)
    private KoalaPastryApplicationBase application; // to get the leafset out

    private PId publicIpIndexId;
    private AtomicBoolean nodeStarted;

    public PublicIpIndexResolver() {
        koalaIdUtils = null;
        piIdBuilder = null;
        dhtClientFactory = null;
        application = null;
        publicIpIndexId = null;
        nodeStarted = new AtomicBoolean(false);
    }

    private boolean iAmClosestInRegion() {
        return koalaIdUtils.isIdClosestToMe(application.getNodeIdFull(), application.getLeafNodeHandles(), Id.build(publicIpIndexId.getIdAsHex()), NodeScope.REGION);
    }

    @Scheduled(fixedDelay = ONE_HOUR)
    public void resolve() {
        if (!nodeStarted.get()) {
            LOG.info("Not received node started event, so not doing anything");
            return;
        }

        if (publicIpIndexId == null) {
            LOG.debug("Building publicIpIndexId...");
            publicIpIndexId = piIdBuilder.getPId(PublicIpAllocationIndex.URL).forLocalRegion();
        }

        if (!iAmClosestInRegion()) {
            LOG.info("Not the closest to the public ip index record in my region, so doing nothing!");
            return;
        }

        LOG.info("Looking up public ip index record for possible anomalies with terminated instances");

        PublicIpAllocationIndex publicIpAllocationIndex = (PublicIpAllocationIndex) dhtClientFactory.createBlockingReader().get(publicIpIndexId);
        if (publicIpAllocationIndex == null) {
            LOG.warn("Unable to read the public ip allocation index");
            return;
        }

        Map<Long, InstanceRecord> currentAllocations = publicIpAllocationIndex.getCurrentAllocations();
        LOG.debug(String.format("Searching through the current allocation map of size %d in the public ip index", currentAllocations.size()));
        // instancePIds is ConcurrentHashMap because of fan-out complexity :)
        final Map<PId, Entry<Long, InstanceRecord>> instancePIds = new ConcurrentHashMap<PId, Entry<Long, InstanceRecord>>();
        for (final Entry<Long, InstanceRecord> publicIpAllocation : currentAllocations.entrySet()) {
            final String instanceId = publicIpAllocation.getValue() == null ? null : publicIpAllocation.getValue().getInstanceId();
            if (instanceId != null) {
                instancePIds.put(piIdBuilder.getPIdForEc2AvailabilityZone(Instance.getUrl(instanceId)), publicIpAllocation);
            }
        }

        // suspectPublicIpRecords is ConcurrentHashMap because scatter gather continuations might be adding into it
        // after the timeout has expired and we are removing the entries from the public ip index
        Map<Long, InstanceRecord> suspectPublicIpRecords = new ConcurrentHashMap<Long, InstanceRecord>();
        scatterGatherInstanceRecords(suspectPublicIpRecords, instancePIds);

        LOG.info(String.format("Removing suspected instance list of size %d from the public ip index", suspectPublicIpRecords.size()));
        removeSuspectRecordsFromPublicIpIndex(suspectPublicIpRecords);
    }

    private void scatterGatherInstanceRecords(final Map<Long, InstanceRecord> suspectPublicIpRecords, final Map<PId, Entry<Long, InstanceRecord>> instancePIds) {
        List<ScatterGatherContinuationRunnable> runnables = new ArrayList<ScatterGatherContinuationRunnable>();
        for (final Entry<PId, Entry<Long, InstanceRecord>> entry : instancePIds.entrySet()) {
            PublicIpIndexInstanceContinuation publicIpIndexInstanceContinuation = new PublicIpIndexInstanceContinuation(entry.getValue(), suspectPublicIpRecords);
            final PiScatterGatherContinuation<Instance> piScatterGatherContinuation = new PiScatterGatherContinuation<Instance>(publicIpIndexInstanceContinuation);
            runnables.add(new ScatterGatherContinuationRunnable(piScatterGatherContinuation) {
                @Override
                public void run() {
                    dhtClientFactory.createReader().getAsync(entry.getKey(), piScatterGatherContinuation);
                }
            });
        }
        scatterGatherContinuationRunner.execute(runnables, TWENTY, TimeUnit.SECONDS);
    }

    private void removeSuspectRecordsFromPublicIpIndex(final Map<Long, InstanceRecord> suspectPublicIpRecords) {
        if (!suspectPublicIpRecords.isEmpty()) {
            dhtClientFactory.createWriter().update(publicIpIndexId, new UpdateResolvingPiContinuation<PublicIpAllocationIndex>() {
                @Override
                public PublicIpAllocationIndex update(PublicIpAllocationIndex existingEntity, PublicIpAllocationIndex requestedEntity) {
                    int removedCount = 0;
                    for (Entry<Long, InstanceRecord> suspectPublicIpRecord : suspectPublicIpRecords.entrySet()) {
                        InstanceRecord suspectInstanceRecord = existingEntity.getCurrentAllocations().get(suspectPublicIpRecord.getKey());
                        if (suspectPublicIpRecord.getValue().equals(suspectInstanceRecord)) {
                            existingEntity.getCurrentAllocations().remove(suspectPublicIpRecord.getKey());
                            removedCount++;
                        }
                    }
                    if (removedCount > 0) {
                        LOG.debug(String.format("Removing a total of %d instances from the public ip index", removedCount));
                        return existingEntity;
                    }

                    LOG.debug("Not removing any instances from the public ip index");
                    return null;
                }

                @Override
                public void handleResult(PublicIpAllocationIndex result) {
                    LOG.info("Successfully updated and cleaned up the public ip allocation index");
                }
            });
        }
    }

    @Override
    public void onApplicationEvent(NodeStartedEvent event) {
        nodeStarted.set(true);
    }

    private final class PublicIpIndexInstanceContinuation extends PiContinuation<Instance> {
        private Entry<Long, InstanceRecord> publicIpAllocation;
        private final Map<Long, InstanceRecord> suspectPublicIpRecords;

        private PublicIpIndexInstanceContinuation(Entry<Long, InstanceRecord> aPublicIpAllocation, Map<Long, InstanceRecord> aSuspectPublicIpRecords) {
            publicIpAllocation = aPublicIpAllocation;
            suspectPublicIpRecords = aSuspectPublicIpRecords;
        }

        @Override
        public void handleResult(Instance instance) {
            LOG.debug(String.format("handleResult(%s)", instance));
            if (instance == null) {
                LOG.debug(String.format("Adding instanceId %s into the list of suspected instances since the instance record couldn't be found", publicIpAllocation.getValue().getInstanceId()));
                suspectPublicIpRecords.put(publicIpAllocation.getKey(), publicIpAllocation.getValue());
            } else if (InstanceState.TERMINATED.equals(instance.getState()) || InstanceState.FAILED.equals(instance.getState())) {
                LOG.debug(String.format("Adding instanceId %s into the list of suspected instances since the instance is %s", instance.getInstanceId(), instance.getState()));
                suspectPublicIpRecords.put(publicIpAllocation.getKey(), publicIpAllocation.getValue());
            }
        }
    }
}
