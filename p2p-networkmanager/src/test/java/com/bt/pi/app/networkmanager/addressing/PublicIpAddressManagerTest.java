package com.bt.pi.app.networkmanager.addressing;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.isA;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import rice.Continuation;

import com.bt.pi.app.common.entities.Instance;
import com.bt.pi.app.common.entities.InstanceAddress;
import com.bt.pi.app.common.entities.InstanceRecord;
import com.bt.pi.app.common.entities.NetworkRule;
import com.bt.pi.app.common.entities.NoFreePublicAddressesAvailableException;
import com.bt.pi.app.common.entities.PublicIpAllocationIndex;
import com.bt.pi.app.common.entities.SecurityGroup;
import com.bt.pi.app.common.entities.util.ResourceRange;
import com.bt.pi.app.common.id.PiIdBuilder;
import com.bt.pi.app.common.net.utils.IpAddressUtils;
import com.bt.pi.core.continuation.GenericContinuation;
import com.bt.pi.core.continuation.UpdateResolvingContinuation;
import com.bt.pi.core.dht.DhtClientFactory;
import com.bt.pi.core.dht.DhtReader;
import com.bt.pi.core.dht.DhtWriter;
import com.bt.pi.core.id.KoalaIdFactory;
import com.bt.pi.core.id.PId;
import com.bt.pi.core.parser.KoalaPiEntityFactory;
import com.bt.pi.core.testing.GenericContinuationAnswer;
import com.bt.pi.core.testing.TestFriendlyContinuation;
import com.bt.pi.core.testing.ThrowingContinuationAnswer;
import com.bt.pi.core.testing.UpdateResolvingContinuationAnswer;

@RunWith(MockitoJUnitRunner.class)
@SuppressWarnings("unchecked")
public class PublicIpAddressManagerTest {
    private static final String ALLOCATED_PUBLIC_ADDRESS_1 = "1.2.3.4";
    private static final String ALLOCATED_PUBLIC_ADDRESS_2 = "5.6.7.8";
    private static final String ALLOCATED_PUBLIC_ADDRESS_3 = "9.10.11.12";
    private static final String TEST_PUBLIC_IP = "111.222.222.111";
    private PublicIpAddressManager addressManager;
    private SecurityGroup securityGroup1;
    private SecurityGroup securityGroup2;
    private Instance instance;
    private String instanceWithNoPublicIp = "i-noPubIp";
    private String instanceId1 = "i-nstId";
    private String instanceId2 = "i-nstId2";
    private String instanceId3 = "i-nstId3";
    private String instanceId4 = "i-nstId4";
    private KoalaIdFactory koalaIdFactory;
    private PiIdBuilder piIdBuilder;
    private PublicIpAllocationIndex publicIpIndex;
    @Mock
    DhtClientFactory dhtClientFactory;
    @Mock
    DhtWriter dhtWriter;
    @Mock
    DhtReader dhtReader;
    @Mock
    AddressAssignmentExecutor addressAssignmentExecutor;

