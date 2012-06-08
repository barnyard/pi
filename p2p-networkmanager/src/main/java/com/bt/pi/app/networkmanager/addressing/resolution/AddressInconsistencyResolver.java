package com.bt.pi.app.networkmanager.addressing.resolution;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.Executor;

import javax.annotation.Resource;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.stereotype.Component;

import com.bt.pi.app.common.entities.InstanceAddress;
import com.bt.pi.app.common.entities.PublicIpAllocationIndex;
import com.bt.pi.app.common.entities.SecurityGroup;
import com.bt.pi.app.common.entities.SubnetAllocationIndex;
import com.bt.pi.app.common.entities.util.ResourceRange;
import com.bt.pi.app.common.id.PiIdBuilder;
import com.bt.pi.app.common.net.NetworkCommandRunner;
import com.bt.pi.app.common.net.utils.IpAddressUtils;
import com.bt.pi.app.common.net.utils.VlanAddressUtils;
import com.bt.pi.core.application.resource.ConsumedDhtResourceRegistry;
import com.bt.pi.core.conf.Property;
import com.bt.pi.core.continuation.PiContinuation;
import com.bt.pi.core.dht.cache.DhtCache;

@Component
public class AddressInconsistencyResolver {
    private static final Log LOG = LogFactory.getLog(AddressInconsistencyResolver.class);
    private static final String VNET_PUBLIC_INTERFACE = "vnet.public.interface";
    private static final String DEFAULT_PUBLIC_INTERFACE = "eth1";

    private String publicInterface;

    private PiIdBuilder piIdBuilder;
    private DhtCache dhtCache;
    private ConsumedDhtResourceRegistry consumedDhtResourceRegistry;
    private NetworkCommandRunner networkCommandRunner;
    private AddressDeleteQueue addressDeleteQueue;
    private Executor executor;

    public AddressInconsistencyResolver() {
        this.piIdBuilder = null;
        this.networkCommandRunner = null;
        this.consumedDhtResourceRegistry = null;
        this.dhtCache = null;
        this.addressDeleteQueue = null;
        this.executor = null;
        this.publicInterface = DEFAULT_PUBLIC_INTERFACE;
    }

    @Property(key = VNET_PUBLIC_INTERFACE, defaultValue = DEFAULT_PUBLIC_INTERFACE)
    public void setVnetPublicInterface(String value) {
        this.publicInterface = value;
    }

    @Resource
    public void setPiIdBuilder(PiIdBuilder aPiIdBuilder) {
        this.piIdBuilder = aPiIdBuilder;
    }

    @Resource
    public void setNetworkCommandRunner(NetworkCommandRunner aNetworkCommandRunner) {
        this.networkCommandRunner = aNetworkCommandRunner;
    }

    @Resource
    public void setConsumedDhtResourceRegistry(ConsumedDhtResourceRegistry aConsumedDhtResourceRegistry) {
        this.consumedDhtResourceRegistry = aConsumedDhtResourceRegistry;
    }

    @Resource(name = "generalCache")
    public void setDhtCache(DhtCache aDhtCache) {
        this.dhtCache = aDhtCache;
    }

    @Resource
    public void setAddressDeleteQueue(AddressDeleteQueue anAddressDeleteQueue) {
        this.addressDeleteQueue = anAddressDeleteQueue;
    }

    @Resource(name = "taskExecutor")
    public void setExecutor(Executor anExecutor) {
        this.executor = anExecutor;
    }

    public void refreshIpAddressesOnBridges() {
        LOG.debug(String.format("Refreshing Bridge IP addresses on all pi bridges"));

        dhtCache.get(piIdBuilder.getPId(SubnetAllocationIndex.URL).forLocalRegion(), new PiContinuation<SubnetAllocationIndex>() {
            @Override
            public void handleResult(final SubnetAllocationIndex result) {
                if (result == null) {
                    LOG.debug("SubnetAllocationIndex not found in dht, cannot resolve");
                    return;
                }

                executor.execute(new Runnable() {
                    @Override
                    public void run() {
                        Map<String, String> piBridgeAddresses = networkCommandRunner.getAllAddressesOnAllPiBridges();

                        Collection<SecurityGroup> securityGroups = (Collection<SecurityGroup>) consumedDhtResourceRegistry.getByType(SecurityGroup.class);
                        Map<String, SecurityGroup> addressesUsedByNetworkManager = new HashMap<String, SecurityGroup>();
                        for (SecurityGroup secGroup : securityGroups) {
                            if (secGroup.getNetworkAddress() != null) {
                                addressesUsedByNetworkManager.put(secGroup.getRouterAddress(), secGroup);
                            }
                        }
                        addMissingAddressesOnNetworkInterface(new AddressManipulator(piBridgeAddresses, addressesUsedByNetworkManager, result.getResourceRanges()) {
                            @Override
                            public void addIpAddress(String address, SecurityGroup securityGroup) {
                                networkCommandRunner.addGatewayIp(address, securityGroup.getSlashnet(), securityGroup.getBroadcastAddress(), VlanAddressUtils.getBridgeNameForVlan(securityGroup.getVlanId()));
                            }
                        });
                    }
                });
            }
        });
    }

