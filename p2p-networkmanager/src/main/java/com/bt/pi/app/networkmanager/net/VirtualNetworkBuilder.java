package com.bt.pi.app.networkmanager.net;

import java.net.URI;

import javax.annotation.Resource;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.stereotype.Component;

import com.bt.pi.app.common.entities.ResourceSchemes;
import com.bt.pi.app.common.entities.SecurityGroup;
import com.bt.pi.app.common.net.NetworkCommandRunner;
import com.bt.pi.app.common.net.utils.VlanAddressUtils;
import com.bt.pi.app.networkmanager.iptables.IpTablesManager;
import com.bt.pi.app.networkmanager.iptables.IpTablesUpdateException;
import com.bt.pi.core.application.resource.ConsumedUriResourceRegistry;
import com.bt.pi.core.cli.commands.CommandExecutionException;
import com.bt.pi.core.conf.Property;
import com.bt.pi.core.continuation.LoggingContinuation;

@Component
public class VirtualNetworkBuilder {
    private static final String VNET_INTERFACE = "vnet.private.interface";
    private static final Log LOG = LogFactory.getLog(NetworkManager.class);
    private static final String FAILED_TO_CREATE_NETWORK_S = "Failed to create network %s";
    private static final String DEFAULT_PRIVATE_INTERFACE = "eth0";
    @Resource
    private NetworkCommandRunner networkCommandRunner;
    @Resource
    private IpTablesManager ipTablesManager;
    @Resource
    private ConsumedUriResourceRegistry consumedUriResourceRegistry;
    private String privateInterface;

    public VirtualNetworkBuilder() {
        super();
        this.networkCommandRunner = null;
        this.ipTablesManager = null;
        this.consumedUriResourceRegistry = null;
        this.privateInterface = DEFAULT_PRIVATE_INTERFACE;
    }

    @Property(key = VNET_INTERFACE, defaultValue = DEFAULT_PRIVATE_INTERFACE)
    public void setVnetInterface(String value) {
        this.privateInterface = value;
    }

    public void setUpVirtualNetworkForSecurityGroup(SecurityGroup securityGroup) {
        LOG.debug(String.format("setUpVirtualNetworkForSecurityGroup(%s)", securityGroup));
        try {
            addManagedNetwork(securityGroup.getVlanId(), securityGroup.getSecurityGroupId());
            networkCommandRunner.addGatewayIp(securityGroup.getRouterAddress(), securityGroup.getSlashnet(), securityGroup.getBroadcastAddress(), VlanAddressUtils.getBridgeNameForVlan(securityGroup.getVlanId()));
            ipTablesManager.refreshIpTables();
        } catch (IpTablesUpdateException e) {
            throw new NetworkCreationException(String.format(FAILED_TO_CREATE_NETWORK_S, securityGroup.getSecurityGroupId()), e);
        } catch (CommandExecutionException e) {
            throw new NetworkCreationException(String.format(FAILED_TO_CREATE_NETWORK_S, securityGroup.getSecurityGroupId()), e);
        }
    }

    public void refreshXenVifOnBridge(long vlanId, long domainId) {
        LOG.debug(String.format("refreshXenVifOnBridge(%d, %s)", vlanId, domainId));
        this.networkCommandRunner.refreshXenVifOnBridge(vlanId, domainId);
    }

    public void setUpVirtualNetworkForInstance(long vlanId, String instanceId) {
        LOG.debug(String.format("setUpVirtualNetworkForInstance(%d, %s)", vlanId, instanceId));
        addManagedNetwork(vlanId, instanceId);
    }

    public void tearDownVirtualNetworkForSecurityGroup(SecurityGroup securityGroup) {
        LOG.debug(String.format("tearDownVirtualNetworkForSecurityGroup(%s)", securityGroup));

        networkCommandRunner.deleteGatewayIp(securityGroup.getRouterAddress(), securityGroup.getSlashnet(), securityGroup.getBroadcastAddress(), securityGroup.getVlanId());

        removeManagedNetworkIfLastConsumer(securityGroup.getVlanId(), securityGroup.getSecurityGroupId());
        ipTablesManager.refreshIpTables();
    }

    public void tearDownVirtualNetworkForInstance(long vlanId, String instanceId) {
        LOG.debug(String.format("tearDownVirtualNetworkForInstance(%d, %s)", vlanId, instanceId));
        removeManagedNetworkIfLastConsumer(vlanId, instanceId);
    }

    // we will be a little defensive here and do an unconditional add, since it should be idempotent
    protected void addManagedNetwork(long vlanId, String consumerId) {
        consumedUriResourceRegistry.registerConsumer(URI.create(getVlanUri(vlanId)), consumerId, new LoggingContinuation<Boolean>());
        networkCommandRunner.addManagedNetwork(vlanId, privateInterface);
        LOG.debug(String.format("Set up managed network for vlan %d, consumer %s", vlanId, consumerId));
    }

    private String getVlanUri(long vlanId) {
        return String.format("%s:%d", ResourceSchemes.VIRTUAL_NETWORK, vlanId);
    }

    protected void removeManagedNetworkIfLastConsumer(long vlanId, String consumerId) {
        boolean isLast = consumedUriResourceRegistry.deregisterConsumer(URI.create(getVlanUri(vlanId)), consumerId);
        if (isLast) {
            networkCommandRunner.removeManagedNetwork(vlanId, privateInterface);
        } else {
            LOG.debug(String.format("Not tearing down managed network for vlan %d, consumer %s, as other consumers still exist", vlanId, consumerId));
        }
    }

    public void refreshNetwork() {
        LOG.debug(String.format("refreshNetwork()"));
        ipTablesManager.refreshIpTables();
    }
}
