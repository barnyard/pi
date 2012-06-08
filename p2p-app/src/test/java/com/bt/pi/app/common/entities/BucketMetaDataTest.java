package com.bt.pi.app.common.entities;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Calendar;
import java.util.Locale;

import org.junit.Test;

import com.bt.pi.core.parser.KoalaJsonParser;

public class BucketMetaDataTest {
    private String bucketName = "testBucket";

    @Test
    public void testBucketMetaData() {
        // setup

        // act
        BucketMetaData result = new BucketMetaData(bucketName);

        // assert
        assertEquals(bucketName, result.getName());
        assertDate(result.getCreationDate());
        assertEquals(CannedAclPolicy.PRIVATE, result.getCannedAclPolicy());
        assertEquals(BucketMetaData.DEFAULT_LOCATION, result.getLocation());
    }

    @Test
    public void testEquals() throws InterruptedException {
        // setup
        BucketMetaData bucketMetaData1 = new BucketMetaData(bucketName);
        Thread.sleep(200);
        BucketMetaData bucketMetaData2 = new BucketMetaData(bucketName);

        // act
        boolean result = bucketMetaData1.equals(bucketMetaData2);

        // assert
        assertTrue(result);
    }

    @Test
    public void testNotEquals() throws InterruptedException {
        // setup
        BucketMetaData bucketMetaData1 = new BucketMetaData(bucketName);
        Thread.sleep(200);
        BucketMetaData bucketMetaData2 = new BucketMetaData(bucketName + "2");

        // act
        boolean result = bucketMetaData1.equals(bucketMetaData2);

        // assert
        assertFalse(result);
    }

    @Test
    public void testFromName() {
        // setup

        // act
        BucketMetaData result = BucketMetaData.fromName(bucketName);

        // assert
        assertEquals(new BucketMetaData(bucketName), result);
    }

    @Test
    public void testConstructorWithCannedAclPolicy() {
        // setup
        CannedAclPolicy cannedAclPolicy = CannedAclPolicy.BUCKET_OWNER_FULL_CONTROL;

        // act
        BucketMetaData result = new BucketMetaData(bucketName, cannedAclPolicy);

        // assert
        assertEquals(bucketName, result.getName());
        assertEquals(cannedAclPolicy, result.getCannedAclPolicy());
        assertEquals(BucketMetaData.DEFAULT_LOCATION, result.getLocation());
    }

    @Test
    public void testConstructorWithCannedAclPolicyAndLocation() {
        // setup
        CannedAclPolicy cannedAclPolicy = CannedAclPolicy.BUCKET_OWNER_FULL_CONTROL;

        // act
        BucketMetaData result = new BucketMetaData(bucketName, cannedAclPolicy, "location");

        // assert
        assertEquals(bucketName, result.getName());
        assertEquals(cannedAclPolicy, result.getCannedAclPolicy());
        assertEquals("location", result.getLocation());
    }

    private void assertDate(Calendar creationDate) {
        long timeInMillis = creationDate.getTimeInMillis();
        long now = System.currentTimeMillis();
        assertTrue(Math.abs(timeInMillis - now) < 200);
    }

    @Test
    public void shouldBeAbleToRoundTripToFromJsonNotDeleted() {
        // setup
        KoalaJsonParser koalaJsonParser = new KoalaJsonParser();
        String location = "lalaland";
        CannedAclPolicy cannedAclPolicy = CannedAclPolicy.AWS_EXEC_READ;
        BucketMetaData bucketMetaData = new BucketMetaData(bucketName, cannedAclPolicy, location);

        // act
        String json = koalaJsonParser.getJson(bucketMetaData);
        BucketMetaData reverse = (BucketMetaData) koalaJsonParser.getObject(json, BucketMetaData.class);

        // assert
        assertEquals(bucketMetaData.getCannedAclPolicy(), reverse.getCannedAclPolicy());
        assertEquals(bucketMetaData.getName(), reverse.getName());
        assertEquals(bucketMetaData.getCreationDate(), reverse.getCreationDate());
        assertEquals(bucketMetaData.getLocation(), reverse.getLocation());
        assertEquals(bucketMetaData.isDeleted(), reverse.isDeleted());
    }

    @Test
    public void shouldBeAbleToRoundTripToFromJsonDeleted() {
        // setup
        KoalaJsonParser koalaJsonParser = new KoalaJsonParser();
        String location = "lalaland";
        CannedAclPolicy cannedAclPolicy = CannedAclPolicy.AWS_EXEC_READ;
        BucketMetaData bucketMetaData = new BucketMetaData(bucketName, cannedAclPolicy, location);
        bucketMetaData.setDeleted(true);

        // act
        String json = koalaJsonParser.getJson(bucketMetaData);
        BucketMetaData reverse = (BucketMetaData) koalaJsonParser.getObject(json, BucketMetaData.class);

        // assert
        assertEquals(bucketMetaData.getCannedAclPolicy(), reverse.getCannedAclPolicy());
        assertEquals(bucketMetaData.getName(), reverse.getName());
        assertEquals(bucketMetaData.getCreationDate(), reverse.getCreationDate());
        assertEquals(bucketMetaData.getLocation(), reverse.getLocation());
        assertEquals(bucketMetaData.isDeleted(), reverse.isDeleted());
    }

    @Test
    public void testGetUrl() {
        // setup
        BucketMetaData bucketMetaData = new BucketMetaData(bucketName);

        // act
        String result = bucketMetaData.getUrl();

        // assert
        assertEquals(ResourceSchemes.BUCKET_META_DATA + ":" + bucketName.toLowerCase(Locale.getDefault()), result);
    }
}
