package com.bt.pi.app.common.entities.watchers.instance;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import org.junit.Before;
import org.junit.Test;

import com.bt.pi.app.common.entities.Instance;
import com.bt.pi.app.common.entities.watchers.instance.InstanceRefreshContinuation;
import com.bt.pi.app.common.entities.watchers.instance.InstanceRefreshHandler;

public class InstanceRefreshContinuationTest {
    private InstanceRefreshContinuation instanceRefreshContinuation;
    private InstanceRefreshHandler instanceRefreshHandler;
    private Instance instance;

    @Before
    public void before() {
        instance = mock(Instance.class);
        instanceRefreshHandler = mock(InstanceRefreshHandler.class);

        instanceRefreshContinuation = new InstanceRefreshContinuation(instanceRefreshHandler);
    }

    @Test
    public void shouldDelegateHandlingOfInstanceRecordRefresh() {
        // act
        instanceRefreshContinuation.handleResult(instance);

        // assert
        verify(instanceRefreshHandler).handleInstanceRefresh(instance);
    }
}
