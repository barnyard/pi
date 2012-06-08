package com.bt.pi.app.volumemanager;

import org.springframework.stereotype.Component;

import com.bt.pi.app.common.entities.User;
import com.bt.pi.app.common.entities.Volume;
import com.bt.pi.app.common.entities.VolumeState;
import com.bt.pi.app.common.resource.PiQueue;
import com.bt.pi.core.id.PId;

@Component
public class RemoveVolumeFromUserTaskProcessingQueueContinuation extends RemoveResourceFromUserTaskProcessingQueueContinuationBase<Volume> {
    public RemoveVolumeFromUserTaskProcessingQueueContinuation() {
    }

    @Override
    protected String getOwnerId(Volume result) {
        return result.getOwnerId();
    }

    @Override
    protected PiQueue getPiQueueForResource() {
        return PiQueue.REMOVE_VOLUME_FROM_USER;
    }

    @Override
    protected boolean removeResourceFromUser(User user, Volume volume) {
        return user.getVolumeIds().remove(volume.getVolumeId());
    }

    @Override
    protected boolean setResourceStatusToBuried(Volume volume) {
        if (VolumeState.BURIED.equals(volume.getStatus()))
            return false;

        volume.setStatus(VolumeState.BURIED);
        return true;
    }

    @Override
    protected String getResourceId(Volume volume) {
        return volume.getVolumeId();
    }

    @Override
    protected PId getResourcePastryId(String uri) {
        return getPiIdBuilder().getPIdForEc2AvailabilityZone(uri);
    }
}
