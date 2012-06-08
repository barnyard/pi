package com.bt.pi.app.common.entities;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.apache.commons.lang.builder.ToStringBuilder;

import com.bt.pi.core.entity.Deletable;
import com.bt.pi.core.entity.PiEntityBase;

public class NodeVolumeBackupRecord extends PiEntityBase implements Deletable {
    private static final int MULTIPLE = 37;
    private static final int INITIAL = 17;
    private static final String SCHEME = "volbackup";
    private boolean deleted;
    private String nodeId;
    private long lastBackup;

    public NodeVolumeBackupRecord() {
    }

    public NodeVolumeBackupRecord(String aNodeId) {
        super();
        this.nodeId = aNodeId;
    }

    @Override
    public String getType() {
        return getClass().getSimpleName();
    }

    @Override
    public String getUriScheme() {
        return SCHEME;
    }

    @Override
    public String getUrl() {
        return NodeVolumeBackupRecord.getUrl(nodeId);
    }

    public static String getUrl(String aNodeId) {
        return String.format("%s:%s", SCHEME, aNodeId);
    }

    @Override
    public boolean isDeleted() {
        return deleted;
    }

    @Override
    public void setDeleted(boolean b) {
        this.deleted = b;
    }

    public void setNodeId(String aNodeId) {
        this.nodeId = aNodeId;
    }

    public String getNodeId() {
        return this.nodeId;
    }

    public void setLastBackup(long lastBackupTime) {
        this.lastBackup = lastBackupTime;
    }

    public long getLastBackup() {
        return this.lastBackup;
    }

    @Override
    public boolean equals(final Object other) {
        if (!(other instanceof NodeVolumeBackupRecord))
            return false;
        NodeVolumeBackupRecord castOther = (NodeVolumeBackupRecord) other;
        return new EqualsBuilder().append(deleted, castOther.deleted).append(nodeId, castOther.nodeId).append(lastBackup, castOther.lastBackup).isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(INITIAL, MULTIPLE).append(deleted).append(nodeId).append(lastBackup).toHashCode();
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this).append("nodeId", nodeId).append("lastBackup", lastBackup).toString();
    }
}
