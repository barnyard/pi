package com.bt.pi.app.instancemanager.handlers;

import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.isA;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.test.util.ReflectionTestUtils;

import com.bt.pi.app.common.entities.InstanceTypeConfiguration;
import com.bt.pi.app.common.entities.InstanceTypes;
import com.bt.pi.app.common.id.PiIdBuilder;
import com.bt.pi.app.common.resource.PiTopics;
import com.bt.pi.core.continuation.PiContinuation;
import com.bt.pi.core.dht.cache.DhtCache;
import com.bt.pi.core.id.PId;
import com.bt.pi.core.testing.GenericContinuationAnswer;

@RunWith(MockitoJUnitRunner.class)
public class InstanceManagerApplicationSubscriptionHelperTest {
    private InstanceManagerApplicationSubscriptionHelper instanceManagerApplicationSubscriptionHelper;
    private InstanceTypes instanceTypes;
    private AnycastHandler anycastHandler;
    @Mock
    private DhtCache instanceTypesCache;
    @Mock
    private PId instanceTypesId;
    @Mock
    private PiIdBuilder piIdBuilder;
    @Mock
    private InstanceManagerApplication instanceManagerApplication;
    @Mock
    private SystemResourceState systemResourceState;
    private GenericContinuationAnswer<InstanceTypes> instanceTypesAnswer;

    @SuppressWarnings("unchecked")
    @Before
    public void before() {
        when(piIdBuilder.getPId(InstanceTypes.URL_STRING)).thenReturn(instanceTypesId);

        instanceTypes = setupMultipleInstanceTypes();
        instanceTypesAnswer = new GenericContinuationAnswer<InstanceTypes>(instanceTypes);
        doAnswer(instanceTypesAnswer).when(instanceTypesCache).get(eq(instanceTypesId), isA(PiContinuation.class));

        when(systemResourceState.getFreeCores()).thenReturn(1);
        when(systemResourceState.getFreeDiskInMB()).thenReturn(5 * 1024L);
        when(systemResourceState.getFreeMemoryInMB()).thenReturn(1024L);

        this.anycastHandler = new AnycastHandler();
        this.anycastHandler.setSystemResourceState(systemResourceState);

        instanceManagerApplicationSubscriptionHelper = new InstanceManagerApplicationSubscriptionHelper();
        instanceManagerApplicationSubscriptionHelper.setInstanceTypesCache(instanceTypesCache);
        instanceManagerApplicationSubscriptionHelper.setPiIdBuilder(piIdBuilder);
        instanceManagerApplicationSubscriptionHelper.setAnycastHandler(anycastHandler);
        instanceManagerApplicationSubscriptionHelper.setPubSubApp(instanceManagerApplication);

        instanceManagerApplicationSubscriptionHelper.setupInstanceTypesId();
    }

    private InstanceTypes setupMultipleInstanceTypes() {
        InstanceTypes instanceTypes = new InstanceTypes();
        instanceTypes.addInstanceType(new InstanceTypeConfiguration("test1", 1, 1024, 5));
        instanceTypes.addInstanceType(new InstanceTypeConfiguration("test2", 3, 4096, 20));
        instanceTypes.addInstanceType(new InstanceTypeConfiguration("test3", 1, 512, 10));
        return instanceTypes;
    }

    @Test
    public void testSystemResourceWatcherSubscribe() throws Exception {
        // act
        instanceManagerApplicationSubscriptionHelper.checkCapacityAndSubscribeUnSubscribe();

        // assert
        verify(instanceManagerApplication).subscribe(PiTopics.RUN_INSTANCE.getPiLocation(), instanceManagerApplication);
        verify(instanceManagerApplication, never()).unsubscribe(PiTopics.RUN_INSTANCE.getPiLocation(), instanceManagerApplication);
    }

    @Test
    public void testSystemResourceWatcherDontSubscribeWhenInsufficientCapacity() throws Exception {
        // setup
        when(systemResourceState.getFreeMemoryInMB()).thenReturn(512L);

        // act
        instanceManagerApplicationSubscriptionHelper.checkCapacityAndSubscribeUnSubscribe();

        // assert
        verify(instanceManagerApplication, never()).subscribe(PiTopics.RUN_INSTANCE.getPiLocation(), instanceManagerApplication);
        verify(instanceManagerApplication, never()).unsubscribe(PiTopics.RUN_INSTANCE.getPiLocation(), instanceManagerApplication);
    }

    @Test
    public void shouldIgnoreDeprecatedInstanceTypesWhenCheckingCapacity() throws Exception {
        // setup
        when(systemResourceState.getFreeMemoryInMB()).thenReturn(1000L);
        when(systemResourceState.getFreeDiskInMB()).thenReturn(20000L);
        instanceTypes.getInstanceTypes().get("test3").setDeprecated(true);
        instanceTypesAnswer = new GenericContinuationAnswer<InstanceTypes>(instanceTypes);
        ReflectionTestUtils.setField(instanceManagerApplicationSubscriptionHelper, "areResourcesAvailable", new AtomicBoolean(true));

        // act
        instanceManagerApplicationSubscriptionHelper.checkCapacityAndSubscribeUnSubscribe();

        // assert
        verify(instanceManagerApplication, never()).subscribe(PiTopics.RUN_INSTANCE.getPiLocation(), instanceManagerApplication);
        verify(instanceManagerApplication).unsubscribe(PiTopics.RUN_INSTANCE.getPiLocation(), instanceManagerApplication);
    }

    @Test
    public void testSystemResourceWatcherUnSubscribe() throws Exception {
        // setup
        when(systemResourceState.getFreeMemoryInMB()).thenReturn(1024L).thenReturn(1024L).thenReturn(512L);

        // act
        instanceManagerApplicationSubscriptionHelper.checkCapacityAndSubscribeUnSubscribe();
        instanceManagerApplicationSubscriptionHelper.checkCapacityAndSubscribeUnSubscribe();
        instanceManagerApplicationSubscriptionHelper.checkCapacityAndSubscribeUnSubscribe();

        // assert
        verify(instanceManagerApplication).subscribe(PiTopics.RUN_INSTANCE.getPiLocation(), instanceManagerApplication);
        verify(instanceManagerApplication).unsubscribe(PiTopics.RUN_INSTANCE.getPiLocation(), instanceManagerApplication);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testSystemResourceWatcherNoOpIfCachedEntityIsNull() throws Exception {
        // setup
        instanceTypesAnswer = new GenericContinuationAnswer<InstanceTypes>(null);
        doAnswer(instanceTypesAnswer).when(instanceTypesCache).get(eq(instanceTypesId), isA(PiContinuation.class));

        // act
        instanceManagerApplicationSubscriptionHelper.checkCapacityAndSubscribeUnSubscribe();

        // assert
        verify(instanceManagerApplication, never()).subscribe(PiTopics.RUN_INSTANCE.getPiLocation(), instanceManagerApplication);
        verify(instanceManagerApplication, never()).unsubscribe(PiTopics.RUN_INSTANCE.getPiLocation(), instanceManagerApplication);
    }
}