    @Before
    public void before() {
        instance = new Instance();
        instance.setInstanceId(instanceId1);

        koalaIdFactory = new KoalaIdFactory(99, 99);
        koalaIdFactory.setKoalaPiEntityFactory(new KoalaPiEntityFactory());
        piIdBuilder = new PiIdBuilder();
        piIdBuilder.setKoalaIdFactory(koalaIdFactory);

        publicIpIndex = new PublicIpAllocationIndex();
        publicIpIndex.getAllocationMap().put(IpAddressUtils.ipToLong(ALLOCATED_PUBLIC_ADDRESS_1), new InstanceRecord(instanceId1, "userId"));
        publicIpIndex.getAllocationMap().put(IpAddressUtils.ipToLong(ALLOCATED_PUBLIC_ADDRESS_2), new InstanceRecord(instanceId3, "abuserId"));

        Set<ResourceRange> ranges = new HashSet<ResourceRange>();
        ranges.add(new ResourceRange(IpAddressUtils.ipToLong("1.2.3.1"), IpAddressUtils.ipToLong("1.2.3.5")));
        ranges.add(new ResourceRange(IpAddressUtils.ipToLong("5.6.7.1"), IpAddressUtils.ipToLong("5.6.7.10")));
        ranges.add(new ResourceRange(IpAddressUtils.ipToLong("9.10.11.1"), IpAddressUtils.ipToLong("9.10.11.15")));
        ranges.add(new ResourceRange(IpAddressUtils.ipToLong("13.14.15.1"), IpAddressUtils.ipToLong("13.14.15.20")));
        publicIpIndex.setResourceRanges(ranges);

        when(dhtClientFactory.createWriter()).thenReturn(dhtWriter);
        when(dhtClientFactory.createReader()).thenReturn(dhtReader);

        securityGroup1 = new SecurityGroup("bozo", "default", 10L, "172.0.0.0", "255.255.255.240", "147.149.2.5", new HashSet<NetworkRule>());
        securityGroup1.getInstances().put(instanceWithNoPublicIp, new InstanceAddress("172.0.0.3", null, "aa:aa:aa:aa:aa:aa"));
        securityGroup1.getInstances().put(instanceId2, new InstanceAddress("172.0.0.2", ALLOCATED_PUBLIC_ADDRESS_1, "aa:aa:aa:aa:aa:aa"));

        securityGroup2 = new SecurityGroup("nutter", "default", 11L, "172.0.0.16", "255.255.255.240", "147.149.2.5", new HashSet<NetworkRule>());
        securityGroup2.getInstances().put(instanceId3, new InstanceAddress("172.0.0.18", ALLOCATED_PUBLIC_ADDRESS_2, "aa:aa:aa:aa:aa:aa"));

        SecurityGroup securityGroup3 = new SecurityGroup("abc", "default", 12L, "172.0.0.32", null, null, new HashSet<NetworkRule>());
        securityGroup3.getInstances().put(instanceId4, new InstanceAddress("172.0.0.45", ALLOCATED_PUBLIC_ADDRESS_3, "d0:0d:4a:1d:08:f7"));

        addressManager = new PublicIpAddressManager();
        injectDependencies(addressManager);
    }

    private void injectDependencies(PublicIpAddressManager addressManager) {
        addressManager.setDhtClientFactory(dhtClientFactory);
        addressManager.setPiIdBuilder(piIdBuilder);
        addressManager.setAddressAssignmentExecutor(addressAssignmentExecutor);
    }

    @Test
    public void testAllocatePublicIpAddress() throws InterruptedException {
        // setup
        PublicIpAllocationIndex index = new PublicIpAllocationIndex();
        index.getResourceRanges().add(new ResourceRange(IpAddressUtils.ipToLong(TEST_PUBLIC_IP), IpAddressUtils.ipToLong(TEST_PUBLIC_IP)));

        UpdateResolvingContinuationAnswer answer = new UpdateResolvingContinuationAnswer(index);
        doAnswer(answer).when(dhtWriter).update((PId) anyObject(), isA(UpdateResolvingContinuation.class));

        GenericContinuationAnswer<Boolean> assignAnswer = new GenericContinuationAnswer<Boolean>(true);
        doAnswer(assignAnswer).when(addressAssignmentExecutor).assignPublicIpAddressToInstance(eq(TEST_PUBLIC_IP), eq(instanceId1), isA(GenericContinuation.class));

        TestFriendlyContinuation<String> continuation = new TestFriendlyContinuation<String>();

        // act
        addressManager.allocatePublicIpAddressForInstance(instanceId1, continuation);

        // assert
        assertTrue(continuation.completedLatch.await(5, TimeUnit.SECONDS));
        assertEquals(TEST_PUBLIC_IP, continuation.lastResult);
        assertTrue(((PublicIpAllocationIndex) answer.getResult()).getAllocationMap().containsKey(IpAddressUtils.ipToLong(TEST_PUBLIC_IP)));
    }

    @Test
    public void testAllocatePublicIpAddressReturnsNullWhenAddressAssignmentFails() throws InterruptedException {
        // setup
        PublicIpAllocationIndex index = new PublicIpAllocationIndex();
        index.getResourceRanges().add(new ResourceRange(IpAddressUtils.ipToLong(TEST_PUBLIC_IP), IpAddressUtils.ipToLong(TEST_PUBLIC_IP)));

        UpdateResolvingContinuationAnswer answer = new UpdateResolvingContinuationAnswer(index);
        doAnswer(answer).when(dhtWriter).update((PId) anyObject(), isA(UpdateResolvingContinuation.class));

        GenericContinuationAnswer<Boolean> assignAnswer = new GenericContinuationAnswer<Boolean>(false);
        doAnswer(assignAnswer).when(addressAssignmentExecutor).assignPublicIpAddressToInstance(eq(TEST_PUBLIC_IP), eq(instanceId1), isA(GenericContinuation.class));

        TestFriendlyContinuation<String> continuation = new TestFriendlyContinuation<String>();

        // act
        addressManager.allocatePublicIpAddressForInstance(instanceId1, continuation);

        // assert
        assertTrue(continuation.completedLatch.await(5, TimeUnit.SECONDS));
        assertEquals(null, continuation.lastResult);
    }

