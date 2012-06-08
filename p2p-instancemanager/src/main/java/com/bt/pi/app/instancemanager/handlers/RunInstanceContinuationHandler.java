package com.bt.pi.app.instancemanager.handlers;

import javax.annotation.Resource;

import org.springframework.stereotype.Component;

import rice.Continuation;

import com.bt.pi.app.common.entities.Instance;

@Component
public class RunInstanceContinuationHandler extends AbstractHandler {

    @Resource
    private RunInstanceHandler runInstanceHandler;

    public RunInstanceContinuationHandler() {
        runInstanceHandler = null;
    }

    public Continuation<Instance, Exception> getContinuation(String nodeIdFull) {
        RunInstanceContinuation runInstanceContinuation = new RunInstanceContinuation();
        runInstanceContinuation.setPiIdBuilder(getPiIdBuilder());
        runInstanceContinuation.setRunInstanceHandler(runInstanceHandler);
        runInstanceContinuation.setDhtClientFactory(getDhtClientFactory());
        runInstanceContinuation.setNodeId(nodeIdFull);
        runInstanceContinuation.setExecutor(getTaskExecutor());
        return runInstanceContinuation;
    }
}
