/* (c) British Telecommunications plc, 2009, All Rights Reserved */
package com.bt.pi.app.common.entities;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.apache.commons.lang.builder.ToStringBuilder;
import org.codehaus.jackson.annotate.JsonIgnore;
import org.codehaus.jackson.annotate.JsonProperty;

import com.bt.pi.app.common.entities.watchers.instance.InstanceWatchingStrategy;
import com.bt.pi.app.common.images.platform.ImagePlatform;
import com.bt.pi.core.application.resource.watched.FiniteLifespanEntity;
import com.bt.pi.core.application.resource.watched.WatchedResource;
import com.bt.pi.core.entity.Deletable;

/**
 * Instance entity for machine instances
 */

@WatchedResource(watchingStrategy = InstanceWatchingStrategy.class, defaultInitialResourceRefreshIntervalMillis = Instance.DEFAULT_INITIAL_INTERVAL_MILLIS, defaultRepeatingResourceRefreshIntervalMillis = Instance.DEFAULT_REPEATING_INTERVAL_MILLIS, initialResourceRefreshIntervalMillisProperty = Instance.INITIAL_INTERVAL_MILLIS_PROPERTY, repeatingResourceRefreshIntervalMillisProperty = Instance.REPEATING_INTERVAL_MILLIS_PROPERTY)
public class Instance extends ReservationBase implements HeartbeatTimestampResource, FiniteLifespanEntity, Deletable {
    public static final String INITIAL_INTERVAL_MILLIS_PROPERTY = "instance.manager.instance.subscribe.initial.wait.time.millis";
    public static final String REPEATING_INTERVAL_MILLIS_PROPERTY = "instance.manager.instance.subscribe.interval.millis";
    public static final long DEFAULT_INITIAL_INTERVAL_MILLIS = 60 * 1000;
    public static final long DEFAULT_REPEATING_INTERVAL_MILLIS = 5 * 60 * 1000;
    private static final String INSTANCE_BURIED_INTERVAL_MILLIS = "instance.buried.interval.millis";
    private static final int HASH_MULTIPLE = 37;
    private static final int HASH_INITIAL = 17;
    private static final long DEFAULT_BURIED_INTERVAL_MILLIS = 24 * 60 * 60 * 1000;
    private String instanceId;
    private int imageSizeInMB;
    private String privateMacAddress;
    private int vlanId;
    private String publicIpAddress;
    private String privateIpAddress;
    private String memoryInKB;
    private int vcpus;
    @JsonProperty
    private InstanceState state;
    private long launchTime;
    private String reasonForLastStateTransition;
    private int launchIndex;
    private String nodeId;
    private String hostname;
    private Long lastHeartbeatTimestamp;
    private long buriedIntervalMillis;
    private boolean restartRequested;
    private int regionCode;
    private int availabilityZoneCode;
    @JsonProperty
    private long stateChangeTimeStamp;
    @JsonProperty
    private InstanceAction actionRequired;
    @JsonProperty
    private InstanceActivityState instanceActivityState = InstanceActivityState.GREEN;
    @JsonProperty
    private long instanceActivityStateChangeTimestamp = System.currentTimeMillis();

    public Instance() {
        this(null);
    }

    public Instance(String anInstanceId, String aUserId, String aSecurityGroupName) {
        this(anInstanceId, aUserId, aSecurityGroupName, ImagePlatform.linux);
    }

    public Instance(String anInstanceId, String aUserId, String aSecurityGroupName, ImagePlatform aPlatform) {
        super();
        heartbeat();
        initialiseBuriedIntervalMillis();
        instanceId = anInstanceId;
        setUserId(aUserId);
        setSecurityGroupName(aSecurityGroupName);
        setPlatform(aPlatform);
    }

    public Instance(ReservationBase reservationBase) {
        super(reservationBase);
        heartbeat();
        initialiseBuriedIntervalMillis();
        if (reservationBase instanceof Instance) {
            Instance anInstance = (Instance) reservationBase;

            if (anInstance.getLastHeartbeatTimestamp() != null)
                lastHeartbeatTimestamp = anInstance.getLastHeartbeatTimestamp();
            setInstanceId(anInstance.getInstanceId());
            setImageSizeInMB(anInstance.getImageSizeInMB());
            setPrivateMacAddress(anInstance.getPrivateMacAddress());
            setVlanId(anInstance.getVlanId());
            setPublicIpAddress(anInstance.getPublicIpAddress());
            setPrivateIpAddress(anInstance.getPrivateIpAddress());
            setMemoryInKB(anInstance.getMemoryInKB());
            setVcpus(anInstance.getVcpus());
            setState(anInstance.getState());
            setLaunchTime(anInstance.getLaunchTime());
            setReasonForLastStateTransition(anInstance.getReasonForLastStateTransition());
            setLaunchIndex(anInstance.getLaunchIndex());
            setNodeId(anInstance.getNodeId());
            setHostname(anInstance.getHostname());
            setRestartRequested(anInstance.isRestartRequested());
            setInstanceActivityState(anInstance.getInstanceActivityState());
            setInstanceActivityStateChangeTimestamp(anInstance.getInstanceActivityStateChangeTimestamp());
        }
    }