    @Test
    public void testAllocatePublicIpAddressNoAddressesLeft() throws InterruptedException {
        // setup
        final PublicIpAllocationIndex index = mock(PublicIpAllocationIndex.class);
        when(index.allocateIpAddressToInstance(instanceId1)).thenThrow(new NoFreePublicAddressesAvailableException("", null));

        UpdateResolvingContinuationAnswer answer = new UpdateResolvingContinuationAnswer(index);
        doAnswer(answer).when(dhtWriter).update((PId) anyObject(), isA(UpdateResolvingContinuation.class));

        TestFriendlyContinuation<String> continuation = new TestFriendlyContinuation<String>();

        // act
        addressManager.allocatePublicIpAddressForInstance(instanceId1, continuation);

        // assert
        assertTrue(continuation.completedLatch.await(5, TimeUnit.SECONDS));
        assertEquals(null, continuation.lastResult);
        assertEquals(null, answer.getResult());
    }

    @Test
    public void testAllocatePublicIpAddressDelegatesExceptionToCallingContinuation() throws InterruptedException {
        // setup
        final PublicIpAllocationIndex index = mock(PublicIpAllocationIndex.class);
        when(index.allocateIpAddressToInstance(instanceId1)).thenThrow(new NoFreePublicAddressesAvailableException("", null));

        Exception e = new Exception("oops");
        ThrowingContinuationAnswer answer = new ThrowingContinuationAnswer(e);
        doAnswer(answer).when(dhtWriter).update((PId) anyObject(), isA(UpdateResolvingContinuation.class));

        TestFriendlyContinuation<String> continuation = new TestFriendlyContinuation<String>();

        // act
        addressManager.allocatePublicIpAddressForInstance(instanceId1, continuation);

        // assert
        assertTrue(continuation.completedLatch.await(5, TimeUnit.SECONDS));
        assertEquals(null, continuation.lastResult);
        assertEquals(e, continuation.lastException);
    }

    @Test
    public void shouldNoOpWhenAllocatingPublicIpAddressAndNoFreeAddresses() throws InterruptedException {
        // setup
        final PublicIpAllocationIndex index = mock(PublicIpAllocationIndex.class);
        when(index.allocateIpAddressToInstance(instanceId1)).thenThrow(new NoFreePublicAddressesAvailableException("", null));

        UpdateResolvingContinuationAnswer answer = new UpdateResolvingContinuationAnswer(index);
        doAnswer(answer).when(dhtWriter).update((PId) anyObject(), isA(UpdateResolvingContinuation.class));

        TestFriendlyContinuation<String> continuation = new TestFriendlyContinuation<String>();

        // act
        addressManager.allocatePublicIpAddressForInstance(instanceId1, continuation);

        // assert
        assertTrue(continuation.completedLatch.await(5, TimeUnit.SECONDS));
        assertEquals(null, continuation.lastResult);
        assertEquals(null, continuation.lastException);
        assertEquals(null, answer.getResult());
    }

    @Test
    public void shouldReleasePublicIpAddress() {
        // setup
        UpdateResolvingContinuationAnswer answer = new UpdateResolvingContinuationAnswer(publicIpIndex);
        doAnswer(answer).when(dhtWriter).update((PId) anyObject(), isA(UpdateResolvingContinuation.class));

        GenericContinuationAnswer<Boolean> assignAnswer = new GenericContinuationAnswer<Boolean>(true);
        doAnswer(assignAnswer).when(addressAssignmentExecutor).unassignPublicIpAddressFromInstance(eq(instanceId3), eq(securityGroup2.getSecurityGroupId()), isA(GenericContinuation.class));

        GenericContinuation<Boolean> continuation = mock(GenericContinuation.class);

        // act
        addressManager.releasePublicIpAddressForInstance(instanceId3, securityGroup2.getSecurityGroupId(), continuation);

        // assert
        verify(continuation).receiveResult(true);
        assertEquals(2, ((PublicIpAllocationIndex) answer.getResult()).getAllocationMap().size());
        assertNull(((PublicIpAllocationIndex) answer.getResult()).getAllocationMap().get(IpAddressUtils.ipToLong(ALLOCATED_PUBLIC_ADDRESS_2)).getInstanceId());
        assertEquals("userId", ((PublicIpAllocationIndex) answer.getResult()).getAllocationMap().get(IpAddressUtils.ipToLong(ALLOCATED_PUBLIC_ADDRESS_1)).getOwnerId());
    }

