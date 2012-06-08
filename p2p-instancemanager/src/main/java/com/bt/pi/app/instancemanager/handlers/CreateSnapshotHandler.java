package com.bt.pi.app.instancemanager.handlers;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.stereotype.Component;

import com.bt.pi.app.common.entities.Snapshot;
import com.bt.pi.app.common.entities.SnapshotState;
import com.bt.pi.app.common.resource.PiQueue;
import com.bt.pi.core.application.ReceivedMessageContext;
import com.bt.pi.core.entity.EntityResponseCode;
import com.bt.pi.core.util.SerialExecutorRunnable;

@Component("InstanceManager.CreateSnapshotHandler")
public class CreateSnapshotHandler extends AbstractHandler {
    private static final Log LOG = LogFactory.getLog(CreateSnapshotHandler.class);

    public CreateSnapshotHandler() {
    }

    public void createSnapshot(final Snapshot snapshot, final ReceivedMessageContext messageContext) {
        LOG.debug(String.format("createSnapshot(%s, %s)", snapshot, messageContext));
        getSerialExecutor().execute(new SerialExecutorRunnable(PiQueue.CREATE_SNAPSHOT.getUrl(), snapshot.getUrl()) {
            @Override
            public void run() {
                try {
                    createSnapshotInDisk(snapshot, getAbsoluteLocalVolumeFilename(snapshot.getVolumeId()));
                    snapshot.setStatus(SnapshotState.COMPLETE);
                } catch (Throwable t) {
                    LOG.error(t.getMessage(), t);
                    snapshot.setStatus(SnapshotState.ERROR);
                } finally {
                    messageContext.sendResponse(EntityResponseCode.OK, snapshot);
                }
            }
        });
    }
}
