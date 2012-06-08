package com.bt.pi.app.common.util;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public enum HashDigest {
    MD5("MD5"), SHA1("SHA1"), SHA224("SHA-224"), SHA256("SHA-256");

    private static final Log LOG = LogFactory.getLog(HashDigest.class);

    private String digestName;

    private HashDigest(String aDigestName) {
        this.digestName = aDigestName;
    }

    public MessageDigest getMessageDigest() {
        try {
            return MessageDigest.getInstance(digestName);
        } catch (NoSuchAlgorithmException e) {
            LOG.error(String.format("Unable to create message digest for algorithm %s", this.name()), e);
            return null;
        }
    }

    @Override
    public String toString() {
        return digestName;
    }
}
