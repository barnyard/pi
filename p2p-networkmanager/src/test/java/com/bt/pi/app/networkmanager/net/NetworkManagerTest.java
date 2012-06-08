package com.bt.pi.app.networkmanager.net;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.isA;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.HashSet;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

import org.junit.Before;
import org.junit.Test;

import rice.Continuation;

import com.bt.pi.app.common.entities.Instance;
import com.bt.pi.app.common.entities.InstanceAddress;
import com.bt.pi.app.common.entities.NetworkRule;
import com.bt.pi.app.common.entities.SecurityGroup;
import com.bt.pi.app.common.id.PiIdBuilder;
import com.bt.pi.app.common.net.SubnetAllocationResult;
import com.bt.pi.app.common.net.SubnetAllocator;
import com.bt.pi.app.common.net.VlanAllocator;
import com.bt.pi.app.common.net.utils.IpAddressUtils;
import com.bt.pi.app.networkmanager.addressing.PrivateIpAddressManager;
import com.bt.pi.core.application.resource.ConsumedDhtResourceRegistry;
import com.bt.pi.core.continuation.UpdateResolvingContinuation;
import com.bt.pi.core.id.KoalaIdFactory;
import com.bt.pi.core.id.PId;
import com.bt.pi.core.parser.KoalaPiEntityFactory;
import com.bt.pi.core.testing.GenericContinuationAnswer;
import com.bt.pi.core.testing.TestFriendlyContinuation;
import com.bt.pi.core.testing.ThrowingContinuationAnswer;
import com.bt.pi.core.testing.UpdateResolvingContinuationAnswer;

@SuppressWarnings("unchecked")
public class NetworkManagerTest {
    private static final String netName = "default";
    private static final String userId = "bozo";
    private static final String instanceId = "i-12345678";

    private NetworkManager networkManager;
    private VirtualNetworkBuilder virtualNetworkBuilder;
    private PId securityGroupDhtId;
    private SecurityGroup securityGroup1;
    private SecurityGroup securityGroup2;
    private SecurityGroup securityGroup3;
    private ConsumedDhtResourceRegistry consumedDhtResourceRegistry;
    private PrivateIpAddressManager privateIpAddressManager;
    private Instance instance;
    private Semaphore dhcpDaemonSemaphore;
    private GenericContinuationAnswer<Boolean> registerAnswer;
    private UpdateResolvingContinuationAnswer secGroupAnswer;
    private TestFriendlyContinuation<Instance> instanceContinuation;
    private KoalaIdFactory koalaIdFactory;
    private PiIdBuilder piIdBuilder;
    private VlanAllocator vlanAllocator;
    private SubnetAllocator subnetAllocator;
    private GenericContinuationAnswer<Long> vlanAllocatorAnswer;
    private GenericContinuationAnswer<SubnetAllocationResult> subnetAllocatorAnswer;

