/* (c) British Telecommunications plc, 2009, All Rights Reserved */
package com.bt.pi.app.common.entities;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.codehaus.jackson.annotate.JsonIgnore;
import org.codehaus.jackson.annotate.JsonProperty;

import com.bt.pi.core.entity.Backupable;
import com.bt.pi.core.entity.Deletable;
import com.bt.pi.core.entity.EntityScope;
import com.bt.pi.core.entity.PiEntityBase;
import com.bt.pi.core.scope.NodeScope;

@Backupable
@EntityScope(scope = NodeScope.GLOBAL)
public class User extends PiEntityBase implements Deletable {
    static final String DEFAULT_MAX_INSTANCES = "5";
    static final String DEFAULT_MAX_CORES = "8";
    private static final String SEMI_COLON = ";";
    private String username;
    private String realName;
    private String emailAddress;
    private boolean enabled;
    private String apiAccessKey;
    private String apiSecretKey;
    private byte[] certificate;
    private String externalRefId;
    private boolean deleted;

    @JsonProperty
    private Set<String> instanceIds;
    private Set<String> terminatedInstanceIds;
    private Set<String> securityGroupIds;
    private Set<String> volumeIds;
    private Set<String> bucketNames;
    private Set<String> imageIds;
    private Set<KeyPair> keyPairs;
    private Set<String> snapshotIds;
    @JsonProperty
    private Integer maxInstances;
    @JsonProperty
    private Integer maxCores;

    public User() {
        this.instanceIds = new HashSet<String>();
        this.terminatedInstanceIds = new HashSet<String>();
        this.securityGroupIds = new HashSet<String>();
        this.volumeIds = new HashSet<String>();
        this.bucketNames = new HashSet<String>();
        this.imageIds = new HashSet<String>();
        this.keyPairs = new HashSet<KeyPair>();
        this.snapshotIds = new HashSet<String>();
        enabled = true;
    }

    public User(String aUsername, String anApiAccessKey, String anApiSecretKey) {
        this();
        username = aUsername;
        apiAccessKey = anApiAccessKey;
        apiSecretKey = anApiSecretKey;
    }

    public User(String aUsername, String aRealName, String anEmailAddress, boolean isEnabled) {
        this();
        username = aUsername;
        realName = aRealName;
        emailAddress = anEmailAddress;
        enabled = isEnabled;
    }

    public User(String aUsername, String aRealName, String anEmailAddress, boolean isEnabled, String theExternalRefId) {
        this(aUsername, aRealName, anEmailAddress, isEnabled);
        setExternalRefId(theExternalRefId);
    }

    public byte[] getCertificate() {
        return certificate;
    }

    public void setCertificate(byte[] aCertificate) {
        certificate = aCertificate;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String aUsername) {
        this.username = aUsername;
    }

    public String getApiAccessKey() {
        return apiAccessKey;
    }

    public void setApiAccessKey(String anAwsAccessKey) {
        this.apiAccessKey = anAwsAccessKey;
    }

    public String getApiSecretKey() {
        return apiSecretKey;
    }

    public void setApiSecretKey(String anAwsSecretKey) {
        this.apiSecretKey = anAwsSecretKey;
    }

    public void addInstance(String instanceId) {
        this.instanceIds.add(instanceId);
    }

    public void addInstance(String instanceId, String instanceType) {
        this.instanceIds.add(instanceId + SEMI_COLON + instanceType);
    }

    // this returns an array to make it immutable, use addInstance and removeInstance to alter
    @JsonIgnore
    public String[] getInstanceIds() {
        Set<String> parseInstanceIds = parseInstanceIds();
        return parseInstanceIds.toArray(new String[parseInstanceIds.size()]);
    }

    private Set<String> parseInstanceIds() {
        Set<String> result = new HashSet<String>();
        for (String instanceId : instanceIds) {
            if (instanceId.contains(SEMI_COLON)) {
                result.add(instanceId.split(SEMI_COLON)[0]);
            } else
                result.add(instanceId);
        }
        return result;
    }

    public Set<String> getTerminatedInstanceIds() {
        return terminatedInstanceIds;
    }

    public boolean terminateInstance(String instanceId) {
        boolean result = false;
        result |= removeInstance(instanceIds, instanceId);
        result |= terminatedInstanceIds.add(instanceId);
        return result;
    }

    public Set<String> getVolumeIds() {
        return this.volumeIds;
    }

    public Set<String> getImageIds() {
        return this.imageIds;
    }

    public Set<String> getBucketNames() {
        return bucketNames;
    }

    public Set<String> getSecurityGroupIds() {
        return securityGroupIds;
    }

    public Set<KeyPair> getKeyPairs() {
        return keyPairs;
    }

    public Set<String> getSnapshotIds() {
        return snapshotIds;
    }

    public KeyPair getKeyPair(String keyName) {
        if (keyName != null) {
            Iterator<KeyPair> iterator = keyPairs.iterator();
            while (iterator.hasNext()) {
                KeyPair keyPair = iterator.next();
                if (keyName.equals(keyPair.getKeyName()))
                    return keyPair;
            }
        }

        return null;
    }

