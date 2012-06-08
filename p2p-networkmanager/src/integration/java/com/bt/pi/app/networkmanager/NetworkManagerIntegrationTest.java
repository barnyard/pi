package com.bt.pi.app.networkmanager;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.argThat;
import static org.mockito.Matchers.contains;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.isA;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Map.Entry;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.ArgumentMatcher;
import org.springframework.context.support.AbstractApplicationContext;

import rice.Continuation;

import com.bt.pi.app.common.entities.AllocatableResourceIndexBase;
import com.bt.pi.app.common.entities.Instance;
import com.bt.pi.app.common.entities.InstanceAddress;
import com.bt.pi.app.common.entities.InstanceRecord;
import com.bt.pi.app.common.entities.InstanceState;
import com.bt.pi.app.common.entities.NetworkRule;
import com.bt.pi.app.common.entities.PublicIpAddress;
import com.bt.pi.app.common.entities.PublicIpAllocationIndex;
import com.bt.pi.app.common.entities.SecurityGroup;
import com.bt.pi.app.common.entities.SubnetAllocationIndex;
import com.bt.pi.app.common.entities.VlanAllocationIndex;
import com.bt.pi.app.common.id.PiIdBuilder;
import com.bt.pi.app.common.net.NetworkCommandRunner;
import com.bt.pi.app.common.net.utils.IpAddressUtils;
import com.bt.pi.app.common.net.utils.VlanAddressUtils;
import com.bt.pi.app.common.resource.PiQueue;
import com.bt.pi.app.common.resource.PiTopics;
import com.bt.pi.app.common.testing.StubDeviceUtils;
import com.bt.pi.app.networkmanager.addressing.resolution.AddressDeleteQueue;
import com.bt.pi.app.networkmanager.dhcp.DhcpManager;
import com.bt.pi.app.networkmanager.handlers.QueueTaskUriHelper;
import com.bt.pi.app.networkmanager.iptables.IpTablesManager;
import com.bt.pi.core.application.MessageContext;
import com.bt.pi.core.application.PubSubMessageContext;
import com.bt.pi.core.application.ReceivedMessageContext;
import com.bt.pi.core.application.activation.ApplicationRecord;
import com.bt.pi.core.application.activation.AvailabilityZoneScopedApplicationRecord;
import com.bt.pi.core.application.activation.SharedRecordConditionalApplicationActivator;
import com.bt.pi.core.application.activation.TimeStampedPair;
import com.bt.pi.core.application.resource.ConsumedDhtResourceRegistry;
import com.bt.pi.core.application.watcher.task.TaskProcessingQueueWatcherInitiatorBase;
import com.bt.pi.core.continuation.GenericContinuation;
import com.bt.pi.core.continuation.UpdateResolver;
import com.bt.pi.core.dht.BlockingDhtReader;
import com.bt.pi.core.dht.BlockingDhtWriter;
import com.bt.pi.core.dht.DhtClientFactory;
import com.bt.pi.core.entity.EntityMethod;
import com.bt.pi.core.entity.EntityResponseCode;
import com.bt.pi.core.entity.PiEntity;
import com.bt.pi.core.entity.TaskProcessingQueue;
import com.bt.pi.core.id.KoalaIdFactory;
import com.bt.pi.core.id.PId;
import com.bt.pi.core.id.PiId;
import com.bt.pi.core.node.KoalaNode;
import com.bt.pi.core.testing.StubCommandExecutor;
import com.bt.pi.core.util.common.CommandResult;
import com.bt.pi.core.util.common.CommandRunner;

public class NetworkManagerIntegrationTest extends IntegrationTestBase {
    private static final Log LOG = LogFactory.getLog(NetworkManagerIntegrationTest.class);
    private Instance testInstance;
    private Instance anotherTestInstance;
    private static SecurityGroup securityGroup;
    private static final String ELASTIC_IP_ADDRESS = "6.6.6.6";
    private static PId securityGroupRecordId;
    private static final String OUT_OF_RANGE_PUBLIC_IP_ADDRESS = "10.0.0.3";

    @BeforeClass
    public static void beforeClass() throws Exception {
        IntegrationTestBase.beforeClassBase(2);
    }

    @Before
    public void before() throws Exception {
        LOG.debug("#### running " + testName.getMethodName());
        seedPublicIpAddresses();
        seedVlans();
        seedSubnets();
        createSecurityGroup();
        createTestInstance();
        waitForLiveNetworkManager();
        clearConsumedDhtResourceRegistries();
        clearDeviceUtils();
        changeContextToNode(getLiveNetworkAppNodePort());
        System.clearProperty("securityGroupRefreshRunnerInitialIntervalMillisOverride");
        System.clearProperty("securityGroupConsumerWatcherInitialIntervalMillisOverride");
        clearDeliveredQueues();
        setDeviceHasAddress(true);
    }

    private void clearDeliveredQueues() {
        for (Entry<String, AbstractApplicationContext> entry : applicationContexts.entrySet()) {
            TestNetworkManagerApplication networkManagerApplication = (TestNetworkManagerApplication) entry.getValue().getBean("networkManagerApplication");
            networkManagerApplication.clearDeliveredQueue();
        }
    }

    private void clearDeviceUtils() {
        for (Entry<String, AbstractApplicationContext> entry : applicationContexts.entrySet()) {
            StubDeviceUtils stubDeviceUtils = (StubDeviceUtils) entry.getValue().getBean("deviceUtils");
            stubDeviceUtils.clear();
        }
    }

    private void clearConsumedDhtResourceRegistries() {
        for (Entry<String, AbstractApplicationContext> entry : applicationContexts.entrySet()) {
            ConsumedDhtResourceRegistry consumedDhtResourceRegistry = (ConsumedDhtResourceRegistry) entry.getValue().getBean("consumedDhtResourceRegistry");
            consumedDhtResourceRegistry.clearAll(SecurityGroup.class);
        }
    }

    private static void createSecurityGroup() {
        Long VLAN_ID = 100L;

        securityGroup = new SecurityGroup("chuckNorris", "deltaForce", VLAN_ID, "172.30.0.64", "255.255.255.240", "147.149.2.5", null);
        securityGroupRecordId = getPiIdBuilder().getPId(securityGroup.getUrl()).forLocalRegion();
        BlockingDhtWriter blockingDhtWriter = getDhtClientFactory().createBlockingWriter();
        blockingDhtWriter.update(securityGroupRecordId, securityGroup, new UpdateResolver<SecurityGroup>() {
            @Override
            public SecurityGroup update(SecurityGroup existingEntity, SecurityGroup requestedEntity) {
                if (null != existingEntity)
                    requestedEntity.setVersion(existingEntity.getVersion() + 1);
                return requestedEntity;
            }
        });
    }

    private void createTestInstance() {
        testInstance = new Instance();
        testInstance.setInstanceId(getPiIdBuilder().generateBase62Ec2Id("i", getKoalaNode().getKoalaIdFactory().getGlobalAvailabilityZoneCode()));
        testInstance.setSecurityGroupName(securityGroup.getOwnerIdGroupNamePair().getGroupName());
        testInstance.setUserId(securityGroup.getOwnerIdGroupNamePair().getOwnerId());
        testInstance.setAvailabilityZoneCode(getKoalaIdFactory().getAvailabilityZoneWithinRegion());
        testInstance.setRegionCode(getKoalaIdFactory().getRegion());
        testInstance.setState(InstanceState.RUNNING);
        BlockingDhtWriter blockingDhtWriter = getDhtClientFactory().createBlockingWriter();
        blockingDhtWriter.put(getPiIdBuilder().getPIdForEc2AvailabilityZone(testInstance.getUrl()), testInstance);
    }

    private static PiIdBuilder getPiIdBuilder() {
        return (PiIdBuilder) applicationContexts.get(applicationContexts.firstKey()).getBean("piIdBuilder");
    }

    private KoalaNode getKoalaNode() {
        return (KoalaNode) applicationContexts.get(applicationContexts.firstKey()).getBean("koalaNode");
    }

    private KoalaIdFactory getKoalaIdFactory() {
        return (KoalaIdFactory) applicationContexts.get(applicationContexts.firstKey()).getBean("koalaIdFactory");
    }

    private static DhtClientFactory getDhtClientFactory() {
        return (DhtClientFactory) applicationContexts.get(applicationContexts.firstKey()).getBean("dhtClientFactory");
    }

    @Test
    public void thereShouldOnlyBeOneNetworkManager() throws InterruptedException {
        // act
        int liveNetworkManagerCount = getLiveNetworkManagerCount();

        // assert
        assertEquals(1, liveNetworkManagerCount);
    }

