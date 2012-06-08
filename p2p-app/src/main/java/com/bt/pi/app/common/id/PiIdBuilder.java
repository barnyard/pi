package com.bt.pi.app.common.id;

import java.math.BigInteger;
import java.util.Arrays;
import java.util.Random;

import javax.annotation.Resource;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.stereotype.Component;

import com.bt.pi.app.common.entities.AvailabilityZones;
import com.bt.pi.app.common.entities.Regions;
import com.bt.pi.app.common.resource.PiQueue;
import com.bt.pi.app.common.util.Base62Utils;
import com.bt.pi.core.entity.Locatable;
import com.bt.pi.core.id.KoalaIdFactory;
import com.bt.pi.core.id.PId;

@Component
public class PiIdBuilder {
    private static final String S_S = "%s-%s";
    private static final int BITS_IN_HEX_DIGIT = 4;
    private static final int HEX_RADIX = 16;
    private static final Log LOG = LogFactory.getLog(PiIdBuilder.class);
    private static final int NUM_BITS_IN_GLOBAL_AVAILABILITY_ZONE_CODE = 16;
    private static final int BASE_62_EC2_ID_BIT_COUNT = 44;
    private static final int STANDARD_EC2_ID_BIT_COUNT = 32;
    private static final int EC2_ID_STRING_LENGTH = 8;
    private KoalaIdFactory koalaIdFactory;
    private Random rand;

    public PiIdBuilder() {
        koalaIdFactory = null;
        rand = new Random();
    }

    @Resource
    public void setKoalaIdFactory(KoalaIdFactory aKoalaIdFactory) {
        koalaIdFactory = aKoalaIdFactory;
    }

    public PId getNodeIdFromNodeId(String nodeId) {
        return koalaIdFactory.buildPIdFromHexString(nodeId);
    }

    public PId getPId(String url) {
        return koalaIdFactory.buildPId(url);
    }

    public PId getPId(Locatable locatable) {
        return koalaIdFactory.buildPId(locatable);
    }

    public PId getPIdForEc2AvailabilityZone(Locatable locateable) {
        int globalAvailabilityZoneCode = getGlobalAvailabilityZoneCodeFromEc2Id(locateable.getUrl());
        return koalaIdFactory.buildPId(locateable).forGlobalAvailablityZoneCode(globalAvailabilityZoneCode);
    }

    public PId getPIdForEc2AvailabilityZone(String ec2Uri) {
        int globalAvailabilityZoneCode = getGlobalAvailabilityZoneCodeFromEc2Id(ec2Uri);
        return koalaIdFactory.buildPId(ec2Uri).forGlobalAvailablityZoneCode(globalAvailabilityZoneCode);
    }

    public PId getPiQueuePId(PiQueue queue) {
        return getPId(queue.getUrl());
    }

    public PId getRegionsId() {
        return getPId(Regions.URL);
    }

    public PId getAvailabilityZonesId() {
        return getPId(AvailabilityZones.URL);
    }

    /*
     * Basically takes <foo>-<base62String>
     * chops off the first bit.. then does the regular avz extraction.
     */
    public int getGlobalAvailabilityZoneCodeFromEc2Id(String ec2Id) {
        String[] parts = ec2Id.split("-");
        LOG.debug("Ec2Id" + ec2Id + " ec2parts: " + Arrays.toString(parts));
        if (parts.length != 2) {
            throw new IllegalArgumentException("Bad ec2 id: " + ec2Id);
        }

        if (parts[1].length() > EC2_ID_STRING_LENGTH) {
            throw new IllegalArgumentException("ec2 id too long: " + ec2Id);
        }

        return getGlobalAvailabilityZoneCodeFromBase62Number(parts[1]);
    }

    public int getGlobalAvailabilityZoneCodeFromBase62Number(String base62String) {
        String maxIdValueHex = org.apache.commons.lang.StringUtils.rightPad("", BASE_62_EC2_ID_BIT_COUNT / BITS_IN_HEX_DIGIT, "F");
        BigInteger num = Base62Utils.decodeBase62(base62String);
        if (num.compareTo(new BigInteger(maxIdValueHex, HEX_RADIX)) > 0)
            throw new IllegalArgumentException(String.format("ec2 identifier out of range: %s", base62String));

        int bitsToShift = BASE_62_EC2_ID_BIT_COUNT - NUM_BITS_IN_GLOBAL_AVAILABILITY_ZONE_CODE;
        BigInteger availabilityNum = num.shiftRight(bitsToShift);
        return availabilityNum.intValue();
    }

    public String generateStandardEc2Id(String prefix) {
        BigInteger randomId = new BigInteger(STANDARD_EC2_ID_BIT_COUNT, rand);
        String paddedId = padIdString(randomId.toString(HEX_RADIX), EC2_ID_STRING_LENGTH);
        return String.format(S_S, prefix, paddedId);
    }

    public String generateBase62Ec2Id(String prefix, int globalAvailabilityZoneCode) {
        BigInteger randomId = new BigInteger(BASE_62_EC2_ID_BIT_COUNT - NUM_BITS_IN_GLOBAL_AVAILABILITY_ZONE_CODE, rand);
        BigInteger randomIdWithGlobalAvailabilityZoneCode = addGlobalAvailabilityZoneCodeToNumber(globalAvailabilityZoneCode, randomId);
        String base62Id = Base62Utils.encodeToBase62(randomIdWithGlobalAvailabilityZoneCode);

        String paddedBase62Id = padIdString(base62Id, EC2_ID_STRING_LENGTH);

        String res = String.format(S_S, prefix, paddedBase62Id);
        LOG.debug(String.format("Generated ec2 id: " + res));
        return res;
    }

    protected BigInteger addGlobalAvailabilityZoneCodeToNumber(int globalAvailabilityZoneCode, BigInteger number) {
        BigInteger availabilityZoneNum = new BigInteger(String.valueOf(globalAvailabilityZoneCode)).shiftLeft(BASE_62_EC2_ID_BIT_COUNT - NUM_BITS_IN_GLOBAL_AVAILABILITY_ZONE_CODE);
        return availabilityZoneNum.add(number);
    }

    private String padIdString(String str, int minCharLength) {
        return StringUtils.leftPad(str, minCharLength, '0');
    }
}