    @Test
    public void shouldNotReleaseAddressWhenPublicAddressRecordNotWritten() {
        // setup
        UpdateResolvingContinuationAnswer answer = new UpdateResolvingContinuationAnswer(publicIpIndex);
        answer.forceUpdateResult(null);
        doAnswer(answer).when(dhtWriter).update((PId) anyObject(), isA(UpdateResolvingContinuation.class));

        GenericContinuation<Boolean> continuation = mock(GenericContinuation.class);

        // act
        addressManager.releasePublicIpAddressForInstance(instanceWithNoPublicIp, securityGroup1.getSecurityGroupId(), continuation);

        // assert
        verify(continuation).receiveResult(false);
        assertEquals(2, ((PublicIpAllocationIndex) answer.getResult()).getAllocationMap().size());
    }

    @Test
    public void shouldAssociateAnAddressWithAnInstanceByUnassigningPreviousAddressAndAssigningTheNewOne() {
        // setup
        GenericContinuationAnswer readAnswer = new GenericContinuationAnswer(publicIpIndex);
        doAnswer(readAnswer).when(dhtReader).getAsync((PId) anyObject(), isA(Continuation.class));

        GenericContinuationAnswer<Boolean> unassignAnswer = new GenericContinuationAnswer<Boolean>(true);
        doAnswer(unassignAnswer).when(addressAssignmentExecutor).unassignPublicIpAddressFromInstance(eq(instanceId1), eq(securityGroup1.getSecurityGroupId()), isA(GenericContinuation.class));

        GenericContinuationAnswer<Boolean> assignAnswer = new GenericContinuationAnswer<Boolean>(true);
        doAnswer(assignAnswer).when(addressAssignmentExecutor).assignPublicIpAddressToInstance(eq(ALLOCATED_PUBLIC_ADDRESS_1), eq(instanceId1), isA(GenericContinuation.class));

        securityGroup1.getInstances().put(instanceId1, new InstanceAddress("172.0.0.2", ALLOCATED_PUBLIC_ADDRESS_3, "aa:aa:aa:aa:aa:aa"));

        GenericContinuation<Boolean> continuation = mock(GenericContinuation.class);

        // act
        addressManager.associatePublicIpAddressWithInstance(ALLOCATED_PUBLIC_ADDRESS_1, instanceId1, securityGroup1.getSecurityGroupId(), continuation);

        // assert
        verify(continuation).receiveResult(true);
    }

    @Test
    public void shouldNotAssociateAnAddressWithAnInstanceWhenPublicRecordIndexDoesNotContainAddressAssignment() {
        // setup
        GenericContinuationAnswer readAnswer = new GenericContinuationAnswer(publicIpIndex);
        doAnswer(readAnswer).when(dhtReader).getAsync((PId) anyObject(), isA(Continuation.class));

        GenericContinuation<Boolean> continuation = mock(GenericContinuation.class);

        // act
        addressManager.associatePublicIpAddressWithInstance(TEST_PUBLIC_IP, instanceId1, securityGroup1.getSecurityGroupId(), continuation);

        // assert
        verify(continuation).receiveResult(false);
    }

    @Test
    public void shouldNotAssociateAnAddressWithAnInstanceWhenPublicRecordIndexInconsistent() {
        // setup
        GenericContinuationAnswer readAnswer = new GenericContinuationAnswer(publicIpIndex);
        doAnswer(readAnswer).when(dhtReader).getAsync((PId) anyObject(), isA(Continuation.class));

        GenericContinuation<Boolean> continuation = mock(GenericContinuation.class);

        // act
        addressManager.associatePublicIpAddressWithInstance(ALLOCATED_PUBLIC_ADDRESS_1, instanceId2, securityGroup1.getSecurityGroupId(), continuation);

        // assert
        verify(continuation).receiveResult(false);
    }

