package com.bt.pi.api.service;

import static org.junit.Assert.assertEquals;
import static org.mockito.AdditionalMatchers.or;
import static org.mockito.Matchers.anyCollection;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.isA;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Collection;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.runners.MockitoJUnitRunner;
import org.mockito.stubbing.Answer;

import com.bt.pi.app.common.entities.AvailabilityZone;
import com.bt.pi.app.common.entities.AvailabilityZones;
import com.bt.pi.app.common.entities.Instance;
import com.bt.pi.app.common.id.PiIdBuilder;
import com.bt.pi.app.instancemanager.handlers.InstanceManagerApplication;
import com.bt.pi.core.application.MessageContext;
import com.bt.pi.core.continuation.scattergather.PiScatterGatherContinuation;
import com.bt.pi.core.continuation.scattergather.ScatterGatherContinuationRunnable;
import com.bt.pi.core.continuation.scattergather.ScatterGatherContinuationRunner;
import com.bt.pi.core.dht.DhtClientFactory;
import com.bt.pi.core.dht.DhtReader;
import com.bt.pi.core.entity.EntityMethod;
import com.bt.pi.core.id.KoalaIdFactory;
import com.bt.pi.core.id.PId;

@RunWith(MockitoJUnitRunner.class)
public class RebootInstanceServiceHelperTest {
    private static final String AVAILABILITY_ZONE_1_NAME = "availabilityZone1";
    private static final int AVAILABILITY_ZONE_1_CODE = 10;
    private static final int REGION_ZONE_1_CODE = 1;
    @InjectMocks
    private RebootInstanceServiceHelper rebootInstanceServiceHelper = new RebootInstanceServiceHelper();
    @Mock
    private ApiApplicationManager apiApplicationManager;
    @Mock
    private DhtClientFactory dhtClientFactory;
    @Mock
    private ScatterGatherContinuationRunner scatterGatherContinuationRunner;
    @Mock
    private DhtReader dhtReader;
    @Mock
    private KoalaIdFactory koalaIdFactory;
    @Mock
    private PiIdBuilder piIdBuilder;
    private String ownerId = "ownerId";
    @Mock
    private MessageContext instanceManagerMessageContext;
    @Mock
    private MessageContext secondMessageContext;
    private Instance instance123;
    private Instance instance456;
    @Mock
    private PId id123;
    @Mock
    private PId id456;
    @Mock
    private PId nodeId;
    private String nodeIdStr = "aNodeIdStr";
    private Collection<String> instanceIds;

    @Before
    public void before() {
        when(koalaIdFactory.buildPId(eq(nodeIdStr))).thenReturn(nodeId);
        doAnswer(new Answer<PId>() {
            @Override
            public PId answer(InvocationOnMock invocation) throws Throwable {
                String id = (String) invocation.getArguments()[0];
                if (id.equals(Instance.getUrl("i-123")))
                    return id123;
                if (id.equals(Instance.getUrl("i-456")))
                    return id456;
                return null;
            }
        }).when(koalaIdFactory).buildId(isA(String.class));

        when(piIdBuilder.getPIdForEc2AvailabilityZone(anyString())).thenReturn(id123).thenReturn(id456);

        when(apiApplicationManager.newMessageContext()).thenReturn(instanceManagerMessageContext).thenReturn(secondMessageContext);
        when(apiApplicationManager.getKoalaIdFactory()).thenReturn(koalaIdFactory);

        AvailabilityZones zones = new AvailabilityZones();
        zones.addAvailabilityZone(new AvailabilityZone(AVAILABILITY_ZONE_1_NAME, AVAILABILITY_ZONE_1_CODE, REGION_ZONE_1_CODE, AVAILABILITY_ZONE_1_NAME));
        when(apiApplicationManager.getAvailabilityZonesRecord()).thenReturn(zones);

        when(koalaIdFactory.getRegion()).thenReturn(REGION_ZONE_1_CODE);

        setupDht();

        rebootInstanceServiceHelper.setPiIdBuilder(piIdBuilder);
    }

    private void setupDht() {
        when(dhtClientFactory.createReader()).thenReturn(dhtReader);
        setupInstancesInDht();
    }

