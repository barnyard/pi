package com.bt.pi.app.common.util;

import java.security.MessageDigest;
import java.security.SecureRandom;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.bouncycastle.util.encoders.UrlBase64;
import org.springframework.stereotype.Component;

@Component
public class DigestUtils {
    private static final Log LOG = LogFactory.getLog(DigestUtils.class);

    public DigestUtils() {

    }

    public String getDigestBase64(String input, HashDigest hashDigest, boolean randomize) {
        LOG.debug(String.format("getDigestBase64(%s, %s, %s)", input, hashDigest, randomize));
        byte[] messageDigest = getMessageDigest(input, hashDigest, randomize);
        return new String(UrlBase64.encode(messageDigest));
    }

    public String getDigestBase62(String input, HashDigest hashDigest, boolean randomize) {
        LOG.debug(String.format("getDigestBase62(%s, %s, %s)", input, hashDigest, randomize));
        byte[] messageDigest = getMessageDigest(input, hashDigest, randomize);
        return Base62Utils.encodeToBase62(messageDigest);
    }

    private byte[] getMessageDigest(String input, HashDigest hashDigest, boolean randomize) {
        LOG.debug(String.format("getMessageDigest(%s, %s, %s)", input, hashDigest, randomize));
        byte[] inputBytes = input.getBytes();
        MessageDigest messageDigest = hashDigest.getMessageDigest();
        messageDigest.update(inputBytes);
        if (randomize) {
            SecureRandom random = new SecureRandom();
            random.setSeed(System.currentTimeMillis());
            byte[] randomBytes = random.generateSeed(inputBytes.length);
            messageDigest.update(randomBytes);
        }

        return messageDigest.digest();
    }

}