    @Test
    public void shouldNotAssociateAnAddressWithAnInstanceWhenUnassignmnentOfPreviousAddrFails() {
        // setup
        GenericContinuationAnswer readAnswer = new GenericContinuationAnswer(publicIpIndex);
        doAnswer(readAnswer).when(dhtReader).getAsync((PId) anyObject(), isA(Continuation.class));

        GenericContinuationAnswer<Boolean> unassignAnswer = new GenericContinuationAnswer<Boolean>(false);
        doAnswer(unassignAnswer).when(addressAssignmentExecutor).unassignPublicIpAddressFromInstance(eq(instanceId1), eq(securityGroup1.getSecurityGroupId()), isA(GenericContinuation.class));

        securityGroup1.getInstances().put(instanceId1, new InstanceAddress("172.0.0.2", ALLOCATED_PUBLIC_ADDRESS_3, "aa:aa:aa:aa:aa:aa"));

        GenericContinuation<Boolean> continuation = mock(GenericContinuation.class);

        // act
        addressManager.associatePublicIpAddressWithInstance(ALLOCATED_PUBLIC_ADDRESS_1, instanceId1, securityGroup1.getSecurityGroupId(), continuation);

        // assert
        verify(continuation).receiveResult(false);
    }

    @Test
    public void shouldNotAssociateAnAddressWithAnInstanceWhenAssignmentOfNewAddrFails() {
        // setup
        GenericContinuationAnswer readAnswer = new GenericContinuationAnswer(publicIpIndex);
        doAnswer(readAnswer).when(dhtReader).getAsync((PId) anyObject(), isA(Continuation.class));

        GenericContinuationAnswer<Boolean> unassignAnswer = new GenericContinuationAnswer<Boolean>(true);
        doAnswer(unassignAnswer).when(addressAssignmentExecutor).unassignPublicIpAddressFromInstance(eq(instanceId1), eq(securityGroup1.getSecurityGroupId()), isA(GenericContinuation.class));

        GenericContinuationAnswer<Boolean> assignAnswer = new GenericContinuationAnswer<Boolean>(false);
        doAnswer(assignAnswer).when(addressAssignmentExecutor).assignPublicIpAddressToInstance(eq(ALLOCATED_PUBLIC_ADDRESS_1), eq(instanceId1), isA(GenericContinuation.class));

        securityGroup1.getInstances().put(instanceId1, new InstanceAddress("172.0.0.2", ALLOCATED_PUBLIC_ADDRESS_3, "aa:aa:aa:aa:aa:aa"));

        GenericContinuation<Boolean> continuation = mock(GenericContinuation.class);

        // act
        addressManager.associatePublicIpAddressWithInstance(ALLOCATED_PUBLIC_ADDRESS_1, instanceId1, securityGroup1.getSecurityGroupId(), continuation);

        // assert
        verify(continuation).receiveResult(false);
    }

    @Test
    public void shouldDisassociateAnAddressFromAnInstanceByUnassigningElasticAddressAndAssigningANewDynamicOne() {
        // setup
        GenericContinuationAnswer<Boolean> unassignAnswer = new GenericContinuationAnswer<Boolean>(true);
        doAnswer(unassignAnswer).when(addressAssignmentExecutor).unassignPublicIpAddressFromInstance(eq(instanceId1), eq(securityGroup1.getSecurityGroupId()), isA(GenericContinuation.class));

        PublicIpAllocationIndex index = new PublicIpAllocationIndex();
        index.getResourceRanges().add(new ResourceRange(IpAddressUtils.ipToLong(TEST_PUBLIC_IP), IpAddressUtils.ipToLong(TEST_PUBLIC_IP)));

        UpdateResolvingContinuationAnswer answer = new UpdateResolvingContinuationAnswer(index);
        doAnswer(answer).when(dhtWriter).update((PId) anyObject(), isA(UpdateResolvingContinuation.class));

        GenericContinuationAnswer<Boolean> assignAnswer = new GenericContinuationAnswer<Boolean>(true);
        doAnswer(assignAnswer).when(addressAssignmentExecutor).assignPublicIpAddressToInstance(eq(TEST_PUBLIC_IP), eq(instanceId1), isA(GenericContinuation.class));

        GenericContinuation<String> continuation = mock(GenericContinuation.class);

        // act
        addressManager.disassociatePublicIpAddressFromInstance(ALLOCATED_PUBLIC_ADDRESS_1, instanceId1, securityGroup1.getSecurityGroupId(), continuation);

        // assert
        verify(continuation).receiveResult(TEST_PUBLIC_IP);
        assertEquals(instanceId1, ((PublicIpAllocationIndex) answer.getResult()).getAllocationMap().get(IpAddressUtils.ipToLong(TEST_PUBLIC_IP)).getInstanceId());
    }

