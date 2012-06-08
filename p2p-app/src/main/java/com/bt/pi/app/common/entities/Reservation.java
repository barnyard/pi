/* (c) British Telecommunications plc, 2009, All Rights Reserved */
package com.bt.pi.app.common.entities;

import java.util.HashSet;
import java.util.Set;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.codehaus.jackson.annotate.JsonIgnore;

import com.bt.pi.core.entity.Deletable;

/**
 * A reservation is a what is created upon a run instance API command.
 */

public class Reservation extends ReservationBase implements Deletable {
    private static final String SCHEME = "res";
    private static final int HASH_MULTIPLE = 61;
    private static final int HASH_INITIAL = 37;
    private Set<String> instanceIds;
    private int minCount;
    private int maxCount;

    public Reservation() {
        super();
        instanceIds = new HashSet<String>();
    }

    public void addInstanceId(String instanceId) {
        instanceIds.add(instanceId);
    }

    public Set<String> getInstanceIds() {
        return instanceIds;
    }

    public void setInstanceIds(Set<String> anInstanceIds) {
        this.instanceIds = anInstanceIds;
    }

    public int getMaxCount() {
        return maxCount;
    }

    public void setMaxCount(int aMaxCount) {
        this.maxCount = aMaxCount;
    }

    public int getMinCount() {
        return minCount;
    }

    public void setMinCount(int aMinCount) {
        this.minCount = aMinCount;
    }

    public String getType() {
        return getClass().getSimpleName();
    }

    public String getUrl() {
        return Reservation.getUrl(getReservationId());
    }

    public static String getUrl(String entityKey) {
        return String.format(SCHEME + ":%s", entityKey);
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(HASH_INITIAL, HASH_MULTIPLE).append(instanceIds).append(maxCount).append(minCount).toHashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (!super.equals(obj))
            return false;
        if (getClass() != obj.getClass())
            return false;
        final Reservation other = (Reservation) obj;
        return new EqualsBuilder().append(instanceIds, other.instanceIds).append(maxCount, other.maxCount).append(minCount, other.minCount).isEquals();
    }

    @Override
    public String toString() {
        return String.format("%s,instanceIds=%s,maxCount=%s,minCount=%s", super.toString(), instanceIds, maxCount, minCount);
    }

    @Override
    public String getUriScheme() {
        return SCHEME;
    }

    /**
     * Reservation doesn't need to be in the dht. Implementing deletable makes sure that we can now garbage-collect
     * persisted instances
     */
    @JsonIgnore
    @Override
    public boolean isDeleted() {
        return true;
    }

    /**
     * Reservation doesn't need to be in the dht. Implementing deletable makes sure that we can now garbage-collect
     * persisted instances
     */
    @JsonIgnore
    @Override
    public void setDeleted(boolean b) {
        // do nothing
    }
}