    // TODO: pass this in via constructor from a Spring beans @Property
    protected void initialiseBuriedIntervalMillis() {
        buriedIntervalMillis = Long.parseLong(System.getProperty(INSTANCE_BURIED_INTERVAL_MILLIS, Long.toString(DEFAULT_BURIED_INTERVAL_MILLIS)));
    }

    @JsonIgnore
    protected long getBuriedIntervalMillis() {
        return buriedIntervalMillis;
    }

    @Override
    public String getType() {
        return getClass().getSimpleName();
    }

    public int getLaunchIndex() {
        return launchIndex;
    }

    public void setLaunchIndex(int aLaunchIndex) {
        launchIndex = aLaunchIndex;
    }

    public String getReasonForLastStateTransition() {
        return reasonForLastStateTransition;
    }

    public void setReasonForLastStateTransition(String aReason) {
        reasonForLastStateTransition = aReason;
    }

    public long getLaunchTime() {
        return launchTime;
    }

    public void setLaunchTime(long aLaunchTime) {
        launchTime = aLaunchTime;
    }

    public String getInstanceId() {
        return this.instanceId;
    }

    public void setInstanceId(String id) {
        this.instanceId = id;
    }

    public void setImageSizeInMB(int anImageSizeInMB) {
        this.imageSizeInMB = anImageSizeInMB;
    }

    public int getImageSizeInMB() {
        return imageSizeInMB;
    }

    public void setPrivateMacAddress(String aPrivateMacAddress) {
        this.privateMacAddress = aPrivateMacAddress;
    }

    public String getPrivateMacAddress() {
        return privateMacAddress;
    }

    public void setMemoryInKB(String aMemoryInKB) {
        this.memoryInKB = aMemoryInKB;
    }

    public String getMemoryInKB() {
        return memoryInKB;
    }

    public void setVcpus(int numberOfVcpu) {
        this.vcpus = numberOfVcpu;
    }

    public int getVcpus() {
        return vcpus;
    }

    public String getPublicIpAddress() {
        return publicIpAddress;
    }

    public void setPublicIpAddress(String aPublicIpAddress) {
        this.publicIpAddress = aPublicIpAddress;
    }

    public String getPrivateIpAddress() {
        return privateIpAddress;
    }

    public void setPrivateIpAddress(String aPrivateIpAddress) {
        this.privateIpAddress = aPrivateIpAddress;
    }

    public int getVlanId() {
        return vlanId;
    }

    public void setVlanId(int aVlanId) {
        this.vlanId = aVlanId;
    }

    @JsonIgnore
    public InstanceState getState() {
        return state;
    }

    @JsonIgnore
    public boolean isPending() {
        return InstanceState.PENDING.equals(getState());
    }

    @JsonIgnore
    // ignore here to avoid clobbering the timestamp when json is deserialised - class variable is now a @JsonProperty
    public void setState(InstanceState aState) {
        if (this.state != null && aState.ordinal() < this.state.ordinal()) {
            if (!InstanceState.CRASHED.equals(this.state) || !InstanceState.RUNNING.equals(aState)) {
                String message = String.format("Instance: %s, Trying to set state from %s to %s", this.instanceId, this.state, aState);
                throw new IllegalStateException(message);
            }
        }
        this.state = aState;
        this.stateChangeTimeStamp = System.currentTimeMillis();
    }

    public String getNodeId() {
        return nodeId;
    }

    public void setNodeId(String aNodeId) {
        this.nodeId = aNodeId;
    }

    public String getHostname() {
        return hostname;
    }

    public void setHostname(String aHostname) {
        this.hostname = aHostname;
    }

    public boolean isRestartRequested() {
        return restartRequested;
    }

    public void setRestartRequested(boolean isRestartRequested) {
        this.restartRequested = isRestartRequested;
    }

    public int getRegionCode() {
        return regionCode;
    }

    public void setRegionCode(int region) {
        this.regionCode = region;
    }

    /**
     * @return the availabilityZoneCode
     */
    public int getAvailabilityZoneCode() {
        return availabilityZoneCode;
    }

