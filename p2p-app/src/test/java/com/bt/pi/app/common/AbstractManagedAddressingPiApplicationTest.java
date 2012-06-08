package com.bt.pi.app.common;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.anyCollection;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.isA;
import static org.mockito.Matchers.isNull;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TimerTask;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.junit.Before;
import org.junit.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import rice.p2p.commonapi.Id;
import rice.p2p.commonapi.NodeHandle;

import com.bt.pi.app.common.entities.AvailabilityZone;
import com.bt.pi.app.common.entities.AvailabilityZones;
import com.bt.pi.app.common.id.PiIdBuilder;
import com.bt.pi.app.common.net.NetworkCommandRunner;
import com.bt.pi.core.application.activation.ApplicationActivator;
import com.bt.pi.core.application.activation.ApplicationRecord;
import com.bt.pi.core.application.activation.ApplicationRegistry;
import com.bt.pi.core.application.activation.AvailabilityZoneScopedSharedRecordConditionalApplicationActivator;
import com.bt.pi.core.application.activation.SharedRecordConditionalApplicationActivator;
import com.bt.pi.core.application.activation.TimeStampedPair;
import com.bt.pi.core.application.resource.ConsumedDhtResourceRegistry;
import com.bt.pi.core.continuation.GenericContinuation;
import com.bt.pi.core.continuation.PiContinuation;
import com.bt.pi.core.dht.cache.DhtCache;
import com.bt.pi.core.id.PId;
import com.bt.pi.core.scope.NodeScope;

public class AbstractManagedAddressingPiApplicationTest {
    private static final String IP_ADDRESS = "1.2.3.4";
    private static final String PUBLIC_INTERFACE = "eth0";
    private String name = "testApplication";

    private Id nodeId;
    private PId genericId;
    private NodeHandle nodeHandle;
    private ApplicationRecord applicationRecord;
    private Map<String, TimeStampedPair<String>> nodeMap;

    private ApplicationRegistry applicationRegistry;
    private AvailabilityZoneScopedSharedRecordConditionalApplicationActivator applicationActivator;
    private NetworkCommandRunner networkCommandRunner;
    private AbstractManagedAddressingPiApplication abstractManagedAddressingPiApplication;
    private PiIdBuilder piIdBuilder;
    private ConsumedDhtResourceRegistry consumedDhtResourceRegistry;
    private DhtCache dhtCache;
    private ApplicationPublicInterfaceWatcher applicationPublicInterfaceWatcher;

    @Before
    public void setup() throws Exception {
        setupApplicationActivator();

        nodeId = mock(Id.class);
        genericId = mock(PId.class);
        nodeHandle = mock(NodeHandle.class);
        networkCommandRunner = mock(NetworkCommandRunner.class);
        applicationPublicInterfaceWatcher = mock(ApplicationPublicInterfaceWatcher.class);

        piIdBuilder = mock(PiIdBuilder.class);
        when(piIdBuilder.getAvailabilityZonesId()).thenReturn(genericId);

        consumedDhtResourceRegistry = mock(ConsumedDhtResourceRegistry.class);
        dhtCache = mock(DhtCache.class);

        abstractManagedAddressingPiApplication = new AbstractManagedAddressingPiApplication(name) {
            @Override
            public NodeHandle getNodeHandle() {
                return nodeHandle;
            }

            @Override
            public Id getNodeId() {
                return nodeId;
            }

            @Override
            public SharedRecordConditionalApplicationActivator getActivatorFromApplication() {
                return applicationActivator;
            }

            @Override
            public void handleNodeDeparture(String nodeId) {
            }
        };
        abstractManagedAddressingPiApplication.setVnetPublicInterface(PUBLIC_INTERFACE);
        abstractManagedAddressingPiApplication.setNetworkCommandRunner(networkCommandRunner);
        abstractManagedAddressingPiApplication.setPiIdBuilder(piIdBuilder);
        abstractManagedAddressingPiApplication.setConsumedDhtResourceRegistry(consumedDhtResourceRegistry);
        abstractManagedAddressingPiApplication.setDhtCache(dhtCache);
        abstractManagedAddressingPiApplication.setApplicationPublicInterfaceWatcher(applicationPublicInterfaceWatcher);
    }