    @Test
    public void shouldSetupNetworkForInstanceAndSendBackInstanceEntityWithNetworkFoo() throws Exception {
        // setup
        final ReceivedMessageContext receivedMessageContext = setupReceivedMessageContext(EntityMethod.CREATE, testInstance);

        // act
        currentNetworkManagerApplication.deliver(getPIdFromNodeId(), receivedMessageContext);

        // assert instance message sent back with correct network values
        verifyCalledEventually(15, new Runnable() {
            public void run() {
                verify(receivedMessageContext).sendResponse(eq(EntityResponseCode.OK), (PiEntity) argThat(new ArgumentMatcher<PiEntity>() {

                    @Override
                    public boolean matches(Object argument) {
                        Instance resultInstance = (Instance) testInstance;
                        assertEquals(1, resultInstance.getVlanId());
                        assertEquals("172.30.0.2", resultInstance.getPrivateIpAddress());
                        assertEquals(VlanAddressUtils.getMacAddressFromInstanceId(testInstance.getInstanceId()), resultInstance.getPrivateMacAddress());

                        BlockingDhtReader blockingDhtReader = currentDhtClientFactory.createBlockingReader();
                        SecurityGroup persistedGroup = (SecurityGroup) blockingDhtReader.get(currentPiIdBuilder.getPId(securityGroup.getUrl()).forLocalRegion());
                        assertNotNull(persistedGroup);
                        System.err.println("persisted group: " + persistedGroup);

                        blockingDhtReader = currentDhtClientFactory.createBlockingReader();
                        Instance persistedInstance = (Instance) blockingDhtReader.get(currentPiIdBuilder.getPIdForEc2AvailabilityZone(testInstance.getUrl()));
                        assertEquals("172.30.0.2", persistedInstance.getPrivateIpAddress());
                        assertEquals(VlanAddressUtils.getMacAddressFromInstanceId(testInstance.getInstanceId()), persistedInstance.getPrivateMacAddress());

                        return true;
                    }
                }));
            }
        });

        Thread.sleep(2000);

        // assert vlan allocated correctly in vlan index
        VlanAllocationIndex vlanAllocationIndex = (VlanAllocationIndex) currentDhtClientFactory.createBlockingReader().get(currentPiIdBuilder.getPId(VlanAllocationIndex.URL).forLocalRegion());
        assertEquals(1, vlanAllocationIndex.getAllocationMap().size());
        assertTrue(vlanAllocationIndex.getAllocationMap().keySet().contains(1L));
        assertEquals(securityGroup.getOwnerIdGroupNamePair().toString(), vlanAllocationIndex.getAllocationMap().get(1L).getSecurityGroupId());

        // assert public ip index updated correctly
        PublicIpAllocationIndex publicIpAllocationIndex = (PublicIpAllocationIndex) currentDhtClientFactory.createBlockingReader().get(currentPiIdBuilder.getPId(PublicIpAllocationIndex.URL).forLocalRegion());
        assertEquals(1, publicIpAllocationIndex.getAllocationMap().size());
        assertTrue(publicIpAllocationIndex.getAllocationMap().keySet().contains(IpAddressUtils.ipToLong(VALID_PUBLIC_ADDRESS_1)));
        assertEquals(testInstance.getInstanceId(), publicIpAllocationIndex.getAllocationMap().get(IpAddressUtils.ipToLong(VALID_PUBLIC_ADDRESS_1)).getInstanceId());

        // assert public ip index updated correctly
        SubnetAllocationIndex subnetAllocationIndex = (SubnetAllocationIndex) currentDhtClientFactory.createBlockingReader().get(currentPiIdBuilder.getPId(SubnetAllocationIndex.URL).forLocalRegion());
        assertEquals(1, subnetAllocationIndex.getAllocationMap().size());
        assertTrue(subnetAllocationIndex.getAllocationMap().keySet().contains(IpAddressUtils.ipToLong("172.30.0.0")));
        assertEquals(securityGroup.getSecurityGroupId(), subnetAllocationIndex.getAllocationMap().get(IpAddressUtils.ipToLong("172.30.0.0")).getSecurityGroupId());
        assertEquals("147.149.2.5", subnetAllocationIndex.getDnsAddress());

        // assert that sec group was added to resource manager
        assertEquals(1, currentConsumedDhtResourceRegistry.getByType(SecurityGroup.class).size());
    }

    @Test
    public void shouldTearDownNetworkForInstance() throws Exception {
        // setup
        final ReceivedMessageContext receivedMessageContextCreate = setupReceivedMessageContext(EntityMethod.CREATE, testInstance);
        final ReceivedMessageContext receivedMessageContextDelete = setupReceivedMessageContext(EntityMethod.DELETE, testInstance);

        // create
        currentNetworkManagerApplication.deliver(getPIdFromNodeId(), receivedMessageContextCreate);
        verifyCalledEventually(15, new Runnable() {
            public void run() {
                verify(receivedMessageContextCreate).sendResponse(eq(EntityResponseCode.OK), isA(Instance.class));
            }
        });

        // act
        currentNetworkManagerApplication.deliver(getPIdFromNodeId(), receivedMessageContextDelete);

        // assert
        verifyCalledEventually(15, new Runnable() {
            @Override
            public void run() {
                assertNumVlansAllocated(0);
                assertNumAddrsAllocated(0);
                assertNumSubnetsAllocated(0);
                assertEquals(0, currentConsumedDhtResourceRegistry.getByType(SecurityGroup.class).size());
                verifyCommand("ip addr del 10.0.0.1/32 dev eth1");
            }
        });
    }

    private PId getPIdFromNodeId() {
        return new PiId(currentPastryNode.getId().toStringFull(), currentKoalaNode.getKoalaIdFactory().getGlobalAvailabilityZoneCode());
    }

    @Test
    public void shouldAssociateAnElasticAddressWithInstance() throws Exception {
        // setup
        addDeviceHasAddress(ELASTIC_IP_ADDRESS, false);

        PublicIpAddress publicIpAddressEntity = new PublicIpAddress(ELASTIC_IP_ADDRESS, testInstance.getInstanceId(), testInstance.getUserId(), testInstance.getSecurityGroupName());

        final ReceivedMessageContext receivedMessageContextCreate = setupReceivedMessageContext(EntityMethod.CREATE, testInstance);
        final ReceivedMessageContext receivedMessageContextAssociate = setupReceivedMessageContext(EntityMethod.CREATE, publicIpAddressEntity);

        // create
        currentNetworkManagerApplication.deliver(getPIdFromNodeId(), receivedMessageContextCreate);
        verifyCalledEventually(15, new Runnable() {
            public void run() {
                verify(receivedMessageContextCreate).sendResponse(eq(EntityResponseCode.OK), isA(Instance.class));
            }
        });

        // allocate addr
        currentDhtClientFactory.createBlockingWriter().update(currentPiIdBuilder.getPId(PublicIpAllocationIndex.URL).forLocalRegion(), null, new UpdateResolver<PublicIpAllocationIndex>() {
            @Override
            public PublicIpAllocationIndex update(PublicIpAllocationIndex existingEntity, PublicIpAllocationIndex requestedEntity) {
                existingEntity.getAllocationMap().remove(IpAddressUtils.ipToLong(VALID_PUBLIC_ADDRESS_1));
                existingEntity.getAllocationMap().put(IpAddressUtils.ipToLong(ELASTIC_IP_ADDRESS), new InstanceRecord(testInstance.getInstanceId(), testInstance.getUserId()));
                return existingEntity;
            }
        });

        // associate
        currentNetworkManagerApplication.deliver(getPIdFromNodeId(), receivedMessageContextAssociate);

        // assert
        verifyCalledEventually(15, new Runnable() {
            @Override
            public void run() {
                assertEquals(ELASTIC_IP_ADDRESS, ((SecurityGroup) currentConsumedDhtResourceRegistry.getCachedEntity(securityGroupRecordId)).getInstances().get(testInstance.getInstanceId()).getPublicIpAddress());
                assertEquals(ELASTIC_IP_ADDRESS, ((SecurityGroup) currentDhtClientFactory.createBlockingReader().get(securityGroupRecordId)).getInstances().get(testInstance.getInstanceId()).getPublicIpAddress());
                verifyCommand("ip addr del 10.0.0.1/32 dev eth1");
                verifyCommand("ip addr add " + ELASTIC_IP_ADDRESS + "/32 dev eth1");
                verifyCommand("arping -f -U -I eth1 -s " + ELASTIC_IP_ADDRESS + " 123.456.789.12");
            }
        });
    }

    @Test
    public void shouldProcessSecurityGroupUpdateQueueItemByNetworkManager() throws Exception {
        // setup
        SecurityGroup securityGroup = new SecurityGroup("sgOwner", "staleSg1");
        currentDhtClientFactory.createBlockingWriter().put(currentPiIdBuilder.getPId(securityGroup).forLocalRegion(), securityGroup);

        changeContextToNode(getNodeAboveLive());
        setupQueueWatcherInitiator(currentSecurityGroupUpdateTaskQueueWatcherInitiator, 4000);

        // act
        currentTaskProcessingQueueHelper.addUrlToQueue(currentPiIdBuilder.getPiQueuePId(PiQueue.UPDATE_SECURITY_GROUP).forLocalScope(PiQueue.UPDATE_SECURITY_GROUP.getNodeScope()), securityGroup.getUrl());

        // assert
        waitForQueueSize(PiQueue.UPDATE_SECURITY_GROUP, 0, 20);
        setupQueueWatcherInitiator(currentSecurityGroupUpdateTaskQueueWatcherInitiator, 30000);
    }

    @Test
    public void shouldProcessSecurityGroupDeleteQueueItemByNetworkManager() throws Exception {
        // setup
        SecurityGroup securityGroup = new SecurityGroup("sgOwner", "staleSg2");
        currentDhtClientFactory.createBlockingWriter().put(currentPiIdBuilder.getPId(securityGroup).forLocalRegion(), securityGroup);

        changeContextToNode(getNodeAboveLive());
        setupQueueWatcherInitiator(currentSecurityGroupDeleteTaskQueueWatcherInitiator, 4000);
        currentNetworkManagerApplication.forceActivationCheck();
        Thread.sleep(500);

        // act
        PId queueId = currentPiIdBuilder.getPiQueuePId(PiQueue.REMOVE_SECURITY_GROUP).forLocalScope(PiQueue.REMOVE_SECURITY_GROUP.getNodeScope());
        currentTaskProcessingQueueHelper.addUrlToQueue(queueId, securityGroup.getUrl());

        // assert
        waitForQueueSize(PiQueue.REMOVE_SECURITY_GROUP, 0, 20);
        setupQueueWatcherInitiator(currentSecurityGroupDeleteTaskQueueWatcherInitiator, 30000);
    }

