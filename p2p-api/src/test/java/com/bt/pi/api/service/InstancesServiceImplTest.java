/* (c) British Telecommunications plc, 2009, All Rights Reserved */
package com.bt.pi.api.service;

import static org.junit.Assert.assertEquals;
import static org.mockito.AdditionalMatchers.or;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.isA;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.runners.MockitoJUnitRunner;
import org.mockito.stubbing.Answer;

import rice.p2p.commonapi.Id;

import com.bt.pi.api.entities.ReservationInstances;
import com.bt.pi.api.utils.IdFactory;
import com.bt.pi.app.common.entities.ImageIndex;
import com.bt.pi.app.common.entities.Instance;
import com.bt.pi.app.common.entities.Reservation;
import com.bt.pi.app.common.entities.SecurityGroup;
import com.bt.pi.app.common.entities.User;
import com.bt.pi.app.common.id.PiIdBuilder;
import com.bt.pi.app.common.resource.PiTopics;
import com.bt.pi.app.instancemanager.handlers.InstanceStateTransition;
import com.bt.pi.app.instancemanager.handlers.TerminateInstanceServiceHelper;
import com.bt.pi.core.application.MessageContext;
import com.bt.pi.core.application.PubSubMessageContext;
import com.bt.pi.core.continuation.scattergather.PiScatterGatherContinuation;
import com.bt.pi.core.continuation.scattergather.UpdateResolvingPiScatterGatherContinuation;
import com.bt.pi.core.dht.BlockingDhtReader;
import com.bt.pi.core.dht.BlockingDhtWriter;
import com.bt.pi.core.dht.DhtClientFactory;
import com.bt.pi.core.dht.DhtReader;
import com.bt.pi.core.dht.DhtWriter;
import com.bt.pi.core.id.KoalaIdFactory;
import com.bt.pi.core.id.PId;

@RunWith(MockitoJUnitRunner.class)
public class InstancesServiceImplTest {
    @InjectMocks
    private InstancesServiceImpl instancesService = new InstancesServiceImpl();
    private String nodeIdStr = "aNodeIdStr";
    @Mock
    private Reservation reservation;
    @Mock
    private ApiApplicationManager apiApplicationManager;
    @Mock
    private DhtClientFactory dhtClientFactory;
    @Mock
    private BlockingDhtReader blockingDhtReader;
    @Mock
    private BlockingDhtWriter blockingDhtWriter;
    @Mock
    private DhtReader dhtReader;
    @Mock
    private DhtWriter dhtWriter;
    @Mock
    private KoalaIdFactory koalaIdFactory;
    private PiIdBuilder piIdBuilder;
    private ConcurrentMap<String, Set<Instance>> instanceMap;
    @Mock
    private DescribeInstancesServiceHelper describeInstancesServiceHelper;
    @Mock
    private PId instanceId;
    @Mock
    private PId securityGroupId;
    private PId userId;
    private String ownerId = "ownerId";
    private User user;
    @Mock
    private UserManagementService userManagementService;
    @Mock
    private MessageContext instanceManagerMessageContext;
    @Mock
    private MessageContext secondMessageContext;
    @Mock
    private PubSubMessageContext pubSubMessageContext;
    @Mock
    private TerminateInstanceServiceHelper terminateInstanceServiceHelper;
    private Instance instance123;
    private Instance instance456;
    @Mock
    private PId id123;
    @Mock
    private PId id456;
    @Mock
    private Id nodeId;
    @Mock
    private RebootInstanceServiceHelper rebootInstanceServiceHelper;
    @Mock
    private PId reservationId;
    private ImageIndex imageIndex;
    @Mock
    private RunInstancesServiceHelper runInstancesServiceHelper;

