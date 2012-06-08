package com.bt.pi.app.instancemanager.watchers;

import java.util.concurrent.Executor;

import javax.annotation.Resource;

import com.bt.pi.app.common.id.PiIdBuilder;
import com.bt.pi.core.application.watcher.service.WatcherService;
import com.bt.pi.core.continuation.scattergather.ScatterGatherContinuationRunner;
import com.bt.pi.core.dht.DhtClientFactory;

public abstract class AbstractWatcherRunnable implements Runnable {
    @Resource
    private PiIdBuilder piIdBuilder;
    @Resource
    private WatcherService watcherService;
    @Resource
    private DhtClientFactory dhtClientFactory;
    @Resource
    private Executor taskExecutor;
    @Resource
    private ScatterGatherContinuationRunner scatterGatherContinuationRunner;

    public AbstractWatcherRunnable() {
        this.watcherService = null;
        this.piIdBuilder = null;
        this.dhtClientFactory = null;
        this.taskExecutor = null;
        this.scatterGatherContinuationRunner = null;
    }

    @Override
    public abstract void run();

    protected PiIdBuilder getPiIdBuilder() {
        return piIdBuilder;
    }

    protected ScatterGatherContinuationRunner getScatterGatherContinuationRunner() {
        return scatterGatherContinuationRunner;
    }

    protected WatcherService getWatcherService() {
        return watcherService;
    }

    protected DhtClientFactory getDhtClientFactory() {
        return dhtClientFactory;
    }

    protected Executor getTaskExecutor() {
        return taskExecutor;
    }
}