    public void refreshIpAddressesOnPublicInterface() {
        LOG.debug(String.format("Refreshing IP addresses on adapter %s", publicInterface));
        dhtCache.get(piIdBuilder.getPId(PublicIpAllocationIndex.URL).forLocalRegion(), new PiContinuation<PublicIpAllocationIndex>() {
            @Override
            public void handleResult(final PublicIpAllocationIndex result) {
                if (result == null) {
                    LOG.debug("PublicAllocationIndex not found in dht, cannot resolve");
                    return;
                }

                executor.execute(new Runnable() {
                    @Override
                    public void run() {
                        Map<String, SecurityGroup> addressesInGroups = getPublicAddressesKnownInUse();

                        Collection<String> addressesOnIface = networkCommandRunner.getAllAddressesOnDevice(publicInterface);
                        Map<String, String> addressesOnPublicIface = new HashMap<String, String>();
                        for (String address : addressesOnIface)
                            addressesOnPublicIface.put(IpAddressUtils.addSlash32ToIpAddress(address), publicInterface);

                        reconcileAddresses(new AddressManipulator(addressesOnPublicIface, addressesInGroups, result.getResourceRanges()) {
                            @Override
                            public void addIpAddress(String address, SecurityGroup securityGroup) {
                                networkCommandRunner.addIpAddressAndSendArping(address, publicInterface);
                            }
                        });
                    }
                });
            }
        });
    }

    private void reconcileAddresses(AddressManipulator addressManipulator) {
        LOG.debug("reconcileAddresses");
        if (addressManipulator.getCachedIPRanges() == null)
            return;

        deleteExtraneousAddressesOnNetworkInterface(addressManipulator);
        addMissingAddressesOnNetworkInterface(addressManipulator);
    }

    private void addMissingAddressesOnNetworkInterface(AddressManipulator addressManipulator) {
        for (Entry<String, SecurityGroup> entry : addressManipulator.getAddressesInGroups().entrySet()) {
            String address = entry.getKey();
            SecurityGroup securityGroup = entry.getValue();
            if (!addressManipulator.getAddressesOnIface().containsKey(address)) {
                LOG.debug(String.format("Adding IP address %s as part of refresh", address));
                addressManipulator.addIpAddress(address, securityGroup);
            }
        }
    }

    private void deleteExtraneousAddressesOnNetworkInterface(AddressManipulator addressManipulator) {
        LOG.debug("deleteExtraneousAddressesOnNetworkInterface");
        for (Entry<String, String> addressOnIface : addressManipulator.getAddressesOnIface().entrySet()) {
            final String address = addressOnIface.getKey();
            final String iface = addressOnIface.getValue();
            LOG.debug(String.format("Checking if address %s on interface %s is in group %s.", address, iface, addressManipulator.getAddressesInGroups().keySet()));
            if (!addressManipulator.getAddressesInGroups().keySet().contains(address)) {
                LOG.debug(String.format("Checking address range: %s ", addressManipulator.getCachedIPRanges()));
                for (ResourceRange range : addressManipulator.getCachedIPRanges()) {
                    Long addressAsLong = addressManipulator.getAddressAsLong(address);
                    if (addressAsLong >= range.getMin() && addressAsLong <= range.getMax()) {
                        LOG.debug(String.format("Address %o in public ip resource range (%o, %o), adding to queue for immediate deletion", addressAsLong, range.getMin(), range.getMax()));
                        addressDeleteQueue.add(new AddressInconsistencyDeleteItem(addressDeleteQueue.getPriorityThreshold(), address, iface, networkCommandRunner));
                        break;
                    }
                }
            }
        }
    }

    private Map<String, SecurityGroup> getPublicAddressesKnownInUse() {
        Map<String, SecurityGroup> addressesInGroups = new HashMap<String, SecurityGroup>();
        Collection<SecurityGroup> securityGroups = (Collection<SecurityGroup>) consumedDhtResourceRegistry.getByType(SecurityGroup.class);
        for (SecurityGroup secGroup : securityGroups) {
            for (InstanceAddress instance : secGroup.getInstances().values()) {
                if (null != instance.getPublicIpAddress()) {
                    addressesInGroups.put(IpAddressUtils.addSlash32ToIpAddress(instance.getPublicIpAddress()), secGroup);
                }
            }
        }
        return addressesInGroups;
    }

}
