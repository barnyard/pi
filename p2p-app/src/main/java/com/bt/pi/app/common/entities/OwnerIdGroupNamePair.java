/* (c) British Telecommunications plc, 2009, All Rights Reserved */
package com.bt.pi.app.common.entities;

public class OwnerIdGroupNamePair {
    private String ownerId;
    private String groupName;

    public OwnerIdGroupNamePair() {
    }

    public OwnerIdGroupNamePair(String anOwnerId, String aGroupName) {
        ownerId = anOwnerId;
        groupName = aGroupName;
    }

    public String getSecurityGroupId() {
        return String.format(SecurityGroup.SEC_GROUP_ID_FORMAT_STRING, ownerId, groupName);
    }

    public String getGroupName() {
        return groupName;
    }

    public void setGroupName(String aGroupName) {
        this.groupName = aGroupName;
    }

    public String getOwnerId() {
        return ownerId;
    }

    public void setOwnerId(String anOwnerId) {
        this.ownerId = anOwnerId;
    }

    @Override
    public int hashCode() {
        final int PRIME = 31;
        int result = 1;
        result = PRIME * result + ((groupName == null) ? 0 : groupName.hashCode());
        result = PRIME * result + ((ownerId == null) ? 0 : ownerId.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        final OwnerIdGroupNamePair other = (OwnerIdGroupNamePair) obj;
        if (groupName == null) {
            if (other.groupName != null)
                return false;
        } else if (!groupName.equals(other.groupName))
            return false;
        if (ownerId == null) {
            if (other.ownerId != null)
                return false;
        } else if (!ownerId.equals(other.ownerId))
            return false;
        return true;
    }

    @Override
    public String toString() {
        return String.format(SecurityGroup.SEC_GROUP_ID_FORMAT_STRING, ownerId, groupName);
    }
}
