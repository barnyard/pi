package com.bt.pi.api.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.argThat;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.isA;
import static org.mockito.Matchers.isNull;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentMatcher;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.runners.MockitoJUnitRunner;
import org.mockito.stubbing.Answer;

import com.bt.pi.app.common.entities.AvailabilityZone;
import com.bt.pi.app.common.entities.AvailabilityZones;
import com.bt.pi.app.common.entities.Instance;
import com.bt.pi.app.common.entities.InstanceRecord;
import com.bt.pi.app.common.entities.InstanceState;
import com.bt.pi.app.common.entities.PublicIpAddress;
import com.bt.pi.app.common.entities.PublicIpAllocationIndex;
import com.bt.pi.app.common.entities.User;
import com.bt.pi.app.common.entities.util.ResourceRange;
import com.bt.pi.app.common.id.PiIdBuilder;
import com.bt.pi.app.common.net.utils.IpAddressUtils;
import com.bt.pi.app.common.resource.PiQueue;
import com.bt.pi.app.networkmanager.NetworkManagerApplication;
import com.bt.pi.core.application.MessageContext;
import com.bt.pi.core.application.watcher.task.TaskProcessingQueueContinuation;
import com.bt.pi.core.application.watcher.task.TaskProcessingQueueHelper;
import com.bt.pi.core.continuation.UpdateResolver;
import com.bt.pi.core.dht.BlockingDhtReader;
import com.bt.pi.core.dht.BlockingDhtWriter;
import com.bt.pi.core.dht.DhtClientFactory;
import com.bt.pi.core.dht.cache.BlockingDhtCache;
import com.bt.pi.core.entity.EntityMethod;
import com.bt.pi.core.entity.PiEntity;
import com.bt.pi.core.id.PId;
import com.bt.pi.core.testing.UpdateResolverAnswer;

@RunWith(MockitoJUnitRunner.class)
public class ElasticIpAddressesServiceImplTest {
    private static final String NO_INSTANCE_ID = "i-noinstance";
    private static final String INSTANCE_ID = "i-nst";
    @InjectMocks
    private ElasticIpAddressesServiceImpl elasticIpAddressesService = new ElasticIpAddressesServiceImpl();
    private String existingOwnerId = "existing-owner-id";
    private String newOwnerId = "new-owner-id";
    private List<String> addresses = new ArrayList<String>();
    private PublicIpAllocationIndex publicIpAllocationIndex;
    private Instance instance;
    private User user;
    private UpdateResolverAnswer instanceUpdateResolverAnswer;
    private UpdateResolverAnswer publicIpIndexUpdateResolverAnswer;
    @Mock
    private MessageContext messageContext;
    @Mock
    private TaskProcessingQueueHelper taskProcessingQueueHelper;
    @Mock
    private PId publicIpIndexRecordId;
    @Mock
    private PId userRecordId;
    @Mock
    private PId securityGroupRecordId;
    @Mock
    private PId instancePId;
    @Mock
    private DhtClientFactory dhtClientFactory;
    @Mock
    private BlockingDhtWriter dhtWriterForPublicIpAddress;
    @Mock
    private BlockingDhtWriter dhtWriterForInstance;
    @Mock
    private PiIdBuilder piIdBuilder;
    @Mock
    private BlockingDhtReader dhtReader;
    @Mock
    private BlockingDhtCache blockingDhtCache;
    @Mock
    private ApiApplicationManager apiApplicationManager;
    // @Mock
    // private KoalaIdFactory koalaIdFactory;
    @Mock
    private DescribeInstancesServiceHelper describeInstancesServiceHelper;
    private ElasticIpAddressOperationHelper elasticIpAddressOperationHelper;
    @Mock
    private PId associateAddressQueueId;
    @Mock
    private PId disassociateAddressQueueId;
    private int globalAvzCodeFromInstanceId = 0x3322;
    private CountDownLatch continuationLatch;

