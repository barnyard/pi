/* (c) British Telecommunications plc, 2009, All Rights Reserved */
package com.bt.pi.app.common.entities;

import java.util.Calendar;
import java.util.Locale;

import org.codehaus.jackson.annotate.JsonIgnore;
import org.codehaus.jackson.annotate.JsonProperty;

import com.bt.pi.core.entity.Deletable;
import com.bt.pi.core.entity.PiEntityBase;

/*
 * json pojo to store bucket info
 */

public class BucketMetaData extends PiEntityBase implements Deletable {
    protected static final String DEFAULT_LOCATION = "EU";
    @JsonProperty
    private String name;
    @JsonProperty
    private Calendar creationDate;
    @JsonProperty
    private CannedAclPolicy cannedAclPolicy;
    /**
     * NOTE: This is the same as region name.
     */
    @JsonProperty
    private String location;
    @JsonProperty
    private boolean deleted;
    @JsonProperty
    private String userName;

    public BucketMetaData() {
        super();
    }

    public BucketMetaData(String bucketName) {
        this(bucketName, CannedAclPolicy.PRIVATE, DEFAULT_LOCATION);
    }

    public BucketMetaData(String bucketName, CannedAclPolicy aCannedAclPolicy) {
        this(bucketName, aCannedAclPolicy, DEFAULT_LOCATION);
    }

    public BucketMetaData(String bucketName, CannedAclPolicy aCannedAclPolicy, String aLocation) {
        this.name = bucketName;
        resetCreationDate();
        if (null == aCannedAclPolicy)
            this.cannedAclPolicy = CannedAclPolicy.PRIVATE;
        else
            this.cannedAclPolicy = aCannedAclPolicy;
        this.location = aLocation;
    }

    public CannedAclPolicy getCannedAclPolicy() {
        return this.cannedAclPolicy;
    }

    public Calendar getCreationDate() {
        return creationDate;
    }

    public void resetCreationDate() {
        this.creationDate = Calendar.getInstance();
    }

    public String getLocation() {
        return this.location;
    }

    public static BucketMetaData fromName(String bucketName) {
        return new BucketMetaData(bucketName);
    }

    public void setCannedAccessPolicy(CannedAclPolicy aCannedAclPolicy) {
        this.cannedAclPolicy = aCannedAclPolicy;
    }

    public void setLocation(String aLocation) {
        this.location = aLocation;
    }

    public String getName() {
        return name;
    }

    public void setName(String aName) {
        this.name = aName;
    }

    public boolean isDeleted() {
        return this.deleted;
    }

    public void setDeleted(boolean b) {
        this.deleted = b;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        BucketMetaData other = (BucketMetaData) obj;
        if (getName() == null) {
            if (other.getName() != null)
                return false;
        } else if (!getName().equals(other.getName()))
            return false;
        return true;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((name == null) ? 0 : name.hashCode());
        return result;
    }

    @Override
    public String getType() {
        return this.getClass().getSimpleName();
    }

    @Override
    @JsonIgnore
    public String getUrl() {
        return getUrl(getName().toLowerCase(Locale.getDefault()));
    }

    public static String getUrl(String entityKey) {
        return String.format("%s:%s", ResourceSchemes.BUCKET_META_DATA, entityKey);
    }

    public void setUsername(String aUserName) {
        this.userName = aUserName;
    }

    public String getUsername() {
        return this.userName;
    }

    @Override
    public String toString() {
        return "BucketMetaData [cannedAclPolicy=" + cannedAclPolicy + ", creationDate=" + creationDate + ", deleted=" + deleted + ", location=" + location + ", name=" + name + ", userName=" + userName + "]";
    }

    @Override
    public String getUriScheme() {
        return ResourceSchemes.BUCKET_META_DATA.toString();
    }
}
