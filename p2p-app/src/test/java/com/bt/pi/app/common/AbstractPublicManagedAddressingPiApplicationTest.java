package com.bt.pi.app.common;

import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.isA;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;

import rice.p2p.commonapi.Id;
import rice.p2p.commonapi.NodeHandle;

import com.bt.pi.app.common.net.NetworkCommandRunner;
import com.bt.pi.app.common.net.iptables.ManagedAddressingApplicationIpTablesManager;
import com.bt.pi.core.application.activation.ApplicationRecord;
import com.bt.pi.core.application.activation.ApplicationRegistry;
import com.bt.pi.core.application.activation.AvailabilityZoneScopedSharedRecordConditionalApplicationActivator;
import com.bt.pi.core.application.activation.SharedRecordConditionalApplicationActivator;
import com.bt.pi.core.application.activation.TimeStampedPair;
import com.bt.pi.core.application.resource.ConsumedDhtResourceRegistry;
import com.bt.pi.core.id.KoalaIdFactory;
import com.bt.pi.core.scope.NodeScope;

public class AbstractPublicManagedAddressingPiApplicationTest {
    private static final int PORT = 1234;
    private static final String IP_ADDRESS = "1.2.3.4";
    private static final String PUBLIC_INTERFACE = "eth0";
    private String name = "testApplication";

    private Id nodeId;
    private NodeHandle nodeHandle;
    private ApplicationRecord applicationRecord;
    private Map<String, TimeStampedPair<String>> nodeMap;

    private ApplicationRegistry applicationRegistry;
    private AvailabilityZoneScopedSharedRecordConditionalApplicationActivator applicationActivator;
    private ManagedAddressingApplicationIpTablesManager ipTablesManagerBaseForApplication;
    private NetworkCommandRunner networkCommandRunner;
    private AbstractPublicManagedAddressingPiApplication abstractPublicManagedAddressingPiApplication;
    private KoalaIdFactory koalaIdFactory;
    private ConsumedDhtResourceRegistry consumedDhtResourceRegistry;

    @Before
    public void setup() throws Exception {
        setupApplicationActivator();

        nodeId = mock(Id.class);
        nodeHandle = mock(NodeHandle.class);
        ipTablesManagerBaseForApplication = mock(ManagedAddressingApplicationIpTablesManager.class);
        networkCommandRunner = mock(NetworkCommandRunner.class);
        koalaIdFactory = mock(KoalaIdFactory.class);
        Id id = mock(Id.class);
        when(koalaIdFactory.buildId(isA(String.class))).thenReturn(id);
        consumedDhtResourceRegistry = mock(ConsumedDhtResourceRegistry.class);

        abstractPublicManagedAddressingPiApplication = new AbstractPublicManagedAddressingPiApplication(name) {
            @Override
            public NodeHandle getNodeHandle() {
                return nodeHandle;
            }

            @Override
            public Id getNodeId() {
                return nodeId;
            }

            @Override
            public void handleNodeDeparture(String nodeId) {
            }

            @Override
            public SharedRecordConditionalApplicationActivator getActivatorFromApplication() {
                return applicationActivator;
            }

            @Override
            protected int getPort() {
                return PORT;
            }
        };
        abstractPublicManagedAddressingPiApplication.setVnetPublicInterface(PUBLIC_INTERFACE);
        abstractPublicManagedAddressingPiApplication.setManagedAddressingApplicationIpTablesManager(ipTablesManagerBaseForApplication);
        abstractPublicManagedAddressingPiApplication.setNetworkCommandRunner(networkCommandRunner);
        abstractPublicManagedAddressingPiApplication.setKoalaIdFactory(koalaIdFactory);
        abstractPublicManagedAddressingPiApplication.setConsumedDhtResourceRegistry(consumedDhtResourceRegistry);
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
    public void shouldEnableIpTablesOnActivationIfActivationSucceeds() throws Exception {
        // setup
        when(applicationRecord.getAssociatedResource(nodeId)).thenReturn(IP_ADDRESS);

        // act
        abstractPublicManagedAddressingPiApplication.becomeActive();

        // assert
        verify(ipTablesManagerBaseForApplication).enablePiAppChainForApplication(name, IP_ADDRESS, PORT);
    }

    @Test
    public void shouldNotEnableIpTablesOnActivationIfExternalExposureNotRequired() throws Exception {
        // setup
        when(applicationRecord.getAssociatedResource(nodeId)).thenReturn(null);

        // act
        abstractPublicManagedAddressingPiApplication.becomeActive();

        // assert
        verify(ipTablesManagerBaseForApplication, never()).enablePiAppChainForApplication(anyString(), anyString(), anyInt());
    }

    @Test
    public void shouldDisableIpTablesOnPassivationIfExternalExposureRequired() throws Exception {
        // act
        abstractPublicManagedAddressingPiApplication.becomePassive();

        // assert
        verify(ipTablesManagerBaseForApplication).disablePiAppChainForApplication(name);
    }
}
