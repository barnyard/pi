/* (c) British Telecommunications plc, 2009, All Rights Reserved */
package com.bt.pi.api.service;

import java.util.Collection;

import javax.annotation.Resource;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.stereotype.Component;

import com.bt.pi.api.utils.AlternativeResultUpdateResolver;
import com.bt.pi.app.common.entities.InstanceTypeConfiguration;
import com.bt.pi.app.common.entities.InstanceTypes;
import com.bt.pi.app.common.entities.KeyPair;
import com.bt.pi.app.common.entities.User;
import com.bt.pi.core.conf.Property;
import com.bt.pi.core.continuation.UpdateResolver;
import com.bt.pi.core.dht.cache.BlockingDhtCache;
import com.bt.pi.core.id.PId;

/**
 * Service layer for user based entities in the DHT
 */
@Component
public class UserService extends ServiceBaseImpl {
    private static final Log LOG = LogFactory.getLog(UserService.class);

    private static final String ADDING_INSTANCE_ID_S_TO_USER_S_IN_DHT_RESOLVER = "Adding instanceId %s to user %s in DHT resolver";
    private static final String ADDING_SNAPSHOT_ID_S_TO_USER_S_IN_DHT_RESOLVER = "Adding snapshotId %s to user %s in DHT resolver";
    private static final String REMOVING_SNAPSHOT_ID_S_FROM_USER_S_IN_DHT_RESOLVER = "Removing snapshotId %s from user %s in DHT resolver";
    private static final String FIFTY = "50";

    @Resource(name = "userBlockingCache")
    private BlockingDhtCache userBlockingCache;

    @Resource(name = "generalBlockingCache")
    private BlockingDhtCache instanceTypeCache;

    private int maxVolumesPerUser;
    private int maxPrivateImagesPerUser;

    public UserService() {
        super();
        userBlockingCache = null;
        instanceTypeCache = null;
    }

    @Property(key = "max.volumes.per.user", defaultValue = FIFTY)
    public void setMaxVolumesPerUser(int max) {
        maxVolumesPerUser = max;
    }

    @Property(key = "max.private.images.per.user", defaultValue = FIFTY)
    public void setMaxPrivateImagesPerUser(int max) {
        maxPrivateImagesPerUser = max;
    }

    public void addKeyPairToUser(final String userId, final KeyPair keyPair) {
        LOG.debug(String.format("addKeyPairToUser(%s, %s)", userId, keyPair));
        PId userPiId = getPiIdBuilder().getPId(User.getUrl(userId));
        AlternativeResultUpdateResolver<RuntimeException, User> updateResolver = new AlternativeResultUpdateResolver<RuntimeException, User>() {
            @Override
            public User update(User existingEntity, User requestedEntity) {
                LOG.debug(String.format("Adding keyPair %s to user %s in DHT resolver", keyPair.getKeyName(), existingEntity.getUsername()));

                if (existingEntity.getKeyPairs().contains(keyPair)) {
                    setResult(new IllegalStateException(String.format("Key pair %s already exists for user %s", keyPair.getKeyName(), existingEntity.getUsername())));
                    return null;
                }

                existingEntity.getKeyPairs().add(keyPair);
                return existingEntity;
            }
        };
        userBlockingCache.update(userPiId, updateResolver);

        if (updateResolver.getResult() != null) {
            throw updateResolver.getResult();
        }
    }

    public void removeKeyPairFromUser(final String userId, final KeyPair keyPair) {
        LOG.debug(String.format("removeKeyPairFromUser(%s, %s)", userId, keyPair));
        PId userPiId = getPiIdBuilder().getPId(User.getUrl(userId));
        userBlockingCache.update(userPiId, new UpdateResolver<User>() {
            @Override
            public User update(User existingEntity, User requestedEntity) {
                if (existingEntity.getKeyPairs().remove(keyPair)) {
                    return existingEntity;
                }
                return null;
            }
        });
    }

