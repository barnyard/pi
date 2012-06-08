package com.bt.pi.api.service;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.argThat;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.isA;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.hamcrest.Description;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentMatcher;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.runners.MockitoJUnitRunner;
import org.mockito.stubbing.Answer;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import com.bt.pi.app.common.entities.AvailabilityZone;
import com.bt.pi.app.common.entities.AvailabilityZones;
import com.bt.pi.app.common.entities.Instance;
import com.bt.pi.app.common.entities.InstanceState;
import com.bt.pi.app.common.entities.Region;
import com.bt.pi.app.common.entities.User;
import com.bt.pi.app.common.id.PiIdBuilder;
import com.bt.pi.core.continuation.UpdateResolver;
import com.bt.pi.core.continuation.scattergather.PiScatterGatherContinuation;
import com.bt.pi.core.continuation.scattergather.ScatterGatherContinuationRunnable;
import com.bt.pi.core.continuation.scattergather.ScatterGatherContinuationRunner;
import com.bt.pi.core.dht.DhtClientFactory;
import com.bt.pi.core.dht.DhtReader;
import com.bt.pi.core.dht.DhtWriter;
import com.bt.pi.core.dht.cache.BlockingDhtCache;
import com.bt.pi.core.id.PId;

@RunWith(MockitoJUnitRunner.class)
public class DescribeInstancesServiceHelperTest {
    private static final int SIX_HOURS = 6 * 60 * 60 * 1000;
    private static final String RESERVATION_ID = "r-123";
    private static final String AVAILABILITY_ZONE_1_NAME = "availabilityZone1";
    private static final int AVAILABILITY_ZONE_1_CODE = 10;
    private static final String REGION_ZONE_1_NAME = "Region1";
    private static final int REGION_ZONE_1_CODE = 1;
    @InjectMocks
    private DescribeInstancesServiceHelper describeInstancesServiceHelper = new DescribeInstancesServiceHelper();
    @Mock
    private ApiApplicationManager apiApplicationManager;
    @Mock
    private PId userId;
    @Mock
    private PId instanceId;
    private User user;
    private List<String> instanceIds;
    @Mock
    private DhtWriter dhtWriter;
    @Mock
    private DhtReader dhtReader;
    @Mock
    private DhtClientFactory dhtClientFactory;
    @Mock
    private PiIdBuilder piIdBuilder;
    @Mock
    private BlockingDhtCache blockingDhtCache;
    @Mock
    private ThreadPoolTaskExecutor threadPoolTaskExecutor;
    @Mock
    private ScatterGatherContinuationRunner scatterGatherContinuationRunner;
    private boolean withState = false;
    private int index = 0;