    @Before
    public void before() {
        this.koalaIdFactory = new KoalaIdFactory(99, 99);
        koalaIdFactory.setKoalaPiEntityFactory(new KoalaPiEntityFactory());
        piIdBuilder = new PiIdBuilder();
        piIdBuilder.setKoalaIdFactory(koalaIdFactory);

        instance = new Instance();
        instance.setInstanceId(instanceId);
        instance.setUserId(userId);
        instance.setSecurityGroupName(netName);

        instanceContinuation = new TestFriendlyContinuation<Instance>();

        dhcpDaemonSemaphore = new Semaphore(0);

        securityGroup1 = new SecurityGroup("bozo", "default", 10L, "172.0.0.0", "255.255.255.240", "147.149.2.5", new HashSet<NetworkRule>());
        securityGroup1.getInstances().put("i-111", new InstanceAddress("172.0.0.3", null, "aa:aa:aa:aa:aa:aa"));
        securityGroup1.getInstances().put("i-222", new InstanceAddress("172.0.0.2", "1.2.3.4", "aa:aa:aa:aa:aa:aa"));

        securityGroup2 = new SecurityGroup("nutter", "default", 11L, "172.0.0.16", "255.255.255.240", "147.149.2.5", new HashSet<NetworkRule>());
        securityGroup2.getInstances().put("i-333", new InstanceAddress("172.0.0.18", "5.6.7.8", "aa:aa:aa:aa:aa:aa"));

        securityGroup3 = new SecurityGroup("abc", "default", 12L, null, null, null, new HashSet<NetworkRule>());
        securityGroup3.getInstances().put("i-444", new InstanceAddress("172.0.0.45", "9.10.11.12", "d0:0d:4a:1d:08:f7"));

        vlanAllocatorAnswer = new GenericContinuationAnswer<Long>(13L);
        vlanAllocator = mock(VlanAllocator.class);
        doAnswer(vlanAllocatorAnswer).when(vlanAllocator).allocateVlanInLocalRegion(eq("bozo:default"), isA(Continuation.class));

        subnetAllocatorAnswer = new GenericContinuationAnswer<SubnetAllocationResult>(new SubnetAllocationResult(IpAddressUtils.ipToLong("172.0.0.48"), IpAddressUtils.ipToLong("255.255.255.240"), "147.249.2.5"));
        subnetAllocator = mock(SubnetAllocator.class);
        doAnswer(subnetAllocatorAnswer).when(subnetAllocator).allocateSubnetInLocalRegion(eq("bozo:default"), isA(Continuation.class));

        virtualNetworkBuilder = mock(VirtualNetworkBuilder.class);

        securityGroupDhtId = piIdBuilder.getPId(securityGroup1.getUrl()).forLocalRegion();
        System.err.println("secGroupId: " + securityGroupDhtId.toStringFull());
        registerAnswer = new GenericContinuationAnswer<Boolean>(true, 3);
        secGroupAnswer = new UpdateResolvingContinuationAnswer(securityGroup1);

        consumedDhtResourceRegistry = mock(ConsumedDhtResourceRegistry.class);
        doAnswer(registerAnswer).when(consumedDhtResourceRegistry).registerConsumer(eq(securityGroupDhtId), eq(instanceId), eq(SecurityGroup.class), isA(Continuation.class));
        doAnswer(secGroupAnswer).when(consumedDhtResourceRegistry).update(eq(securityGroupDhtId), isA(UpdateResolvingContinuation.class));
        when(consumedDhtResourceRegistry.deregisterConsumer(securityGroupDhtId, instanceId)).thenReturn(true);
        when(consumedDhtResourceRegistry.getCachedEntity(securityGroupDhtId)).thenReturn(securityGroup1);

        privateIpAddressManager = new PrivateIpAddressManager() {
            public void refreshDhcpDaemon() {
                dhcpDaemonSemaphore.release();
            };
        };

        networkManager = new NetworkManager();
        injectDependencies(networkManager);
    }

    private void injectDependencies(NetworkManager networkManager) {
        networkManager.setPiIdBuilder(piIdBuilder);
        networkManager.setVirtualNetworkBuilder(virtualNetworkBuilder);
        networkManager.setConsumedDhtResourceRegistry(consumedDhtResourceRegistry);
        networkManager.setPrivateIpAddressManager(privateIpAddressManager);
        networkManager.setVlanAllocator(vlanAllocator);
        networkManager.setSubnetAllocator(subnetAllocator);
    }

    @Test
    public void shouldSetUpNetworkForInstanceWhenVlanAllocationSucceeds() throws Exception {
        // act
        networkManager.setupNetworkForInstance(instance, instanceContinuation);

        // assert
        assertTrue(instanceContinuation.completedLatch.await(5, TimeUnit.SECONDS));
        verify(this.virtualNetworkBuilder).setUpVirtualNetworkForSecurityGroup(securityGroup1);
        assertEquals("255.255.255.240", securityGroup1.getNetmask());
        assertEquals("172.0.0.48", securityGroup1.getNetworkAddress());
        assertTrue(dhcpDaemonSemaphore.tryAcquire(3, TimeUnit.SECONDS));

        assertEquals(13L, securityGroup1.getVlanId(), 0);
        assertEquals(13, instanceContinuation.lastResult.getVlanId());
        assertEquals(securityGroup1.getInstances().get(instanceId).getMacAddress(), instanceContinuation.lastResult.getPrivateMacAddress());
        assertTrue(instanceContinuation.lastResult.getPrivateMacAddress().startsWith("00:34:f0:b1:db:68"));
        assertEquals("172.0.0.50", securityGroup1.getInstances().get(instanceId).getPrivateIpAddress());
        assertEquals("172.0.0.50", instanceContinuation.lastResult.getPrivateIpAddress());
    }