    @Override
    public String toString() {
        return String.format("[user:[username=%s,realName=%s,emailAddress=%s,isEnabled=%s,apiAccessKey=%s,apiSecretKey=%s,instanceIds=%s,terminatedInstanceIds=%s,volumeIds=%s,bucketNames=%s,certificate=%s,imageIds=%s,keyPairs=%s,snapshotIds=%s]]",
                username, realName, emailAddress, enabled, apiAccessKey, apiSecretKey, StringUtils.join(instanceIds, ','), StringUtils.join(terminatedInstanceIds, ','), StringUtils.join(volumeIds, ','), StringUtils.join(bucketNames, ','),
                Arrays.toString(certificate), StringUtils.join(imageIds, ','), StringUtils.join(keyPairs, ','), StringUtils.join(snapshotIds, ','));
    }

    @Override
    public int hashCode() {
        final int PRIME = 31;
        return new HashCodeBuilder(PRIME, PRIME).append(username).append(realName).append(emailAddress).append(enabled).append(apiAccessKey).append(apiSecretKey).append(instanceIds).append(terminatedInstanceIds).append(volumeIds)
                .append(securityGroupIds).append(bucketNames).append(certificate).append(imageIds).append(keyPairs).append(snapshotIds).toHashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (!(obj instanceof User))
            return false;
        User other = (User) obj;

        return new EqualsBuilder().append(username, other.username).append(realName, other.realName).append(emailAddress, other.emailAddress).append(enabled, other.enabled).append(apiAccessKey, other.apiAccessKey)
                .append(apiSecretKey, other.apiSecretKey).append(instanceIds, other.instanceIds).append(terminatedInstanceIds, other.terminatedInstanceIds).append(volumeIds, other.volumeIds).append(securityGroupIds, other.securityGroupIds)
                .append(bucketNames, other.bucketNames).append(certificate, other.certificate).append(imageIds, other.imageIds).append(keyPairs, other.keyPairs).append(externalRefId, other.externalRefId).append(snapshotIds, other.snapshotIds)
                .isEquals();
    }

    @Override
    public String getType() {
        return getClass().getSimpleName();
    }

    @Override
    public String getUrl() {
        return User.getUrl(getUsername());
    }

    public static String getUrl(String entityKey) {
        return String.format("%s:%s", ResourceSchemes.USER, entityKey);
    }

    public String getRealName() {
        return realName;
    }

    public void setRealName(String aRealName) {
        this.realName = aRealName;
    }

    public String getEmailAddress() {
        return emailAddress;
    }

    public void setEmailAddress(String anEmailAddress) {
        this.emailAddress = anEmailAddress;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean isEnabled) {
        this.enabled = isEnabled;
    }

    public boolean isDeleted() {
        return deleted;
    }

    public void setDeleted(boolean isDeleted) {
        deleted = isDeleted;
    }

    public String getExternalRefId() {
        return externalRefId;
    }

    public void setExternalRefId(String theExternalRefId) {
        this.externalRefId = theExternalRefId;
    }

    @Override
    public String getUriScheme() {
        return ResourceSchemes.USER.toString();
    }

    @JsonIgnore
    public int getMaxCores() {
        if (null != maxCores)
            return maxCores;
        return Integer.parseInt(System.getProperty("user.max.cores", DEFAULT_MAX_CORES));
    }

    @JsonIgnore
    public void setMaxCores(Integer i) {
        this.maxCores = i;
    }

    @JsonIgnore
    public int getMaxInstances() {
        if (null != maxInstances)
            return maxInstances;
        return Integer.parseInt(System.getProperty("user.max.instances", DEFAULT_MAX_INSTANCES));
    }

    @JsonIgnore
    public void setMaxInstances(Integer i) {
        this.maxInstances = i;
    }

    public boolean removeInstance(String instanceId) {
        boolean result = false;
        result |= removeInstance(this.instanceIds, instanceId);
        result |= removeInstance(this.terminatedInstanceIds, instanceId);
        return result;
    }

    private boolean removeInstance(Set<String> set, String instanceIdToBeRemoved) {
        Set<String> toBeRemoved = new HashSet<String>();
        for (String instanceId : set)
            if (instanceIdEquals(instanceId, instanceIdToBeRemoved))
                toBeRemoved.add(instanceId);
        return set.removeAll(toBeRemoved);
    }

    private boolean instanceIdEquals(String with, String without) {
        if (with.equals(without))
            return true;
        if (with.contains(SEMI_COLON))
            return with.split(SEMI_COLON)[0].equals(without);
        return false;
    }

    public void addTerminatedInstance(String instanceId) {
        this.terminatedInstanceIds.add(instanceId);
    }

    public boolean hasInstance(String instanceId) {
        return this.parseInstanceIds().contains(instanceId);
    }

    public void addInstances(Collection<String> ids) {
        this.instanceIds.addAll(ids);
    }

    @JsonIgnore
    public String[] getInstanceTypes() {
        return parseInstanceTypes();
    }

    private String[] parseInstanceTypes() {
        List<String> result = new ArrayList<String>();
        for (String s : instanceIds)
            if (s.contains(SEMI_COLON))
                result.add(s.split(SEMI_COLON)[1]);
            else
                result.add(InstanceTypes.UNKNOWN);
        return result.toArray(new String[result.size()]);
    }
}
