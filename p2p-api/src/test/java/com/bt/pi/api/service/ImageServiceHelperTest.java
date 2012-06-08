package com.bt.pi.api.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.when;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import com.bt.pi.app.common.entities.User;
import com.bt.pi.sss.client.PisssClient;
import com.bt.pi.sss.client.PisssClient.BucketAccess;
import com.bt.pi.sss.exception.BucketObjectNotFoundException;
import com.bt.pi.sss.exception.NoSuchBucketException;

@RunWith(MockitoJUnitRunner.class)
public class ImageServiceHelperTest {
    @InjectMocks
    private ImageServiceHelper imageServiceHelper = new ImageServiceHelper();
    @Mock
    private PisssClient pisssClient;
    private String bucketName = "bucketName";
    private String objectName = "objectName";
    @Mock
    private User user;
    private String username = "username";

    @Before
    public void before() {
        when(user.getUsername()).thenReturn(username);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testRetrieveManifestXmlFromPisssNoSuchBucket() {
        // setup
        when(pisssClient.getObjectFromBucket(bucketName, objectName, user)).thenThrow(new NoSuchBucketException());

        // act
        try {
            this.imageServiceHelper.retrieveManifestXmlFromPisss(bucketName, objectName, user);
            fail();
        } catch (IllegalArgumentException e) {
            // assert
            assertEquals(String.format("No such bucket: %s for user %s", bucketName, username), e.getMessage());
            throw e;
        }
    }

    @Test(expected = IllegalArgumentException.class)
    public void testRetrieveManifestXmlFromPisssBucketObjectNotFound() {
        // setup
        when(pisssClient.getObjectFromBucket(bucketName, objectName, user)).thenThrow(new BucketObjectNotFoundException());

        // act
        try {
            this.imageServiceHelper.retrieveManifestXmlFromPisss(bucketName, objectName, user);
            fail();
        } catch (IllegalArgumentException e) {
            // assert
            assertEquals(String.format("Manifest not found for user %s in bucket %s", username, bucketName), e.getMessage());
            throw e;
        }
    }

    @Test(expected = NotAuthorizedException.class)
    public void testRetrieveBucketAccessFromPisssNoAccess() {
        // setup
        when(pisssClient.getBucketAccess(bucketName, user)).thenReturn(BucketAccess.NONE);

        // act
        try {
            this.imageServiceHelper.retrieveBucketAccessFromPisss(username, bucketName, user);
            fail();
        } catch (NotAuthorizedException e) {
            // assert
            assertEquals(String.format("user %s does not have access to bucket %s", username, bucketName), e.getMessage());
            throw e;
        }
    }
}