    @SuppressWarnings("unchecked")
    @Before
    public void before() {
        instance = spy(new Instance());
        instance.setInstanceId(INSTANCE_ID);
        instance.setSecurityGroupName("default");
        instance.setUserId(existingOwnerId);
        instance.setState(InstanceState.RUNNING);

        user = new User();
        user.setUsername(existingOwnerId);
        user.addInstance(INSTANCE_ID);

        when(piIdBuilder.getPId(PublicIpAllocationIndex.URL)).thenReturn(publicIpIndexRecordId);
        when(piIdBuilder.getPIdForEc2AvailabilityZone(Instance.getUrl(INSTANCE_ID))).thenReturn(instancePId);
        when(blockingDhtCache.getReadThrough(instancePId)).thenReturn(instance);
        when(publicIpIndexRecordId.forLocalRegion()).thenReturn(publicIpIndexRecordId);
        when(piIdBuilder.getPId(User.getUrl(existingOwnerId))).thenReturn(userRecordId);
        when(piIdBuilder.getGlobalAvailabilityZoneCodeFromEc2Id(INSTANCE_ID)).thenReturn(123);
        when(piIdBuilder.getPId("sg:" + existingOwnerId + ":" + "default")).thenReturn(securityGroupRecordId);
        when(securityGroupRecordId.forGlobalAvailablityZoneCode(123)).thenReturn(securityGroupRecordId);
        when(securityGroupRecordId.forGlobalAvailablityZoneCode(13090)).thenReturn(securityGroupRecordId);
        when(piIdBuilder.getGlobalAvailabilityZoneCodeFromEc2Id(INSTANCE_ID)).thenReturn(globalAvzCodeFromInstanceId);
        when(piIdBuilder.getPId(PiQueue.ASSOCIATE_ADDRESS.getUrl())).thenReturn(associateAddressQueueId);
        when(associateAddressQueueId.forRegion(0x33)).thenReturn(associateAddressQueueId);
        when(piIdBuilder.getPId(PiQueue.DISASSOCIATE_ADDRESS.getUrl())).thenReturn(disassociateAddressQueueId);
        when(disassociateAddressQueueId.forRegion(0x33)).thenReturn(disassociateAddressQueueId);
        Set<ResourceRange> addressRanges = new HashSet<ResourceRange>();
        addressRanges.add(new ResourceRange(IpAddressUtils.ipToLong("10.0.0.0"), IpAddressUtils.ipToLong("10.0.0.100")));

        publicIpAllocationIndex = new PublicIpAllocationIndex();
        publicIpAllocationIndex.setResourceRanges(addressRanges);
        publicIpAllocationIndex.allocateElasticIpAddressToUser(existingOwnerId);

        publicIpIndexUpdateResolverAnswer = new UpdateResolverAnswer(publicIpAllocationIndex);
        instanceUpdateResolverAnswer = new UpdateResolverAnswer(instance);

        when(dhtWriterForPublicIpAddress.getValueWritten()).thenReturn(publicIpAllocationIndex);
        when(dhtWriterForInstance.getValueWritten()).thenReturn(instance);

        doAnswer(publicIpIndexUpdateResolverAnswer).when(dhtWriterForPublicIpAddress).update(eq(publicIpIndexRecordId), (PiEntity) isNull(), isA(UpdateResolver.class));
        doAnswer(instanceUpdateResolverAnswer).when(dhtWriterForInstance).update(eq(instancePId), (PiEntity) isNull(), isA(UpdateResolver.class));

        when(dhtReader.get(publicIpIndexRecordId)).thenReturn(publicIpAllocationIndex);
        when(dhtClientFactory.createBlockingWriter()).thenReturn(dhtWriterForPublicIpAddress).thenReturn(dhtWriterForInstance).thenReturn(dhtWriterForPublicIpAddress).thenReturn(dhtWriterForInstance);
        when(dhtClientFactory.createBlockingReader()).thenReturn(dhtReader);
        when(blockingDhtCache.getReadThrough(userRecordId)).thenReturn(user);

        AvailabilityZones zones = new AvailabilityZones();
        zones.addAvailabilityZone(new AvailabilityZone("1", 1, 2, "on"));
        when(apiApplicationManager.getAvailabilityZonesRecord()).thenReturn(zones);

        when(apiApplicationManager.newMessageContext()).thenReturn(messageContext);

        // when(koalaIdFactory.getRegion()).thenReturn(1);

        elasticIpAddressOperationHelper = new ElasticIpAddressOperationHelper();
        elasticIpAddressOperationHelper.setPiIdBuilder(piIdBuilder);
        elasticIpAddressOperationHelper.setBlockingDhtCache(blockingDhtCache);
        elasticIpAddressOperationHelper.setDhtClientFactory(dhtClientFactory);
        elasticIpAddressOperationHelper.setTaskProcessingQueueHelper(taskProcessingQueueHelper);
        elasticIpAddressOperationHelper.setMessageContextFactory(apiApplicationManager);
        elasticIpAddressOperationHelper.setApiApplicationManager(apiApplicationManager);
        // elasticIpAddressOperationHelper.setKoalaIdFactory(koalaIdFactory);
        this.elasticIpAddressesService.setElasticIpAddressOperationHelper(elasticIpAddressOperationHelper);

        ConcurrentMap<String, Set<Instance>> instancesMap = new ConcurrentHashMap<String, Set<Instance>>();
        Set<Instance> instancesSet = new HashSet<Instance>();
        instancesSet.add(instance);
        instancesMap.put(INSTANCE_ID, instancesSet);
        when(describeInstancesServiceHelper.getInstances(Arrays.asList(INSTANCE_ID))).thenReturn(instancesMap);
        when(describeInstancesServiceHelper.getInstances(Arrays.asList(NO_INSTANCE_ID))).thenReturn(new ConcurrentHashMap<String, Set<Instance>>());

        this.continuationLatch = new CountDownLatch(1);
        doAnswer(new Answer<Object>() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                ((TaskProcessingQueueContinuation) invocation.getArguments()[3]).receiveResult(null, null);
                continuationLatch.countDown();
                return null;
            }
        }).when(taskProcessingQueueHelper).addUrlToQueue(isA(PId.class), isA(String.class), anyInt(), isA(TaskProcessingQueueContinuation.class));
    }

    @After
    public void after() {
        instance.setState(InstanceState.RUNNING);
    }

    @Test
    public void allocateAddressShouldAddAddressToPublicIpAddressRecord() {
        // act
        String res = elasticIpAddressesService.allocateAddress(newOwnerId);

        // assert
        assertEquals("10.0.0.1", res);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void allocateAddressShouldAddAddressToPublicIpAddressRecordEvenIfOneUpdateFails() {
        // setup
        doAnswer(new Answer<Object>() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                UpdateResolver updateResolver = (UpdateResolver) invocation.getArguments()[2];
                updateResolver.update(publicIpAllocationIndex, publicIpAllocationIndex);
                updateResolver.update(publicIpAllocationIndex, publicIpAllocationIndex);
                return null;
            }
        }).when(dhtWriterForPublicIpAddress).update(eq(publicIpIndexRecordId), (PiEntity) isNull(), isA(UpdateResolver.class));

        // act
        String res = elasticIpAddressesService.allocateAddress(newOwnerId);

        // assert
        assertEquals("10.0.0.2", res);
    }

    @Test
    public void allocateAddressShouldReturnNullIfPublicIpAddressRecordNotWritten() {
        // setup
        when(dhtWriterForPublicIpAddress.getValueWritten()).thenReturn(null);

        // act
        String res = elasticIpAddressesService.allocateAddress(existingOwnerId);

        // assert
        assertNull(res);
    }

    @Test
    public void associateAddressShouldWriteAddressToInstanceAndPublicIpIndexThenPutOnQueueAndSendMessage() throws InterruptedException {
        // setup
        publicIpAllocationIndex.allocateElasticIpAddressToUser(existingOwnerId);

        // act
        boolean res = elasticIpAddressesService.associateAddress(existingOwnerId, "10.0.0.0", INSTANCE_ID);

        // assert
        assertTrue(continuationLatch.await(250, TimeUnit.MILLISECONDS));
        assertTrue(res);
        assertEquals("10.0.0.0", instance.getPublicIpAddress());
        assertEquals(INSTANCE_ID, publicIpAllocationIndex.getCurrentAllocations().get(IpAddressUtils.ipToLong("10.0.0.0")).getInstanceId());
        verify(taskProcessingQueueHelper).addUrlToQueue(eq(associateAddressQueueId), eq("addr:10.0.0.0;sg=" + existingOwnerId + ":default;inst=" + INSTANCE_ID), eq(5), isA(TaskProcessingQueueContinuation.class));
        verify(messageContext).routePiMessageToApplication(eq(securityGroupRecordId), eq(EntityMethod.CREATE), argThat(new ArgumentMatcher<PublicIpAddress>() {
            @Override
            public boolean matches(Object argument) {
                PublicIpAddress addr = (PublicIpAddress) argument;
                assertEquals("10.0.0.0", addr.getIpAddress());
                assertEquals(INSTANCE_ID, addr.getInstanceId());
                assertEquals(existingOwnerId, addr.getOwnerId());
                assertEquals("default", addr.getSecurityGroupName());
                return true;
            }
        }), eq(NetworkManagerApplication.APPLICATION_NAME));
    }

    @Test(expected = NotFoundException.class)
    public void associateAddressShouldFailForUserWhoDoesNotOwnThisInstance() {
        // setup
        user.removeInstance(INSTANCE_ID);

        // act
        elasticIpAddressesService.associateAddress(existingOwnerId, "10.0.0.0", NO_INSTANCE_ID);
    }

    @Test(expected = UserNotFoundException.class)
    public void associateAddressShouldFailForUserWhoDoesNotOwnThisAddress() {
        // setup
        when(blockingDhtCache.getReadThrough(userRecordId)).thenReturn(null);

        // act
        elasticIpAddressesService.associateAddress("moo cow", "10.0.0.0", INSTANCE_ID);
    }

    @Test(expected = IllegalArgumentException.class)
    public void associateAddressShouldWrapPublicIpAllocationException() {
        // setup
        publicIpAllocationIndex.releaseElasticIpAddressForUser("10.0.0.0", existingOwnerId);

        // act
        elasticIpAddressesService.associateAddress(existingOwnerId, "10.0.0.0", INSTANCE_ID);
    }

    @Test(expected = NotFoundException.class)
    public void associateAddressShouldFailWhenInstanceUpdateFails() {
        // setup
        publicIpAllocationIndex.allocateElasticIpAddressToUser(existingOwnerId);
        instanceUpdateResolverAnswer = new UpdateResolverAnswer(null);

        // act
        elasticIpAddressesService.associateAddress(existingOwnerId, "10.0.0.0", NO_INSTANCE_ID);
    }

    @Test
    public void disassociateAddressShouldClearAddressFromInstanceAndPublicIpIndexAndPutOnQueue() throws InterruptedException {
        // setup
        publicIpAllocationIndex.allocateElasticIpAddressToUser(existingOwnerId);
        elasticIpAddressesService.associateAddress(existingOwnerId, "10.0.0.0", INSTANCE_ID);

        // act
        boolean res = elasticIpAddressesService.disassociateAddress(existingOwnerId, "10.0.0.0");

        // assert
        assertTrue(continuationLatch.await(250, TimeUnit.MILLISECONDS));
        assertTrue(res);
        assertEquals(null, instance.getPublicIpAddress());
        assertEquals(null, publicIpAllocationIndex.getCurrentAllocations().get(IpAddressUtils.ipToLong("10.0.0.0")).getInstanceId());
        verify(taskProcessingQueueHelper).addUrlToQueue(eq(disassociateAddressQueueId), eq("addr:10.0.0.0;sg=" + existingOwnerId + ":default;inst=" + INSTANCE_ID), eq(5), isA(TaskProcessingQueueContinuation.class));
        verify(messageContext).routePiMessageToApplication(eq(securityGroupRecordId), eq(EntityMethod.DELETE), argThat(new ArgumentMatcher<PublicIpAddress>() {
            @Override
            public boolean matches(Object argument) {
                PublicIpAddress addr = (PublicIpAddress) argument;
                assertEquals("10.0.0.0", addr.getIpAddress());
                assertEquals(INSTANCE_ID, addr.getInstanceId());
                assertEquals(existingOwnerId, addr.getOwnerId());
                assertEquals("default", addr.getSecurityGroupName());
                return true;
            }
        }), eq(NetworkManagerApplication.APPLICATION_NAME));
    }

    @Test(expected = NotFoundException.class)
    public void disassociateAddressShouldFailWhenInstanceUpdateFails() {
        // setup
        publicIpAllocationIndex.allocateElasticIpAddressToUser(existingOwnerId);
        elasticIpAddressesService.associateAddress(existingOwnerId, "10.0.0.0", INSTANCE_ID);
        when(dhtWriterForInstance.getValueWritten()).thenReturn(null);

        // act
        elasticIpAddressesService.disassociateAddress(existingOwnerId, "10.0.0.0");
    }

    @Test(expected = IllegalArgumentException.class)
    public void disassociateAddressShouldWrapPublicIpAllocationException() {
        // act
        elasticIpAddressesService.disassociateAddress(existingOwnerId, "3.3.3.3");
    }

    @Test
    public void disassociateAddressShouldNotCallNetworkManagerIfInstanceIsAlreadyTerminated() {
        // setup
        publicIpAllocationIndex.allocateElasticIpAddressToUser(existingOwnerId);
        publicIpAllocationIndex.allocateIpAddressToInstance(null);
        elasticIpAddressOperationHelper = mock(ElasticIpAddressOperationHelper.class);
        elasticIpAddressesService.setElasticIpAddressOperationHelper(elasticIpAddressOperationHelper);

        // act
        boolean res = elasticIpAddressesService.disassociateAddress(existingOwnerId, "10.0.0.0");

        // assert
        assertTrue(res);
        verify(elasticIpAddressOperationHelper, never()).writePublicIpToInstanceRecord(anyString(), anyString());
        verify(elasticIpAddressOperationHelper, never()).enqueueTask(isA(PublicIpAddress.class), isA(PiQueue.class), isA(TaskProcessingQueueContinuation.class));
        verify(elasticIpAddressOperationHelper, never()).sendDisassociateElasticIpAddressRequestToSecurityGroupNode(isA(PublicIpAddress.class));
    }

    @Test
    public void shouldDescribeAllAddressesIfNoShortlistGiven() {
        // setup
        publicIpAllocationIndex.allocateElasticIpAddressToUser(newOwnerId);
        publicIpAllocationIndex.allocateElasticIpAddressToUser(existingOwnerId);
        publicIpAllocationIndex.getCurrentAllocations().get(IpAddressUtils.ipToLong("10.0.0.2")).setInstanceId("i-abc");

        // act
        Map<String, InstanceRecord> res = elasticIpAddressesService.describeAddresses(existingOwnerId, addresses);

        // assert
        assertEquals(2, res.size());
        assertEquals(existingOwnerId, res.get("10.0.0.0").getOwnerId());
        assertEquals(existingOwnerId, res.get("10.0.0.2").getOwnerId());
        assertEquals(null, res.get("10.0.0.0").getInstanceId());
        assertEquals("i-abc", res.get("10.0.0.2").getInstanceId());
    }

    @Test
    public void shouldDescribeAddressesFromShortlist() {
        // setup
        addresses.add("10.0.0.0");
        addresses.add("10.0.0.3");
        addresses.add("10.0.0.99");

        publicIpAllocationIndex.allocateElasticIpAddressToUser(newOwnerId);
        publicIpAllocationIndex.allocateElasticIpAddressToUser(existingOwnerId);
        publicIpAllocationIndex.allocateElasticIpAddressToUser(existingOwnerId);

        // act
        Map<String, InstanceRecord> res = elasticIpAddressesService.describeAddresses(existingOwnerId, addresses);

        // assert
        assertEquals(2, res.size());
        assertEquals(existingOwnerId, res.get("10.0.0.0").getOwnerId());
        assertEquals(existingOwnerId, res.get("10.0.0.3").getOwnerId());
    }

    @Test
    public void shouldReleaseAllocatedAddress() {
        // act
        boolean res = elasticIpAddressesService.releaseAddress(existingOwnerId, "10.0.0.0");

        // assert
        assertTrue(res);
    }

    @Test
    public void shouldFailToReleaseNonAllocatedAddress() {
        // act
        boolean res = elasticIpAddressesService.releaseAddress(existingOwnerId, "10.0.0.99");

        // assert
        assertFalse(res);
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldNotValidateTerminatedInstances() {
        // setup

        when(instance.getState()).thenReturn(InstanceState.TERMINATED);
        // act
        elasticIpAddressesService.associateAddress(existingOwnerId, "12.0.0.1", INSTANCE_ID);
    }
}