    public void addInstancesToUser(String userId, final Collection<String> instanceIds, final String instanceType) {
        LOG.debug(String.format("addInstancesToUser(%s, %s, %s)", userId, instanceIds, instanceType));
        PId userPiId = getPiIdBuilder().getPId(User.getUrl(userId));
        userBlockingCache.update(userPiId, new AlternativeResultUpdateResolver<RuntimeException, User>() {
            @Override
            public User update(User previousEntry, User requestedEntity) {
                for (String instanceId : instanceIds)
                    previousEntry.addInstance(instanceId, instanceType);
                return previousEntry;
            }
        });
    }

    public void addInstanceToUser(String userId, final String instanceId) {
        LOG.debug(String.format("addInstanceToUser(%s, %s)", userId, instanceId));
        PId userPiId = getPiIdBuilder().getPId(User.getUrl(userId));
        userBlockingCache.update(userPiId, new AlternativeResultUpdateResolver<RuntimeException, User>() {
            @Override
            public User update(User previousEntry, User requestedEntity) {
                LOG.debug(String.format(ADDING_INSTANCE_ID_S_TO_USER_S_IN_DHT_RESOLVER, instanceId, previousEntry.getUsername()));
                previousEntry.addInstance(instanceId);
                return previousEntry;
            }
        });
    }

    public void addVolumeToUser(final String userId, final String volumeId) {
        LOG.debug(String.format("addVolumeToUser(%s, %s)", userId, volumeId));
        PId userPiId = getPiIdBuilder().getPId(User.getUrl(userId));
        AlternativeResultUpdateResolver<RuntimeException, User> updateResolver = new AlternativeResultUpdateResolver<RuntimeException, User>() {
            @Override
            public User update(User previousEntry, User requestedEntity) {
                LOG.debug(String.format("Adding volumeId %s to user %s in DHT resolver", volumeId, previousEntry.getUsername()));
                User userToWrite = null;
                if (previousEntry.getVolumeIds().size() < maxVolumesPerUser) {
                    previousEntry.getVolumeIds().add(volumeId);
                    userToWrite = previousEntry;
                } else
                    setResult(new IllegalStateException(String.format("Unable to add volume %s, as user %s has %s volumes when the maximum is %s.", volumeId, userId, previousEntry.getVolumeIds().size(), maxVolumesPerUser)));
                return userToWrite;
            }
        };
        userBlockingCache.update(userPiId, updateResolver);

        if (updateResolver.getResult() != null) {
            throw updateResolver.getResult();
        }
    }

    public void addImageToUser(final String userId, final String imageId) {
        LOG.debug(String.format("addImageToUser(%s, %s)", userId, imageId));
        PId userPiId = getPiIdBuilder().getPId(User.getUrl(userId));
        AlternativeResultUpdateResolver<RuntimeException, User> updateResolver = new AlternativeResultUpdateResolver<RuntimeException, User>() {
            @Override
            public User update(User previousEntry, User requestedEntity) {
                LOG.debug(String.format("Adding imageId %s to user %s in DHT resolver", imageId, previousEntry.getUsername()));
                User userToWrite = null;
                if (previousEntry.getImageIds().size() < maxPrivateImagesPerUser) {
                    previousEntry.getImageIds().add(imageId);
                    userToWrite = previousEntry;
                } else
                    setResult(new IllegalStateException(String.format("Unable to add image %s, as user %s has %s images when the maximum is %s.", imageId, userId, previousEntry.getImageIds().size(), maxPrivateImagesPerUser)));
                return userToWrite;
            }
        };
        userBlockingCache.update(userPiId, updateResolver);

        if (updateResolver.getResult() != null) {
            throw updateResolver.getResult();
        }
    }

    public void addSecurityGroupToUser(String userId, final String groupName) {
        LOG.debug(String.format("addSecurityGroupToUser(%s, %s)", userId, groupName));
        PId userPiId = getPiIdBuilder().getPId(User.getUrl(userId));
        userBlockingCache.update(userPiId, new UpdateResolver<User>() {
            @Override
            public User update(User previousEntry, User requestedEntity) {
                LOG.debug(String.format("Adding security group %s to user %s in DHT resolver", groupName, previousEntry.getUsername()));
                previousEntry.getSecurityGroupIds().add(groupName);
                return previousEntry;
            }
        });
    }

