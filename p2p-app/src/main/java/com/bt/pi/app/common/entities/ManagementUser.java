/* (c) British Telecommunications plc, 2010, All Rights Reserved */
package com.bt.pi.app.common.entities;

import java.util.ArrayList;
import java.util.Collection;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;

public class ManagementUser {

    private final Collection<ManagementRoles> roles;
    private String username;
    private String password;

    public ManagementUser() {
        roles = new ArrayList<ManagementRoles>();
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String theUsername) {
        this.username = theUsername;

    }

    public Collection<ManagementRoles> getRoles() {
        return roles;
    }

    public void setPassword(String thePassword) {
        this.password = thePassword;
    }

    public String getPassword() {
        return this.password;
    }

    @Override
    public boolean equals(Object obj) {
        if (null == obj)
            return false;
        if (this == obj)
            return true;
        if (!(obj instanceof ManagementUser))
            return false;

        ManagementUser other = (ManagementUser) obj;

        return new EqualsBuilder().append(username, other.getUsername()).append(password, other.getPassword()).append(roles, other.getRoles()).isEquals();
    }

    @Override
    public int hashCode() {
        final int PRIME = 31;
        return new HashCodeBuilder(PRIME, PRIME).append(username).append(password).toHashCode();
    }
}
