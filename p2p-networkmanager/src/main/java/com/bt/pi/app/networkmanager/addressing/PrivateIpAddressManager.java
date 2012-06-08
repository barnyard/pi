package com.bt.pi.app.networkmanager.addressing;

import java.util.List;

import javax.annotation.Resource;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.stereotype.Component;

import com.bt.pi.app.common.entities.InstanceAddress;
import com.bt.pi.app.common.entities.SecurityGroup;
import com.bt.pi.app.networkmanager.dhcp.DhcpManager;
import com.bt.pi.core.application.resource.ConsumedDhtResourceRegistry;

@Component
public class PrivateIpAddressManager {
    private static final Log LOG = LogFactory.getLog(PrivateIpAddressManager.class);
    private DhcpManager dhcpManager;
    private ConsumedDhtResourceRegistry consumedDhtResourceRegistry;

    public PrivateIpAddressManager() {
        dhcpManager = null;
        consumedDhtResourceRegistry = null;
    }

    @Resource
    public void setDhcpManager(DhcpManager aDhcpManager) {
        this.dhcpManager = aDhcpManager;
    }

    @Resource
    public void setConsumedDhtResourceRegistry(ConsumedDhtResourceRegistry aConsumedDhtResourceRegistry) {
        this.consumedDhtResourceRegistry = aConsumedDhtResourceRegistry;
    }

    public String allocateAndSetPrivateIpAddress(String instanceId, String macAddress, SecurityGroup securityGroup) {
        LOG.debug(String.format("allocateAndSetPrivateIpAddress(%s, %s, %s)", instanceId, macAddress, securityGroup));

        String allocatedPrivateAddress = null;
        if (securityGroup == null) {
            throw new NetworkNotFoundException("Null security group passed in.");
        }

        if (securityGroup.getInstances().containsKey(instanceId)) {
            InstanceAddress instanceAddress = securityGroup.getInstances().get(instanceId);
            if (instanceAddress != null && instanceAddress.getPrivateIpAddress() != null) {
                String existing = instanceAddress.getPrivateIpAddress();
                LOG.debug(String.format("Reusing previously allocated private ip address %s for instance %s", existing, instanceId));
                return existing;
            }
        }

        for (String privateAddress : securityGroup.getPrivateAddresses()) {
            if (!securityGroup.isPrivateIpAllocated(privateAddress)) {
                securityGroup.addInstance(instanceId, privateAddress, null, macAddress);
                allocatedPrivateAddress = securityGroup.getInstances().get(instanceId).getPrivateIpAddress();
                break;
            }
        }
        if (allocatedPrivateAddress == null) {
            throw new AddressNotAssignedException(String.format("Could not allocate a new private IP address for instance %s in group %s", instanceId, securityGroup));
        }
        return allocatedPrivateAddress;
    }

    public void freePrivateIpAddressForInstance(String instanceId, SecurityGroup securityGroup) {
        LOG.debug(String.format("freePrivateIpAddressForInstance(%s, %s)", instanceId, securityGroup.getSecurityGroupId()));
        Object removed = securityGroup.getInstances().remove(instanceId);
        if (removed == null)
            LOG.info(String.format("Instance %s was not found in security group %s, and hence not removed", instanceId, securityGroup.getSecurityGroupId()));
    }

    public void refreshDhcpDaemon() {
        LOG.debug(String.format("refreshDhcpDaemon()"));
        List<SecurityGroup> clonedSecurityGroups = consumedDhtResourceRegistry.getByType(SecurityGroup.class);
        this.dhcpManager.requestDhcpRefresh(clonedSecurityGroups);
    }

}
