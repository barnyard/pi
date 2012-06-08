package com.bt.pi.app.networkmanager;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.isA;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Collection;
import java.util.concurrent.TimeUnit;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.runners.MockitoJUnitRunner;
import org.mockito.stubbing.Answer;
import org.springframework.context.ApplicationContext;

import rice.p2p.commonapi.Id;
import rice.p2p.commonapi.NodeHandle;
import rice.p2p.scribe.ScribeMultiClient;

import com.bt.pi.app.common.ApplicationPublicInterfaceWatcher;
import com.bt.pi.app.common.net.NetworkCommandRunner;
import com.bt.pi.app.common.resource.PiTopics;
import com.bt.pi.app.networkmanager.addressing.resolution.AddressDeleteQueue;
import com.bt.pi.app.networkmanager.handlers.NetworkCleanupHandler;
import com.bt.pi.app.networkmanager.iptables.IpTablesManager;
import com.bt.pi.core.application.MessageForwardAction;
import com.bt.pi.core.application.PubSubMessageContext;
import com.bt.pi.core.application.ReceivedMessageContext;
import com.bt.pi.core.application.activation.ApplicationRecord;
import com.bt.pi.core.application.activation.ApplicationRegistry;
import com.bt.pi.core.application.activation.ApplicationStatus;
import com.bt.pi.core.application.activation.AvailabilityZoneScopedSharedRecordConditionalApplicationActivator;
import com.bt.pi.core.application.resource.ConsumedDhtResourceRegistry;
import com.bt.pi.core.entity.EntityMethod;
import com.bt.pi.core.entity.PiEntity;
import com.bt.pi.core.entity.PiLocation;
import com.bt.pi.core.id.KoalaIdFactory;
import com.bt.pi.core.id.KoalaIdUtils;
import com.bt.pi.core.parser.KoalaPiEntityFactory;
import com.bt.pi.core.scope.NodeScope;

@RunWith(MockitoJUnitRunner.class)
public class NetworkManagerApplicationTest {
    @InjectMocks
    private NetworkManagerApplication networkManagerApp = spy(new NetworkManagerApplication() {
        @Override
        public Id getNodeId() {
            return id;
        }

        @Override
        public String getNodeIdFull() {
            return id.toStringFull();
        }

        @Override
        protected boolean callSuperBecomeActive() {
            return true;
        }

        @Override
        public java.util.Collection<rice.pastry.NodeHandle> getLeafNodeHandles() {
            return leafNodeHandles;
        }
    });
    private PiEntity entity;
    private Id id;
    @Mock
    private NodeHandle nodeHandle;
    private Id originalDestinationNodeId;
    private Id newDestinationNodeId;
    private KoalaIdFactory koalaIdFactory;
    @Mock
    private ApplicationRecord applicationRecord;
    @Mock
    private NetworkCleanupHandler networkCleanupHandler;
    @Mock
    private AssociateAddressTaskQueueWatcherInitiator associateAddressTaskQueueWatcherInitiator;
    @Mock
    private DisassociateAddressTaskQueueWatcherInitiator disassociateAddressTaskQueueWatcherInitiator;
    @Mock
    private InstanceNetworkManagerTeardownTaskQueueWatcherInitiator instanceNetworkManagerTeardownTaskQueueWatcherInitiator;
    @Mock
    private AvailabilityZoneScopedSharedRecordConditionalApplicationActivator applicationActivator;
    @Mock
    private NetworkResourceWatcherInitiator networkResourceWatcherInitiator;
    @Mock
    private ApplicationRegistry applicationRegistry;
    @Mock
    private ConsumedDhtResourceRegistry consumedDhtResourceRegistry;
    @Mock
    private NetworkCommandRunner networkCommandRunner;
    @Mock
    private NetworkManagerAppDeliveredMessageDispatcher networkManagerAppDeliveredMessageDispatcher;
    @Mock
    private rice.pastry.NodeHandle leafNodeHandle1;
    @Mock
    private rice.pastry.NodeHandle leafNodeHandle2;
    protected Collection<rice.pastry.NodeHandle> leafNodeHandles = Arrays.asList(new rice.pastry.NodeHandle[] { leafNodeHandle1, leafNodeHandle2 });
    @Mock
    private KoalaIdUtils koalaIdUtils;
    @Mock
    private ApplicationContext applicationContext;
    @Mock
    private SecurityGroupUpdateTaskQueueWatcherInitiator securityGroupUpdateTaskQueueWatcherInitiator;
    @Mock
    private SecurityGroupDeleteTaskQueueWatcherInitiator securityGroupDeleteTaskQueueWatcherInitiator;
    @Mock
    private ApplicationPublicInterfaceWatcher applicationPublicInterfaceWatcher;
    @Mock
    private IpTablesManager ipTablesManager;
    @Mock
    private AddressDeleteQueue addressDeleteQueue;