    @Test
    public void shouldProcessInstanceNetworkManagerTeardownQueueItem() throws Exception {
        // setup
        changeContextToNode(getLiveNetworkAppNodePort());
        registerSecurityGroup();

        changeContextToNode(getNodeAboveLive());
        setupQueueWatcherInitiator(currentInstanceNetworkManagerTeardownTaskQueueWatcherInitiator, 4000, 4000);
        currentNetworkManagerApplication.forceActivationCheck();
        Thread.sleep(500);

        // act
        PId queueId = currentPiIdBuilder.getPiQueuePId(PiQueue.INSTANCE_NETWORK_MANAGER_TEARDOWN).forLocalScope(PiQueue.INSTANCE_NETWORK_MANAGER_TEARDOWN.getNodeScope());
        currentTaskProcessingQueueHelper.addUrlToQueue(queueId, testInstance.getUrl());

        // assert
        waitForQueueSize(PiQueue.INSTANCE_NETWORK_MANAGER_TEARDOWN, 0, 20);
        setupQueueWatcherInitiator(currentInstanceNetworkManagerTeardownTaskQueueWatcherInitiator, 30000);
    }

    @Test
    public void shouldAssociateAnElasticAddressWithInstanceByPickingUpStaleQueueItem() throws Exception {
        // setup
        addDeviceHasAddress(ELASTIC_IP_ADDRESS, false);
        PublicIpAddress publicIpAddressEntity = new PublicIpAddress(ELASTIC_IP_ADDRESS, testInstance.getInstanceId(), testInstance.getUserId(), testInstance.getSecurityGroupName());
        final ReceivedMessageContext receivedMessageContextCreate = setupReceivedMessageContext(EntityMethod.CREATE, testInstance);

        // create
        currentNetworkManagerApplication.deliver(getPIdFromNodeId(), receivedMessageContextCreate);
        verifyCalledEventually(15, new Runnable() {
            public void run() {
                verify(receivedMessageContextCreate).sendResponse(eq(EntityResponseCode.OK), isA(Instance.class));
            }
        });

        // allocate addr
        currentDhtClientFactory.createBlockingWriter().update(currentPiIdBuilder.getPId(PublicIpAllocationIndex.URL).forLocalRegion(), null, new UpdateResolver<PublicIpAllocationIndex>() {
            @Override
            public PublicIpAllocationIndex update(PublicIpAllocationIndex existingEntity, PublicIpAllocationIndex requestedEntity) {
                existingEntity.getAllocationMap().remove(IpAddressUtils.ipToLong(VALID_PUBLIC_ADDRESS_1));
                existingEntity.getAllocationMap().put(IpAddressUtils.ipToLong(ELASTIC_IP_ADDRESS), new InstanceRecord(testInstance.getInstanceId(), testInstance.getUserId()));
                return existingEntity;
            }
        });

        // enqueue and trigger dequeue
        changeContextToNode(getNodeAboveLive());
        setupQueueWatcherInitiator(currentAssociateAddressTaskQueueWatcherInitiator, 2000);

        currentNetworkManagerApplication.forceActivationCheck();
        Thread.sleep(500);

        // act
        currentTaskProcessingQueueHelper.addUrlToQueue(currentPiIdBuilder.getPiQueuePId(PiQueue.ASSOCIATE_ADDRESS).forLocalScope(PiQueue.ASSOCIATE_ADDRESS.getNodeScope()), QueueTaskUriHelper.getUriForAssociateAddress(publicIpAddressEntity));

        // assert
        waitForQueueSize(PiQueue.ASSOCIATE_ADDRESS, 0, 15);
        changeContextToNode(getLiveNetworkAppNodePort());
        verifyCalledEventually(15, new Runnable() {
            @Override
            public void run() {
                assertEquals(ELASTIC_IP_ADDRESS, ((SecurityGroup) currentConsumedDhtResourceRegistry.getCachedEntity(securityGroupRecordId)).getInstances().get(testInstance.getInstanceId()).getPublicIpAddress());
                assertEquals(ELASTIC_IP_ADDRESS, ((SecurityGroup) currentDhtClientFactory.createBlockingReader().get(securityGroupRecordId)).getInstances().get(testInstance.getInstanceId()).getPublicIpAddress());
                verifyCommand("ip addr del 10.0.0.1/32 dev eth1");
                verifyCommand("ip addr add " + ELASTIC_IP_ADDRESS + "/32 dev eth1");
                verifyCommand("arping -f -U -I eth1 -s " + ELASTIC_IP_ADDRESS + " 123.456.789.12");
            }
        });
        changeContextToNode(getNodeAboveLive());
        setupQueueWatcherInitiator(currentAssociateAddressTaskQueueWatcherInitiator, 30000);
    }

    @Test
    public void shouldDisassociateAnElasticAddressWithInstance() throws Exception {
        // setup
        addDeviceHasAddress(ELASTIC_IP_ADDRESS, false);

        PublicIpAddress publicIpAddressEntity = new PublicIpAddress(ELASTIC_IP_ADDRESS, testInstance.getInstanceId(), testInstance.getUserId(), testInstance.getSecurityGroupName());

        final ReceivedMessageContext receivedMessageContextCreate = setupReceivedMessageContext(EntityMethod.CREATE, testInstance);
        final ReceivedMessageContext receivedMessageContextAssociate = setupReceivedMessageContext(EntityMethod.CREATE, publicIpAddressEntity);
        final ReceivedMessageContext receivedMessageContextDisassociate = setupReceivedMessageContext(EntityMethod.DELETE, publicIpAddressEntity);

        // create
        currentNetworkManagerApplication.deliver(getPIdFromNodeId(), receivedMessageContextCreate);
        verifyCalledEventually(15, new Runnable() {
            public void run() {
                verify(receivedMessageContextCreate).sendResponse(eq(EntityResponseCode.OK), isA(Instance.class));
            }
        });

        // allocate addr
        currentDhtClientFactory.createBlockingWriter().update(currentPiIdBuilder.getPId(PublicIpAllocationIndex.URL).forLocalRegion(), null, new UpdateResolver<PublicIpAllocationIndex>() {
            @Override
            public PublicIpAllocationIndex update(PublicIpAllocationIndex existingEntity, PublicIpAllocationIndex requestedEntity) {
                existingEntity.getAllocationMap().remove(IpAddressUtils.ipToLong(VALID_PUBLIC_ADDRESS_1));
                existingEntity.getAllocationMap().put(IpAddressUtils.ipToLong(ELASTIC_IP_ADDRESS), new InstanceRecord(testInstance.getInstanceId(), testInstance.getUserId()));
                return existingEntity;
            }
        });

        // associate
        currentNetworkManagerApplication.deliver(getPIdFromNodeId(), receivedMessageContextAssociate);
        verifyCalledEventually(15, new Runnable() {
            public void run() {
                verifyCommand("ip addr add " + ELASTIC_IP_ADDRESS + "/32 dev eth1");
            }
        });
        currentDhtClientFactory.createBlockingWriter().update(currentPiIdBuilder.getPIdForEc2AvailabilityZone(testInstance.getUrl()), null, new UpdateResolver<Instance>() {
            @Override
            public Instance update(Instance existingEntity, Instance requestedEntity) {
                existingEntity.setPublicIpAddress(ELASTIC_IP_ADDRESS);
                return existingEntity;
            }
        });

        // disassociate
        addDeviceHasAddress(ELASTIC_IP_ADDRESS, true);
        currentNetworkManagerApplication.deliver(getPIdFromNodeId(), receivedMessageContextDisassociate);

        // assert
        verifyCalledEventually(15, new Runnable() {
            @Override
            public void run() {
                assertEquals(VALID_PUBLIC_ADDRESS_2, ((SecurityGroup) currentConsumedDhtResourceRegistry.getCachedEntity(securityGroupRecordId)).getInstances().get(testInstance.getInstanceId()).getPublicIpAddress());
                assertEquals(VALID_PUBLIC_ADDRESS_2, ((SecurityGroup) currentDhtClientFactory.createBlockingReader().get(securityGroupRecordId)).getInstances().get(testInstance.getInstanceId()).getPublicIpAddress());
                assertEquals(VALID_PUBLIC_ADDRESS_2, ((Instance) currentDhtClientFactory.createBlockingReader().get(currentPiIdBuilder.getPIdForEc2AvailabilityZone(testInstance))).getPublicIpAddress());
                verifyCommand("ip addr del " + ELASTIC_IP_ADDRESS + "/32 dev eth1");
            }
        });
    }