    @SuppressWarnings("unchecked")
    @Before
    public void before() {
        instanceMap = new ConcurrentHashMap<String, Set<Instance>>();

        when(securityGroupId.forLocalRegion()).thenReturn(securityGroupId);

        when(koalaIdFactory.buildPId("user:userid")).thenReturn(userId);
        when(koalaIdFactory.buildPId(SecurityGroup.getUrl("userid", "default"))).thenReturn(securityGroupId);
        when(koalaIdFactory.buildIdFromToString(eq(nodeIdStr))).thenReturn(nodeId);
        doAnswer(new Answer<PId>() {
            @Override
            public PId answer(InvocationOnMock invocation) throws Throwable {
                String id = (String) invocation.getArguments()[0];
                if (id.equals(Instance.getUrl("i-123")))
                    return id123;
                if (id.equals(Instance.getUrl("i-456")))
                    return id456;
                if (id.startsWith("res:"))
                    return reservationId;
                return instanceId;
            }
        }).when(koalaIdFactory).buildId(isA(String.class));

        piIdBuilder = new PiIdBuilder();
        piIdBuilder.setKoalaIdFactory(koalaIdFactory);

        when(apiApplicationManager.newMessageContext()).thenReturn(instanceManagerMessageContext).thenReturn(secondMessageContext);
        when(apiApplicationManager.newLocalPubSubMessageContext(PiTopics.DECRYPT_IMAGE)).thenReturn(pubSubMessageContext);
        when(apiApplicationManager.getKoalaIdFactory()).thenReturn(koalaIdFactory);

        setupDht();

        when(describeInstancesServiceHelper.getInstances(isA(Collection.class))).thenReturn(instanceMap);

        instancesService.setPiIdBuilder(piIdBuilder);
        instancesService.setIdFactory(new IdFactory());
    }

    private void setupDht() {
        when(dhtClientFactory.createBlockingReader()).thenReturn(blockingDhtReader);
        when(dhtClientFactory.createBlockingWriter()).thenReturn(blockingDhtWriter);
        when(dhtClientFactory.createReader()).thenReturn(dhtReader);
        when(dhtClientFactory.createWriter()).thenReturn(dhtWriter);

        setupUserInDht();
        setupInstancesInDht();
        PId imageIndexId = mock(PId.class);
        when(imageIndexId.forLocalAvailabilityZone()).thenReturn(imageIndexId);
        when(this.koalaIdFactory.buildPId(ImageIndex.URL)).thenReturn(imageIndexId);
        imageIndex = new ImageIndex();
        when(blockingDhtReader.get(imageIndexId)).thenReturn(imageIndex);
    }

