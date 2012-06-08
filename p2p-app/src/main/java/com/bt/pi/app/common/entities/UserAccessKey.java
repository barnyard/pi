/* (c) British Telecommunications plc, 2009, All Rights Reserved */
package com.bt.pi.app.common.entities;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;

import com.bt.pi.core.entity.Backupable;
import com.bt.pi.core.entity.EntityScope;
import com.bt.pi.core.entity.PiEntityBase;
import com.bt.pi.core.scope.NodeScope;

@Backupable
@EntityScope(scope = NodeScope.GLOBAL)
public class UserAccessKey extends PiEntityBase {
    private String accessKey;
    private String username;

    public UserAccessKey() {
    }

    public UserAccessKey(String aUsername, String anAccessKey) {
        username = aUsername;
        accessKey = anAccessKey;
    }

    public void setUsername(String aUsername) {
        username = aUsername;
    }

    public void setAccessKey(String anAccessKey) {
        accessKey = anAccessKey;
    }

    public String getUsername() {
        return username;
    }

    public String getAccessKey() {
        return accessKey;
    }

    @Override
    public String getType() {
        return getClass().getSimpleName();
    }

    @Override
    public String getUrl() {
        return UserAccessKey.getUrl(accessKey);
    }

    public static String getUrl(String entityKey) {
        return String.format("%s:%s", ResourceSchemes.USER_ACCESS_KEY.toString(), entityKey);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (!(obj instanceof UserAccessKey))
            return false;
        UserAccessKey other = (UserAccessKey) obj;

        return new EqualsBuilder().append(username, other.username).append(accessKey, other.accessKey).isEquals();
    }

    @Override
    public int hashCode() {
        final int PRIME = 31;
        return new HashCodeBuilder(PRIME, PRIME).append(username).append(accessKey).toHashCode();
    }

    @Override
    public String getUriScheme() {
        return ResourceSchemes.USER_ACCESS_KEY.toString();
    }
}