    @Test
    public void shouldDisassociateAnElasticAddressWithInstanceByPickingUpStaleQueueItem() throws Exception {
        // setup
        addDeviceHasAddress(ELASTIC_IP_ADDRESS, false);
        PublicIpAddress publicIpAddressEntity = new PublicIpAddress(ELASTIC_IP_ADDRESS, testInstance.getInstanceId(), testInstance.getUserId(), testInstance.getSecurityGroupName());
        final ReceivedMessageContext receivedMessageContextCreate = setupReceivedMessageContext(EntityMethod.CREATE, testInstance);
        final ReceivedMessageContext receivedMessageContextAssociate = setupReceivedMessageContext(EntityMethod.CREATE, publicIpAddressEntity);

        // create
        currentNetworkManagerApplication.deliver(getPIdFromNodeId(), receivedMessageContextCreate);
        verifyCalledEventually(15, new Runnable() {
            public void run() {
                verify(receivedMessageContextCreate).sendResponse(eq(EntityResponseCode.OK), isA(Instance.class));
            }
        });

        // allocate addr
        currentDhtClientFactory.createBlockingWriter().update(currentPiIdBuilder.getPId(PublicIpAllocationIndex.URL).forLocalRegion(), null, new UpdateResolver<PublicIpAllocationIndex>() {
            @Override
            public PublicIpAllocationIndex update(PublicIpAllocationIndex existingEntity, PublicIpAllocationIndex requestedEntity) {
                existingEntity.getAllocationMap().remove(IpAddressUtils.ipToLong(VALID_PUBLIC_ADDRESS_1));
                existingEntity.getAllocationMap().put(IpAddressUtils.ipToLong(ELASTIC_IP_ADDRESS), new InstanceRecord(testInstance.getInstanceId(), testInstance.getUserId()));
                return existingEntity;
            }
        });

        // associate
        currentNetworkManagerApplication.deliver(getPIdFromNodeId(), receivedMessageContextAssociate);
        verifyCalledEventually(15, new Runnable() {
            public void run() {
                verifyCommand("ip addr add " + ELASTIC_IP_ADDRESS + "/32 dev eth1");
            }
        });
        currentDhtClientFactory.createBlockingWriter().update(currentPiIdBuilder.getPIdForEc2AvailabilityZone(testInstance.getUrl()), null, new UpdateResolver<Instance>() {
            @Override
            public Instance update(Instance existingEntity, Instance requestedEntity) {
                existingEntity.setPublicIpAddress(ELASTIC_IP_ADDRESS);
                return existingEntity;
            }
        });

        // disassociate - enqueue and trigger dequeue
        addDeviceHasAddress(ELASTIC_IP_ADDRESS, true);
        setupQueueWatcherInitiator(currentDisassociateAddressTaskQueueWatcherInitiator, 2000);

        changeContextToNode(getNodeAboveLive());

        currentNetworkManagerApplication.forceActivationCheck();
        Thread.sleep(500);

        // act
        currentTaskProcessingQueueHelper.addUrlToQueue(currentPiIdBuilder.getPiQueuePId(PiQueue.DISASSOCIATE_ADDRESS).forLocalScope(PiQueue.DISASSOCIATE_ADDRESS.getNodeScope()), QueueTaskUriHelper.getUriForAssociateAddress(publicIpAddressEntity));

        // assert
        changeContextToNode(getLiveNetworkAppNodePort());
        waitForQueueSize(PiQueue.DISASSOCIATE_ADDRESS, 0, 15);
        verifyCalledEventually(30, new Runnable() {
            @Override
            public void run() {
                assertEquals(VALID_PUBLIC_ADDRESS_2, ((SecurityGroup) currentConsumedDhtResourceRegistry.getCachedEntity(securityGroupRecordId)).getInstances().get(testInstance.getInstanceId()).getPublicIpAddress());
                assertEquals(VALID_PUBLIC_ADDRESS_2, ((SecurityGroup) currentDhtClientFactory.createBlockingReader().get(securityGroupRecordId)).getInstances().get(testInstance.getInstanceId()).getPublicIpAddress());
                assertEquals(VALID_PUBLIC_ADDRESS_2, ((Instance) currentDhtClientFactory.createBlockingReader().get(currentPiIdBuilder.getPIdForEc2AvailabilityZone(testInstance))).getPublicIpAddress());
                verifyCommand("ip addr del " + ELASTIC_IP_ADDRESS + "/32 dev eth1");
            }
        });
        setupQueueWatcherInitiator(currentDisassociateAddressTaskQueueWatcherInitiator, 30000);
    }

    private void createAnotherTestInstance() {
        anotherTestInstance = new Instance();
        anotherTestInstance.setInstanceId(getPiIdBuilder().generateBase62Ec2Id("i", getKoalaNode().getKoalaIdFactory().getGlobalAvailabilityZoneCode()));
        anotherTestInstance.setSecurityGroupName(securityGroup.getOwnerIdGroupNamePair().getGroupName());
        anotherTestInstance.setUserId(securityGroup.getOwnerIdGroupNamePair().getOwnerId());
        anotherTestInstance.setAvailabilityZoneCode(getKoalaIdFactory().getAvailabilityZoneWithinRegion());
        anotherTestInstance.setRegionCode(getKoalaIdFactory().getRegion());
        BlockingDhtWriter blockingDhtWriter = getDhtClientFactory().createBlockingWriter();
        blockingDhtWriter.put(getPiIdBuilder().getPIdForEc2AvailabilityZone(anotherTestInstance.getUrl()), anotherTestInstance);
    }

    @Test
    public void shouldSetUpAndTearDownNetworkForTwoInstancesInSameGroup() throws Exception {
        // setup
        createAnotherTestInstance();

        final ReceivedMessageContext receivedMessageContextCreateFirst = setupReceivedMessageContext(EntityMethod.CREATE, testInstance);
        final ReceivedMessageContext receivedMessageContextCreateSecond = setupReceivedMessageContext(EntityMethod.CREATE, anotherTestInstance);
        final ReceivedMessageContext receivedMessageContextDeleteFirst = setupReceivedMessageContext(EntityMethod.DELETE, testInstance);
        final ReceivedMessageContext receivedMessageContextDeleteSecond = setupReceivedMessageContext(EntityMethod.DELETE, anotherTestInstance);

        // act
        currentNetworkManagerApplication.deliver(getPIdFromNodeId(), receivedMessageContextCreateFirst);
        currentNetworkManagerApplication.deliver(getPIdFromNodeId(), receivedMessageContextCreateSecond);

        verifyCalledEventually(30, new Runnable() {
            public void run() {
                verify(receivedMessageContextCreateFirst).sendResponse(eq(EntityResponseCode.OK), isA(Instance.class));
                verify(receivedMessageContextCreateSecond).sendResponse(eq(EntityResponseCode.OK), isA(Instance.class));
            }
        });

        assertNumVlansAllocated(1);
        assertNumAddrsAllocated(2);
        assertNumSubnetsAllocated(1);
        assertEquals(1, currentConsumedDhtResourceRegistry.getByType(SecurityGroup.class).size());
        assertSecGroupUpdated(1L, 2);

        currentNetworkManagerApplication.deliver(getPIdFromNodeId(), receivedMessageContextDeleteFirst);
        currentNetworkManagerApplication.deliver(getPIdFromNodeId(), receivedMessageContextDeleteSecond);

        // assert
        verifyCalledEventually(15, new Runnable() {
            @Override
            public void run() {
                assertNumVlansAllocated(0);
                assertNumAddrsAllocated(0);
                assertNumSubnetsAllocated(0);
                assertEquals(0, currentConsumedDhtResourceRegistry.getByType(SecurityGroup.class).size());
                assertSecGroupUpdated(1L, 0);
            }
        });
    }

    @Test
    public void shouldBeAbleToAcquireNetworkForInstance() throws Exception {
        // setup
        setDeviceHasAddress(false);

        final ReceivedMessageContext receivedMessageContextCreate = setupReceivedMessageContext(EntityMethod.CREATE, testInstance);
        final ReceivedMessageContext receivedMessageContextDelete = setupReceivedMessageContext(EntityMethod.UPDATE, testInstance);

        // create
        currentNetworkManagerApplication.deliver(getPIdFromNodeId(), receivedMessageContextCreate);
        verifyCalledEventually(15, new Runnable() {
            public void run() {
                verify(receivedMessageContextCreate).sendResponse(eq(EntityResponseCode.OK), isA(Instance.class));
            }
        });

        currentConsumedDhtResourceRegistry.deregisterConsumer(currentPiIdBuilder.getPId(securityGroup).forLocalRegion(), testInstance.getInstanceId());
        assertEquals(0, currentConsumedDhtResourceRegistry.getByType(SecurityGroup.class).size());

        // act
        currentNetworkManagerApplication.deliver(getPIdFromNodeId(), receivedMessageContextDelete);

        // assert
        verifyCalledEventually(15, new Runnable() {
            @Override
            public void run() {
                assertEquals(1, currentConsumedDhtResourceRegistry.getByType(SecurityGroup.class).size());
                verifyCommand("brctl addif pibr1 eth0.1", 2);
                verifyCommand("ip addr add 10.0.0.1/32 dev eth1", 2);
            }
        });
    }

