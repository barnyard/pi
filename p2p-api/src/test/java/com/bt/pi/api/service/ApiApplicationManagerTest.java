package com.bt.pi.api.service;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.concurrent.TimeUnit;

import org.junit.Before;
import org.junit.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import rice.Continuation;
import rice.p2p.commonapi.NodeHandle;

import com.bt.pi.app.common.entities.AvailabilityZone;
import com.bt.pi.app.common.entities.AvailabilityZoneNotFoundException;
import com.bt.pi.app.common.entities.AvailabilityZones;
import com.bt.pi.app.common.entities.Region;
import com.bt.pi.app.common.entities.Regions;
import com.bt.pi.app.common.id.PiIdBuilder;
import com.bt.pi.app.common.net.NetworkCommandRunner;
import com.bt.pi.app.common.resource.PiTopics;
import com.bt.pi.core.application.MessageContext;
import com.bt.pi.core.application.PubSubMessageContext;
import com.bt.pi.core.application.resource.ConsumedDhtResourceRegistry;
import com.bt.pi.core.dht.cache.BlockingDhtCache;
import com.bt.pi.core.id.KoalaIdFactory;
import com.bt.pi.core.id.PId;
import com.bt.pi.core.parser.KoalaPiEntityFactory;
import com.bt.pi.core.util.MDCHelper;

public class ApiApplicationManagerTest {
    private NodeHandle nodeHandle;

    private KoalaIdFactory koalaIdFactory;
    private PiIdBuilder piIdBuilder;
    private NetworkCommandRunner networkCommandRunner;
    private ConsumedDhtResourceRegistry consumedDhtResourceRegistry;
    private BlockingDhtCache blockingDhtCache;
    private ApiApplicationManager apiApplicationManager;
    private boolean isSuperBecomeActiveSuccess;

    @Before
    public void before() {
        isSuperBecomeActiveSuccess = true;
        nodeHandle = mock(NodeHandle.class);
        koalaIdFactory = new KoalaIdFactory(0, 0);
        koalaIdFactory.setKoalaPiEntityFactory(new KoalaPiEntityFactory());

        piIdBuilder = new PiIdBuilder();
        piIdBuilder.setKoalaIdFactory(koalaIdFactory);

        networkCommandRunner = mock(NetworkCommandRunner.class);
        blockingDhtCache = mock(BlockingDhtCache.class);
        consumedDhtResourceRegistry = mock(ConsumedDhtResourceRegistry.class);

        apiApplicationManager = new ApiApplicationManager() {
            @Override
            public NodeHandle getNodeHandle() {
                return nodeHandle;
            }

            @Override
            protected boolean callSuperBecomeActive() {
                return isSuperBecomeActiveSuccess;
            }
        };

        apiApplicationManager.setPiIdBuilder(piIdBuilder);
        apiApplicationManager.setKoalaIdFactory(koalaIdFactory);
        apiApplicationManager.setActivationCheckPeriodSecs(321);
        apiApplicationManager.setStartTimeoutMillis(123);
        apiApplicationManager.setNetworkCommandRunner(networkCommandRunner);
        apiApplicationManager.setBlockingDhtCache(blockingDhtCache);
        apiApplicationManager.setConsumedDhtResourceRegistry(consumedDhtResourceRegistry);
    }

    @Test
    public void shouldGetActivationTimeFromConfig() {
        // act
        int res = apiApplicationManager.getActivationCheckPeriodSecs();

        // assert
        assertEquals(321, res);
    }

    @Test
    public void shouldGetStartTimeFromConfigIfDefined() {
        // act
        long res = apiApplicationManager.getStartTimeout();

        // assert
        assertEquals(123, res);
    }

    @Test
    public void shouldGetStartTimeUnitAsMillis() {
        // act
        TimeUnit res = apiApplicationManager.getStartTimeoutUnit();

        // assert
        assertEquals(TimeUnit.MILLISECONDS, res);
    }

    @Test
    public void shouldGetAppName() {
        assertEquals("pi-api-manager", apiApplicationManager.getApplicationName());
    }

    @Test
    public void shouldGetMutuallyExclusiveApps() {
        // act
        assertEquals(2, apiApplicationManager.getPreferablyExcludedApplications().size());
    }

    @Test
    public void shouldInjectTransactionUIDIntoMessageContextIfInMDC() {
        // setup
        MDCHelper.putTransactionUID("123");

        // act
        MessageContext res = apiApplicationManager.newMessageContext();

        // assert
        assertEquals("123", res.getTransactionUID());
    }

    @Test
    public void shouldInjectTransactionUIDIntoPubSubMessageContextIfInMDC() {
        // setup
        MDCHelper.putTransactionUID("234");

        // act
        PubSubMessageContext res = apiApplicationManager.newLocalPubSubMessageContext(PiTopics.RUN_INSTANCE);

        // assert
        assertEquals("234", res.getTransactionUID());
    }