    public void removeInstanceFromUser(final String userId, final String instanceId) {
        LOG.debug(String.format("removeInstanceFromUser(%s, %s)", userId, instanceId));
        PId userPiId = getPiIdBuilder().getPId(User.getUrl(userId));
        userBlockingCache.update(userPiId, new UpdateResolver<User>() {
            @Override
            public User update(User existingEntity, User requestedEntity) {
                existingEntity.removeInstance(instanceId);
                return existingEntity;
            }
        });
    }

    public void removeImageFromUser(final String userId, final String imageId) {
        LOG.debug(String.format("removeImageFromUser(%s, %s)", userId, imageId));
        PId userPiId = getPiIdBuilder().getPId(User.getUrl(userId));
        userBlockingCache.update(userPiId, new UpdateResolver<User>() {
            @Override
            public User update(User existingEntity, User requestedEntity) {
                existingEntity.getImageIds().remove(imageId);
                return existingEntity;
            }
        });
    }

    public void removeSecurityGroupFromUser(final String userId, final String groupName) {
        LOG.debug(String.format("removeSecurityGroupFromUser(%s, %s)", userId, groupName));
        PId userPiId = getPiIdBuilder().getPId(User.getUrl(userId));
        userBlockingCache.update(userPiId, new UpdateResolver<User>() {
            @Override
            public User update(User existingEntity, User requestedEntity) {
                existingEntity.getSecurityGroupIds().remove(groupName);
                return existingEntity;
            }
        });
    }

    public void addSnapshotToUser(String userId, final String snapshotId) {
        LOG.debug(String.format("addSnapshotToUser(%s, %s)", userId, snapshotId));
        PId userPiId = getPiIdBuilder().getPId(User.getUrl(userId));
        userBlockingCache.update(userPiId, new AlternativeResultUpdateResolver<RuntimeException, User>() {
            @Override
            public User update(User previousEntry, User requestedEntity) {
                LOG.debug(String.format(ADDING_SNAPSHOT_ID_S_TO_USER_S_IN_DHT_RESOLVER, snapshotId, previousEntry.getUsername()));
                previousEntry.getSnapshotIds().add(snapshotId);
                return previousEntry;
            }
        });
    }

    public void removeSnapshotFromUser(String userId, final String snapshotId) {
        LOG.debug(String.format("removeSnapshotFromUser(%s, %s)", userId, snapshotId));
        PId userPiId = getPiIdBuilder().getPId(User.getUrl(userId));
        userBlockingCache.update(userPiId, new AlternativeResultUpdateResolver<RuntimeException, User>() {
            @Override
            public User update(User previousEntry, User requestedEntity) {
                LOG.debug(String.format(REMOVING_SNAPSHOT_ID_S_FROM_USER_S_IN_DHT_RESOLVER, snapshotId, previousEntry.getUsername()));
                previousEntry.getSnapshotIds().remove(snapshotId);
                return previousEntry;
            }
        });
    }

    public int getCurrentCores(String userId) {
        LOG.debug(String.format("getCurrentCores(%s)", userId));
        User user = getUser(userId);
        if (null == user)
            return Integer.MAX_VALUE;

        PId instanceTypesId = getPiIdBuilder().getPId(InstanceTypes.URL_STRING);
        InstanceTypes instanceTypes = instanceTypeCache.get(instanceTypesId);

        int result = 0;
        for (String instanceType : user.getInstanceTypes()) {
            InstanceTypeConfiguration instanceTypeConfiguration = instanceTypes.getInstanceTypeConfiguration(instanceType);
            if (null == instanceTypeConfiguration) {
                LOG.warn(String.format("unable to find instance type %s, counting as 1 core", instanceType));
                result++;
                continue;
            }
            result += instanceTypeConfiguration.getNumCores();
        }
        return result;
    }

    public int getMaxCores(String userId) {
        LOG.debug(String.format("getMaxCores(%s)", userId));
        User user = getUser(userId);
        if (null == user)
            return 0;
        return user.getMaxCores();
    }

    private User getUser(String userId) {
        PId userPiId = getPiIdBuilder().getPId(User.getUrl(userId));
        return userBlockingCache.get(userPiId);
    }
}
