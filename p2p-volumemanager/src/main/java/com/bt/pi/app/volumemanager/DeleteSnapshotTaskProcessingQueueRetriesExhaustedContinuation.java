package com.bt.pi.app.volumemanager;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.stereotype.Component;

import com.bt.pi.app.common.entities.SnapshotState;
import com.bt.pi.core.application.watcher.task.TaskProcessingQueueRetriesExhaustedContinuation;
import com.bt.pi.core.dht.DhtWriter;
import com.bt.pi.core.id.PId;

/*
 * deal with a delete snapshot request that has failed even after retrying
 */
@Component
public class DeleteSnapshotTaskProcessingQueueRetriesExhaustedContinuation extends AbstractVolumeTaskProcessingQueueContinuation implements TaskProcessingQueueRetriesExhaustedContinuation {
    private static final Log LOG = LogFactory.getLog(DeleteSnapshotTaskProcessingQueueRetriesExhaustedContinuation.class);

    public DeleteSnapshotTaskProcessingQueueRetriesExhaustedContinuation() {
        super();
    }

    @Override
    public void receiveResult(String uri, String nodeId) {
        LOG.debug(String.format("receiveResult(%s, %s)", uri, nodeId));
        PId id = getPiIdbuilder().getPIdForEc2AvailabilityZone(uri);
        DhtWriter writer = getDhtClientFactory().createWriter();

        writer.update(id, new SnapshotStatusUpdatingContinuation(SnapshotState.ERROR, "deletion"));
    }
}
