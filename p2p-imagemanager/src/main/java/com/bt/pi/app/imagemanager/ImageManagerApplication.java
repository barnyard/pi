/* (c) British Telecommunications plc, 2009, All Rights Reserved */

package com.bt.pi.app.imagemanager;

import java.util.Arrays;

import javax.annotation.Resource;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.stereotype.Component;

import com.bt.pi.app.common.AbstractPiCloudApplication;
import com.bt.pi.app.common.entities.Image;
import com.bt.pi.app.common.entities.util.QueueOwnerRemovalContinuation;
import com.bt.pi.app.common.entities.util.QueueOwnerRemovalHelper;
import com.bt.pi.app.common.os.FileManager;
import com.bt.pi.app.common.resource.PiQueue;
import com.bt.pi.app.common.resource.PiTopics;
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
public class ImageManagerApplication extends AbstractPiCloudApplication {
    public static final String APPLICATION_NAME = "pi-image-manager";
    protected static final String DEFAULT_START_TIMEOUT_MILLIS = "60000";
    protected static final String DEFAULT_ACTIVATION_CHECK_PERIOD_SECS = "120";
    private static final Log LOG = LogFactory.getLog(ImageManagerApplication.class);
    private static final String DEFAULT_QUEUE_WATCHING_APPLICATIONS_PER_REGION = "4";
    private static final String DEFAULT_QUEUE_WATCHING_APPLICATIONS_OFFSET = "0";
    private static final String DEFAULT_IMAGE_PROCESSING_PATH = "var/image_processing";

    private String imagesProcessingPath = DEFAULT_IMAGE_PROCESSING_PATH;
    @Resource
    private DecryptionHandler decryptionHandler;
    @Resource(type = AlwaysOnApplicationActivator.class)
    private ApplicationActivator applicationActivator;
    @Resource
    private ImageManagerApplicationWatcherManager imageManagerApplicationWatcherManager;
    @Resource
    private QueueOwnerRemovalHelper queueOwnerRemovalHelper;
    @Resource
    private FileManager fileManager;

    private int queueWatchingApplicationsPerRegion = Integer.parseInt(DEFAULT_QUEUE_WATCHING_APPLICATIONS_PER_REGION);
    private int queueWatchingApplicationsOffset = Integer.parseInt(DEFAULT_QUEUE_WATCHING_APPLICATIONS_OFFSET);

    public ImageManagerApplication() {
        super(APPLICATION_NAME);
        this.decryptionHandler = null;
        this.imageManagerApplicationWatcherManager = null;
        this.queueOwnerRemovalHelper = null;
        this.fileManager = null;
    }

    @Property(key = "image.manager.queue.watching.applications.per.region", defaultValue = DEFAULT_QUEUE_WATCHING_APPLICATIONS_PER_REGION)
    public void setQueueWatchingApplicationsPerRegion(int value) {
        this.queueWatchingApplicationsPerRegion = value;
    }

    @Property(key = "image.manager.queue.watching.applications.offset", defaultValue = DEFAULT_QUEUE_WATCHING_APPLICATIONS_OFFSET)
    public void setQueueWatchinApplicationsOffset(int value) {
        this.queueWatchingApplicationsOffset = value;
    }

    @Property(key = "image.processing.path", defaultValue = DEFAULT_IMAGE_PROCESSING_PATH)
    public void setImageProcessingPath(String value) {
        this.imagesProcessingPath = value;
    }

    @Override
    public boolean handleAnycast(PubSubMessageContext pubSubMessageContext, EntityMethod entityMethod, PiEntity piEntity) {
        if (piEntity instanceof Image) {
            Image image = (Image) piEntity;
            LOG.debug(String.format("handleAnycast(%s)", image.getImageId()));
            decryptionHandler.decrypt(image, this);
            return true;
        }
        return false;
    }

    @Override
    protected void onApplicationStarting() {
        super.subscribe(PiTopics.DECRYPT_IMAGE.getPiLocation(), this);

        manageQueueWatcher();

        cleanupImageProcessingDirectory();
    }

    private void cleanupImageProcessingDirectory() {
        LOG.debug(String.format("Cleaning up any leftover files in %s", this.imagesProcessingPath));
        fileManager.deleteAllFilesInDirectory(this.imagesProcessingPath);
    }

    private void manageQueueWatcher() {
        if (iAmAQueueWatchingApplication(this.queueWatchingApplicationsPerRegion, this.queueWatchingApplicationsOffset, NodeScope.AVAILABILITY_ZONE))
            this.imageManagerApplicationWatcherManager.createTaskProcessingQueueWatcher(getNodeIdFull());
        else
            this.imageManagerApplicationWatcherManager.removeTaskProcessingQueueWatcher();
    }

    @Property(key = "imagemanager.activation.check.period.secs", defaultValue = DEFAULT_ACTIVATION_CHECK_PERIOD_SECS)
    public void setActivationCheckPeriodSecs(int value) {
        super.setActivationCheckPeriodSecs(value);
    }

    @Override
    public ApplicationActivator getApplicationActivator() {
        return applicationActivator;
    }

    @Property(key = "imagemanager.start.timeout.millis", defaultValue = DEFAULT_START_TIMEOUT_MILLIS)
    public void setStartTimeout(long value) {
        super.setStartTimeoutMillis(value);
    }

    @Override
    public void handleNodeDeparture(final String nodeId) {
        LOG.debug(String.format("handleNodeDeparture(%s)", nodeId));
        QueueOwnerRemovalContinuation removalContinuation = new QueueOwnerRemovalContinuation(nodeId) {
            @Override
            public void handleResult(TaskProcessingQueue result) {
                manageQueueWatcher();
            }
        };
        queueOwnerRemovalHelper.removeNodeIdFromAllQueues(Arrays.asList(new PiLocation[] { PiQueue.DECRYPT_IMAGE.getPiLocation() }), removalContinuation);
    }

    @Override
    public void handleNodeArrival(String nodeId) {
        manageQueueWatcher();
    }

    @Override
    public void becomePassive() {
    }

    @Override
    public boolean becomeActive() {
        return true;
    }

}