    @Test
    public void shouldDisassociateAnAddressFromAnInstanceWhenAlreadyDisassociatedInPublicIpIndexRecord() {
        // setup
        GenericContinuationAnswer<Boolean> unassignAnswer = new GenericContinuationAnswer<Boolean>(true);
        doAnswer(unassignAnswer).when(addressAssignmentExecutor).unassignPublicIpAddressFromInstance(eq(instanceId1), eq(securityGroup1.getSecurityGroupId()), isA(GenericContinuation.class));

        publicIpIndex.getAllocationMap().get(IpAddressUtils.ipToLong(ALLOCATED_PUBLIC_ADDRESS_1)).setInstanceId(instanceId4);
        publicIpIndex.getAllocationMap().put(IpAddressUtils.ipToLong(ALLOCATED_PUBLIC_ADDRESS_2), new InstanceRecord(instanceId1, null));

        UpdateResolvingContinuationAnswer answer = new UpdateResolvingContinuationAnswer(publicIpIndex);
        doAnswer(answer).when(dhtWriter).update((PId) anyObject(), isA(UpdateResolvingContinuation.class));

        GenericContinuationAnswer<Boolean> assignAnswer = new GenericContinuationAnswer<Boolean>(true);
        doAnswer(assignAnswer).when(addressAssignmentExecutor).assignPublicIpAddressToInstance(eq(ALLOCATED_PUBLIC_ADDRESS_2), eq(instanceId1), isA(GenericContinuation.class));

        GenericContinuation<String> continuation = mock(GenericContinuation.class);

        // act
        addressManager.disassociatePublicIpAddressFromInstance(ALLOCATED_PUBLIC_ADDRESS_1, instanceId1, securityGroup1.getSecurityGroupId(), continuation);

        // assert
        verify(continuation).receiveResult(ALLOCATED_PUBLIC_ADDRESS_2);
        assertEquals(instanceId1, ((PublicIpAllocationIndex) answer.getResult()).getAllocationMap().get(IpAddressUtils.ipToLong(ALLOCATED_PUBLIC_ADDRESS_2)).getInstanceId());
    }

    @Test
    public void shouldFailADisassociateOfAddressFromAnInstanceWhenUnassignmentFails() {
        // setup
        GenericContinuationAnswer<Boolean> unassignAnswer = new GenericContinuationAnswer<Boolean>(false);
        doAnswer(unassignAnswer).when(addressAssignmentExecutor).unassignPublicIpAddressFromInstance(eq(instanceId1), eq(securityGroup1.getSecurityGroupId()), isA(GenericContinuation.class));

        GenericContinuation<String> continuation = mock(GenericContinuation.class);

        // act
        addressManager.disassociatePublicIpAddressFromInstance(ALLOCATED_PUBLIC_ADDRESS_1, instanceId1, securityGroup1.getSecurityGroupId(), continuation);

        // assert
        verify(continuation).receiveResult(null);
    }

    @Test
    public void shouldFailADisassociateOfAddressFromAnInstanceWhenAllocationFails() {
        // setup
        GenericContinuationAnswer<Boolean> unassignAnswer = new GenericContinuationAnswer<Boolean>(true);
        doAnswer(unassignAnswer).when(addressAssignmentExecutor).unassignPublicIpAddressFromInstance(eq(instanceId1), eq(securityGroup1.getSecurityGroupId()), isA(GenericContinuation.class));

        UpdateResolvingContinuationAnswer answer = new UpdateResolvingContinuationAnswer(null);
        doAnswer(answer).when(dhtWriter).update((PId) anyObject(), isA(UpdateResolvingContinuation.class));

        GenericContinuation<String> continuation = mock(GenericContinuation.class);

        // act
        addressManager.disassociatePublicIpAddressFromInstance(ALLOCATED_PUBLIC_ADDRESS_1, instanceId1, securityGroup1.getSecurityGroupId(), continuation);

        // assert
        verify(continuation).receiveResult(null);
    }
}
