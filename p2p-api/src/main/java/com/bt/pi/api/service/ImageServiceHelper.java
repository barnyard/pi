/* (c) British Telecommunications plc, 2009, All Rights Reserved */
package com.bt.pi.api.service;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.annotation.Resource;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import com.bt.pi.app.common.entities.Image;
import com.bt.pi.app.common.entities.ImageIndex;
import com.bt.pi.app.common.entities.User;
import com.bt.pi.app.common.resource.PiTopics;
import com.bt.pi.core.application.PubSubMessageContext;
import com.bt.pi.core.continuation.UpdateResolver;
import com.bt.pi.core.entity.EntityMethod;
import com.bt.pi.core.entity.PiEntity;
import com.bt.pi.core.id.PId;
import com.bt.pi.sss.client.PisssClient;
import com.bt.pi.sss.client.PisssClient.BucketAccess;
import com.bt.pi.sss.exception.BucketObjectNotFoundException;
import com.bt.pi.sss.exception.NoSuchBucketException;

@Component
public class ImageServiceHelper extends ServiceBaseImpl {
    private static final Log LOG = LogFactory.getLog(ImageServiceHelper.class);
    private static final String SLASH = "/";

    @Resource
    private PisssClient pisssClient;

    public ImageServiceHelper() {
        this.pisssClient = null;
    }

    public void anycast(final PiEntity entity, PiTopics topic) {
        PubSubMessageContext pubSubMessageContext = getApiApplicationManager().newLocalPubSubMessageContext(topic);
        pubSubMessageContext.randomAnycast(EntityMethod.CREATE, entity);
    }

    public void validateImageManifestLocation(String imageManifestLocation) {
        LOG.debug(String.format("validateImageManfifestLocation(%s)", imageManifestLocation));
        if (!StringUtils.hasText(imageManifestLocation))
            throw new IllegalArgumentException("image manifest location must be supplied");
        if (imageManifestLocation.indexOf(SLASH) < 0)
            throw new IllegalArgumentException("image manifest location must be <bucketName>/<fileName>");
    }

    public void addImageToIndex(final Image image) {
        LOG.debug(String.format("addImageToIndex(%s)", image));
        getDhtClientFactory().createBlockingWriter().update(getPiIdBuilder().getPId(ImageIndex.URL).forLocalRegion(), null, new UpdateResolver<ImageIndex>() {
            @Override
            public ImageIndex update(ImageIndex existingEntity, ImageIndex requestedEntity) {
                if (null == existingEntity) {
                    ImageIndex result = new ImageIndex();
                    result.getImages().add(image.getImageId());
                    return result;
                }
                existingEntity.getImages().add(image.getImageId());
                return existingEntity;
            }
        });
    }

    public void removeImageFromImageIndex(final String imageId) {
        LOG.debug(String.format("removeImageFromImageIndex(%s)", imageId));
        PId id = getPiIdBuilder().getPId(ImageIndex.URL).forLocalRegion();
        getDhtClientFactory().createBlockingWriter().update(id, null, new UpdateResolver<ImageIndex>() {
            @Override
            public ImageIndex update(ImageIndex existingEntity, ImageIndex requestedEntity) {
                if (null == existingEntity)
                    return new ImageIndex();
                if (existingEntity.getImages().remove(imageId))
                    return existingEntity;
                return null;
            }
        });
    }

    public void deleteFile(String name) {
        File f = new File(name);
        if (f.exists())
            if (f.delete())
                LOG.debug(String.format("%s deleted", name));
    }

    public Set<String> getNewStringSet() {
        return new HashSet<String>();
    }

    public List<Image> getNewImageList() {
        return new ArrayList<Image>();
    }

    public List<PId> getNewPIdList() {
        return new ArrayList<PId>();
    }

    protected String retrieveManifestXmlFromPisss(String bucketName, String objectName, User user) {
        LOG.debug(String.format("retrieveManifestXmlFromPisss(%s, %s, %s)", bucketName, objectName, user.getUsername()));
        String manifestXml = null;
        try {
            manifestXml = pisssClient.getObjectFromBucket(bucketName, objectName, user);
            LOG.debug(manifestXml);
        } catch (NoSuchBucketException e) {
            throw new IllegalArgumentException(String.format("No such bucket: %s for user %s", bucketName, user.getUsername()));
        } catch (BucketObjectNotFoundException e) {
            throw new IllegalArgumentException(String.format("Manifest not found for user %s in bucket %s", user.getUsername(), bucketName));
        }
        return manifestXml;
    }

    protected BucketAccess retrieveBucketAccessFromPisss(String ownerId, String bucketName, User user) {
        LOG.debug(String.format("retrieveBucketAccessFromPisss(%s, %s, %s)", ownerId, bucketName, user.getUsername()));
        BucketAccess access = pisssClient.getBucketAccess(bucketName, user);
        if (BucketAccess.NONE.equals(access))
            throw new NotAuthorizedException(String.format("user %s does not have access to bucket %s", ownerId, bucketName));
        return access;
    }
}
