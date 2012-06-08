/* (c) British Telecommunications plc, 2009, All Rights Reserved */
package com.bt.pi.app.common.entities;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;

public class BlockDeviceMapping {

    private static final int HASH_MULTIPLE = 31;
    private static final int HASH_INITIAL = 73;

    private String virtualName;
    private String deviceName;
    private String volumeId;

    public BlockDeviceMapping() {
    }

    public BlockDeviceMapping(String aVolumeId) {
        this.volumeId = aVolumeId;
    }

    public BlockDeviceMapping(String aVirtualName, String aDeviceName) {
        virtualName = aVirtualName;
        deviceName = aDeviceName;
    }

    public String getDeviceName() {
        return deviceName;
    }

    public void setDeviceName(String aDeviceName) {
        this.deviceName = aDeviceName;
    }

    public String getVirtualName() {
        return virtualName;
    }

    public void setVirtualName(String aVirtualName) {
        this.virtualName = aVirtualName;
    }

    public String getVolumeId() {
        return this.volumeId;
    }

    public void setVolumeId(String aVolumeId) {
        this.volumeId = aVolumeId;
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(HASH_INITIAL, HASH_MULTIPLE).append(virtualName).append(deviceName).append(volumeId).toHashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        BlockDeviceMapping other = (BlockDeviceMapping) obj;
        return new EqualsBuilder().append(deviceName, other.deviceName).append(virtualName, other.virtualName).append(volumeId, other.volumeId).isEquals();
    }
}