    @SuppressWarnings("unchecked")
    @Test
    public void shouldBailOutOfBecomeActiveIfSuperFails() throws Exception {
        // setup
        isSuperBecomeActiveSuccess = false;

        // act
        apiApplicationManager.becomeActive();

        // assert
        verify(consumedDhtResourceRegistry, never()).registerConsumer(any(PId.class), anyString(), any(Class.class), any(Continuation.class));
    }

    @Test(expected = AvailabilityZoneNotFoundException.class)
    public void shouldThrowWhenGettingAvzByNameIfAvailabilityZoneDoesNotExist() throws Exception {
        // setup
        AvailabilityZones availabilityZones = new AvailabilityZones();
        availabilityZones.addAvailabilityZone(new AvailabilityZone("a", 11, 22, "s"));

        when(consumedDhtResourceRegistry.getCachedEntity(koalaIdFactory.buildPId(new AvailabilityZones().getUrl()))).thenReturn(availabilityZones);

        apiApplicationManager.becomeActive();

        // act
        apiApplicationManager.getAvailabilityZoneByName("b");
    }

    @Test(expected = AvailabilityZoneNotFoundException.class)
    public void shouldThrowIfAvailabilityZonesRecordDoesNotExist() throws Exception {
        // setup
        apiApplicationManager.becomeActive();

        // act
        apiApplicationManager.getAvailabilityZoneByName("b");
    }

    @Test
    public void shouldRemoveNodeFromApplicationRecordAndForceCheck() {
        // setup
        String anotherNodeId = "anotherNode";

        apiApplicationManager = spy(new ApiApplicationManager());

        doAnswer(new Answer<NodeHandle>() {
            @Override
            public NodeHandle answer(InvocationOnMock invocation) throws Throwable {
                return nodeHandle;
            }
        }).when(apiApplicationManager).getNodeHandle();

        doNothing().when(apiApplicationManager).removeNodeIdFromApplicationRecord(anotherNodeId);
        doNothing().when(apiApplicationManager).forceActivationCheck();

        // act
        apiApplicationManager.handleNodeDeparture(anotherNodeId);

        // assert
        verify(apiApplicationManager).removeNodeIdFromApplicationRecord(anotherNodeId);
    }

    @Test
    public void shouldGetAvailabilityZoneByNameFromBlockingCache() throws Exception {
        // setup
        AvailabilityZone availabilityZone = new AvailabilityZone("a", 1, 2, "s");

        AvailabilityZones availabilityZones = new AvailabilityZones();
        availabilityZones.addAvailabilityZone(availabilityZone);

        when(blockingDhtCache.get(piIdBuilder.getAvailabilityZonesId())).thenReturn(availabilityZones);

        apiApplicationManager.becomeActive();

        // act
        AvailabilityZone result = apiApplicationManager.getAvailabilityZoneByName("a");

        // assert
        assertEquals(availabilityZone, result);
    }

    @Test
    public void shouldGetAvailabilityZoneByCodeFromBlockingCache() throws Exception {
        // setup
        AvailabilityZone availabilityZone = new AvailabilityZone("a", 1, 2, "s");

        AvailabilityZones availabilityZones = new AvailabilityZones();
        availabilityZones.addAvailabilityZone(availabilityZone);

        when(blockingDhtCache.get(piIdBuilder.getAvailabilityZonesId())).thenReturn(availabilityZones);

        apiApplicationManager.becomeActive();

        // act
        AvailabilityZone result = apiApplicationManager.getAvailabilityZoneByGlobalAvailabilityZoneCode(0x0201);

        // assert
        assertEquals(availabilityZone, result);
    }

    @Test
    public void shouldGetRegionFromBlockingCache() throws Exception {
        // setup
        Region region = new Region("a", 1, "b", "");

        Regions regions = new Regions();
        regions.addRegion(region);

        when(blockingDhtCache.get(piIdBuilder.getRegionsId())).thenReturn(regions);

        apiApplicationManager.becomeActive();

        // act
        Region result = apiApplicationManager.getRegion("a");

        // assert
        assertEquals(region, result);
    }

    @Test
    public void shouldGetAvailabilityZonesFromBlockingCache() throws Exception {
        // setup
        AvailabilityZones availabilityZones = new AvailabilityZones();
        when(blockingDhtCache.get(piIdBuilder.getAvailabilityZonesId())).thenReturn(availabilityZones);

        apiApplicationManager.becomeActive();

        // act
        AvailabilityZones result = apiApplicationManager.getAvailabilityZonesRecord();

        // assert
        assertEquals(availabilityZones, result);
    }

    @Test
    public void shouldGetRegionsFromBlockingCache() throws Exception {
        // setup
        Regions regions = new Regions();
        when(blockingDhtCache.get(piIdBuilder.getRegionsId())).thenReturn(regions);

        apiApplicationManager.becomeActive();

        // act
        Regions result = apiApplicationManager.getRegions();

        // assert
        assertEquals(regions, result);
    }
}
