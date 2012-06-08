package com.bt.pi.app.networkmanager;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.isA;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import org.junit.Before;
import org.junit.Test;

import com.bt.pi.app.common.entities.PublicIpAllocationIndex;
import com.bt.pi.app.common.entities.SubnetAllocationIndex;
import com.bt.pi.app.common.entities.VlanAllocationIndex;
import com.bt.pi.app.common.entities.watchers.InactiveConsumerPurgingWatcher;
import com.bt.pi.app.common.id.PiIdBuilder;
import com.bt.pi.core.application.watcher.service.WatcherService;
import com.bt.pi.core.dht.DhtClientFactory;
import com.bt.pi.core.id.KoalaIdFactory;
import com.bt.pi.core.parser.KoalaPiEntityFactory;

public class NetworkResourceWatcherInitiatorTest {
    private NetworkResourceWatcherInitiator networkResourceWatcherInitiator;
    private KoalaIdFactory koalaIdFactory;
    private PiIdBuilder piIdBuilder;
    private DhtClientFactory dhtClientFactory;
    private WatcherService watcherService;

    @Before
    public void before() {
        koalaIdFactory = new KoalaIdFactory(99, 99);
        koalaIdFactory.setKoalaPiEntityFactory(new KoalaPiEntityFactory());
        piIdBuilder = new PiIdBuilder();
        piIdBuilder.setKoalaIdFactory(koalaIdFactory);

        dhtClientFactory = mock(DhtClientFactory.class);
        watcherService = mock(WatcherService.class);

        networkResourceWatcherInitiator = new NetworkResourceWatcherInitiator();
        networkResourceWatcherInitiator.setDhtClientFactory(dhtClientFactory);
        networkResourceWatcherInitiator.setWatcherService(watcherService);
        networkResourceWatcherInitiator.setPiIdBuilder(piIdBuilder);
    }

    @Test
    public void shouldNotInitiateWhenZeroAppsGiven() {
        // act
        networkResourceWatcherInitiator.initiateWatchers(0);

        // assert
        verify(watcherService, never()).replaceTask(anyString(), any(Runnable.class), anyLong(), anyLong());
    }

    @Test
    public void shouldInitiateAllWatchers() {
        // act
        networkResourceWatcherInitiator.initiateWatchers(4);

        // assert
        verify(watcherService).replaceTask(eq(VlanAllocationIndex.class.getSimpleName() + "-watcher"), isA(InactiveConsumerPurgingWatcher.class), eq(1800 * 1000L), eq(4 * 3600L * 1000L));
        verify(watcherService).replaceTask(eq(SubnetAllocationIndex.class.getSimpleName() + "-watcher"), isA(InactiveConsumerPurgingWatcher.class), eq(1800 * 1000L), eq(4 * 3600L * 1000L));
        verify(watcherService).replaceTask(eq(PublicIpAllocationIndex.class.getSimpleName() + "-watcher"), isA(InactiveConsumerPurgingWatcher.class), eq(1800 * 1000L), eq(4 * 3600L * 1000L));
    }

    @Test
    public void shouldInitiateWithOverridenStartTime() {
        // setup
        networkResourceWatcherInitiator.setInitialIntervalMillis(1);

        // act
        networkResourceWatcherInitiator.initiateWatchers(4);

        // assert
        verify(watcherService).replaceTask(eq(VlanAllocationIndex.class.getSimpleName() + "-watcher"), isA(InactiveConsumerPurgingWatcher.class), eq(1L), eq(4 * 3600L * 1000L));
        verify(watcherService).replaceTask(eq(SubnetAllocationIndex.class.getSimpleName() + "-watcher"), isA(InactiveConsumerPurgingWatcher.class), eq(1L), eq(4 * 3600L * 1000L));
        verify(watcherService).replaceTask(eq(PublicIpAllocationIndex.class.getSimpleName() + "-watcher"), isA(InactiveConsumerPurgingWatcher.class), eq(1L), eq(4 * 3600L * 1000L));
    }
}
