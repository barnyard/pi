/* (c) British Telecommunications plc, 2009, All Rights Reserved */
package com.bt.pi.app.common.entities;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;

public class InstanceAddress {
	private static final int HASH_MULTIPLE = 37;
	private static final int HASH_INITIAL = 17;
	private String privateIpAddress;
	private String publicIpAddress;
	private String macAddress;

	public InstanceAddress(){}
	
	public InstanceAddress(String aPrivateIpAddress, String aPublicIpAddress, String aMacAddress) {
		super();
		this.privateIpAddress = aPrivateIpAddress;
		this.publicIpAddress = aPublicIpAddress;
		this.macAddress = aMacAddress;
	}

	public InstanceAddress(InstanceAddress instanceAddress) {
		super();
		this.privateIpAddress = instanceAddress.getPrivateIpAddress();
		this.publicIpAddress = instanceAddress.getPublicIpAddress();
		this.macAddress = instanceAddress.getMacAddress();
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (!(obj instanceof InstanceAddress))
			return false;
		InstanceAddress other = (InstanceAddress) obj;
		return new EqualsBuilder().append(macAddress, other.macAddress).append(privateIpAddress, other.privateIpAddress).append(publicIpAddress, other.publicIpAddress).isEquals();
	}

	@Override
	public int hashCode() {
		return new HashCodeBuilder(HASH_INITIAL, HASH_MULTIPLE).append(macAddress).append(privateIpAddress).append(publicIpAddress).toHashCode();
	}

	public String getPrivateIpAddress() {
		return privateIpAddress;
	}

	public String getPublicIpAddress() {
		return publicIpAddress;
	}

	public String getMacAddress() {
		return macAddress;
	}
	
	public void setMacAddress(String aMacAddress) {
		this.macAddress = aMacAddress;
	}

	public void setPublicIpAddress(String aPublicIpAddress) {
		this.publicIpAddress = aPublicIpAddress;
	}

	public void setPrivateIpAddress(String aPrivateIpAddress) {
		this.privateIpAddress = aPrivateIpAddress;
	}

	@Override
	public String toString() {
		return "InstanceAddress [macAddress=" + macAddress + ", privateIpAddress=" + privateIpAddress + ", publicIpAddress=" + publicIpAddress + "]";
	}
}