    @Test
    public void shouldTrustVirtualNetworkBuilderToSetUpNetworkEvenWhenNetworkAlreadyExists() throws Exception {
        // setup
        registerAnswer = new GenericContinuationAnswer<Boolean>(false, 3);
        doAnswer(registerAnswer).when(consumedDhtResourceRegistry).registerConsumer(eq(securityGroupDhtId), eq(instanceId), eq(SecurityGroup.class), isA(Continuation.class));

        // act
        networkManager.setupNetworkForInstance(instance, instanceContinuation);

        // assert
        assertTrue(instanceContinuation.completedLatch.await(3, TimeUnit.SECONDS));
        verify(this.virtualNetworkBuilder).setUpVirtualNetworkForSecurityGroup(securityGroup1);
        assertTrue(instanceContinuation.lastResult.getPrivateMacAddress().startsWith("00:34:f0:b1:db:68"));
    }

    @Test
    public void shouldFailSetUpNetworkForInstanceWhenNoVlansLeft() throws Exception {
        // setup
        doAnswer(new GenericContinuationAnswer<Long>(null)).when(vlanAllocator).allocateVlanInLocalRegion(eq("bozo:default"), isA(Continuation.class));

        instanceContinuation = new TestFriendlyContinuation<Instance>();

        // act
        networkManager.setupNetworkForInstance(instance, instanceContinuation);

        // assert
        assertTrue(instanceContinuation.completedLatch.await(3, TimeUnit.SECONDS));
        assertEquals(null, instanceContinuation.lastResult);
        assertEquals(NetworkCreationException.class, instanceContinuation.lastException.getClass());
        verify(this.consumedDhtResourceRegistry).deregisterConsumer(securityGroupDhtId, instanceId);
        verify(this.virtualNetworkBuilder, never()).setUpVirtualNetworkForSecurityGroup(securityGroup1);
    }

    @Test
    public void shouldFailSetUpNetworkForInstanceWhenNoSubnetsLeft() throws Exception {
        // setup
        doAnswer(new GenericContinuationAnswer<SubnetAllocationResult>(null)).when(subnetAllocator).allocateSubnetInLocalRegion(eq("bozo:default"), isA(Continuation.class));

        instanceContinuation = new TestFriendlyContinuation<Instance>();

        // act
        networkManager.setupNetworkForInstance(instance, instanceContinuation);

        // assert
        assertTrue(instanceContinuation.completedLatch.await(3, TimeUnit.SECONDS));
        assertEquals(null, instanceContinuation.lastResult);
        assertEquals(NetworkCreationException.class, instanceContinuation.lastException.getClass());
        verify(this.consumedDhtResourceRegistry).deregisterConsumer(securityGroupDhtId, instanceId);
        verify(this.virtualNetworkBuilder, never()).setUpVirtualNetworkForSecurityGroup(securityGroup1);
    }

    @Test
    public void shouldFailToSetUpNetworkForInstanceWhenSecGroupUpdateFails() throws Exception {
        // setup
        secGroupAnswer.forceUpdateResult(null);

        // act
        networkManager.setupNetworkForInstance(instance, instanceContinuation);

        // assert
        assertTrue(instanceContinuation.completedLatch.await(3, TimeUnit.SECONDS));
        assertEquals(null, instanceContinuation.lastResult);
        assertEquals(NetworkCreationException.class, instanceContinuation.lastException.getClass());
        verify(this.consumedDhtResourceRegistry).deregisterConsumer(securityGroupDhtId, instanceId);
        verify(this.virtualNetworkBuilder, never()).setUpVirtualNetworkForSecurityGroup(securityGroup1);
    }

    @Test
    public void shouldCleanUpWhenExceptionOccursOnVlanSetup() throws Exception {
        // setup
        ThrowingContinuationAnswer throwingAnswer = new ThrowingContinuationAnswer(new RuntimeException("oops"));
        doAnswer(throwingAnswer).when(vlanAllocator).allocateVlanInLocalRegion(eq("bozo:default"), isA(Continuation.class));

        // act
        networkManager.setupNetworkForInstance(instance, instanceContinuation);

        // assert
        assertTrue(instanceContinuation.completedLatch.await(3, TimeUnit.SECONDS));
        assertEquals(null, instanceContinuation.lastResult);
        assertEquals(RuntimeException.class, instanceContinuation.lastException.getClass());
        verify(this.consumedDhtResourceRegistry).deregisterConsumer(securityGroupDhtId, instanceId);
    }

