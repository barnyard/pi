/* (c) British Telecommunications plc, 2010, All Rights Reserved */
package com.bt.pi.app.common.entities;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;

import com.bt.pi.core.entity.PiEntityBase;

public class ManagementUsers extends PiEntityBase {
    public static final String SCHEME = "management";
    public static final String URL = SCHEME + ":users";

    private final Map<String, ManagementUser> userMap;

    public ManagementUsers() {
        userMap = new HashMap<String, ManagementUser>();
    }

    @Override
    public String getType() {
        return this.getClass().getSimpleName();
    }

    @Override
    public String getUrl() {
        return URL;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (!(obj instanceof ManagementUsers))
            return false;
        ManagementUsers other = (ManagementUsers) obj;

        return new EqualsBuilder().append(userMap, other.getUserMap()).isEquals();
    }

    @Override
    public int hashCode() {
        final int PRIME = 31;
        return new HashCodeBuilder(PRIME, PRIME).append(userMap).toHashCode();
    }

    public Map<String, ManagementUser> getUserMap() {
        return userMap;
    }

    @Override
    public String getUriScheme() {
        return SCHEME;
    }
}
