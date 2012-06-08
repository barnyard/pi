package com.bt.pi.app.common.entities.util;

import com.bt.pi.core.continuation.UpdateResolvingPiContinuation;
import com.bt.pi.core.entity.TaskProcessingQueue;

public abstract class QueueOwnerRemovalContinuation extends UpdateResolvingPiContinuation<TaskProcessingQueue> {
    private String owner;

    public QueueOwnerRemovalContinuation(String anOwnerToRemove) {
        super();
        owner = anOwnerToRemove;
    }

    public String getOwner() {
        return owner;
    }

    @Override
    public TaskProcessingQueue update(TaskProcessingQueue existingEntity, TaskProcessingQueue requestedEntity) {
        existingEntity.removeOwnerFromAllTasks(owner);
        return existingEntity;
    }
}
