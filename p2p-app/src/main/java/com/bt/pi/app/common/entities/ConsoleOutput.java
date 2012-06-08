/* (c) British Telecommunications plc, 2009, All Rights Reserved */
package com.bt.pi.app.common.entities;

import org.apache.commons.lang.NotImplementedException;
import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.codehaus.jackson.annotate.JsonIgnore;

import com.bt.pi.app.common.images.platform.ImagePlatform;
import com.bt.pi.core.entity.PiEntity;

public class ConsoleOutput implements PiEntity {
    private String output;
    private long timestamp;
    private String instanceId;
    private ImagePlatform imagePlatform;

    public ConsoleOutput() {
    }

    public ConsoleOutput(String anInstanceId, ImagePlatform anImagePlatform) {
        this(null, -1, anInstanceId, anImagePlatform);
    }

    public ConsoleOutput(String anOutput, long aTimestamp, String anInstanceId, ImagePlatform anImagePlatform) {
        output = anOutput;
        timestamp = aTimestamp;
        instanceId = anInstanceId;
        imagePlatform = anImagePlatform;
    }

    public String getOutput() {
        return output;
    }

    public void setOutput(String anOutput) {
        output = anOutput;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long aTimestamp) {
        timestamp = aTimestamp;
    }

    public void setInstanceId(String anInstanceId) {
        instanceId = anInstanceId;
    }

    public String getInstanceId() {
        return instanceId;
    }

    /**
     * @return the machineType
     */
    public ImagePlatform getImagePlatform() {
        return imagePlatform;
    }

    /**
     * @param machineType
     *            the machineType to set
     */
    public void setImagePlatform(ImagePlatform anImagePlatform) {
        this.imagePlatform = anImagePlatform;
    }

    @Override
    public String getType() {
        return ConsoleOutput.class.getSimpleName();
    }

    @JsonIgnore
    @Override
    public void setVersion(long version) {
        throw new NotImplementedException();
    }

    @JsonIgnore
    @Override
    public long getVersion() {
        throw new NotImplementedException();
    }

    @Override
    public void incrementVersion() {
    }

    @Override
    public String getUrl() {
        return null;
    }

    @Override
    public boolean equals(final Object other) {
        if (!(other instanceof ConsoleOutput))
            return false;
        ConsoleOutput castOther = (ConsoleOutput) other;
        return new EqualsBuilder().append(output, castOther.output).append(timestamp, castOther.timestamp).append(instanceId, castOther.instanceId).append(imagePlatform, castOther.imagePlatform).isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder().append(output).append(timestamp).append(instanceId).append(imagePlatform).toHashCode();
    }

    @Override
    public String getUriScheme() {
        return ConsoleOutput.class.getSimpleName();
    }
}
