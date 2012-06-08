package com.bt.pi.api.service;

import java.util.Arrays;

import javax.annotation.Resource;

import org.apache.commons.collections.Closure;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.stereotype.Component;

import com.bt.pi.app.common.entities.User;

@Component
public class DeleteUserHelper {
    private static final Log LOG = LogFactory.getLog(DeleteUserHelper.class);
    @Resource
    private InstancesService instancesService;
    @Resource
    private ManagementImageService imageService;
    @Resource
    private UserPisssHelper userPisssHelper;
    @Resource
    private SecurityGroupService securityGroupService;
    @Resource
    private ElasticBlockStorageService elasticBlockStorageService;

    public DeleteUserHelper() {
        instancesService = null;
        userPisssHelper = null;
        imageService = null;
        securityGroupService = null;
    }

    public void cleanupUserResources(User user) {

        tryCatch(user, new Closure() {

            @Override
            public void execute(Object input) {
                User user = (User) input;
                instancesService.terminateInstances(user.getUsername(), Arrays.asList(user.getInstanceIds()));
            }
        }, "There was an issue with terminate user instances");

        tryCatch(user, new Closure() {
            @Override
            public void execute(Object arg) {
                User user = (User) arg;
                for (String imageId : user.getImageIds()) {
                    imageService.deregisterImageWithoutMachineTypeCheck(user.getUsername(), imageId);
                }
            }
        }, "There was an issue with deregistering user images");

        tryCatch(user, new Closure() {
            @Override
            public void execute(Object input) {
                User user = (User) input;
                userPisssHelper.deleteBucketsFromUser(user.getUsername(), user.getBucketNames());
            }
        }, "There was an issue with deleting buckets");

        tryCatch(user, new Closure() {

            @Override
            public void execute(Object input) {
                User user = (User) input;

                for (String groupName : user.getSecurityGroupIds()) {
                    securityGroupService.deleteSecurityGroup(user.getUsername(), groupName);
                }

            }
        }, "There was an issue with deleting security groups");

        tryCatch(user, new Closure() {

            @Override
            public void execute(Object input) {
                User user = (User) input;
                for (String snapshotId : user.getSnapshotIds()) {
                    elasticBlockStorageService.deleteSnapshot(user.getUsername(), snapshotId);
                }
            }
        }, "There was an issue with deleting snapshots");

        tryCatch(user, new Closure() {

            @Override
            public void execute(Object input) {
                User user = (User) input;

                for (String volumeId : user.getVolumeIds()) {
                    elasticBlockStorageService.deleteVolume(user.getUsername(), volumeId);
                }

            }
        }, "There was an issue with deleting volumes");
    }

    private void tryCatch(User user, Closure closure, String exceptionMessage) {
        try {
            closure.execute(user);
        } catch (Throwable t) {
            LOG.warn(String.format("User: %s, Error message: %s", user.getUsername(), exceptionMessage), t);
        }
    }
}
