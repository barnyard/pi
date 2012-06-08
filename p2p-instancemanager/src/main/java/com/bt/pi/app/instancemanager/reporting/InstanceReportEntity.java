package com.bt.pi.app.instancemanager.reporting;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

import org.apache.commons.lang.builder.HashCodeBuilder;
import org.codehaus.jackson.annotate.JsonIgnore;

import com.bt.pi.app.common.entities.Instance;
import com.bt.pi.core.application.reporter.TimeBasedReportableEntity;

public class InstanceReportEntity extends TimeBasedReportableEntity<InstanceReportEntity> {
    private DateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ");
    private String instanceId;
    private String ownerId;
    private Date launchTime;
    private long creationTime;
    private String publicIpAddress;
    private String privateIpAddress;

    public InstanceReportEntity() {
        launchTime = null;
        df.setTimeZone(TimeZone.getTimeZone("UTC"));
        creationTime = new Date().getTime();
    }

    public InstanceReportEntity(String anInstanceId, String anOwnerId, long aLaunchTime, String thePublicIpAddress, String thePrivateIpAddress) {
        this();
        instanceId = anInstanceId;
        ownerId = anOwnerId;
        launchTime = new Date(aLaunchTime);
        publicIpAddress = thePublicIpAddress;
        privateIpAddress = thePrivateIpAddress;
    }

    public InstanceReportEntity(Instance instance) {
        this();
        this.instanceId = instance.getInstanceId();
        this.launchTime = new Date(instance.getLaunchTime());
        this.ownerId = instance.getUserId();
        publicIpAddress = instance.getPublicIpAddress();
        privateIpAddress = instance.getPrivateIpAddress();
    }

    @JsonIgnore
    @Override
    public Object getId() {
        return instanceId;
    }

    public String getInstanceId() {
        return instanceId;
    }

    public void setInstanceId(String anInstanceId) {
        instanceId = anInstanceId;
    }

    public String getOwnerId() {
        return ownerId;
    }

    public void setOwnerId(String anOwnerId) {
        ownerId = anOwnerId;
    }

    public String getLaunchTime() {
        return df.format(launchTime);
    }

    public void setLaunchTime(String s) throws ParseException {
        this.launchTime = df.parse(s);
    }

    public String getPublicIpAddress() {
        return publicIpAddress;
    }

    public void setPublicIpAddress(String thePublicIpAddress) {
        this.publicIpAddress = thePublicIpAddress;
    }

    public String getPrivateIpAddress() {
        return privateIpAddress;
    }

    public void setPrivateIpAddress(String thePrivateIpAddress) {
        this.privateIpAddress = thePrivateIpAddress;
    }

    @Override
    public boolean equals(Object other) {
        if (!(other instanceof InstanceReportEntity))
            return false;
        InstanceReportEntity instanceReportEntity = (InstanceReportEntity) other;
        if (instanceId == null)
            return instanceReportEntity.instanceId == null;

        return instanceId.equals(instanceReportEntity.instanceId);
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder().append(instanceId).toHashCode();
    }

    @JsonIgnore
    @Override
    public Object[] getKeysForMap() {
        return new String[] {};
    }

    @JsonIgnore
    @Override
    public int getKeysForMapCount() {
        return 0;
    }

    @Override
    public int compareTo(InstanceReportEntity o) {
        return String.format("%s", instanceId).compareTo(o.instanceId);
    }

    @Override
    public String getType() {
        return getClass().getSimpleName();
    }

    @Override
    public String getUrl() {
        return null;
    }

    @Override
    public String toString() {
        return String.format("InstanceReportEntity [instanceId=%s,launchTime=%s, ownerId=%s, creationTime=%s, publicIpAddress=%s, privateIpAddress=%s]", instanceId, launchTime, ownerId, creationTime, publicIpAddress, privateIpAddress);
    }

    @Override
    public long getCreationTime() {
        return creationTime;
    }

    public void setCreationTime(long aCreationTime) {
        this.creationTime = aCreationTime;
    }

    @Override
    public String getUriScheme() {
        return getClass().getSimpleName();
    }
}
