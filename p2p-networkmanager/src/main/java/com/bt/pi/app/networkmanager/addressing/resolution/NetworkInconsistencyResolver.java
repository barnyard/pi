package com.bt.pi.app.networkmanager.addressing.resolution;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.annotation.Resource;

import org.springframework.context.ApplicationListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.bt.pi.app.common.entities.SecurityGroup;
import com.bt.pi.core.node.NodeStartedEvent;

@Component
public class NetworkInconsistencyResolver implements ApplicationListener<NodeStartedEvent> {
    private static final int ONE_MINUTE = 60 * 1000;

    private AtomicBoolean nodeStarted;
    private AddressInconsistencyResolver addressInconsistencyResolver;
    private VlanInconsistencyResolver vlanInconsistencyResolver;
    private NetworkManagerTearDownHelper networkManagerTearDownHelper;

    public NetworkInconsistencyResolver() {
        this.nodeStarted = new AtomicBoolean(false);
        this.addressInconsistencyResolver = null;
        this.vlanInconsistencyResolver = null;
        this.networkManagerTearDownHelper = null;
    }

    @Resource
    public void setAddressInconsistencyResolver(AddressInconsistencyResolver anAddressInconsistencyResolver) {
        this.addressInconsistencyResolver = anAddressInconsistencyResolver;
    }

    @Resource
    public void setVlanInconsistencyResolver(VlanInconsistencyResolver aVlanInconsistencyResolver) {
        this.vlanInconsistencyResolver = aVlanInconsistencyResolver;
    }

    @Resource
    public void setNetworkManagerTearDownHelper(NetworkManagerTearDownHelper aNetworkManagerTearDownHelper) {
        this.networkManagerTearDownHelper = aNetworkManagerTearDownHelper;
    }

    @Scheduled(fixedDelay = ONE_MINUTE)
    public void refreshIpAddressesOnPublicInterfaceAndBridges() {
        if (nodeStarted.get()) {
            addressInconsistencyResolver.refreshIpAddressesOnPublicInterface();
            addressInconsistencyResolver.refreshIpAddressesOnBridges();
            vlanInconsistencyResolver.refreshVirtualNetworks();
        }
    }

    @Override
    public void onApplicationEvent(NodeStartedEvent event) {
        nodeStarted.set(true);
    }

    public void tearDownNetworkManagerAddressesForGroups(List<SecurityGroup> groups) {
        networkManagerTearDownHelper.removeAllAddressesFromSecurityGroups(groups);
    }
}
