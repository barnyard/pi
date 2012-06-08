package com.bt.pi.api.service;

import static org.mockito.Matchers.isA;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

import java.util.Arrays;
import java.util.Collection;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import com.bt.pi.app.common.entities.User;

@RunWith(MockitoJUnitRunner.class)
public class DeleteUserHelperTest {
    @InjectMocks
    private DeleteUserHelper deleteUserHelper = new DeleteUserHelper();
    @Mock
    private InstancesService instancesService;
    @Mock
    private ImageServiceImpl imageService;
    @Mock
    private UserPisssHelper userPisssHelper;
    @Mock
    private SecurityGroupService securityGroupService;
    @Mock
    private ElasticBlockStorageService elasticBlockStorageService;

    private User user;
    private String username = "user";
    private String accessKey = "access";
    private String secretKey = "secret";

    @Before
    public void setup() {
        user = new User(username, accessKey, secretKey);
        user.setDeleted(false);
    }

    @Test
    public void cleanupUserResourcesShouldRemoveBucketsFromUser() {
        // setup
        user.getBucketNames().add("bucket1");
        user.getBucketNames().add("bucket2");

        // act
        deleteUserHelper.cleanupUserResources(user);

        // assert
        verify(userPisssHelper).deleteBucketsFromUser(user.getUsername(), user.getBucketNames());
    }

    @Test
    public void cleanupUserResourcesShouldRemoveInstancesFromUser() {
        // setup
        user.addInstance("instance1");
        user.addInstance("instance2");

        // act
        deleteUserHelper.cleanupUserResources(user);

        // assert
        verify(instancesService).terminateInstances(username, Arrays.asList(user.getInstanceIds()));
    }

    @Test
    public void cleanupUserResourcesShouldDeleteSecurityGroupsFromUser() {
        // setup
        user.getSecurityGroupIds().add("secgroup1");
        user.getSecurityGroupIds().add("secgroup2");

        // act
        deleteUserHelper.cleanupUserResources(user);

        // assert
        verify(securityGroupService).deleteSecurityGroup(username, "secgroup1");
        verify(securityGroupService).deleteSecurityGroup(username, "secgroup2");
    }

    @Test
    public void cleanupUserResourcesShouldRemoveImages() {
        // setup
        user.getImageIds().add("image1");
        user.getImageIds().add("image2");

        // act
        deleteUserHelper.cleanupUserResources(user);

        // assert
        verify(imageService).deregisterImageWithoutMachineTypeCheck(username, "image1");
        verify(imageService).deregisterImageWithoutMachineTypeCheck(username, "image2");
    }

    public void cleanupUserResourcesShouldDeleteSnapshots() {
        // setup
        user.getSnapshotIds().add("snapshot1");
        user.getSnapshotIds().add("snapshot2");

        // act
        deleteUserHelper.cleanupUserResources(user);

        // assert
        verify(elasticBlockStorageService).deleteSnapshot(username, "snapshot1");
        verify(elasticBlockStorageService).deleteSnapshot(username, "snapshot2");
    }

    public void cleanupUserResourcesShouldDeleteVolumes() throws Exception {
        // setup
        user.getVolumeIds().add("volume1");
        user.getVolumeIds().add("volume2");

        // act
        deleteUserHelper.cleanupUserResources(user);

        // assert
        verify(elasticBlockStorageService).deleteVolume(username, "volume1");
        verify(elasticBlockStorageService).deleteVolume(username, "volume2");
    }

    @Test
    public void cleanupUserResourcesShouldExitNormallyIfRuntimeExceptionIsThrown() {
        // setup
        user.getImageIds().add("image1");
        doThrow(new RuntimeException()).when(instancesService).terminateInstances(isA(String.class), isA(Collection.class));
        // act
        deleteUserHelper.cleanupUserResources(user);

        // assert
        verify(imageService).deregisterImageWithoutMachineTypeCheck(username, "image1");
    }
}