    /**
     * @param availabilityZoneCode
     *            the availabilityZoneCode to set
     */
    public void setAvailabilityZoneCode(int anAvailabilityZoneCode) {
        this.availabilityZoneCode = anAvailabilityZoneCode;
    }

    @Override
    public String getUrl() {
        return Instance.getUrl(getInstanceId());
    }

    public static String getUrl(String instanceIdentifier) {
        return String.format("%s:%s", ResourceSchemes.INSTANCE, instanceIdentifier);
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        Instance castOther = (Instance) obj;
        return new EqualsBuilder().appendSuper(super.equals(obj)).append(instanceId, castOther.instanceId).append(imageSizeInMB, castOther.imageSizeInMB).append(privateMacAddress, castOther.privateMacAddress).append(memoryInKB, castOther.memoryInKB)
                .append(vcpus, castOther.vcpus).append(state, castOther.state).append(launchTime, castOther.launchTime).append(reasonForLastStateTransition, castOther.reasonForLastStateTransition).append(launchIndex, castOther.launchIndex)
                .append(nodeId, castOther.nodeId).append(regionCode, castOther.regionCode).append(availabilityZoneCode, castOther.availabilityZoneCode).append(stateChangeTimeStamp, castOther.stateChangeTimeStamp)
                .append(instanceActivityState, castOther.instanceActivityState).append(instanceActivityStateChangeTimestamp, castOther.instanceActivityStateChangeTimestamp).isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(HASH_INITIAL, HASH_MULTIPLE).appendSuper(super.hashCode()).append(instanceId).append(imageSizeInMB).append(privateMacAddress).append(memoryInKB).append(vcpus).append(state).append(launchTime)
                .append(reasonForLastStateTransition).append(launchIndex).append(nodeId).append(regionCode).append(availabilityZoneCode).append(stateChangeTimeStamp).append(instanceActivityState).append(instanceActivityStateChangeTimestamp)
                .toHashCode();
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this);
    }

    @Override
    @JsonIgnore
    public boolean isConsumedBy(String consumerId) {
        return getUserId().equals(consumerId);
    }

    @Override
    public Long getLastHeartbeatTimestamp() {
        return lastHeartbeatTimestamp;
    }

    public void setLastHeartbeatTimestamp(Long aLastHeartbeatTimestamp) {
        this.lastHeartbeatTimestamp = aLastHeartbeatTimestamp;
    }

    @Override
    public void heartbeat() {
        lastHeartbeatTimestamp = System.currentTimeMillis();
    }

    @JsonIgnore
    public boolean isBuried() {
        return getLastHeartbeatTimestamp() + buriedIntervalMillis < System.currentTimeMillis();
    }

    @JsonIgnore
    @Override
    public boolean isDead() {
        return state == InstanceState.SHUTTING_DOWN || state == InstanceState.TERMINATED || isBuried();
    }

    @JsonIgnore
    @Override
    public boolean isDeleted() {
        return isBuried() && state.ordinal() >= InstanceState.FAILED.ordinal();
    }

    @JsonIgnore
    @Override
    public void setDeleted(boolean b) {
    }

    @JsonIgnore
    public boolean isTerminated() {
        return InstanceState.TERMINATED.equals(state);
    }

    @JsonIgnore
    public boolean isShuttingDown() {
        return InstanceState.SHUTTING_DOWN.equals(state);
    }

    @JsonIgnore
    public long getStateChangeTimestamp() {
        return stateChangeTimeStamp;
    }

    @Override
    public String getUriScheme() {
        return ResourceSchemes.INSTANCE.toString();
    }

    public InstanceAction anyActionRequired() {
        if (actionRequired == null)
            return InstanceAction.NONE;

        return actionRequired;
    }

    public void setActionRequired(InstanceAction instanceAction) {
        actionRequired = instanceAction;
    }

    private void setInstanceActivityStateChangeTimestamp(long aInstanceActivityStateChangeTimestamp) {
        instanceActivityStateChangeTimestamp = aInstanceActivityStateChangeTimestamp;
    }

    public long getInstanceActivityStateChangeTimestamp() {
        return instanceActivityStateChangeTimestamp;
    }

    @JsonIgnore
    public void setInstanceActivityState(InstanceActivityState aInstanceActivityState) {
        instanceActivityState = aInstanceActivityState;
        setInstanceActivityStateChangeTimestamp(System.currentTimeMillis());
    }

    @JsonIgnore
    public InstanceActivityState getInstanceActivityState() {
        return instanceActivityState == null ? instanceActivityState.GREEN : instanceActivityState;
    }
}
