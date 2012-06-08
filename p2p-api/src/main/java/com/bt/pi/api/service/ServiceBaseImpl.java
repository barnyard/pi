package com.bt.pi.api.service;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import javax.annotation.Resource;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.bt.pi.api.utils.IdFactory;
import com.bt.pi.app.common.entities.ImageIndex;
import com.bt.pi.app.common.entities.User;
import com.bt.pi.app.common.id.PiIdBuilder;
import com.bt.pi.core.application.watcher.task.TaskProcessingQueueHelper;
import com.bt.pi.core.continuation.PiContinuation;
import com.bt.pi.core.continuation.scattergather.PiScatterGatherContinuation;
import com.bt.pi.core.continuation.scattergather.ScatterGatherContinuationRunnable;
import com.bt.pi.core.continuation.scattergather.ScatterGatherContinuationRunner;
import com.bt.pi.core.dht.DhtClientFactory;
import com.bt.pi.core.entity.PiEntity;
import com.bt.pi.core.id.PId;

public class ServiceBaseImpl {
    private static final int SIXTY = 60;
    private static final Log LOG = LogFactory.getLog(ServiceBaseImpl.class);
    private DhtClientFactory dhtClientFactory;
    private ApiApplicationManager apiApplicationManager;
    @Resource
    private UserManagementService userManagementService;
    @Resource
    private UserService userService;
    private PiIdBuilder piIdBuilder;
    private IdFactory idFactory;
    private TaskProcessingQueueHelper taskProcessingQueueHelper;
    private ScatterGatherContinuationRunner scatterGatherContinuationRunner;

    public ServiceBaseImpl() {
        dhtClientFactory = null;
        apiApplicationManager = null;
        userService = null;
        userManagementService = null;
        piIdBuilder = null;
        this.idFactory = null;
        this.taskProcessingQueueHelper = null;
        this.scatterGatherContinuationRunner = null;
    }

    @Resource
    public void setPiIdBuilder(PiIdBuilder aPiIdBuilder) {
        this.piIdBuilder = aPiIdBuilder;
    }

    @Resource
    public void setScatterGatherContinuationRunner(ScatterGatherContinuationRunner aScatterGatherContinuationRunner) {
        this.scatterGatherContinuationRunner = aScatterGatherContinuationRunner;
    }

    @Resource(type = IdFactory.class)
    public void setIdFactory(IdFactory aIdFactory) {
        this.idFactory = aIdFactory;
    }

    @Resource
    public void setDhtClientFactory(DhtClientFactory aDhtClientFactory) {
        this.dhtClientFactory = aDhtClientFactory;
    }

    @Resource
    public void setTaskProcessingQueueHelper(TaskProcessingQueueHelper aTaskProcessingQueueHelper) {
        this.taskProcessingQueueHelper = aTaskProcessingQueueHelper;
    }

    @Resource
    public void setApiApplicationManager(ApiApplicationManager apiManager) {
        this.apiApplicationManager = apiManager;
    }

    protected TaskProcessingQueueHelper getTaskProcessingQueueHelper() {
        return taskProcessingQueueHelper;
    }

    protected ScatterGatherContinuationRunner getScatterGatherContinuationRunner() {
        return scatterGatherContinuationRunner;
    }

    protected IdFactory getIdFactory() {
        return idFactory;
    }

    protected DhtClientFactory getDhtClientFactory() {
        return dhtClientFactory;
    }

    protected ApiApplicationManager getApiApplicationManager() {
        return apiApplicationManager;
    }

    protected UserService getUserService() {
        return userService;
    }

    protected UserManagementService getUserManagementService() {
        return userManagementService;
    }

    protected PiIdBuilder getPiIdBuilder() {
        return piIdBuilder;
    }

    protected <T extends PiEntity> void scatterGather(final List<PId> ids, final PiContinuation<T> continuation, long timeout) {
        LOG.debug(String.format("scatterGather(%s, %s, %s)", ids, continuation, timeout));
        List<ScatterGatherContinuationRunnable> runnables = new ArrayList<ScatterGatherContinuationRunnable>();
        for (final PId id : ids) {
            final PiScatterGatherContinuation<T> piScatterGatherContinuation = new PiScatterGatherContinuation<T>(continuation);
            runnables.add(new ScatterGatherContinuationRunnable(piScatterGatherContinuation) {
                @Override
                public void run() {
                    LOG.debug("Doing async read with " + getDhtClientFactory().createReader() + "for id: " + id + " continuation: " + piScatterGatherContinuation);
                    getDhtClientFactory().createReader().getAsync(id, piScatterGatherContinuation);
                }
            });
        }
        getScatterGatherContinuationRunner().execute(runnables, timeout, TimeUnit.SECONDS);
    }

    protected <T extends PiEntity> void scatterGather(final List<PId> ids, final PiContinuation<T> continuation) {
        LOG.debug(String.format("scatterGather(%s, %s)", ids, continuation));
        scatterGather(ids, continuation, SIXTY);
    }

    protected ImageIndex getLocalImageIndex() {
        PId imageIndexIdForLocalRegion = getPiIdBuilder().getPId(ImageIndex.URL).forLocalRegion();
        ImageIndex res = (ImageIndex) getDhtClientFactory().createBlockingReader().get(imageIndexIdForLocalRegion);
        return res;
    }

    protected void checkImageAccess(String imageId, User user, String imageType) {
        LOG.debug(String.format("checkImageAccess(%s, %s, %s)", imageId, user.getUsername(), imageType));
        if (imageId == null || imageId.length() < 1)
            return;
        if (user.getImageIds().contains(imageId))
            return;

        ImageIndex imageIndex = getLocalImageIndex();
        if (imageIndex.getImages().contains(imageId))
            return;

        throw new IllegalArgumentException(String.format("user %s does not have access to %s %s", user.getUsername(), imageType, imageId));
    }
}