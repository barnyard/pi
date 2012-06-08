package com.bt.pi.app.networkmanager;

import javax.annotation.Resource;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.stereotype.Component;

import com.bt.pi.app.common.entities.InstanceRecord;
import com.bt.pi.app.common.entities.PublicIpAllocationIndex;
import com.bt.pi.app.common.entities.SubnetAllocationIndex;
import com.bt.pi.app.common.entities.SubnetAllocationRecord;
import com.bt.pi.app.common.entities.VlanAllocationIndex;
import com.bt.pi.app.common.entities.VlanAllocationRecord;
import com.bt.pi.app.common.entities.watchers.InactiveConsumerPurgingWatcher;
import com.bt.pi.app.common.id.PiIdBuilder;
import com.bt.pi.core.application.watcher.service.WatcherService;
import com.bt.pi.core.conf.Property;
import com.bt.pi.core.dht.DhtClientFactory;
import com.bt.pi.core.id.PId;

@Component
public class NetworkResourceWatcherInitiator {
    protected static final int DEFAULT_INITIAL_INTERVAL_MILLIS = 1800 * 1000;
    protected static final int MILLIS_IN_ONE_HOUR = 3600 * 1000;
    private static final String S_WATCHER = "%s-watcher";
    private static final Log LOG = LogFactory.getLog(NetworkResourceWatcherInitiator.class);
    private DhtClientFactory dhtClientFactory;
    private WatcherService watcherService;
    private PiIdBuilder piIdBuilder;
    private int initialIntervalMillis;

    public NetworkResourceWatcherInitiator() {
        dhtClientFactory = null;
        watcherService = null;
        piIdBuilder = null;
        initialIntervalMillis = DEFAULT_INITIAL_INTERVAL_MILLIS;
    }

    @Resource
    public void setDhtClientFactory(DhtClientFactory aDhtClientFactory) {
        this.dhtClientFactory = aDhtClientFactory;
    }

    @Resource
    public void setWatcherService(WatcherService aWatcherService) {
        this.watcherService = aWatcherService;
    }

    @Resource
    public void setPiIdBuilder(PiIdBuilder aPiIdBuilder) {
        this.piIdBuilder = aPiIdBuilder;
    }

    @Property(key = "network.resource.watcher.initial.interval.millis", defaultValue = "" + DEFAULT_INITIAL_INTERVAL_MILLIS)
    public void setInitialIntervalMillis(int anInitialIntervalMillis) {
        initialIntervalMillis = anInitialIntervalMillis;
    }

    public void initiateWatchers(int requiredActiveApps) {
        LOG.debug(String.format("initiateWatchers(%d)", requiredActiveApps));
        if (requiredActiveApps < 1) {
            LOG.warn("Required active apps < 1!! Not initialising watchers.");
            return;
        }

        int inactiveCheckIntervalMillis = requiredActiveApps * MILLIS_IN_ONE_HOUR;
        LOG.debug(String.format("Inactive check interval is %d ms", inactiveCheckIntervalMillis));

        PId vlanAllocationIndexId = piIdBuilder.getPId(VlanAllocationIndex.URL).forLocalRegion();
        PId subnetAllocationIndexId = piIdBuilder.getPId(SubnetAllocationIndex.URL).forLocalRegion();
        PId publicIpAllocationIndexId = piIdBuilder.getPId(PublicIpAllocationIndex.URL).forLocalRegion();

        InactiveConsumerPurgingWatcher<VlanAllocationRecord> vlanWatcher = new InactiveConsumerPurgingWatcher<VlanAllocationRecord>(dhtClientFactory, vlanAllocationIndexId);
        watcherService.replaceTask(String.format(S_WATCHER, VlanAllocationIndex.class.getSimpleName()), vlanWatcher, initialIntervalMillis, inactiveCheckIntervalMillis);

        InactiveConsumerPurgingWatcher<SubnetAllocationRecord> subnetWatcher = new InactiveConsumerPurgingWatcher<SubnetAllocationRecord>(dhtClientFactory, subnetAllocationIndexId);
        watcherService.replaceTask(String.format(S_WATCHER, SubnetAllocationIndex.class.getSimpleName()), subnetWatcher, initialIntervalMillis, inactiveCheckIntervalMillis);

        InactiveConsumerPurgingWatcher<InstanceRecord> publicIpWatcher = new InactiveConsumerPurgingWatcher<InstanceRecord>(dhtClientFactory, publicIpAllocationIndexId);
        watcherService.replaceTask(String.format(S_WATCHER, PublicIpAllocationIndex.class.getSimpleName()), publicIpWatcher, initialIntervalMillis, inactiveCheckIntervalMillis);
    }
}