    @Test
    public void shouldRemoveStaleInstanceAddressAllocationRecordFromSecGroup() throws Exception {
        // setup
        System.setProperty("securityGroupRefreshRunnerInitialIntervalMillisOverride", "8000");
        createAnotherTestInstance();

        // add stale instance
        anotherTestInstance.setState(InstanceState.TERMINATED);
        currentDhtClientFactory.createBlockingWriter().put(currentPiIdBuilder.getPIdForEc2AvailabilityZone(anotherTestInstance), anotherTestInstance);

        // and then make sure something is allocated to that stale instance
        currentDhtClientFactory.createBlockingWriter().update(currentPiIdBuilder.getPId(securityGroup.getUrl()).forLocalRegion(), null, new UpdateResolver<PiEntity>() {
            @Override
            public PiEntity update(PiEntity existingEntity, PiEntity requestedEntity) {
                ((SecurityGroup) existingEntity).getInstances().put(anotherTestInstance.getInstanceId(), new InstanceAddress());
                return existingEntity;
            }
        });

        // create net
        final ReceivedMessageContext receivedMessageContextCreate = setupReceivedMessageContext(EntityMethod.CREATE, testInstance);
        // act
        currentNetworkManagerApplication.deliver(getPIdFromNodeId(), receivedMessageContextCreate);
        verifyCalledEventually(15, new Runnable() {
            public void run() {
                verify(receivedMessageContextCreate).sendResponse(eq(EntityResponseCode.OK), isA(Instance.class));
            }
        });

        // assert
        verifyCalledEventually(15, new Runnable() {
            @Override
            public void run() {
                SecurityGroup securityGroupAfter = (SecurityGroup) currentDhtClientFactory.createBlockingReader().get(currentPiIdBuilder.getPId(securityGroup.getUrl()).forLocalRegion());
                assertEquals(1, securityGroupAfter.getInstances().size());
                assertEquals(testInstance.getInstanceId(), securityGroupAfter.getInstances().keySet().iterator().next());
            }
        });
    }

    @Test
    public void shouldTimestampResourcesUsedBySecGroup() throws Exception {
        // setup
        System.setProperty("securityGroupRefreshRunnerInitialIntervalMillisOverride", "10000");
        System.setProperty("securityGroupConsumerWatcherInitialIntervalMillisOverride", "11000");

        // create net
        final ReceivedMessageContext receivedMessageContextCreate = setupReceivedMessageContext(EntityMethod.CREATE, testInstance);
        currentNetworkManagerApplication.deliver(getPIdFromNodeId(), receivedMessageContextCreate);
        verifyCalledEventually(15, new Runnable() {
            public void run() {
                verify(receivedMessageContextCreate).sendResponse(eq(EntityResponseCode.OK), isA(Instance.class));
            }
        });

        long initialVlanAllocationTimestamp = getVlanIndexResourceConsumerTimestamp(1L);
        long initialSubnetAllocationTimestamp = getSubnetIndexResourceConsumerTimestamp("172.30.0.0");
        long initialIpAddressAllocationTimestamp = getPublicIpAddressResourceConsumerTimestamp(VALID_PUBLIC_ADDRESS_1);

        // act
        Thread.sleep(14000);

        // assert
        long laterVlanAllocationTimestamp = getVlanIndexResourceConsumerTimestamp(1L);
        long laterSubnetAllocationTimestamp = getSubnetIndexResourceConsumerTimestamp("172.30.0.0");
        long laterIpAddressAllocationTimestamp = getPublicIpAddressResourceConsumerTimestamp(VALID_PUBLIC_ADDRESS_1);

        assertTrue(laterVlanAllocationTimestamp + " should have been after " + initialVlanAllocationTimestamp, laterVlanAllocationTimestamp > initialVlanAllocationTimestamp);
        assertTrue(laterSubnetAllocationTimestamp + " should have been after " + initialSubnetAllocationTimestamp, laterSubnetAllocationTimestamp > initialSubnetAllocationTimestamp);
        assertTrue(laterIpAddressAllocationTimestamp + " should have been after " + initialIpAddressAllocationTimestamp, laterIpAddressAllocationTimestamp > initialIpAddressAllocationTimestamp);
    }

    @Test
    public void watcherShouldTearDownNetworkWhenInstanceGoesToTerminated() throws Exception {
        // setup
        System.setProperty("securityGroupConsumerWatcherInitialIntervalMillisOverride", "11000");

        // create net
        final ReceivedMessageContext receivedMessageContextCreate = setupReceivedMessageContext(EntityMethod.CREATE, testInstance);
        currentNetworkManagerApplication.deliver(getPIdFromNodeId(), receivedMessageContextCreate);
        verifyCalledEventually(15, new Runnable() {
            public void run() {
                verify(receivedMessageContextCreate).sendResponse(eq(EntityResponseCode.OK), isA(Instance.class));
            }
        });

        // act
        markInstanceAsTerminated();

        // assert
        verifyCalledEventually(15, new Runnable() {
            @Override
            public void run() {
                assertNumVlansAllocated(0);
                assertNumSubnetsAllocated(0);
                assertNumAddrsAllocated(0);
                assertEquals(0, currentConsumedDhtResourceRegistry.getByType(SecurityGroup.class).size());
            }
        });
    }

    @Test
    public void watchersShouldRemoveStaleNetworkResources() throws Exception {
        // setup
        setZeroAllocatedResourceExpiryInterval(currentPiIdBuilder.getPId(VlanAllocationIndex.URL).forLocalRegion());
        setZeroAllocatedResourceExpiryInterval(currentPiIdBuilder.getPId(SubnetAllocationIndex.URL).forLocalRegion());
        setZeroAllocatedResourceExpiryInterval(currentPiIdBuilder.getPId(PublicIpAllocationIndex.URL).forLocalRegion());

        // create net
        Thread.sleep(5000); // temporary step to prevent initial stale vlan / subnet ip purging watcher
        // coming in too soon and 'racing' against the network setup logic
        final ReceivedMessageContext receivedMessageContextCreate = setupReceivedMessageContext(EntityMethod.CREATE, testInstance);
        currentNetworkManagerApplication.deliver(getPIdFromNodeId(), receivedMessageContextCreate);
        verifyCalledEventually(15, new Runnable() {
            public void run() {
                verify(receivedMessageContextCreate).sendResponse(eq(EntityResponseCode.OK), isA(Instance.class));
            }
        });

        assertNumVlansAllocated(1);
        assertNumSubnetsAllocated(1);
        assertNumAddrsAllocated(1);

        // switch active nodes, allowing stale record purging to happen
        changeContextToNode(getNodeAboveLive());
        Thread.sleep(5 * 1000);
        LOG.debug("Getting getCachedApplicationRecord for nodeAboveLive" + getNodeAboveLive() + "--");
        System.err.println("1-- " + currentApplicationRegistry);
        System.err.println("1-- " + currentApplicationRegistry.getCachedApplicationRecord(NetworkManagerApplication.APPLICATION_NAME));
        System.err.println("1-- " + currentApplicationRegistry.getCachedApplicationRecord(NetworkManagerApplication.APPLICATION_NAME).getActiveNodeMap());
        System.err.println("1-- " + currentNetworkManagerApplication);
        currentApplicationRegistry.getCachedApplicationRecord(NetworkManagerApplication.APPLICATION_NAME).getActiveNodeMap().put("1", new TimeStampedPair<String>(currentNetworkManagerApplication.getNodeIdFull()));
        currentNetworkManagerApplication.becomeActive();

        // assert
        verifyCalledEventually(15, new Runnable() {
            @Override
            public void run() {
                assertNumVlansAllocated(0);
                assertNumSubnetsAllocated(0);
                assertNumAddrsAllocated(0);
            }
        });
    }

    @Test
    public void shouldRerouteToActiveNetworkAppIfInactive() throws Exception {
        // setup
        String liveNodeIndex = getLiveNetworkAppNodePort();
        String nodeAboveLive = getNodeAboveLive();

        Instance instance = new Instance("i-BBBBBBBB", "someUser", "default");

        changeContextToNode(nodeAboveLive);
        PId idAbove = new PiId(currentNetworkManagerApplication.getNodeId().toStringFull(), currentNetworkManagerApplication.getKoalaIdFactory().getGlobalAvailabilityZoneCode());
        System.err.println("Id above " + idAbove.toStringFull());

        // act
        Thread.sleep(30000); // seems to need time to register in the application registry ?
        MessageContext messageContext = currentNetworkManagerApplication.newMessageContext();
        messageContext.routePiMessage(idAbove, EntityMethod.CREATE, instance);

        // assert
        changeContextToNode(liveNodeIndex);
        PiEntity piEntity = ((TestNetworkManagerApplication) currentNetworkManagerApplication).getDeliveredMessages().poll(60, TimeUnit.SECONDS);
        assertNotNull(piEntity);
        assertEquals("i-BBBBBBBB", ((Instance) piEntity).getInstanceId());
    }

    @Test
    public void activeNodeShouldCleanUpNetworkIfAnotherActiveNodeWithCloserNodeIdArrives() throws Exception {
        // setup
        // lets pretend we suddenly have 2 active nodes, with the second node being closer to our sec group id
        final PId closerNodeId = currentPiIdBuilder.getPId(securityGroup).forLocalAvailabilityZone();
        currentDhtClientFactory.createBlockingWriter().update(currentPiIdBuilder.getPId(String.format("%s:%s", AvailabilityZoneScopedApplicationRecord.URI_SCHEME, NetworkManagerApplication.APPLICATION_NAME)).forLocalAvailabilityZone(), null,
                new UpdateResolver<ApplicationRecord>() {
                    public ApplicationRecord update(ApplicationRecord existingEntity, ApplicationRecord requestedEntity) {
                        existingEntity.getActiveNodeMap().put("2", new TimeStampedPair<String>(closerNodeId.toStringFull()));
                        System.err.println("network manager app record updated: " + existingEntity);
                        return existingEntity;
                    }
                });

        // put a sec group into the resource manager
        registerSecurityGroup();
        assertEquals(1, currentConsumedDhtResourceRegistry.getByType(SecurityGroup.class).size());

        // act
        // fake out publish and delivery of app record updated event to the right node
        System.err.println("Initiating activation check for test. node: " + currentNetworkManagerApplication.getNodeIdFull());
        SharedRecordConditionalApplicationActivator activator = (SharedRecordConditionalApplicationActivator) currentNetworkManagerApplication.getApplicationActivator();
        activator.initiateActivationChecks(currentNetworkManagerApplication);

        // assert
        verifyCalledEventually(10, new Runnable() {
            @Override
            public void run() {
                verify(currentSpiedOnConsumedDhtResourceRegistry).clearResource(eq(securityGroupRecordId));
                verifyCommand("iptables-save -t nat");
                verifyCommand("iptables-save -t filter");
            }
        });
        assertEquals(0, currentConsumedDhtResourceRegistry.getByType(SecurityGroup.class).size());
    }