    private void setupApplicationActivator() {
        nodeMap = new HashMap<String, TimeStampedPair<String>>();
        nodeMap.put("ipAddress1", null);
        nodeMap.put("ipAddress2", null);
        nodeMap.put(IP_ADDRESS, null);

        applicationRecord = mock(ApplicationRecord.class);
        when(applicationRecord.getActiveNodeMap()).thenReturn(nodeMap);

        applicationRegistry = mock(ApplicationRegistry.class);
        when(applicationRegistry.getCachedApplicationRecord(name)).thenReturn(applicationRecord);

        applicationActivator = mock(AvailabilityZoneScopedSharedRecordConditionalApplicationActivator.class);
        when(applicationActivator.getApplicationRegistry()).thenReturn(applicationRegistry);
        when(applicationActivator.getActivationScope()).thenReturn(NodeScope.AVAILABILITY_ZONE);
    }

    @Test
    public void testGetApplicationName() {
        // act
        String result = abstractManagedAddressingPiApplication.getApplicationName();

        // assert
        assertEquals(name, result);
    }

    @Test
    public void testGetActivationCheckPeriodSecs() {
        // setup
        int value = 123;
        abstractManagedAddressingPiApplication.setActivationCheckPeriodSecs(value);

        // act
        int result = abstractManagedAddressingPiApplication.getActivationCheckPeriodSecs();

        // assert
        assertEquals(value, result);
    }

    @Test
    public void testGetApplicationActivator() {
        // act
        ApplicationActivator result = abstractManagedAddressingPiApplication.getApplicationActivator();

        // assert
        assertEquals(applicationActivator, result);
    }

