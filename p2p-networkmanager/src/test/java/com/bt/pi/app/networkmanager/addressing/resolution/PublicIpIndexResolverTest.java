package com.bt.pi.app.networkmanager.addressing.resolution;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.argThat;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.isA;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentMatcher;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.runners.MockitoJUnitRunner;
import org.mockito.stubbing.Answer;
import org.springframework.util.ReflectionUtils;

import rice.Continuation;
import rice.pastry.Id;
import rice.pastry.NodeHandle;

import com.bt.pi.app.common.entities.Instance;
import com.bt.pi.app.common.entities.InstanceRecord;
import com.bt.pi.app.common.entities.InstanceState;
import com.bt.pi.app.common.entities.PublicIpAllocationIndex;
import com.bt.pi.app.common.id.PiIdBuilder;
import com.bt.pi.app.common.net.utils.IpAddressUtils;
import com.bt.pi.core.application.KoalaPastryApplicationBase;
import com.bt.pi.core.continuation.UpdateResolvingPiContinuation;
import com.bt.pi.core.continuation.scattergather.ScatterGatherContinuationRunnable;
import com.bt.pi.core.continuation.scattergather.ScatterGatherContinuationRunner;
import com.bt.pi.core.dht.BlockingDhtReader;
import com.bt.pi.core.dht.DhtClientFactory;
import com.bt.pi.core.dht.DhtReader;
import com.bt.pi.core.dht.DhtWriter;
import com.bt.pi.core.id.KoalaIdFactory;
import com.bt.pi.core.id.KoalaIdUtils;
import com.bt.pi.core.id.PId;
import com.bt.pi.core.node.NodeStartedEvent;
import com.bt.pi.core.parser.KoalaPiEntityFactory;
import com.bt.pi.core.scope.NodeScope;

@RunWith(MockitoJUnitRunner.class)
public class PublicIpIndexResolverTest {
    @Mock
    private PId pid;
    @Mock
    private KoalaIdUtils koalaIdUtils;
    @Mock
    private KoalaPastryApplicationBase application;
    @Mock
    private PiIdBuilder piIdBuilder;
    @Mock
    private DhtClientFactory dhtClientFactory;
    @Mock
    private BlockingDhtReader blockingDhtReader;
    @Mock
    private DhtReader dhtReader;
    @Mock
    private DhtWriter dhtWriter;
    @Mock
    private ScatterGatherContinuationRunner scatterGatherContinuationRunner;
    @Mock
    private Collection<NodeHandle> leafNodeHandles;

    private String nodeIdFull = "nodeIdFull";
    private String pidHex = "0123456789012345678901234567890123456789";
    private int instanceCount = 10;
    private List<PId> instancePIds = new ArrayList<PId>();
    private PublicIpAllocationIndex publicIpAllocationIndex = new PublicIpAllocationIndex();
    private CountDownLatch latch = new CountDownLatch(1);

    @InjectMocks
    private PublicIpIndexResolver publicIpIndexResolver = new PublicIpIndexResolver();