    @Test
    public void shouldHandleDirectMessageSecurityGroupUpdateByRefreshingIpTables() throws Exception {
        // setup
        addNetworkRulesToSecurityGroupAndAddToDht();
        ReceivedMessageContext receivedMessageContextUpdate = setupReceivedMessageContext(EntityMethod.UPDATE, securityGroup);

        registerSecurityGroup();

        // act
        currentNetworkManagerApplication.deliver(getPIdFromNodeId(), receivedMessageContextUpdate);

        // assert
        verifyCalledEventually(10, new Runnable() {
            @SuppressWarnings("unchecked")
            @Override
            public void run() {
                verify(currentSpiedOnConsumedDhtResourceRegistry).refresh(eq(securityGroupRecordId), isA(Continuation.class));
                verifyCommand("iptables-save -t nat");
                verifyCommand("iptables-save -t filter");
            }
        });
    }

    @Test
    public void shouldHandlePubSubMessageSecurityGroupUpdateByRefreshingIpTables() throws Exception {
        // setup
        addNetworkRulesToSecurityGroupAndAddToDht();
        registerSecurityGroup();

        String liveNodePort = getLiveNetworkAppNodePort();
        String nodeAboveLivePort = getNodeAboveLive();
        changeContextToNode(nodeAboveLivePort);

        PubSubMessageContext pubSubMessageContext = currentNetworkManagerApplication.newLocalPubSubMessageContext(PiTopics.NETWORK_MANAGERS_IN_REGION);

        // act
        pubSubMessageContext.publishContent(EntityMethod.UPDATE, securityGroup);

        // assert
        changeContextToNode(liveNodePort);
        verifyCalledEventually(10, new Runnable() {
            @SuppressWarnings("unchecked")
            @Override
            public void run() {
                verify(currentSpiedOnConsumedDhtResourceRegistry).refresh(eq(securityGroupRecordId), isA(Continuation.class));
                verifyCommand("iptables-save -t nat");
                verifyCommand("iptables-save -t filter");
            }
        });
    }

    @Test
    public void shouldHandlePubSubMessageSecurityGroupDelete() throws Exception {
        // setup
        addNetworkRulesToSecurityGroupAndAddToDht();
        registerSecurityGroup();

        String liveNodePort = getLiveNetworkAppNodePort();
        String nodeAboveLivePort = getNodeAboveLive();
        changeContextToNode(nodeAboveLivePort);

        PubSubMessageContext pubSubMessageContext = currentNetworkManagerApplication.newLocalPubSubMessageContext(PiTopics.NETWORK_MANAGERS_IN_REGION);

        // act
        pubSubMessageContext.publishContent(EntityMethod.DELETE, securityGroup);

        // assert
        changeContextToNode(liveNodePort);
        verifyCalledEventually(10, new Runnable() {
            @Override
            public void run() {
                verify(currentSpiedOnConsumedDhtResourceRegistry).clearResource(eq(securityGroupRecordId));
                verifyCommand("iptables-save -t nat");
                verifyCommand("iptables-save -t filter");
            }
        });
    }

    /*
     * this test is comparing addresses on the interface with what it thinks it's supposed to be responsible for (i.e.) might be in 
     * security groups, and deletes those that it doesn't want.
     */
    @Test
    public void shouldCleanupNetworkWhenNetworkManagerShuttingDown() throws Exception {
        // setup
        final CommandRunner mockRunner = mock(CommandRunner.class);
        final CommandResult commandResultIpAddr = mock(CommandResult.class);
        List<String> lines = new ArrayList<String>();
        for (String line : Arrays.asList(VALID_PUBLIC_ADDRESS_1, VALID_PUBLIC_ADDRESS_2, OUT_OF_RANGE_PUBLIC_IP_ADDRESS)) {
            lines.add("inet " + line + "/32 brd 10.255.255.255 scope global eth1");
        }
        when(commandResultIpAddr.getOutputLines()).thenReturn(lines);

        when(mockRunner.run(org.mockito.Mockito.contains("ip addr del "), eq(10000L))).thenReturn(commandResultIpAddr);
        when(mockRunner.run(org.mockito.Mockito.contains("ip addr show "))).thenReturn(commandResultIpAddr);

        CommandResult commandResultIptables = mock(CommandResult.class);
        when(commandResultIptables.getOutputLines()).thenReturn(sampleIptablesOutput());
        when(mockRunner.run(eq("iptables-save"))).thenReturn(commandResultIptables);
        when(mockRunner.run(eq("iptables-save" + " -t nat"))).thenReturn(commandResultIptables);
        when(mockRunner.run(eq("iptables-save" + " -t filter"))).thenReturn(commandResultIptables);

        // mocking get all pi bridges
        CommandResult result = getBrctlShowOutput();
        when(mockRunner.run("brctl show")).thenReturn(result);
        when(mockRunner.run("ip -o addr show")).thenReturn(getIpAddrListPiBridgeOutput());

        // inject mocks into app ctxs
        NetworkCommandRunner networkCommandRunner = currentApplicationContext.getBean(NetworkCommandRunner.class);
        networkCommandRunner.setCommandRunner(mockRunner);
        IpTablesManager ipTablesManager = currentApplicationContext.getBean(IpTablesManager.class);
        ipTablesManager.setCommandRunner(mockRunner);
        DhcpManager dhcpManager = currentApplicationContext.getBean(DhcpManager.class);
        dhcpManager.setCommandRunner(mockRunner);

        // act
        currentNetworkManagerApplication.onApplicationStarting();

        registerSecurityGroup();
        assertEquals(1, currentConsumedDhtResourceRegistry.getByType(SecurityGroup.class).size());
        waitUntilInCache(currentPiIdBuilder.getPId(PublicIpAllocationIndex.URL).forLocalRegion(), 10000);
        waitUntilInCache(currentPiIdBuilder.getPId(SubnetAllocationIndex.URL).forLocalRegion(), 10000);

        currentNetworkManagerApplication.onApplicationShuttingDown();

        // NOTE: To restart the scheduled task which we cancel on network manager application shutting down
        AddressDeleteQueue addressDeleteQueue = currentApplicationContext.getBean(AddressDeleteQueue.class);
        addressDeleteQueue.startAddressDeleteScheduledTask();

        // assert
        final String ip_del = "ip addr del ";
        verifyCalledEventually(20, new Runnable() {
            public void run() {
                verify(mockRunner, atLeastOnce()).run(contains(ip_del + VALID_PUBLIC_ADDRESS_1), eq(10000L));
                verify(mockRunner, atLeastOnce()).run(contains(ip_del + VALID_PUBLIC_ADDRESS_2), eq(10000L));
                verify(mockRunner, never()).run(contains(OUT_OF_RANGE_PUBLIC_IP_ADDRESS));
                verify(mockRunner, never()).run(contains(OUT_OF_RANGE_PUBLIC_IP_ADDRESS), eq(10000L));
            }
        });

        // it should only delete security group router address on pi bridges
        verifyCalledEventually(20, new Runnable() {
            public void run() {
                verify(mockRunner, atLeastOnce()).run(contains(ip_del + "172.30.0.65/28"), eq(10000L));
            }
        });
    }

