/* (c) British Telecommunications plc, 2009, All Rights Reserved */
package com.bt.pi.app.volumemanager;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Resource;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.stereotype.Component;

import com.bt.pi.app.common.AbstractPiCloudApplication;
import com.bt.pi.app.common.entities.Snapshot;
import com.bt.pi.app.common.entities.Volume;
import com.bt.pi.app.common.entities.util.QueueOwnerRemovalContinuation;
import com.bt.pi.app.common.entities.util.QueueOwnerRemovalHelper;
import com.bt.pi.app.common.resource.PiQueue;
import com.bt.pi.app.common.resource.PiTopics;
import com.bt.pi.app.volumemanager.handlers.SnapshotHandler;
import com.bt.pi.app.volumemanager.handlers.VolumeHandler;
import com.bt.pi.core.application.PubSubMessageContext;
import com.bt.pi.core.application.activation.AlwaysOnApplicationActivator;
import com.bt.pi.core.application.activation.ApplicationActivator;
import com.bt.pi.core.conf.Property;
import com.bt.pi.core.entity.EntityMethod;
import com.bt.pi.core.entity.PiEntity;
import com.bt.pi.core.entity.PiLocation;
import com.bt.pi.core.entity.TaskProcessingQueue;
import com.bt.pi.core.scope.NodeScope;

@Component
public class VolumeManagerApplication extends AbstractPiCloudApplication {
    public static final String APPLICATION_NAME = "pi-volume-manager";
    private static final String DEFAULT_QUEUE_WATCHING_APPLICATIONS_PER_REGION = "2";
    private static final String DEFAULT_QUEUE_WATCHING_APPLICATIONS_OFFSET = "3";
    private static final Log LOG = LogFactory.getLog(VolumeManagerApplication.class);
    private ApplicationActivator applicationActivator;
    @Resource
    private VolumeHandler volumeHandler;
    @Resource
    private SnapshotHandler snapshotHandler;
    @Resource
    private QueueOwnerRemovalHelper queueOwnerRemovalHelper;
    @Resource
    private VolumeManagerQueueManager volumeManagerQueueManager;

    private List<PiLocation> queueLocations;
    private int queueWatchingApplicationsPerRegion = Integer.parseInt(DEFAULT_QUEUE_WATCHING_APPLICATIONS_PER_REGION);
    private int queueWatchingApplicationsOffset = Integer.parseInt(DEFAULT_QUEUE_WATCHING_APPLICATIONS_OFFSET);

    public VolumeManagerApplication() {
        super(APPLICATION_NAME);
        LOG.debug(String.format("%s()", getClass().getSimpleName()));
        this.volumeHandler = null;
        this.queueOwnerRemovalHelper = null;
        volumeManagerQueueManager = null;

        queueLocations = new ArrayList<PiLocation>();
        queueLocations.add(PiQueue.ATTACH_VOLUME.getPiLocation());
        queueLocations.add(PiQueue.CREATE_VOLUME.getPiLocation());
        queueLocations.add(PiQueue.DETACH_VOLUME.getPiLocation());
        queueLocations.add(PiQueue.DELETE_VOLUME.getPiLocation());
        queueLocations.add(PiQueue.CREATE_SNAPSHOT.getPiLocation());
        queueLocations.add(PiQueue.DELETE_SNAPSHOT.getPiLocation());
        queueLocations.add(PiQueue.REMOVE_SNAPSHOT_FROM_USER.getPiLocation());
        queueLocations.add(PiQueue.REMOVE_VOLUME_FROM_USER.getPiLocation());
    }

    @Property(key = "volume.manager.queue.watching.applications.per.region", defaultValue = DEFAULT_QUEUE_WATCHING_APPLICATIONS_PER_REGION)
    public void setQueueWatchingApplicationsPerRegion(int value) {
        this.queueWatchingApplicationsPerRegion = value;
    }

    @Property(key = "volume.manager.queue.watching.applications.offset", defaultValue = DEFAULT_QUEUE_WATCHING_APPLICATIONS_OFFSET)
    public void setQueueWatchinApplicationsOffset(int value) {
        this.queueWatchingApplicationsOffset = value;
    }

    @SuppressWarnings("deprecation")
    @Override
    protected void onApplicationStarting() {
        super.subscribe(PiTopics.CREATE_VOLUME.getPiLocation(), this);
        super.subscribe(PiTopics.DELETE_VOLUME.getPiLocation(), this);
        super.subscribe(PiTopics.ATTACH_VOLUME.getPiLocation(), this);
        super.subscribe(PiTopics.DETACH_VOLUME.getPiLocation(), this);
        super.subscribe(PiTopics.CREATE_SNAPSHOT.getPiLocation(), this);
        super.subscribe(PiTopics.DELETE_SNAPSHOT.getPiLocation(), this);

        manageQueueWatcher();
    }

    private void manageQueueWatcher() {
        if (iAmAQueueWatchingApplication(this.queueWatchingApplicationsPerRegion, this.queueWatchingApplicationsOffset, NodeScope.AVAILABILITY_ZONE)) {
            LOG.debug("I am a queue watching application, creating any volume application watchers");
            volumeManagerQueueManager.createVolumeApplicationWatchers(getNodeIdFull());
        } else {
            LOG.debug("I am not a queue watching application, removing any volume application watchers");
            volumeManagerQueueManager.removeVolumeApplicationWatchers();
        }
    }

    @Property(key = "volumemanager.activation.check.period.secs", defaultValue = DEFAULT_ACTIVATION_CHECK_PERIOD_SECS)
    public void setActivationCheckPeriodSecs(int value) {
        super.setActivationCheckPeriodSecs(value);
    }

    @Resource(type = AlwaysOnApplicationActivator.class)
    public void setApplicationActivator(ApplicationActivator anApplicationActivator) {
        this.applicationActivator = anApplicationActivator;
    }

    @Override
    public ApplicationActivator getApplicationActivator() {
        return applicationActivator;
    }

    @Property(key = "volumemanager.start.timeout.millis", defaultValue = DEFAULT_START_TIMEOUT_MILLIS)
    public void setStartTimeout(long value) {
        super.setStartTimeoutMillis(value);
    }

    /*
     * Here we receive messages from the API to create, delete, attach or detach volumes. The entity should be a 'sparse' Volume.
     */
    @Override
    public boolean handleAnycast(PubSubMessageContext pubSubMessageContext, EntityMethod entityMethod, PiEntity piEntity) {
        LOG.debug(String.format("handleAnycast(%s, %s)", pubSubMessageContext, piEntity));
        if (piEntity instanceof Volume) {
            Volume volume = (Volume) piEntity;
            return this.volumeHandler.handleAnycast(pubSubMessageContext, entityMethod, volume, getNodeIdFull());
        }
        if (piEntity instanceof Snapshot) {
            Snapshot snapshot = (Snapshot) piEntity;
            return this.snapshotHandler.handleAnycast(pubSubMessageContext, entityMethod, snapshot, getNodeIdFull());
        }
        return false;
    }

    @Override
    public void handleNodeDeparture(String nodeId) {
        QueueOwnerRemovalContinuation removalContinuation = new QueueOwnerRemovalContinuation(nodeId) {
            @Override
            public void handleResult(TaskProcessingQueue result) {
                manageQueueWatcher();
            }
        };
        queueOwnerRemovalHelper.removeNodeIdFromAllQueues(queueLocations, removalContinuation);
    }

    @Override
    public void handleNodeArrival(String nodeId) {
        manageQueueWatcher();
    }

    @Override
    public boolean becomeActive() {
        return false;
    }

    @Override
    public void becomePassive() {
    }
}