    @SuppressWarnings("unchecked")
    @Before
    public void setup() {
        when(pid.forLocalRegion()).thenReturn(pid);
        when(pid.getIdAsHex()).thenReturn(pidHex);

        when(application.getNodeIdFull()).thenReturn(nodeIdFull);
        when(application.getLeafNodeHandles()).thenReturn(leafNodeHandles);
        when(koalaIdUtils.isIdClosestToMe(nodeIdFull, leafNodeHandles, Id.build(pidHex), NodeScope.REGION)).thenReturn(true);

        populateInstancePIds();
        publicIpAllocationIndex.setAllocationMap(populateAllocationMap());

        when(piIdBuilder.getPId(PublicIpAllocationIndex.URL)).thenReturn(pid);
        doAnswer(new Answer<PId>() {
            @Override
            public PId answer(InvocationOnMock invocation) throws Throwable {
                String instanceId = (String) invocation.getArguments()[0];
                int counter = Integer.parseInt(String.format("%c", instanceId.charAt(instanceId.length() - 1)));
                return instancePIds.get(counter);
            }
        }).when(piIdBuilder).getPIdForEc2AvailabilityZone(isA(String.class));

        when(dhtClientFactory.createBlockingReader()).thenReturn(blockingDhtReader);
        when(dhtClientFactory.createReader()).thenReturn(dhtReader);
        when(dhtClientFactory.createWriter()).thenReturn(dhtWriter);

        when(blockingDhtReader.get(pid)).thenReturn(publicIpAllocationIndex);
        doAnswer(new Answer<Object>() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                Object updated = ((UpdateResolvingPiContinuation) invocation.getArguments()[1]).update(publicIpAllocationIndex, publicIpAllocationIndex);
                ((UpdateResolvingPiContinuation) invocation.getArguments()[1]).handleResult(updated);
                latch.countDown();

                return null;
            }
        }).when(dhtWriter).update(eq(pid), isA(UpdateResolvingPiContinuation.class));

        doAnswer(new Answer<Object>() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                Collection<ScatterGatherContinuationRunnable> runnables = (Collection<ScatterGatherContinuationRunnable>) invocation.getArguments()[0];
                for (ScatterGatherContinuationRunnable runnable : runnables) {
                    runnable.setLatch(new CountDownLatch(1));
                    runnable.run();
                }
                return null;
            }
        }).when(scatterGatherContinuationRunner).execute(isA(List.class), eq(20L), eq(TimeUnit.SECONDS));
    }

    private void populateInstancePIds() {
        for (int i = 0; i < instanceCount; i++) {
            PId instancePId = mock(PId.class);
            instancePIds.add(instancePId);
        }
    }

    private Instance populateInstance(int i) {
        Instance instance = new Instance();
        instance.setInstanceId(String.format("i-000ABC%d", i));
        instance.setState(InstanceState.RUNNING);
        return instance;
    }

    private Map<Long, InstanceRecord> populateAllocationMap() {
        Map<Long, InstanceRecord> allocationMap = new HashMap<Long, InstanceRecord>();
        for (int i = 0; i < instanceCount; i++)
            allocationMap.put(IpAddressUtils.ipToLong(String.format("10.0.0.%d", i)), populateInstanceRecord(i));

        return allocationMap;
    }

    private InstanceRecord populateInstanceRecord(int counter) {
        return new InstanceRecord(String.format("i-000ABC%d", counter), "unittest");
    }

    @Test
    public void publicIpIndexIdShouldStartWith0037() throws Exception {
        // setup
        KoalaIdFactory koalaIdFactory = new KoalaIdFactory(0, 0);
        koalaIdFactory.setKoalaPiEntityFactory(new KoalaPiEntityFactory());

        piIdBuilder = new PiIdBuilder();
        piIdBuilder.setKoalaIdFactory(koalaIdFactory);

        Field piIdBuilderField = ReflectionUtils.findField(PublicIpIndexResolver.class, "piIdBuilder");
        piIdBuilderField.setAccessible(true);
        ReflectionUtils.setField(piIdBuilderField, publicIpIndexResolver, piIdBuilder);

        Field publicIpIndexIdField = ReflectionUtils.findField(PublicIpIndexResolver.class, "publicIpIndexId");
        publicIpIndexIdField.setAccessible(true);
        ReflectionUtils.setField(publicIpIndexIdField, publicIpIndexResolver, null);

        publicIpIndexResolver.onApplicationEvent(new NodeStartedEvent(this));

        // act
        publicIpIndexResolver.resolve();

        // assert
        PId publicIpIndexId = (PId) ReflectionUtils.getField(publicIpIndexIdField, publicIpIndexResolver);
        assertTrue(publicIpIndexId.getIdAsHex(), publicIpIndexId.getIdAsHex().startsWith("0037"));
    }

    @Test
    public void shouldNotRemoveInstancesFromPublicIpIndexIfNotClosestInRegion() throws Exception {
        // setup
        publicIpIndexResolver.onApplicationEvent(new NodeStartedEvent(this));
        when(koalaIdUtils.isIdClosestToMe(nodeIdFull, leafNodeHandles, Id.build(pidHex), NodeScope.REGION)).thenReturn(false);
        populateSomeInstancesWith(null);

        // act
        publicIpIndexResolver.resolve();

        // assert
        assertFalse(latch.await(1, TimeUnit.SECONDS));
        assertEquals(10, publicIpAllocationIndex.getCurrentAllocations().size());
    }

    @Test
    public void shouldNotRemoveInstancesFromPublicIpIndexIfNodeStartedEventNotFired() throws Exception {
        // setup
        populateSomeInstancesWith(null);

        // act
        publicIpIndexResolver.resolve();

        // assert
        assertFalse(latch.await(1, TimeUnit.SECONDS));
        assertEquals(10, publicIpAllocationIndex.getCurrentAllocations().size());
    }

    @Test
    public void shouldRemoveNullInstancesFromPublicIpIndex() throws Exception {
        // setup
        publicIpIndexResolver.onApplicationEvent(new NodeStartedEvent(this));
        populateSomeInstancesWith(null);

        // act
        publicIpIndexResolver.resolve();

        // assert
        assertTrue(latch.await(1, TimeUnit.SECONDS));
        assertEquals(6, publicIpAllocationIndex.getCurrentAllocations().size());
    }

    @Test
    public void shouldRemoveTerminatedInstancesFromPublicIpIndex() throws Exception {
        // setup
        publicIpIndexResolver.onApplicationEvent(new NodeStartedEvent(this));
        populateSomeInstancesWith(InstanceState.TERMINATED);

        // act
        publicIpIndexResolver.resolve();

        // assert
        assertTrue(latch.await(1, TimeUnit.SECONDS));
        assertEquals(6, publicIpAllocationIndex.getCurrentAllocations().size());
    }

    @Test
    public void shouldRemoveFailedInstancesFromPublicIpIndex() throws Exception {
        // setup
        publicIpIndexResolver.onApplicationEvent(new NodeStartedEvent(this));
        populateSomeInstancesWith(InstanceState.FAILED);

        // act
        publicIpIndexResolver.resolve();

        // assert
        assertTrue(latch.await(1, TimeUnit.SECONDS));
        assertEquals(6, publicIpAllocationIndex.getCurrentAllocations().size());
    }

    @SuppressWarnings("unchecked")
    private void populateSomeInstancesWith(final InstanceState instanceState) {
        doAnswer(new Answer<Object>() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                for (int i = 0; i < instanceCount; i++) {
                    if (instancePIds.get(i).equals(invocation.getArguments()[0])) {
                        if (i % 3 == 0) {
                            if (instanceState == null)
                                ((Continuation) invocation.getArguments()[1]).receiveResult(null);
                            else {
                                Instance instance = populateInstance(i);
                                instance.setState(instanceState);
                                ((Continuation) invocation.getArguments()[1]).receiveResult(instance);
                            }
                        } else
                            ((Continuation) invocation.getArguments()[1]).receiveResult(populateInstance(i));
                    }
                }
                return null;
            }
        }).when(dhtReader).getAsync(argThat(new ArgumentMatcher<PId>() {
            @Override
            public boolean matches(Object argument) {
                return instancePIds.contains(argument);
            }
        }), isA(Continuation.class));
    }
}
