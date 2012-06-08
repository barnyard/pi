package com.bt.pi.app.volumemanager;

import org.springframework.stereotype.Component;

import com.bt.pi.app.common.entities.Snapshot;
import com.bt.pi.app.common.entities.SnapshotState;
import com.bt.pi.app.common.entities.User;
import com.bt.pi.app.common.resource.PiQueue;
import com.bt.pi.core.id.PId;

@Component
public class RemoveSnapshotFromUserTaskProcessingQueueContinuation extends RemoveResourceFromUserTaskProcessingQueueContinuationBase<Snapshot> {
    public RemoveSnapshotFromUserTaskProcessingQueueContinuation() {
    }

    @Override
    protected String getOwnerId(Snapshot result) {
        return result.getOwnerId();
    }

    @Override
    protected PiQueue getPiQueueForResource() {
        return PiQueue.REMOVE_SNAPSHOT_FROM_USER;
    }

    @Override
    protected boolean removeResourceFromUser(User user, Snapshot snapshot) {
        return user.getSnapshotIds().remove(snapshot.getSnapshotId());
    }

    @Override
    protected boolean setResourceStatusToBuried(Snapshot snapshot) {
        if (SnapshotState.BURIED.equals(snapshot.getStatus()))
            return false;

        snapshot.setStatus(SnapshotState.BURIED);
        return true;
    }

    @Override
    protected String getResourceId(Snapshot snapshot) {
        return snapshot.getSnapshotId();
    }

    @Override
    protected PId getResourcePastryId(String uri) {
        return getPiIdBuilder().getPIdForEc2AvailabilityZone(uri);
    }
}