    @Test
    public void shouldCleanUpWhenExceptionOccursOnSubnetSetup() throws Exception {
        // setup
        ThrowingContinuationAnswer throwingAnswer = new ThrowingContinuationAnswer(new RuntimeException("oops"));
        doAnswer(throwingAnswer).when(subnetAllocator).allocateSubnetInLocalRegion(eq("bozo:default"), isA(Continuation.class));

        // act
        networkManager.setupNetworkForInstance(instance, instanceContinuation);

        // assert
        assertTrue(instanceContinuation.completedLatch.await(3, TimeUnit.SECONDS));
        assertEquals(null, instanceContinuation.lastResult);
        assertEquals(RuntimeException.class, instanceContinuation.lastException.getClass());
        verify(this.consumedDhtResourceRegistry).deregisterConsumer(securityGroupDhtId, instanceId);
    }

    @Test
    public void shouldCleanUpWhenExceptionOccursOnSecGroupUpdate() throws Exception {
        // setup
        ThrowingContinuationAnswer throwingAnswer = new ThrowingContinuationAnswer(new RuntimeException("oops"));
        doAnswer(throwingAnswer).when(consumedDhtResourceRegistry).update(eq(securityGroupDhtId), isA(UpdateResolvingContinuation.class));

        // act
        networkManager.setupNetworkForInstance(instance, instanceContinuation);

        // assert
        assertTrue(instanceContinuation.completedLatch.await(5, TimeUnit.SECONDS));
        assertEquals(null, instanceContinuation.lastResult);
        assertEquals(RuntimeException.class, instanceContinuation.lastException.getClass());
        verify(this.consumedDhtResourceRegistry).deregisterConsumer(securityGroupDhtId, instanceId);
    }

    @Test
    public void shouldReleasePrivateAddressSubnetAndVlanAndTearDownNetworkForOnlyInstanceInGroup() throws Exception {
        // setup
        privateIpAddressManager = mock(PrivateIpAddressManager.class);
        networkManager.setPrivateIpAddressManager(privateIpAddressManager);

        // act
        networkManager.releaseNetworkForInstance(userId, netName, instanceId);

        // assert
        verify(this.consumedDhtResourceRegistry).deregisterConsumer(securityGroupDhtId, instanceId);
        verify(this.privateIpAddressManager).freePrivateIpAddressForInstance(instanceId, securityGroup1);
        verify(this.virtualNetworkBuilder).tearDownVirtualNetworkForSecurityGroup(securityGroup1);
        verify(this.vlanAllocator).releaseVlanInLocalRegion(userId + ":" + netName);
        verify(this.subnetAllocator).releaseSubnetInLocalRegion(userId + ":" + netName);
    }

    @Test
    public void shouldReleasePrivateAddressButNotVlanOrNetworForNonLastInstanceInGroup() throws Exception {
        // setup
        when(consumedDhtResourceRegistry.deregisterConsumer(securityGroupDhtId, instanceId)).thenReturn(false);

        privateIpAddressManager = mock(PrivateIpAddressManager.class);
        networkManager.setPrivateIpAddressManager(privateIpAddressManager);

        // act
        networkManager.releaseNetworkForInstance(userId, netName, instanceId);

        // assert
        verify(this.consumedDhtResourceRegistry).deregisterConsumer(securityGroupDhtId, instanceId);
        verify(this.privateIpAddressManager).freePrivateIpAddressForInstance(instanceId, securityGroup1);
        verify(this.virtualNetworkBuilder, never()).tearDownVirtualNetworkForSecurityGroup(securityGroup1);
    }

    @Test
    public void shouldLeaveNetworkForInstanceInPlaceWHenOtherConsumersStillExist() throws Exception {
        // setup
        when(consumedDhtResourceRegistry.deregisterConsumer(securityGroupDhtId, instanceId)).thenReturn(false);

        // act
        networkManager.releaseNetworkForInstance(userId, netName, instanceId);

        // assert
        verify(this.virtualNetworkBuilder, never()).tearDownVirtualNetworkForSecurityGroup(securityGroup1);
    }
}
