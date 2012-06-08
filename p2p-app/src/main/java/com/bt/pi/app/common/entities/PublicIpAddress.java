package com.bt.pi.app.common.entities;

import org.apache.commons.lang.builder.ToStringBuilder;

import com.bt.pi.core.entity.PiEntityBase;

public class PublicIpAddress extends PiEntityBase {
    private String address;
    private String securityGroupName;
    private String instanceId;
    private String ownerId;

    public PublicIpAddress() {
        address = null;
        instanceId = null;
        ownerId = null;
        securityGroupName = null;
    }

    public PublicIpAddress(String anAddress, String anInstanceId, String anOwnerId, String aSecurityGroupName) {
        address = anAddress;
        instanceId = anInstanceId;
        ownerId = anOwnerId;
        securityGroupName = aSecurityGroupName;
    }

    public String getIpAddress() {
        return address;
    }

    public void setIpAddress(String anAddress) {
        this.address = anAddress;
    }

    public String getSecurityGroupName() {
        return securityGroupName;
    }

    public void setSecurityGroupName(String aSecurityGroupName) {
        this.securityGroupName = aSecurityGroupName;
    }

    public String getOwnerId() {
        return ownerId;
    }

    public void setOwnerId(String anOwnerId) {
        this.ownerId = anOwnerId;
    }

    public String getInstanceId() {
        return instanceId;
    }

    public void setInstanceId(String aInstanceId) {
        this.instanceId = aInstanceId;
    }

    @Override
    public String getType() {
        return getClass().getSimpleName();
    }

    @Override
    public String getUrl() {
        return PublicIpAddress.getUrl(address);
    }

    public static String getUrl(String addr) {
        return String.format("%s:%s", ResourceSchemes.ADDRESS, addr);
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this).append(address).append(instanceId).append(ownerId).append(securityGroupName).toString();
    }

    @Override
    public String getUriScheme() {
        return ResourceSchemes.ADDRESS.toString();
    }
}
