package com.bt.pi.app.common.entities.watchers.securitygroup;

import rice.Continuation;

import com.bt.pi.core.continuation.GenericContinuation;

public interface InstanceAddressManager {
    void allocatePublicIpAddressForInstance(final String instanceId, final Continuation<String, Exception> resultContinuation);

    void releasePublicIpAddressForInstance(final String instanceId, final String securityGroupId, GenericContinuation<Boolean> resultContinuation);
}