    /*
     * this test is comparing addresses on the interface with what it thinks it's supposed to be responsible for (i.e.) might be in 
     * security groups, and deletes those that it doesn't want.
     */
    @Test
    public void shouldCleanupNetworkWhenGoingPassive() throws Exception {
        // setup
        final CommandRunner mockRunner = mock(CommandRunner.class);
        final CommandResult commandResultIpAddr = mock(CommandResult.class);
        List<String> lines = new ArrayList<String>();
        for (String line : Arrays.asList(VALID_PUBLIC_ADDRESS_1, VALID_PUBLIC_ADDRESS_2, OUT_OF_RANGE_PUBLIC_IP_ADDRESS)) {
            lines.add("inet " + line + "/32 brd 10.255.255.255 scope global eth1");
        }
        when(commandResultIpAddr.getOutputLines()).thenReturn(lines);

        when(mockRunner.run(org.mockito.Mockito.contains("ip addr del "), eq(10000L))).thenReturn(commandResultIpAddr);
        when(mockRunner.run(org.mockito.Mockito.contains("ip addr show "))).thenReturn(commandResultIpAddr);

        CommandResult commandResultIptables = mock(CommandResult.class);
        when(commandResultIptables.getOutputLines()).thenReturn(sampleIptablesOutput());
        when(mockRunner.run(eq("iptables-save"))).thenReturn(commandResultIptables);
        when(mockRunner.run(eq("iptables-save" + " -t nat"))).thenReturn(commandResultIptables);
        when(mockRunner.run(eq("iptables-save" + " -t filter"))).thenReturn(commandResultIptables);

        // mocking get all pi bridges
        CommandResult result = getBrctlShowOutput();
        when(mockRunner.run("brctl show")).thenReturn(result);
        when(mockRunner.run("ip -o addr show")).thenReturn(getIpAddrListPiBridgeOutput());

        // inject mocks into app ctxs
        NetworkCommandRunner networkCommandRunner = currentApplicationContext.getBean(NetworkCommandRunner.class);
        networkCommandRunner.setCommandRunner(mockRunner);
        IpTablesManager ipTablesManager = currentApplicationContext.getBean(IpTablesManager.class);
        ipTablesManager.setCommandRunner(mockRunner);
        DhcpManager dhcpManager = currentApplicationContext.getBean(DhcpManager.class);
        dhcpManager.setCommandRunner(mockRunner);

        // act
        currentNetworkManagerApplication.onApplicationStarting();

        registerSecurityGroup();
        assertEquals(1, currentConsumedDhtResourceRegistry.getByType(SecurityGroup.class).size());
        waitUntilInCache(currentPiIdBuilder.getPId(PublicIpAllocationIndex.URL).forLocalRegion(), 10000);
        waitUntilInCache(currentPiIdBuilder.getPId(SubnetAllocationIndex.URL).forLocalRegion(), 10000);

        currentNetworkManagerApplication.becomePassive();

        // assert
        final String ip_del = "ip addr del ";
        verifyCalledEventually(120, new Runnable() {
            public void run() {
                verify(mockRunner, atLeastOnce()).run(contains(ip_del + VALID_PUBLIC_ADDRESS_1), eq(10000L));
                verify(mockRunner, atLeastOnce()).run(contains(ip_del + VALID_PUBLIC_ADDRESS_2), eq(10000L));
                verify(mockRunner, never()).run(contains(OUT_OF_RANGE_PUBLIC_IP_ADDRESS));
                verify(mockRunner, never()).run(contains(OUT_OF_RANGE_PUBLIC_IP_ADDRESS), eq(10000L));
            }
        });

        // it should only delete security group router address on pi bridges
        verifyCalledEventually(120, new Runnable() {
            public void run() {
                verify(mockRunner, atLeastOnce()).run(contains(ip_del + "172.30.0.65/28"), eq(10000L));
            }
        });
    }

    private void registerSecurityGroup() throws InterruptedException {
        final CountDownLatch countDownLatch = new CountDownLatch(1);
        currentConsumedDhtResourceRegistry.registerConsumer(securityGroupRecordId, "i-ABC", SecurityGroup.class, new GenericContinuation<Boolean>() {
            public void handleResult(Boolean result) {
                System.out.println("Security group: " + securityGroupRecordId + " registered.");
                countDownLatch.countDown();
            }
        });
        assertTrue(countDownLatch.await(5, TimeUnit.SECONDS));
    }

    private void addNetworkRulesToSecurityGroupAndAddToDht() {
        clearOtherFieldsInSecurityGroup();

        securityGroup.addNetworkRule(newNetworkRule());
        securityGroup.addNetworkRule(newNetworkRule());
        securityGroup.addNetworkRule(newNetworkRule());

        BlockingDhtWriter blockingDhtWriter = currentDhtClientFactory.createBlockingWriter();
        blockingDhtWriter.update(securityGroupRecordId, securityGroup, new UpdateResolver<SecurityGroup>() {
            @Override
            public SecurityGroup update(SecurityGroup existingEntity, SecurityGroup requestedEntity) {
                if (null != existingEntity)
                    requestedEntity.setVersion(existingEntity.getVersion() + 1);
                return requestedEntity;
            }
        });
    }

    private void clearOtherFieldsInSecurityGroup() {
        securityGroup.setDescription(null);
        securityGroup.setDnsAddress(null);
        securityGroup.setInstances(null);
        securityGroup.setNetmask(null);
        securityGroup.setNetworkAddress(null);
        securityGroup.setVlanId(null);
    }

    private NetworkRule newNetworkRule() {
        return new NetworkRule();
    }

    private void setZeroAllocatedResourceExpiryInterval(PId id) {
        BlockingDhtWriter blockingDhtWriter = currentDhtClientFactory.createBlockingWriter();
        blockingDhtWriter.update(id, null, new UpdateResolver<PiEntity>() {
            @Override
            public PiEntity update(PiEntity existingEntity, PiEntity requestedEntity) {
                try {
                    AllocatableResourceIndexBase<?> index = (AllocatableResourceIndexBase<?>) existingEntity;
                    index.setInactiveResourceConsumerTimeoutSec(0L);
                    return index;
                } catch (Throwable t) {
                    t.printStackTrace();
                    throw new RuntimeException(t);
                }
            }
        });
    }

    private void markInstanceAsTerminated() {
        currentDhtClientFactory.createBlockingWriter().update(currentPiIdBuilder.getPIdForEc2AvailabilityZone(testInstance.getUrl()), null, new UpdateResolver<PiEntity>() {
            public PiEntity update(PiEntity existingEntity, PiEntity requestedEntity) {
                ((Instance) existingEntity).setState(InstanceState.TERMINATED);
                return existingEntity;
            }
        });
    }

    private long getVlanIndexResourceConsumerTimestamp(long vlanId) {
        VlanAllocationIndex vlanAllocationIndex = (VlanAllocationIndex) currentDhtClientFactory.createBlockingReader().get(currentPiIdBuilder.getPId(VlanAllocationIndex.URL).forLocalRegion());
        return vlanAllocationIndex.getAllocationMap().get(vlanId).getLastHeartbeatTimestamp();
    }

    private long getSubnetIndexResourceConsumerTimestamp(String ipAddr) {
        SubnetAllocationIndex subnetAllocationIndex = (SubnetAllocationIndex) currentDhtClientFactory.createBlockingReader().get(currentPiIdBuilder.getPId(SubnetAllocationIndex.URL).forLocalRegion());
        return subnetAllocationIndex.getAllocationMap().get(IpAddressUtils.ipToLong(ipAddr)).getLastHeartbeatTimestamp();
    }

    private long getPublicIpAddressResourceConsumerTimestamp(String ipAddr) {
        PublicIpAllocationIndex index = (PublicIpAllocationIndex) currentDhtClientFactory.createBlockingReader().get(currentPiIdBuilder.getPId(PublicIpAllocationIndex.URL).forLocalRegion());
        return index.getAllocationMap().get(IpAddressUtils.ipToLong(ipAddr)).getLastHeartbeatTimestamp();
    }

    private void assertSecGroupUpdated(Long vlanId, int numAddrs) {
        SecurityGroup dhtSecGroup = (SecurityGroup) currentDhtClientFactory.createBlockingReader().get(securityGroupRecordId);
        assertEquals(vlanId, dhtSecGroup.getVlanId());
        assertEquals(numAddrs, dhtSecGroup.getInstances().size());
    }

    private void setupQueueWatcherInitiator(TaskProcessingQueueWatcherInitiatorBase watcherInitiator, int intervalMillis) {
        watcherInitiator.setInitialQueueWatcherIntervalMillis(intervalMillis);
        watcherInitiator.setStaleQueueItemMillis(1);
        watcherInitiator.createTaskProcessingQueueWatcher(currentPastryNode.getNodeId().toStringFull());
    }

    private void setupQueueWatcherInitiator(TaskProcessingQueueWatcherInitiatorBase watcherInitiator, int intervalMillis, int repeatingMillis) {
        watcherInitiator.setInitialQueueWatcherIntervalMillis(intervalMillis);
        watcherInitiator.setRepeatingQueueWatcherIntervalMillis(repeatingMillis);
        watcherInitiator.setStaleQueueItemMillis(1);
        watcherInitiator.createTaskProcessingQueueWatcher(currentPastryNode.getNodeId().toStringFull());
    }

    private String getNodeAboveLive() {
        int liveNodePort = Integer.parseInt(getLiveNetworkAppNodePort());
        liveNodePort++;
        if (liveNodePort > (CLUSTER_START_PORT + numberOfNodes - 1))
            liveNodePort = CLUSTER_START_PORT;
        return Integer.toString(liveNodePort);
    }

    private void addDeviceHasAddress(String address, boolean b) {
        for (Entry<String, AbstractApplicationContext> entry : applicationContexts.entrySet()) {
            StubDeviceUtils stubDeviceUtils = (StubDeviceUtils) entry.getValue().getBean("deviceUtils");
            stubDeviceUtils.addAddress(address, b);
        }
    }

    private void setDeviceHasAddress(boolean b) {
        for (Entry<String, AbstractApplicationContext> entry : applicationContexts.entrySet()) {
            StubDeviceUtils stubDeviceUtils = (StubDeviceUtils) entry.getValue().getBean("deviceUtils");
            stubDeviceUtils.setDeviceAlwaysExists(b);
        }
    }

    private int getNumVlansAllocated() {
        VlanAllocationIndex vlanAllocationIndex = (VlanAllocationIndex) currentDhtClientFactory.createBlockingReader().get(currentPiIdBuilder.getPId(VlanAllocationIndex.URL).forLocalRegion());
        return vlanAllocationIndex.getAllocationMap().size();
    }

    private void verifyCommand(String toMatch, int occurences) {
        StubCommandExecutor stubCommandExecutor = (StubCommandExecutor) currentApplicationContext.getBean("stubCommandExecutor");
        assertTrue(stubCommandExecutor.assertCommand(toMatch.split(" "), occurences));
    }

    private void assertNumVlansAllocated(int num) {
        assertEquals(num, getNumVlansAllocated());
    }