    @Before
    public void before() {
        this.koalaIdFactory = new KoalaIdFactory(99, 99);
        koalaIdFactory.setKoalaPiEntityFactory(new KoalaPiEntityFactory());
        id = koalaIdFactory.buildId("nodeId");
        originalDestinationNodeId = koalaIdFactory.buildId("originalid");
        newDestinationNodeId = koalaIdFactory.buildId("newid");

        when(this.applicationContext.getBean(AvailabilityZoneScopedSharedRecordConditionalApplicationActivator.class)).thenReturn(applicationActivator);
        when(this.applicationRecord.getRequiredActive()).thenReturn(3);
        when(this.applicationRegistry.getCachedApplicationRecord(NetworkManagerApplication.APPLICATION_NAME)).thenReturn(this.applicationRecord);
        when(this.applicationActivator.getApplicationStatus(NetworkManagerApplication.APPLICATION_NAME)).thenReturn(ApplicationStatus.ACTIVE);
        when(this.applicationActivator.getClosestActiveApplicationNodeId(NetworkManagerApplication.APPLICATION_NAME, this.originalDestinationNodeId)).thenReturn(this.originalDestinationNodeId);
        when(this.applicationActivator.getApplicationRegistry()).thenReturn(this.applicationRegistry);

        this.networkManagerApp.setKoalaIdFactory(this.koalaIdFactory);
        this.networkManagerApp.setNetworkManagerActivationCheckPeriodSecs(321);
        this.networkManagerApp.setNetworkManagerStartTimeoutMillis(123);

        when(koalaIdUtils.isIdClosestToMe(eq(id.toStringFull()), eq(leafNodeHandles), isA(Id.class), eq(NodeScope.REGION))).thenReturn(false).thenReturn(true);

        when(nodeHandle.getId()).thenReturn(id);

        doAnswer(new Answer<Object>() {
            public Object answer(InvocationOnMock invocation) throws Throwable {
                return null;
            }
        }).when(networkManagerApp).subscribe(isA(PiLocation.class), isA(ScribeMultiClient.class));

        doAnswer(new Answer<Object>() {
            public Object answer(InvocationOnMock invocation) throws Throwable {
                return null;
            }
        }).when(networkManagerApp).unsubscribe(isA(PiLocation.class), isA(ScribeMultiClient.class));
    }

    @Test
    public void shouldUseAvzApplicationActivator() {
        assertTrue(networkManagerApp.getApplicationActivator() instanceof AvailabilityZoneScopedSharedRecordConditionalApplicationActivator);
    }

    @Test
    public void shouldGetActivationTimeFromConfig() {
        // act
        int res = networkManagerApp.getActivationCheckPeriodSecs();

        // assert
        assertEquals(321, res);
    }

    @Test
    public void shouldGetStartTimeFromConfigIfDefined() {
        // act
        long res = networkManagerApp.getStartTimeout();

        // assert
        assertEquals(123, res);
    }

