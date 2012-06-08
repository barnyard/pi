/* (c) British Telecommunications plc, 2009, All Rights Reserved */
package com.bt.pi.app.common.entities;

import java.io.IOException;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.apache.commons.lang.builder.ToStringBuilder;
import org.codehaus.jackson.JsonParser;
import org.codehaus.jackson.annotate.JsonIgnore;
import org.codehaus.jackson.annotate.JsonProperty;
import org.codehaus.jackson.map.DeserializationContext;
import org.codehaus.jackson.map.JsonDeserializer;
import org.codehaus.jackson.map.annotate.JsonDeserialize;

import com.bt.pi.core.entity.Deletable;

public class Volume extends VolumeBase implements Deletable {
    public static final int BURIED_TIME = 6 * 60 * 60 * 1000;
    private static final int TEN = 10;

    private static final int HASH_MULTIPLE = 73;
    private static final int HASH_INITIAL = 97;

    @JsonProperty
    @JsonDeserialize(using = VolumeStateDeserializer.class)
    private VolumeState status;
    private String device;
    private String attachedStatus;
    private String instanceId;
    private int sizeInGigaBytes;
    private long attachTime;

    public Volume() {
    }

    private static class VolumeStateDeserializer extends JsonDeserializer<VolumeState> {
        public VolumeStateDeserializer() {
        }

        @Override
        public VolumeState deserialize(JsonParser arg0, DeserializationContext arg1) throws IOException {
            String text = arg0.getText();
            if ("ATTACHED".equals(text))
                return VolumeState.IN_USE;
            if ("CREATED".equals(text))
                return VolumeState.AVAILABLE;
            if ("PENDING".equals(text))
                return VolumeState.CREATING;
            return VolumeState.getValue(text);
        }
    }

    public Volume(String anOwnerId, String aVolumeId, String anInstanceId, String aDevice, VolumeState aStatus, long anAttachTime) {
        device = aDevice;
        attachTime = anAttachTime;
        setOwnerId(anOwnerId);
        setVolumeId(aVolumeId);
        setInstanceId(anInstanceId);
        setStatus(aStatus);
    }

    @JsonIgnore
    public void setStatus(VolumeState aStatus) {
        this.status = aStatus;
        setStatusTimestamp(System.currentTimeMillis());
    }

    @JsonIgnore
    public VolumeState getStatus() {
        return status;
    }

    public String getInstanceId() {
        return instanceId;
    }

    public void setInstanceId(String anInstanceId) {
        instanceId = anInstanceId;
    }

    public String getAttachedStatus() {
        return attachedStatus;
    }

    public void setAttachedStatus(String attachedState) {
        attachedStatus = attachedState;
    }

    public long getAttachTime() {
        return attachTime;
    }

    public void setAttachTime(long anAttachTime) {
        attachTime = anAttachTime;
    }

    public String getDevice() {
        return device;
    }

    public void setDevice(String aDevice) {
        this.device = aDevice;
    }

    public String getType() {
        return getClass().getSimpleName();
    }

    public int getSizeInGigaBytes() {
        return sizeInGigaBytes;
    }

    public void setSizeInGigaBytes(int aSize) {
        this.sizeInGigaBytes = aSize;
    }

    public String getUrl() {
        return Volume.getUrl(getVolumeId());
    }

    public static String getUrl(String entityKey) {
        return String.format("%s:%s", ResourceSchemes.VOLUME, entityKey);
    }

    @Override
    public boolean equals(final Object other) {
        if (this == other)
            return true;
        if (!(other instanceof Volume))
            return false;
        Volume castOther = (Volume) other;
        return new EqualsBuilder().appendSuper(super.equals(other)).append(status, castOther.status).append(device, castOther.device).append(attachedStatus, castOther.attachedStatus).append(instanceId, castOther.instanceId).append(sizeInGigaBytes,
                castOther.sizeInGigaBytes).append(attachTime, castOther.attachTime).isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(HASH_INITIAL, HASH_MULTIPLE).appendSuper(super.hashCode()).append(status).append(device).append(attachedStatus).append(instanceId).append(sizeInGigaBytes).append(attachTime).toHashCode();
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this).appendSuper(super.toString()).append("status", status).append("device", device).append("attachedStatus", attachedStatus).append("instanceId", instanceId).append("sizeInGigaBytes", sizeInGigaBytes).append(
                "attachTime", attachTime).toString();
    }

    @Override
    @JsonIgnore
    public boolean isDeleted() {
        return VolumeState.BURIED.equals(status) || (VolumeState.DELETED.equals(status) && (getStatusTimestamp() + (TEN * BURIED_TIME)) < System.currentTimeMillis());
    }

    @Override
    @JsonIgnore
    public void setDeleted(boolean b) {
        if (b)
            setStatus(VolumeState.BURIED);
    }

    @Override
    public String getUriScheme() {
        return ResourceSchemes.VOLUME.toString();
    }
}