    @SuppressWarnings("unchecked")
    private void setupInstancesInDht() {
        instance123 = setupInstanceWithReservationId("i-123", "r-123", ownerId);
        instance456 = setupInstanceWithReservationId("i-456", "r-123", ownerId);

        doAnswer(new Answer<Object>() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                Id id = (Id) invocation.getArguments()[0];
                UpdateResolvingPiScatterGatherContinuation updateResolvingContinuation = (UpdateResolvingPiScatterGatherContinuation) invocation.getArguments()[1];

                updateResolvingContinuation.receiveResult(id == id123 ? instance123 : instance456);
                return null;
            }
        }).when(dhtWriter).update(isA(PId.class), isA(UpdateResolvingPiScatterGatherContinuation.class));

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
    }

    private void setupUserInDht() {
        user = new User();
        user.addInstance("i-123");
        user.addInstance("i-456");

        userId = piIdBuilder.getPId(User.getUrl(ownerId));
        when(this.userManagementService.getUser(ownerId)).thenReturn(user);
    }

    @Test
    public void testRunInstancesDelegatesToHelper() {
        // setup
        ReservationInstances expectedResult = mock(ReservationInstances.class);
        when(runInstancesServiceHelper.runInstances(reservation)).thenReturn(expectedResult);

        // act
        ReservationInstances result = this.instancesService.runInstances(reservation);

        // assert
        assertEquals(expectedResult, result);
    }

    @Test
    public void shouldTerminateSingleInstance() {
        // setup
        List<String> instanceIds = new ArrayList<String>();
        instanceIds.add("i-123");

        Map<String, InstanceStateTransition> instanceTransitionMap = new HashMap<String, InstanceStateTransition>();
        when(terminateInstanceServiceHelper.terminateInstance(eq(ownerId), eq(instanceIds))).thenReturn(instanceTransitionMap);

        // act
        Map<String, InstanceStateTransition> result = instancesService.terminateInstances(ownerId, instanceIds);

        // assert
        assertEquals(instanceTransitionMap, result);
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
    public void shouldDescribeInstanceGivenSingleInstance() {
        // setup
        instanceMap.put("r-123", new HashSet<Instance>(Arrays.asList(new Instance[] { instance123 })));
        Collection<String> instanceIds = Arrays.asList(new String[] { "i-123" });

        // act
        Map<String, Set<Instance>> instances = instancesService.describeInstances(ownerId, instanceIds);

        // assert
        assertEquals(1, instances.size());
        verify(describeInstancesServiceHelper).getInstanceIdsForUser(instanceIds, userId);
    }

    @Test
    public void shouldDescribeInstancesGivenMultipleInstancesSameReservationId() {
        // setup
        instanceMap.put("r-123", new HashSet<Instance>(Arrays.asList(new Instance[] { instance123, instance456 })));
        Collection<String> instanceIds = Arrays.asList(new String[] { "i-123", "i-456" });

        // act
        Map<String, Set<Instance>> instances = instancesService.describeInstances(ownerId, instanceIds);

        // assert
        assertEquals(1, instances.size());
        verify(describeInstancesServiceHelper).getInstanceIdsForUser(instanceIds, userId);
    }

    @Test
    public void shouldDescribeInstanceGivenMultipleInstancesDifferentReservationId() {
        // setup
        when(instance456.getReservationId()).thenReturn("r-456");
        instanceMap.put("r-123", new HashSet<Instance>(Arrays.asList(new Instance[] { instance123 })));
        instanceMap.put("r-456", new HashSet<Instance>(Arrays.asList(new Instance[] { instance456 })));
        Collection<String> instanceIds = Arrays.asList(new String[] { "i-123", "i-456" });

        // act
        Map<String, Set<Instance>> instances = instancesService.describeInstances(ownerId, instanceIds);

        // assert
        assertEquals(2, instances.size());
        instances.containsKey("r-123");
        instances.containsKey("r-456");
        verify(describeInstancesServiceHelper).getInstanceIdsForUser(instanceIds, userId);
    }

    @Test
    public void shouldDescribeInstanceGivenMultipleInstancesWithDifferentReservationOwners() {
        // setup
        instanceMap.put("r-123", new HashSet<Instance>(Arrays.asList(new Instance[] { instance123 })));
        Collection<String> instanceIds = Arrays.asList(new String[] { "i-123", "i-456" });

        // act
        Map<String, Set<Instance>> instances = instancesService.describeInstances(ownerId, instanceIds);

        // assert
        assertEquals(1, instances.size());
        instances.containsKey("r-123");
    }

    @Test
    public void shouldDescribeInstancesGivenNoInstanceIdsWithSingleReservation() {
        // setup
        instanceMap.put("r-123", new HashSet<Instance>(Arrays.asList(new Instance[] { instance123 })));

        // act
        Map<String, Set<Instance>> instances = instancesService.describeInstances(ownerId, new ArrayList<String>());

        // assert
        assertEquals(1, instances.size());
    }

    @Test
    public void shouldDescribeInstancesGivenNoInstanceIdsWithMultipleReservations() {
        // setup
        instanceMap.put("r-123", new HashSet<Instance>(Arrays.asList(new Instance[] { instance123 })));
        instanceMap.put("r-456", new HashSet<Instance>(Arrays.asList(new Instance[] { instance456 })));

        // act
        Map<String, Set<Instance>> instances = instancesService.describeInstances(ownerId, new ArrayList<String>());

        // assert
        assertEquals(2, instances.size());
        instances.containsKey("r-123");
        instances.containsKey("r-456");
    }

    @Test
    public void shouldRebootSingleInstance() {
        // setup
        Collection<String> instanceIds = Arrays.asList(new String[] { "i-123" });

        // act
        instancesService.rebootInstances(ownerId, instanceIds);

        // assert
        verify(rebootInstanceServiceHelper, times(1)).rebootInstances(eq(ownerId), eq(instanceIds), eq(apiApplicationManager));
    }

    @Test
    public void shouldRebootMultipleInstances() {
        // setup
        final Collection<String> instanceIds = Arrays.asList(new String[] { "i-123", "i-456" });

        // act
        instancesService.rebootInstances(ownerId, instanceIds);

        // verify
        verify(rebootInstanceServiceHelper, times(1)).rebootInstances(eq(ownerId), eq(instanceIds), eq(apiApplicationManager));
    }
}