    @Test
    public void shouldGetStartTimeUnitAsMillis() {
        // act
        TimeUnit res = networkManagerApp.getStartTimeoutUnit();

        // assert
        assertEquals(TimeUnit.MILLISECONDS, res);
    }

    @Test
    public void shouldSubscribeToTopicAndInitiateWatchersWithNumRequiredAppsOnBecomingActive() {
        // act
        networkManagerApp.becomeActive();

        // assert
        verify(networkResourceWatcherInitiator).initiateWatchers(3);
        verify(networkManagerApp).subscribe(PiTopics.NETWORK_MANAGERS_IN_REGION.getPiLocation(), networkManagerApp);
    }

    @Test
    public void shouldInitiateWatchersWithNumRequiredAppsOfOneOnBecomingActiveWhenNoCachedRecord() {
        // setup
        when(applicationRegistry.getCachedApplicationRecord(NetworkManagerApplication.APPLICATION_NAME)).thenReturn(null);

        // act
        networkManagerApp.becomeActive();

        // assert
        verify(networkResourceWatcherInitiator).initiateWatchers(1);
    }

    @Test
    public void shouldDelegateDirectMessageToDispatcher() {
        // setup
        ReceivedMessageContext messageContext = mock(ReceivedMessageContext.class);
        when(messageContext.getMethod()).thenReturn(EntityMethod.CREATE);
        when(messageContext.getReceivedEntity()).thenReturn(entity);

        // act
        networkManagerApp.deliver(koalaIdFactory.convertToPId(id), messageContext);

        // assert
        verify(networkManagerAppDeliveredMessageDispatcher).dispatchToHandler(messageContext, EntityMethod.CREATE, entity);
    }

    @Test
    public void shouldDelegatePubSubMessageToDispatcher() {
        // setup
        PubSubMessageContext messageContext = mock(PubSubMessageContext.class);

        // act
        networkManagerApp.deliver(messageContext, EntityMethod.CREATE, entity);

        // assert
        verify(networkManagerAppDeliveredMessageDispatcher).dispatchToHandler(messageContext, EntityMethod.CREATE, entity);
    }

    @Test
    public void shouldUpdate() {
        // act
        networkManagerApp.update(nodeHandle, true);
    }

    @Test
    public void shouldForwardWithoutAlterationIfDestinationCorrect() {
        // act
        MessageForwardAction res = networkManagerApp.forwardPiMessage(true, originalDestinationNodeId, entity);

        // assert
        assertTrue(res.shouldForwardMessage());
        assertNull(res.getNewDestinationNodeId());
    }

    @Test
    public void shouldForwardWithoutAlterationIfNotForThisNode() {
        // act
        MessageForwardAction res = networkManagerApp.forwardPiMessage(false, originalDestinationNodeId, entity);

        // assert
        assertTrue(res.shouldForwardMessage());
        assertNull(res.getNewDestinationNodeId());
    }

    @Test
    public void shouldForwardWithoutAlterationIfNoActiveNodes() {
        // setup
        when(applicationActivator.getClosestActiveApplicationNodeId(NetworkManagerApplication.APPLICATION_NAME, originalDestinationNodeId)).thenReturn(null);

        // act
        MessageForwardAction res = networkManagerApp.forwardPiMessage(true, originalDestinationNodeId, entity);

        // assert
        assertTrue(res.shouldForwardMessage());
        assertNull(res.getNewDestinationNodeId());
    }

    @Test
    public void shouldAlterDestinationIfDestinationNotCurrent() {
        // setup
        when(applicationActivator.getClosestActiveApplicationNodeId(NetworkManagerApplication.APPLICATION_NAME, originalDestinationNodeId)).thenReturn(newDestinationNodeId);

        // act
        MessageForwardAction res = networkManagerApp.forwardPiMessage(true, originalDestinationNodeId, entity);

        // assert
        assertTrue(res.shouldForwardMessage());
        assertEquals(newDestinationNodeId, res.getNewDestinationNodeId());
    }

