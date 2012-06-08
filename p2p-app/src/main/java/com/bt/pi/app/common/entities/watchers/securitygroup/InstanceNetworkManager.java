package com.bt.pi.app.common.entities.watchers.securitygroup;

import rice.Continuation;

import com.bt.pi.app.common.entities.Instance;

public interface InstanceNetworkManager {

    void setupNetworkForInstance(final Instance instance, final Continuation<Instance, Exception> resContinuation);

    void releaseNetworkForInstance(String userId, String networkName, final String instanceId);

}