    @Before
    public void setup() {
        this.instanceIds = Arrays.asList("1", "2", "3", "4", "5", "6", "7");

        this.user = new User();
        this.user.addInstances(instanceIds);

        setupPiIdBuilder();
        setupDhtReader();
        setupDhtClientFactory();
        setupBlockingDhtCache();

        doAnswer(new Answer<Object>() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                Runnable r = (Runnable) invocation.getArguments()[0];
                return new Thread(r);
            }
        }).when(threadPoolTaskExecutor).createThread(isA(Runnable.class));

        when(piIdBuilder.getGlobalAvailabilityZoneCodeFromEc2Id(anyString())).thenReturn(AVAILABILITY_ZONE_1_CODE);

        when(apiApplicationManager.getRegion(REGION_ZONE_1_NAME)).thenReturn(new Region(REGION_ZONE_1_NAME, REGION_ZONE_1_CODE, "bob", ""));
        AvailabilityZones zones = new AvailabilityZones();
        zones.addAvailabilityZone(new AvailabilityZone(AVAILABILITY_ZONE_1_NAME, AVAILABILITY_ZONE_1_CODE, REGION_ZONE_1_CODE, AVAILABILITY_ZONE_1_NAME));
        when(apiApplicationManager.getAvailabilityZonesRecord()).thenReturn(zones);
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    private void setupBlockingDhtCache() {
        doAnswer(new Answer<Object>() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                UpdateResolver resolver = (UpdateResolver) invocation.getArguments()[1];
                resolver.update(user, user);
                return null;
            }
        }).when(this.blockingDhtCache).update(eq(userId), isA(UpdateResolver.class));

        when(this.blockingDhtCache.get(userId)).thenReturn(user);
    }

    private void setupDhtClientFactory() {
        when(dhtClientFactory.createWriter()).thenReturn(dhtWriter);
        when(dhtClientFactory.createReader()).thenReturn(dhtReader);
    }

    private void setupPiIdBuilder() {
        doAnswer(new Answer<PId>() {
            @Override
            public PId answer(InvocationOnMock invocation) throws Throwable {
                return instanceId;
            }
        }).when(piIdBuilder).getPIdForEc2AvailabilityZone(isA(String.class));
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    private void setupDhtReader() {
        doAnswer(new Answer<Object>() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                PiScatterGatherContinuation<Instance> piScatterGatherContinuation = (PiScatterGatherContinuation) invocation.getArguments()[1];
                Instance inst = setupInstance();
                piScatterGatherContinuation.receiveResult(inst);

                return null;
            }
        }).when(dhtReader).getAsync(argThat(new ArgumentMatcher<PId>() {
            @Override
            public boolean matches(Object arg0) {
                return arg0 == instanceId;
            }

            @Override
            public void describeTo(Description arg0) {
            }
        }), isA(PiScatterGatherContinuation.class));
    }

    private Instance setupInstance() {
        Instance instance = mock(Instance.class);
        when(instance.getInstanceId()).thenReturn(instanceIds.get(index));
        when(instance.getReservationId()).thenReturn(RESERVATION_ID);
        when(instance.getStateChangeTimestamp()).thenReturn(System.currentTimeMillis());
        when(instance.getState()).thenReturn(InstanceState.RUNNING);
        if (instanceIds.get(index).equals("99")) {
            index++;
            return null;
        }
        if (!withState) {
            index++;
            return instance;
        }
        if (index % 2 == 0) {
            when(instance.getState()).thenReturn(InstanceState.RUNNING);
            if (index == 2)
                when(instance.getState()).thenReturn(InstanceState.FAILED);
            if (index == 4) {
                when(instance.getState()).thenReturn(InstanceState.FAILED);
                when(instance.getStateChangeTimestamp()).thenReturn(System.currentTimeMillis() - SIX_HOURS * 2);
            }
        } else {
            when(instance.getState()).thenReturn(InstanceState.TERMINATED);
            if (index == 3)
                when(instance.getStateChangeTimestamp()).thenReturn(0L);
            if (index == 5)
                when(instance.getStateChangeTimestamp()).thenReturn(System.currentTimeMillis() - SIX_HOURS * 2);
        }
        index++;
        return instance;
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testGetInstances() throws Exception {
        // setup
        withState = false;
        doAnswer(new Answer<Object>() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                Collection<ScatterGatherContinuationRunnable> runnables = (Collection<ScatterGatherContinuationRunnable>) invocation.getArguments()[0];
                CountDownLatch latch = new CountDownLatch(runnables.size());
                for (ScatterGatherContinuationRunnable runnable : runnables) {
                    runnable.setLatch(latch);
                    runnable.run();
                }
                return null;
            }
        }).when(scatterGatherContinuationRunner).execute(isA(Collection.class), anyInt(), isA(TimeUnit.class));

        // act
        ConcurrentMap<String, Set<Instance>> instances = describeInstancesServiceHelper.getInstances(instanceIds);
        Thread.sleep(1000);

        // assert
        assertThat(instances.size(), equalTo(1));
        assertEquals(instanceIds.size(), instances.get(RESERVATION_ID).size());
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testGetInstancesIgnoresExpiredTerminatedAndFailedInstance() throws Exception {
        // setup
        withState = true;

        doAnswer(new Answer<Object>() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                Collection<ScatterGatherContinuationRunnable> runnables = (Collection<ScatterGatherContinuationRunnable>) invocation.getArguments()[0];
                CountDownLatch latch = new CountDownLatch(runnables.size());
                for (ScatterGatherContinuationRunnable runnable : runnables) {
                    runnable.setLatch(latch);
                    runnable.run();
                }
                return null;
            }
        }).when(scatterGatherContinuationRunner).execute(isA(Collection.class), anyInt(), isA(TimeUnit.class));

        // act
        ConcurrentMap<String, Set<Instance>> instances = describeInstancesServiceHelper.getInstances(instanceIds);
        Thread.sleep(1000);

        // assert
        assertThat(instances.size(), equalTo(1));
        Set<Instance> instanceSet = instances.get(RESERVATION_ID);
        assertEquals(instanceIds.size() - 2, instanceSet.size());
        for (Instance instance : instanceSet) {
            System.out.println(instance.getInstanceId() + " " + instance.getState() + " " + instance.getStateChangeTimestamp());
            if (instance.getInstanceId().equals("6"))
                fail("instance 6 should be excluded");
            if (instance.getInstanceId().equals("5"))
                fail("instance 5 should be excluded");
        }
    }

    @Test
    public void testGetInstancesHandlesMissingInstance() throws Exception {
        // setup
        this.instanceIds = Arrays.asList("99", "1");
        withState = false;
        doAnswer(new Answer<Object>() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                Collection<ScatterGatherContinuationRunnable> runnables = (Collection<ScatterGatherContinuationRunnable>) invocation.getArguments()[0];
                CountDownLatch latch = new CountDownLatch(runnables.size());
                for (ScatterGatherContinuationRunnable runnable : runnables) {
                    runnable.setLatch(latch);
                    runnable.run();
                }
                return null;
            }
        }).when(scatterGatherContinuationRunner).execute(isA(Collection.class), anyInt(), isA(TimeUnit.class));

        // act
        ConcurrentMap<String, Set<Instance>> instances = describeInstancesServiceHelper.getInstances(instanceIds);
        Thread.sleep(500);

        // assert
        assertThat(instances.size(), equalTo(1));
        Set<Instance> instanceSet = instances.get(RESERVATION_ID);
        assertEquals(1, instanceSet.size());
        assertEquals("1", instanceSet.iterator().next().getInstanceId());
    }

    @Test
    public void testGetInstanceIdsForUser() throws Exception {
        // setup
        int count = 0;
        List<String> tmpList = new ArrayList<String>();
        for (String s : user.getInstanceIds()) {
            count++;
            if (count == 3)
                break;
            tmpList.add(s);
        }
        for (String s : tmpList) {
            user.removeInstance(s);
            user.addTerminatedInstance(s);
        }

        // act
        List<String> result = new ArrayList<String>();
        result.addAll(describeInstancesServiceHelper.getInstanceIdsForUser(instanceIds, userId));

        // assert
        Collections.sort(result);
        assertThat(result, equalTo(instanceIds));
    }
}
