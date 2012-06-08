package com.bt.pi.app.networkmanager.addressing.resolution;

import java.net.URI;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import javax.annotation.Resource;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.stereotype.Component;

import com.bt.pi.app.common.entities.ResourceSchemes;
import com.bt.pi.app.common.entities.SecurityGroup;
import com.bt.pi.app.common.net.NetworkCommandRunner;
import com.bt.pi.app.common.os.DeviceUtils;
import com.bt.pi.core.application.resource.ConsumedDhtResourceRegistry;
import com.bt.pi.core.application.resource.ConsumedUriResourceRegistry;
import com.bt.pi.core.conf.Property;
import com.bt.pi.core.continuation.LoggingContinuation;

@Component
public class VlanInconsistencyResolver {
    private static final Log LOG = LogFactory.getLog(VlanInconsistencyResolver.class);
    private static final String S_D = "%s:%d";
    private static final String VNET_PRIVATE_INTERFACE = "vnet.private.interface";
    private static final String DEFAULT_PRIVATE_INTERFACE = "eth0";
    private static final String DOT = ".";

    private String privateInterface;

    private NetworkCommandRunner networkCommandRunner;
    private DeviceUtils deviceUtils;
    private ConsumedUriResourceRegistry consumedUriResourceRegistry;
    private ConsumedDhtResourceRegistry consumedDhtResourceRegistry;
    private AddressDeleteQueue addressDeleteQueue;

    public VlanInconsistencyResolver() {
        this.networkCommandRunner = null;
        this.deviceUtils = null;
        this.consumedUriResourceRegistry = null;
        this.consumedDhtResourceRegistry = null;
        this.addressDeleteQueue = null;
    }

    @Property(key = VNET_PRIVATE_INTERFACE, defaultValue = DEFAULT_PRIVATE_INTERFACE)
    public void setVnetPrivateInterface(String value) {
        this.privateInterface = value;
    }

    @Resource
    public void setNetworkCommandRunner(NetworkCommandRunner aNetworkCommandRunner) {
        this.networkCommandRunner = aNetworkCommandRunner;
    }

    @Resource
    public void setDeviceUtils(DeviceUtils aDeviceUtils) {
        this.deviceUtils = aDeviceUtils;
    }

    @Resource
    public void setConsumedUriResourceRegistry(ConsumedUriResourceRegistry aConsumedUriResourceRegistry) {
        this.consumedUriResourceRegistry = aConsumedUriResourceRegistry;
    }

    @Resource
    public void setConsumedDhtResourceRegistry(ConsumedDhtResourceRegistry aConsumedDhtResourceRegistry) {
        this.consumedDhtResourceRegistry = aConsumedDhtResourceRegistry;
    }

    @Resource
    public void setAddressDeleteQueue(AddressDeleteQueue anAddressDeleteQueue) {
        this.addressDeleteQueue = anAddressDeleteQueue;
    }

    public void refreshVirtualNetworks() {
        LOG.debug(String.format("Refreshing virtual networks"));

        Set<URI> vlanIds = consumedUriResourceRegistry.getResourceIdsByScheme(ResourceSchemes.VIRTUAL_NETWORK.toString());

        /*
         *  Because the vlanId gets inserted into the consumedUriResourceRegistry a few continuations after a security 
         *  group gets added, sometimes this fails and then the pibr never actually gets onto the network manager. So we 
         *  do some reconciliation between the security group and vlan registries. For now, we aren't looking for entries
         *  in the vlan registry that aren't in the security group list...needs some thought on whether that is needed but
         *  certainly not until we see some issues pertaining to that! 
         */
        List<SecurityGroup> securityGroups = consumedDhtResourceRegistry.getByType(SecurityGroup.class);
        for (SecurityGroup securityGroup : securityGroups) {
            Long vlanId = securityGroup.getVlanId();
            if (null == vlanId)
                continue;
            URI vlanUri = URI.create(String.format(S_D, ResourceSchemes.VIRTUAL_NETWORK, vlanId));
            if (!vlanIds.contains(vlanUri)) {
                LOG.debug(String.format("Vlan Id %d present in the security groups in consumedDhtResourceRegistry is not present in the list of vlans in consumedUriResourceRegistry", securityGroup.getVlanId()));
                consumedUriResourceRegistry.registerConsumer(vlanUri, securityGroup.getSecurityGroupId(), new LoggingContinuation<Boolean>());
                networkCommandRunner.addManagedNetwork(vlanId, privateInterface);
            }
        }

        List<String> deviceList = deviceUtils.getDeviceList();
        for (URI uri : vlanIds) {
            long vlanId = Long.parseLong(uri.getSchemeSpecificPart());
            LOG.debug(String.format("Checking to see if vlan %d exists", vlanId));
            if (!deviceUtils.deviceExists(networkCommandRunner.getVlanInterface(vlanId, privateInterface), deviceList)) {
                networkCommandRunner.addManagedNetwork(vlanId, privateInterface);
            }
        }

        Collection<String> allVlanDevicesForInterface = deviceUtils.getAllVlanDevicesForInterface(privateInterface, deviceList);
        for (String vlan : allVlanDevicesForInterface) {
            if (vlan.contains(DOT)) {
                long vlanId = Long.parseLong(vlan.split(String.format("\\%s", DOT))[1]);
                if (!vlanIds.contains(URI.create(String.format(S_D, ResourceSchemes.VIRTUAL_NETWORK, vlanId))))
                    addressDeleteQueue.add(new VlanDeleteItem(vlanId, privateInterface, networkCommandRunner));
            }
        }
    }
}
