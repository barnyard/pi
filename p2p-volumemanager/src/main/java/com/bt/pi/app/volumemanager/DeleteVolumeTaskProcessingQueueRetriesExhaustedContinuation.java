package com.bt.pi.app.volumemanager;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.stereotype.Component;

import com.bt.pi.app.common.entities.VolumeState;
import com.bt.pi.core.application.watcher.task.TaskProcessingQueueRetriesExhaustedContinuation;
import com.bt.pi.core.dht.DhtWriter;
import com.bt.pi.core.id.PId;

/*
 * deal with a create volume request that has failed even after retrying
 */
@Component
public class DeleteVolumeTaskProcessingQueueRetriesExhaustedContinuation extends AbstractVolumeTaskProcessingQueueContinuation implements TaskProcessingQueueRetriesExhaustedContinuation {
    private static final Log LOG = LogFactory.getLog(DeleteVolumeTaskProcessingQueueRetriesExhaustedContinuation.class);

    public DeleteVolumeTaskProcessingQueueRetriesExhaustedContinuation() {
        super();
    }

    @Override
    public void receiveResult(String uri, String nodeId) {
        LOG.debug(String.format("receiveResult(%s, %s)", uri, nodeId));
        PId id = getPiIdbuilder().getPIdForEc2AvailabilityZone(uri);
        DhtWriter writer = getDhtClientFactory().createWriter();

        writer.update(id, new VolumeStatusUpdatingContinuation(VolumeState.AVAILABLE, "deletion"));
    }
}
