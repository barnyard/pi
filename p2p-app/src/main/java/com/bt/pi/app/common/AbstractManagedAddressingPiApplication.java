/* (c) British Telecommunications plc, 2009, All Rights Reserved */
package com.bt.pi.app.common;

import java.util.HashSet;
import java.util.Set;

import javax.annotation.Resource;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.bt.pi.app.common.net.NetworkCommandRunner;
import com.bt.pi.core.application.activation.ApplicationRecord;
import com.bt.pi.core.application.activation.SharedRecordConditionalApplicationActivator;
import com.bt.pi.core.application.activation.UnknownApplicationException;
import com.bt.pi.core.application.resource.ConsumedDhtResourceRegistry;
import com.bt.pi.core.conf.Property;

public abstract class AbstractManagedAddressingPiApplication extends AbstractPiCloudApplication {
    private static final Log LOG = LogFactory.getLog(AbstractManagedAddressingPiApplication.class);
    private static final String DEFAULT_VNET_PUBLIC_INTERACE = "eth1";
    private String vnetPublicInterface = DEFAULT_VNET_PUBLIC_INTERACE;
    private ConsumedDhtResourceRegistry consumedDhtResourceRegistry;
    private NetworkCommandRunner networkCommandRunner;
    private ApplicationPublicInterfaceWatcher applicationPublicInterfaceWatcher;

    public AbstractManagedAddressingPiApplication(String name) {
        super(name);
        this.consumedDhtResourceRegistry = null;
        this.networkCommandRunner = null;
        applicationPublicInterfaceWatcher = null;
        applicationPublicInterfaceWatcher = null;
    }

    @Resource
    public void setNetworkCommandRunner(NetworkCommandRunner aNetworkCommandRunner) {
        this.networkCommandRunner = aNetworkCommandRunner;
    }

    @Resource
    public void setApplicationPublicInterfaceWatcher(ApplicationPublicInterfaceWatcher anApplicationPublicInterfaceWatcher) {
        applicationPublicInterfaceWatcher = anApplicationPublicInterfaceWatcher;
    }

    public NetworkCommandRunner getNetworkCommandRunner() {
        return networkCommandRunner;
    }

    @Resource
    public void setConsumedDhtResourceRegistry(ConsumedDhtResourceRegistry aConsumedDhtResourceRegistry) {
        this.consumedDhtResourceRegistry = aConsumedDhtResourceRegistry;
    }

    protected ConsumedDhtResourceRegistry getConsumedDhtResourceRegistry() {
        return consumedDhtResourceRegistry;
    }

    protected abstract SharedRecordConditionalApplicationActivator getActivatorFromApplication();

    @Override
    public SharedRecordConditionalApplicationActivator getApplicationActivator() {
        return getActivatorFromApplication();
    }

    @Property(key = "vnet.public.interface", defaultValue = DEFAULT_VNET_PUBLIC_INTERACE)
    public void setVnetPublicInterface(String value) {
        this.vnetPublicInterface = value;
    }

    protected String getVnetPublicInterface() {
        return vnetPublicInterface;
    }

    @Override
    public boolean becomeActive() {
        LOG.info(String.format("Application %s going ACTIVE", getApplicationName()));

        String publicIpAddress = getPublicIpAddressForApplicationFromActivationRecord();
        if (publicIpAddress == null) {
            LOG.warn(String.format("No app address in cached app record for %s!", getApplicationName()));
            deletePublicIpAddressesFromInterfaceExcept(null);
            return false;
        }

        getNetworkCommandRunner().ifUp(getVnetPublicInterface());

        deletePublicIpAddressesFromInterfaceExcept(publicIpAddress);
        networkCommandRunner.addIpAddressAndSendArping(publicIpAddress, getVnetPublicInterface());
        networkCommandRunner.addDefaultGatewayRouteToDevice(getVnetPublicInterface());
        return true;
    }

    @Override
    public void becomePassive() {
        LOG.info(String.format("Application %s going PASSIVE", getApplicationName()));
        deletePublicIpAddressesFromInterfaceExcept(null);
    }

    @Override
    protected void onApplicationShuttingDown() {
        super.onApplicationShuttingDown();
        LOG.info(String.format("Application %s shutting down", getApplicationName()));
        applicationPublicInterfaceWatcher.stopWatchingApplicationPublicAddressOnShuttingDown();
        deletePublicIpAddressesFromInterfaceExcept(null);
    }

    protected String getPublicIpAddressForApplicationFromActivationRecord() {
        ApplicationRecord applicationRecord = getApplicationActivator().getApplicationRegistry().getCachedApplicationRecord(getApplicationName());
        if (null == applicationRecord) {
            return null;
        }
        return applicationRecord.getAssociatedResource(getNodeId());
    }

    protected void deletePublicIpAddressesFromInterfaceExcept(String publicIpAddress) {
        Set<String> publicIpAddressPool = getPublicIpAddressPoolFromActivationRecord();
        if (publicIpAddressPool.isEmpty()) {
            LOG.warn(String.format("No IP addresses in IP address pool from activation record"));
            return;
        }

        LOG.debug(String.format("deletePublicIpAddressesFromInterfaceExcept(%s), Public IP Address Pool: %s", publicIpAddress, publicIpAddressPool));
        Set<String> addressesToDelete = new HashSet<String>(publicIpAddressPool);
        if (publicIpAddress != null)
            addressesToDelete.remove(publicIpAddress);
        networkCommandRunner.ipAddressesDelete(addressesToDelete, getVnetPublicInterface());
    }

    private Set<String> getPublicIpAddressPoolFromActivationRecord() {
        try {
            ApplicationRecord applicationRecord = getApplicationActivator().getApplicationRegistry().getCachedApplicationRecord(getApplicationName());
            if (null == applicationRecord)
                return new HashSet<String>();

            return applicationRecord.getActiveNodeMap().keySet();
        } catch (UnknownApplicationException e) {
            LOG.warn("Unable to find cached application", e);
            return new HashSet<String>();
        }
    }

    /**
     * The activation check is triggered by this call.
     * 
     * @param id
     */
    public void removeNodeIdFromApplicationRecord(String id) {
        LOG.info("Deactivating Node:" + id);
        getApplicationActivator().deActivateNode(id, this);
    }

    public void forceActivationCheck() {
        getApplicationActivator().checkAndActivate(this, null);
    }
}