    @Test
    public void shouldInitialiseNetworkCleanupHandlerAndTaskQueueWatcherOnApplicationStartup() {
        // act
        networkManagerApp.onApplicationStarting();

        // assert
        verify(associateAddressTaskQueueWatcherInitiator).createTaskProcessingQueueWatcher(id.toStringFull());
        verify(disassociateAddressTaskQueueWatcherInitiator).createTaskProcessingQueueWatcher(id.toStringFull());
        verify(instanceNetworkManagerTeardownTaskQueueWatcherInitiator).createTaskProcessingQueueWatcher(id.toStringFull());
    }

    @Test
    public void whenGoingPassiveItShouldUnsubscribeFromTopicAndCleanupTheNetwork() {
        // act
        networkManagerApp.becomePassive();

        // assert
        verify(networkCleanupHandler).releaseAllSecurityGroups();
        verify(networkManagerApp).unsubscribe(PiTopics.NETWORK_MANAGERS_IN_REGION.getPiLocation(), networkManagerApp);
    }

    @Test
    public void whenAppShuttingDownShouldCleanupTheNetwork() {
        // act
        networkManagerApp.onApplicationShuttingDown();

        // assert
        verify(networkCleanupHandler).releaseAllSecurityGroups();
        verify(addressDeleteQueue).removeAllAddressesInQueueOnShuttingDown();
    }

    @Test
    public void shouldRemoveNodeFromApplicationRecordAndForceCheck() {
        // setup
        String nodeId = "nodeId";

        // act
        this.networkManagerApp.handleNodeDeparture(nodeId);

        // verify
        verify(applicationActivator).deActivateNode(eq(nodeId), eq(networkManagerApp));
    }

    @Test
    public void handleNodeDepartureShouldCheckQueueWatchingStatus() {
        // setup
        when(koalaIdUtils.isIdClosestToMe(eq(id.toStringFull()), eq(leafNodeHandles), isA(Id.class), eq(NodeScope.REGION))).thenReturn(false);

        // act
        networkManagerApp.handleNodeDeparture(id.toStringFull());

        // assert
        verify(this.associateAddressTaskQueueWatcherInitiator).removeTaskProcessingQueueWatcher();
        verify(this.disassociateAddressTaskQueueWatcherInitiator).removeTaskProcessingQueueWatcher();
        verify(this.instanceNetworkManagerTeardownTaskQueueWatcherInitiator).removeTaskProcessingQueueWatcher();
    }

    @Test
    public void handleNodeArrivalShouldCheckQueueWatchingStatus() {
        // setup
        when(koalaIdUtils.isIdClosestToMe(eq(id.toStringFull()), eq(leafNodeHandles), isA(Id.class), eq(NodeScope.REGION))).thenReturn(false);

        // act
        networkManagerApp.handleNodeArrival(id.toStringFull());

        // assert
        verify(this.associateAddressTaskQueueWatcherInitiator).removeTaskProcessingQueueWatcher();
        verify(this.disassociateAddressTaskQueueWatcherInitiator).removeTaskProcessingQueueWatcher();
        verify(this.instanceNetworkManagerTeardownTaskQueueWatcherInitiator).removeTaskProcessingQueueWatcher();
    }

    @Test
    public void refreshIpTablesAtRegularIntervalsShouldNotRunWhenPassive() {
        // setup

        // act
        this.networkManagerApp.refreshIpTablesAtRegularIntervals();

        // assert
        verify(ipTablesManager, never()).refreshIpTables();
    }

    @Test
    public void refreshIpTablesAtRegularIntervalsShouldOnlyRunWhenActive() {
        // setup
        networkManagerApp.becomeActive();

        // act
        this.networkManagerApp.refreshIpTablesAtRegularIntervals();

        // assert
        verify(ipTablesManager).refreshIpTables();
    }
}
