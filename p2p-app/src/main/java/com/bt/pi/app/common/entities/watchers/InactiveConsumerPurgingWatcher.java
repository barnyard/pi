package com.bt.pi.app.common.entities.watchers;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.bt.pi.app.common.entities.AllocatableResourceIndex;
import com.bt.pi.app.common.entities.HeartbeatTimestampResource;
import com.bt.pi.core.continuation.UpdateResolvingPiContinuation;
import com.bt.pi.core.dht.DhtClientFactory;
import com.bt.pi.core.dht.DhtWriter;
import com.bt.pi.core.id.PId;

public class InactiveConsumerPurgingWatcher<T extends HeartbeatTimestampResource> implements Runnable {
    private static final int ONE_THOUSAND = 1000;
    private static final Log LOG = LogFactory.getLog(InactiveConsumerPurgingWatcher.class);
    private DhtClientFactory dhtClientFactory;
    private PId id;

    public InactiveConsumerPurgingWatcher(DhtClientFactory aDhtClientFactory, PId aId) {
        dhtClientFactory = aDhtClientFactory;
        id = aId;
    }

    @Override
    public void run() {
        LOG.info(String.format("Running %s for record %s", getClass().getSimpleName(), id.toStringFull()));

        final AtomicInteger numRemoved = new AtomicInteger(0);
        DhtWriter dhtWriter = dhtClientFactory.createWriter();
        dhtWriter.update(id, new UpdateResolvingPiContinuation<AllocatableResourceIndex<T>>() {
            @Override
            public AllocatableResourceIndex<T> update(AllocatableResourceIndex<T> existingEntity, AllocatableResourceIndex<T> requestedEntity) {
                if (existingEntity == null) {
                    LOG.warn(String.format("Existing entity was null for id %s", id.toStringFull()));
                    return existingEntity;
                }
                Long indexEntityTimeoutSec = existingEntity.getInactiveResourceConsumerTimeoutSec();
                if (indexEntityTimeoutSec == null) {
                    LOG.warn(String.format("Index entity timeout value for %s was null", existingEntity));
                    return null;
                }

                List<Long> keysToRemove = new ArrayList<Long>();
                Map<Long, T> allocationMap = existingEntity.getCurrentAllocations();
                for (Entry<Long, T> entry : allocationMap.entrySet()) {
                    long now = getNow();
                    HeartbeatTimestampResource heartbeatTimestampEntity = entry.getValue();
                    Long lastHeartbeatTimestamp = heartbeatTimestampEntity.getLastHeartbeatTimestamp();
                    if (lastHeartbeatTimestamp == null) {
                        LOG.warn(String.format("Found hearbeat-less consumer record %s for resource %s, timestamping it now", heartbeatTimestampEntity, entry.getKey()));
                        heartbeatTimestampEntity.heartbeat();
                    } else if (indexEntityTimeoutSec >= 0 && lastHeartbeatTimestamp + indexEntityTimeoutSec * ONE_THOUSAND < now) {
                        LOG.info(String.format("Provisionally purging inactive consumer record %s of resource %s", heartbeatTimestampEntity, entry.getKey()));
                        keysToRemove.add(entry.getKey());
                    }
                }

                numRemoved.set(keysToRemove.size());
                for (Long l : keysToRemove) {
                    allocationMap.remove(l);
                }

                return existingEntity;
            }

            @Override
            public void handleResult(AllocatableResourceIndex<T> result) {
                LOG.info(String.format("Wrote purged resource record %s, removing %d inactive consumer(s) ", result, numRemoved.get()));
            }
        });
    }

    protected long getNow() {
        return System.currentTimeMillis();
    }
}