    @Test
    public void testGetStartTimeout() {
        // setup
        long value = 44556;
        abstractManagedAddressingPiApplication.setStartTimeoutMillis(value);

        // act
        long result = this.abstractManagedAddressingPiApplication.getStartTimeout();

        // assert
        assertEquals(value, result);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testDeletePublicIpAddressesFromInterfaceExceptNoApplicationRecord() {
        // setup
        when(applicationRegistry.getCachedApplicationRecord(name)).thenReturn(null);

        // act
        this.abstractManagedAddressingPiApplication.deletePublicIpAddressesFromInterfaceExcept(null);

        // assert
        verify(networkCommandRunner, never()).ipAddressesDelete(anyCollection(), anyString());
    }

    @Test
    public void shouldInvokeApplicationActivatorToActivate() {

        // act
        abstractManagedAddressingPiApplication.forceActivationCheck();

        // assert
        verify(applicationActivator).checkAndActivate(eq(abstractManagedAddressingPiApplication), (TimerTask) isNull());
    }

    @Test
    public void shouldInvokeApplicationActivatorToDeactivateNode() {

        // act
        String nodeId = "someNode";
        abstractManagedAddressingPiApplication.removeNodeIdFromApplicationRecord(nodeId);

        // verify
        verify(applicationActivator).deActivateNode(eq(nodeId), eq(abstractManagedAddressingPiApplication));
    }

    @Test
    public void shouldReturnTrueOnAddPublicIpAddressIfIpAddressInApplicationRecordExists() throws Exception {
        // setup
        when(applicationRecord.getAssociatedResource(nodeId)).thenReturn(IP_ADDRESS);

        // act
        boolean result = abstractManagedAddressingPiApplication.becomeActive();

        // assert
        assertTrue(result);
    }

    @Test
    public void shouldReturnFalseIfIpAddressInApplicationRecordIsNull() throws Exception {
        // act
        boolean result = abstractManagedAddressingPiApplication.becomeActive();

        // assert
        assertFalse(result);
    }

    @Test
    public void shouldAddIpAddressAndGatewayRouteOnActivation() throws Exception {
        // setup
        when(applicationRecord.getAssociatedResource(nodeId)).thenReturn(IP_ADDRESS);

        // act
        abstractManagedAddressingPiApplication.becomeActive();

        // assert
        verify(networkCommandRunner).ifUp(PUBLIC_INTERFACE);
        verify(networkCommandRunner).addIpAddressAndSendArping(IP_ADDRESS, PUBLIC_INTERFACE);
        verify(networkCommandRunner).addDefaultGatewayRouteToDevice(PUBLIC_INTERFACE);
    }

    @Test
    public void shouldDeleteOtherIpAddressesOnActivation() throws Exception {
        // setup
        when(applicationRecord.getAssociatedResource(nodeId)).thenReturn(IP_ADDRESS);

        // act
        abstractManagedAddressingPiApplication.becomeActive();

        // assert
        Set<String> expectedAddrs = new HashSet<String>(Arrays.asList(new String[] { "ipAddress1", "ipAddress2" }));
        verify(networkCommandRunner).ipAddressesDelete(expectedAddrs, PUBLIC_INTERFACE);
    }

    @Test
    public void shouldRemoveAllPublicIpAddressesOnPassivation() throws Exception {
        // act
        abstractManagedAddressingPiApplication.becomePassive();

        // assert
        Set<String> expectedAddrs = new HashSet<String>(Arrays.asList(new String[] { IP_ADDRESS, "ipAddress1", "ipAddress2" }));
        verify(networkCommandRunner).ipAddressesDelete(expectedAddrs, PUBLIC_INTERFACE);
    }

    @Test
    public void shouldRemoveAllPublicIpAddressesOnAppShuttingDown() throws Exception {
        // act
        abstractManagedAddressingPiApplication.onApplicationShuttingDown();

        // assert
        Set<String> expectedAddrs = new HashSet<String>(Arrays.asList(new String[] { IP_ADDRESS, "ipAddress1", "ipAddress2" }));
        verify(networkCommandRunner).ipAddressesDelete(expectedAddrs, PUBLIC_INTERFACE);
    }

    @Test
    public void shouldStopApplicationPublicIpAddressWatcherOnShuttingDown() {
        // act
        abstractManagedAddressingPiApplication.onApplicationShuttingDown();

        // assert
        verify(applicationPublicInterfaceWatcher).stopWatchingApplicationPublicAddressOnShuttingDown();
    }

    @Test
    public void shouldRemoveNodeId() throws Exception {
        // setup
        String id = "id";

        // act
        abstractManagedAddressingPiApplication.removeNodeIdFromApplicationRecord(id);

        // assert
        verify(applicationActivator).deActivateNode(id, abstractManagedAddressingPiApplication);
    }

    @Test
    public void shouldCheckAndActivate() throws Exception {
        // act
        abstractManagedAddressingPiApplication.forceActivationCheck();

        // assert
        verify(applicationActivator).checkAndActivate(eq(abstractManagedAddressingPiApplication), (TimerTask) isNull());
    }

    @SuppressWarnings("unchecked")
    @Test
    public void shouldGetAvailabilityZoneFromNonBlockingCache() throws Exception {
        // setup
        final AvailabilityZone availabilityZone = new AvailabilityZone("a", 1, 0, "s");

        final AvailabilityZones availabilityZones = new AvailabilityZones();
        availabilityZones.addAvailabilityZone(availabilityZone);

        doAnswer(new Answer<Object>() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                ((PiContinuation<AvailabilityZones>) invocation.getArguments()[1]).handleResult(availabilityZones);
                return null;
            }
        }).when(dhtCache).get(eq(genericId), isA(PiContinuation.class));

        final CountDownLatch latch = new CountDownLatch(1);
        GenericContinuation<AvailabilityZone> continuation = new GenericContinuation<AvailabilityZone>() {
            @Override
            public void handleResult(AvailabilityZone result) {
                assertEquals(availabilityZone, result);
                latch.countDown();
            }
        };

        abstractManagedAddressingPiApplication.becomeActive();

        // act
        abstractManagedAddressingPiApplication.getAvailabilityZoneByName("a", continuation);

        // assert
        assertTrue(latch.await(1, TimeUnit.SECONDS));
    }
}
