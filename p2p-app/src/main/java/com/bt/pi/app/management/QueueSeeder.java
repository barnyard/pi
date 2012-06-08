package com.bt.pi.app.management;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.stereotype.Component;

import com.bt.pi.app.common.resource.PiQueue;
import com.bt.pi.core.entity.TaskProcessingQueue;
import com.bt.pi.core.id.PId;
import com.bt.pi.core.scope.NodeScope;

@Component
public class QueueSeeder extends SeederBase {

    private static final Log LOG = LogFactory.getLog(QueueSeeder.class);

    public QueueSeeder() {
        super();
    }

    public boolean addQueue(PiQueue piQueue) {
        LOG.info(String.format("Adding taskProcessingQueue for %s", piQueue));
        PId queueId = getPiIdBuilder().getPiQueuePId(piQueue).forLocalScope(piQueue.getNodeScope());

        TaskProcessingQueue queue = new TaskProcessingQueue(piQueue.getUrl());
        boolean result = getDhtClientFactory().createBlockingWriter().writeIfAbsent(queueId, queue);

        return result;
    }

    public boolean addQueuesForRegion(int regionCode) {
        LOG.info(String.format("Adding taskProcessingQueues for region %s", regionCode));

        boolean success = true;
        for (PiQueue piQueue : PiQueue.values()) {
            if (piQueue.getPiLocation().getNodeScope() != NodeScope.REGION)
                continue;
            PId queueId = getPiIdBuilder().getPId(piQueue.getUrl()).forRegion(regionCode);
            TaskProcessingQueue queue = new TaskProcessingQueue(piQueue.getUrl());
            boolean res = getDhtClientFactory().createBlockingWriter().writeIfAbsent(queueId, queue);
            success = success && res;
        }

        return success;
    }

    public boolean addQueuesForAvailabilityZone(int globalAvailabilityZoneCode) {
        LOG.info(String.format("Adding taskProcessingQueues for global avz code %s", globalAvailabilityZoneCode));

        boolean success = true;
        for (PiQueue piQueue : PiQueue.values()) {
            if (piQueue.getPiLocation().getNodeScope() != NodeScope.AVAILABILITY_ZONE)
                continue;
            PId queueId = getPiIdBuilder().getPId(piQueue.getUrl()).forGlobalAvailablityZoneCode(globalAvailabilityZoneCode);
            TaskProcessingQueue queue = new TaskProcessingQueue(piQueue.getUrl());
            boolean res = getDhtClientFactory().createBlockingWriter().writeIfAbsent(queueId, queue);
            success = success && res;
        }

        return success;
    }
}
