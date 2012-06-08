/* (c) British Telecommunications plc, 2009, All Rights Reserved */
package com.bt.pi.app.common.entities;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;

public class KeyPair {
    private static final int HASH_MULTIPLE = 11;
    private static final int HASH_INITIAL = 7;

    private String keyName;
    private String keyFingerprint;
    private String keyMaterial;

    public KeyPair() {
        super();
    }

    public KeyPair(String aKeyName) {
        this(aKeyName, null, null);
    }

    public KeyPair(String aKeyName, String aKeyFingerprint, String aKeyMaterial) {
        super();
        this.keyName = aKeyName;
        this.keyFingerprint = aKeyFingerprint;
        this.keyMaterial = aKeyMaterial;
    }

    public String getKeyFingerprint() {
        return keyFingerprint;
    }

    public void setKeyFingerprint(String aKeyFingerprint) {
        this.keyFingerprint = aKeyFingerprint;
    }

    public String getKeyMaterial() {
        return keyMaterial;
    }

    public void setKeyMaterial(String aKeyMaterial) {
        this.keyMaterial = aKeyMaterial;
    }

    public String getKeyName() {
        return keyName;
    }

    public void setKeyName(String aKeyName) {
        this.keyName = aKeyName;
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(HASH_INITIAL, HASH_MULTIPLE).append(keyName).toHashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        KeyPair other = (KeyPair) obj;
        return new EqualsBuilder().append(keyName, other.keyName).isEquals();
    }

    @Override
    public String toString() {
        return String.format("[keyPair:[keyName=%s,keyFingerPrint=%s,keyMaterial=%s]]", keyName, keyFingerprint, keyMaterial);
    }
}