    @SuppressWarnings("unchecked")
    private void setupInstancesInDht() {
        instance123 = setupInstanceWithReservationId("i-123", "r-123", ownerId);
        instance456 = setupInstanceWithReservationId("i-456", "r-123", ownerId);

        // setup dht reader to return the
        doAnswer(new Answer<Object>() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                PiScatterGatherContinuation<Instance> piContinuation = (PiScatterGatherContinuation<Instance>) invocation.getArguments()[1];

                if (invocation.getArguments()[0].equals(id123)) {
                    piContinuation.receiveResult(instance123);
                } else
                    piContinuation.receiveResult(instance456);
                return null;
            }
        }).when(dhtReader).getAnyAsync(or(eq(id123), eq(id456)), isA(PiScatterGatherContinuation.class));

        instanceIds = Arrays.asList(new String[] { "i-123", "i-456" });
        when(instance123.getNodeId()).thenReturn(nodeIdStr);
        when(instance456.getNodeId()).thenReturn(nodeIdStr);
    }

    private Instance setupInstanceWithReservationId(String instanceId, String reservationId, String userId) {
        Instance instance = mock(Instance.class);
        when(instance.getInstanceId()).thenReturn(instanceId);
        when(instance.getReservationId()).thenReturn(reservationId);
        when(instance.getUserId()).thenReturn(userId);
        when(instance.getLastHeartbeatTimestamp()).thenReturn(System.currentTimeMillis());
        return instance;
    }

    @Test
    public void shouldRebootMultipleInstances() {
        // setup
        final Collection<String> instanceIds = Arrays.asList(new String[] { "i-123", "i-456" });
        when(instance123.getNodeId()).thenReturn(nodeIdStr);
        when(instance456.getNodeId()).thenReturn(nodeIdStr);
        when(piIdBuilder.getNodeIdFromNodeId(anyString())).thenReturn(nodeId);
        setupScatterGather(instanceIds);

        // act
        rebootInstanceServiceHelper.rebootInstances(ownerId, instanceIds, apiApplicationManager);

        // verify
        verify(instance123).setRestartRequested(eq(true));
        verify(instance456).setRestartRequested(eq(true));
        verify(instanceManagerMessageContext).routePiMessageToApplication(eq(nodeId), eq(EntityMethod.UPDATE), eq(instance123), eq(InstanceManagerApplication.APPLICATION_NAME));
        verify(secondMessageContext).routePiMessageToApplication(eq(nodeId), eq(EntityMethod.UPDATE), eq(instance456), eq(InstanceManagerApplication.APPLICATION_NAME));
    }

    @Test
    public void shouldNotRebootMultipleInstancesDueToAnInvalidUser() {
        // setup

        setupScatterGather(instanceIds);

        // act
        rebootInstanceServiceHelper.rebootInstances("nonOwner", instanceIds, apiApplicationManager);

        // verify
        verify(instance123, never()).setRestartRequested(eq(true));
        verify(instance456, never()).setRestartRequested(eq(true));
        verify(instanceManagerMessageContext, never()).routePiMessageToApplication(eq(nodeId), eq(EntityMethod.UPDATE), eq(instance123), eq(InstanceManagerApplication.APPLICATION_NAME));
        verify(secondMessageContext, never()).routePiMessageToApplication(eq(nodeId), eq(EntityMethod.UPDATE), eq(instance456), eq(InstanceManagerApplication.APPLICATION_NAME));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void shouldRebootSingleInstance() {
        // setup
        instanceIds = Arrays.asList(new String[] { "i-123" });

        // act
        rebootInstanceServiceHelper.rebootInstances(ownerId, instanceIds, apiApplicationManager);

        // assert
        verify(scatterGatherContinuationRunner, times(1)).execute(anyCollection(), anyLong(), isA(TimeUnit.class));
    }

    @SuppressWarnings("unchecked")
    private void setupScatterGather(final Collection<String> instanceIds) {
        // setup scatter gather to run runnables
        doAnswer(new Answer<Object>() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                Collection<ScatterGatherContinuationRunnable> runnables = (Collection<ScatterGatherContinuationRunnable>) invocation.getArguments()[0];
                int i = 0;
                for (ScatterGatherContinuationRunnable runnable : runnables) {
                    runnable.setLatch(mock(CountDownLatch.class));
                    runnable.run();
                    i++;
                }
                assertEquals(instanceIds.size(), i);
                return null;
            }
        }).when(scatterGatherContinuationRunner).execute(anyCollection(), anyLong(), isA(TimeUnit.class));
    }
}