    private int getNumAddrsAllocated() {
        PublicIpAllocationIndex publicIpAllocationIndex = (PublicIpAllocationIndex) currentDhtClientFactory.createBlockingReader().get(currentPiIdBuilder.getPId(PublicIpAllocationIndex.URL).forLocalRegion());
        return publicIpAllocationIndex.getAllocationMap().size();
    }

    private void assertNumAddrsAllocated(int num) {
        assertEquals(num, getNumAddrsAllocated());
    }

    private int getNumSubnetsAllocated() {
        SubnetAllocationIndex subnetAllocationIndex = (SubnetAllocationIndex) currentDhtClientFactory.createBlockingReader().get(currentPiIdBuilder.getPId(SubnetAllocationIndex.URL).forLocalRegion());
        return subnetAllocationIndex.getAllocationMap().size();
    }

    private void assertNumSubnetsAllocated(int num) {
        assertEquals(num, getNumSubnetsAllocated());
    }

    private ReceivedMessageContext setupReceivedMessageContext(EntityMethod entityMethod, PiEntity entity) {
        ReceivedMessageContext receivedMessageContext = mock(ReceivedMessageContext.class);
        when(receivedMessageContext.getMethod()).thenReturn(entityMethod);
        when(receivedMessageContext.getReceivedEntity()).thenReturn(entity);
        return receivedMessageContext;
    }

    private List<String> sampleIptablesOutput() {
        List<String> sb = new ArrayList<String>();
        sb.add("*nat");
        sb.add(":PREROUTING ACCEPT [0:0]");
        sb.add(":POSTROUTING ACCEPT [2:120]");
        sb.add(":OUTPUT ACCEPT [2:120]");
        sb.add(":POST-9W0ioE23GzlYxZDnfEkrFw== - [0:0]");
        sb.add(":POST-hgvHy68+HHdL/uFREcfE1Q== - [0:0]");
        sb.add(":POST-sOPwadtSCBogG/2OYDBM0w== - [0:0]");
        sb.add(":bb - [0:0]");
        sb.add("-A PREROUTING -d 1.2.3.4/32 -j DNAT --to-destination 172.0.0.2 ");
        sb.add("-A PREROUTING -d 5.6.7.8/32 -j DNAT --to-destination 172.0.0.18");
        sb.add("-A PREROUTING -d 9.10.11.12/32 -j DNAT --to-destination 172.0.0.45");
        sb.add("-A POSTROUTING -d 172.0.0.32/28 -j POST-hgvHy68+HHdL/uFREcfE1Q==");
        sb.add("-A POSTROUTING -d 172.0.0.16/28 -j POST-9W0ioE23GzlYxZDnfEkrFw==");
        sb.add("-A POSTROUTING -d 172.0.0.0/28 -j POST-sOPwadtSCBogG/2OYDBM0w==");
        sb.add("-A OUTPUT -d 1.2.3.4/32 -j DNAT --to-destination 172.0.0.2");
        sb.add("-A OUTPUT -d 5.6.7.8/32 -j DNAT --to-destination 172.0.0.18");
        sb.add("-A OUTPUT -d 9.10.11.12/32 -j DNAT --to-destination 172.0.0.45");
        sb.add("-A POST-9W0ioE23GzlYxZDnfEkrFw== -d 172.0.0.18/32 -j SNAT --to-source 5.6.7.8");
        sb.add("-A POST-hgvHy68+HHdL/uFREcfE1Q== -d 172.0.0.45/32 -j SNAT --to-source 9.10.11.12");
        sb.add("-A POST-sOPwadtSCBogG/2OYDBM0w== -d 172.0.0.2/32 -j SNAT --to-source 1.2.3.4");
        sb.add("COMMIT");
        sb.add("# Completed on Fri Oct 16 09:48:48 2009");
        sb.add("# Generated by iptables-save v1.4.1.1 on Fri Oct 16 09:48:48 2009");
        sb.add("*filter");
        sb.add(":INPUT ACCEPT [212801:217224757]");
        sb.add(":FORWARD ACCEPT [0:0]");
        sb.add(":OUTPUT ACCEPT [148444:25148602]");
        sb.add(":FLTR-9W0ioE23GzlYxZDnfEkrFw== - [0:0]");
        sb.add(":FLTR-hgvHy68+HHdL/uFREcfE1Q== - [0:0]");
        sb.add(":FLTR-sOPwadtSCBogG/2OYDBM0w== - [0:0]");
        sb.add(":aa+aa - [0:0]");
        sb.add(":aa/aa - [0:0]");
        sb.add(":aa_aa - [0:0]");
        sb.add(":pi-chain - [0:0]");
        sb.add("-A FORWARD -j pi-chain");
        sb.add("-A FLTR-9W0ioE23GzlYxZDnfEkrFw== -s 1.2.3.4/32 -d 172.0.0.16/28 -p udp -j ACCEPT");
        sb.add("-A FLTR-sOPwadtSCBogG/2OYDBM0w== -d 172.0.0.0/28 -p tcp -m tcp --dport 22 -j ACCEPT");
        sb.add("-A pi-chain -d 172.0.0.0/28 -j FLTR-sOPwadtSCBogG/2OYDBM0w==");
        sb.add("-A pi-chain -d 172.0.0.16/28 -j FLTR-9W0ioE23GzlYxZDnfEkrFw==");
        sb.add("-A pi-chain -d 172.0.0.32/28 -j FLTR-hgvHy68+HHdL/uFREcfE1Q==");
        sb.add("COMMIT");
        sb.add("# Completed on Fri Oct 16 09:48:48 2009");
        return sb;
    }

    private CommandResult getBrctlShowOutput() {
        ArrayList<String> brctlOutput = new ArrayList<String>();
        brctlOutput.add("bridge name     bridge id               STP enabled     interfaces");
        brctlOutput.add("pibr101         8000.001e68c60d42       no              vif1.0");
        brctlOutput.add("                                                          eth0.201");
        brctlOutput.add("pibr102         8000.001e68c60d42       no              vif1.0");
        brctlOutput.add("                                                          eth0.201");
        brctlOutput.add("pibr103         8000.001e68c60d42       no              vif1.0");
        brctlOutput.add("                                                          eth0.201");
        brctlOutput.add("virbr0          8000.000000000000       yes");
        brctlOutput.add("xenbr.eth0              8000.feffffffffff       no              peth0");
        brctlOutput.add("                                                                vif0.0");

        CommandResult brctlResult = new CommandResult(0, brctlOutput, new ArrayList<String>());
        return brctlResult;
    }

    private CommandResult getIpAddrListPiBridgeOutput() {
        ArrayList<String> addrList = new ArrayList<String>();
        addrList.add("260: pibr103: <BROADCAST,MULTICAST,UP,LOWER_UP> mtu 1500 qdisc noqueue \\    link/ether 00:1e:68:c5:e7:6e brd ff:ff:ff:ff:ff:ff");
        addrList.add("260: pibr103    inet 172.31.0.67/28 brd 10.19.128.63 scope global pibr103");
        addrList.add("262: pibr100: <BROADCAST,MULTICAST,UP,LOWER_UP> mtu 1500 qdisc noqueue \\    link/ether 00:1e:68:c5:e7:6e brd ff:ff:ff:ff:ff:ff");
        addrList.add("262: pibr100    inet 10.19.128.1/28 brd 10.19.128.15 scope global pibr100");
        addrList.add("264: pibr102: <BROADCAST,MULTICAST,UP,LOWER_UP> mtu 1500 qdisc noqueue \\    link/ether 00:1e:68:c5:e7:6e brd ff:ff:ff:ff:ff:ff");
        addrList.add("264: pibr102    inet 172.30.0.66/28 brd 10.19.128.47 scope global pibr102");
        addrList.add("266: pibr359: <BROADCAST,MULTICAST,UP,LOWER_UP> mtu 1500 qdisc noqueue \\    link/ether 00:1e:68:c5:e7:6e brd ff:ff:ff:ff:ff:ff");
        addrList.add("266: pibr359    inet 10.19.143.177/28 brd 10.19.143.191 scope global pibr359");
        addrList.add("268: pibr101: <BROADCAST,MULTICAST,UP,LOWER_UP> mtu 1500 qdisc noqueue \\    link/ether 00:1e:68:c5:e7:6e brd ff:ff:ff:ff:ff:ff");
        addrList.add("268: pibr101    inet 172.30.0.65/28 brd 10.19.128.31 scope global pibr101");

        return new CommandResult(0, addrList, new ArrayList<String>());
    }

    private void waitUntilInCache(PId id, long timeout) {
        long finishBy = new Date().getTime() + timeout;
        while (new Date().before(new Date(finishBy))) {
            if (null != currentConsumedDhtResourceRegistry.getCachedEntity(id))
                break;
            try {
                Thread.sleep(1000);
            } catch (InterruptedException ee) {
                throw new RuntimeException(ee);
            }
        }
    }

    private void waitForQueueSize(PiQueue piQueue, int size, int seconds) throws InterruptedException {
        System.err.println("waiting up to " + seconds + " seconds for queue " + piQueue + " to have " + size + " entries");
        for (int i = 0; i < seconds; i++) {
            BlockingDhtReader dhtBlockingReader = currentDhtClientFactory.createBlockingReader();
            TaskProcessingQueue taskProcessingQueue = (TaskProcessingQueue) dhtBlockingReader.get(currentPiIdBuilder.getPiQueuePId(piQueue).forLocalScope(piQueue.getNodeScope()));
            if (size == taskProcessingQueue.size())
                return;
            Thread.sleep(1000);
        }
        fail("queue " + piQueue + " not at " + size + " after " + seconds + " seconds");
    }
}
