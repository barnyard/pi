/* (c) British Telecommunications plc, 2009, All Rights Reserved */
package com.bt.pi.api.service;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.annotation.Resource;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.stereotype.Component;

import com.bt.pi.app.common.entities.Image;
import com.bt.pi.app.common.entities.ImageIndex;
import com.bt.pi.app.common.entities.ImageState;
import com.bt.pi.app.common.entities.MachineType;
import com.bt.pi.app.common.entities.User;
import com.bt.pi.app.common.images.platform.ImagePlatform;
import com.bt.pi.app.common.resource.PiQueue;
import com.bt.pi.app.common.resource.PiTopics;
import com.bt.pi.app.imagemanager.reporting.ImageReportEntity;
import com.bt.pi.app.imagemanager.xml.Manifest;
import com.bt.pi.app.imagemanager.xml.ManifestBuilder;
import com.bt.pi.core.application.watcher.task.TaskProcessingQueueContinuation;
import com.bt.pi.core.conf.Property;
import com.bt.pi.core.continuation.PiContinuation;
import com.bt.pi.core.continuation.UpdateResolver;
import com.bt.pi.core.id.PId;
import com.bt.pi.sss.client.PisssClient.BucketAccess;

@Component
public class ImageServiceImpl extends ServiceBaseImpl implements ManagementImageService {
    protected static final String DEFAULT_DECRYPTION_RETRIES = "5";
    private static final String DEFAULT_IMAGES_PATH = "var/images";
    private static final String SLASH = "/";
    private static final int THIRTY_SECONDS = 30;
    private static final Log LOG = LogFactory.getLog(ImageServiceImpl.class);
    @Resource
    private ManifestBuilder manifestBuilder;
    @Resource
    private ImageRetriever imageRetriever;
    private int decrytionRetries = Integer.parseInt(DEFAULT_DECRYPTION_RETRIES);
    private String imagesPath = DEFAULT_IMAGES_PATH;
    private ImageServiceHelper imageServiceHelper;

    public ImageServiceImpl() {
        this.manifestBuilder = null;
        this.imageServiceHelper = null;
    }

    @Resource
    public void setImageServiceHelper(ImageServiceHelper imageServiceHelper2) {
        this.imageServiceHelper = imageServiceHelper2;
    }

    @Property(key = "image.decryption.retries", defaultValue = DEFAULT_DECRYPTION_RETRIES)
    public void setDecrytionRetries(int value) {
        this.decrytionRetries = value;
    }

    @Property(key = "image.path", defaultValue = DEFAULT_IMAGES_PATH)
    public void setImagesPath(String value) {
        this.imagesPath = value;
    }

    // TODO If imageIds is not null request supernodes for the list of images requested only.
    public Set<Image> describeImages(String ownerId, List<String> imageIds) {
        LOG.debug(String.format("describeImages(%s, %s)", ownerId, imageIds));

        // get image index
        ImageIndex imageIndex = getLocalImageIndex();

        Set<String> imagesToGet = imageServiceHelper.getNewStringSet();

        if (null == imageIndex) {
            LOG.warn("no ImageIndex found in DHT");
        }

        User user = getUserManagementService().getUser(ownerId);

        if (null != imageIds && imageIds.size() > 0) {
            for (String imageId : imageIds) {
                if (imageIndex != null && imageIndex.getImages().contains(imageId))
                    imagesToGet.add(imageId);
                if (user.getImageIds().contains(imageId))
                    imagesToGet.add(imageId);
            }
        } else {
            if (imageIndex != null)
                imagesToGet.addAll(imageIndex.getImages());
            imagesToGet.addAll(user.getImageIds());
        }

        LOG.debug(String.format("getting image details: %s", imagesToGet));

        final List<PId> ids = imageServiceHelper.getNewPIdList();
        for (String imageId : imagesToGet) {
            ids.add(getPiIdBuilder().getPId(Image.getUrl(imageId)));
        }
        // results should be a set.
        Set<Image> results = new HashSet<Image>();
        Set<Image> resultsFromSupernodes = getListOfImagesFromSupernodes(imageIds);
        List<PId> idsToRemove = imageServiceHelper.getNewPIdList();
        results = checkAndScatterGatherMissingImages(ids, results, resultsFromSupernodes, idsToRemove);
        return results;
    }

    private Set<Image> checkAndScatterGatherMissingImages(final List<PId> ids, final Set<Image> results, Set<Image> resultsFromSupernodes, List<PId> idsToRemove) {

        for (Image image : resultsFromSupernodes) {
            PId imagePid = getPiIdBuilder().getPId(Image.getUrl(image.getImageId()));
            if (ids.contains(imagePid)) {
                results.add(image);
                idsToRemove.add(imagePid);
            }
        }
        ids.removeAll(idsToRemove);
        if (ids.size() > 0) {
            scatterGatherImagesMissingFromCache(ids, results);
        }
        return results;
    }

    private Set<Image> getListOfImagesFromSupernodes(List<String> imageIds) {
        Set<ImageReportEntity> entities = imageRetriever.retrieveImagesFromSupernodes(imageIds);
        Set<Image> images = new HashSet<Image>();
        for (ImageReportEntity entity : entities) {
            images.add(entity.getImage());
        }
        return images;
    }

    private Set<Image> scatterGatherImagesMissingFromCache(final List<PId> ids, final Set<Image> images) {

        scatterGather(ids, new PiContinuation<Image>() {
            @Override
            public void handleResult(Image result) {
                images.add(result);
            }
        }, THIRTY_SECONDS);
        return images;
    }

    public String registerImage(String ownerId, String imageManifestLocation) {
        LOG.debug(String.format("registerImage(%s, %s)", ownerId, imageManifestLocation));
        return registerImage(ownerId, imageManifestLocation, MachineType.MACHINE);
    }

    public String registerImage(String ownerId, String imageManifestLocation, MachineType machineType) {
        LOG.debug(String.format("registerImage(%s, %s, %s)", ownerId, imageManifestLocation, machineType));

        imageServiceHelper.validateImageManifestLocation(imageManifestLocation);

        // check manifest is in S3
        String bucketName = imageManifestLocation.split(SLASH)[0];
        String objectName = imageManifestLocation.split(SLASH)[1];
        User user = getUserManagementService().getUser(ownerId);
        String manifestXml = imageServiceHelper.retrieveManifestXmlFromPisss(bucketName, objectName, user);

        // get the users access to the bucket
        BucketAccess access = imageServiceHelper.retrieveBucketAccessFromPisss(ownerId, bucketName, user);

        // create dht record for image
        String imageId = null;
        switch (machineType) {
        case MACHINE:
            imageId = getIdFactory().createNewImageId();
            break;
        case KERNEL:
            imageId = getIdFactory().createNewKernelId();
            break;
        case RAMDISK:
            imageId = getIdFactory().createNewRamdiskId();
            break;
        default:
            break;
        }
        boolean isPublic = BucketAccess.PUBLIC.equals(access);
        Manifest manifest = manifestBuilder.build(manifestXml);
        String arch = manifest.getArch();

        String kernelId = null;
        String ramdiskId = null;
        if (MachineType.MACHINE.equals(machineType)) {
            kernelId = manifest.getKernelId();
            ramdiskId = manifest.getRamdiskId();
            checkImageAccess(kernelId, user, "kernel");
            checkImageAccess(ramdiskId, user, "ramdisk");
        }

        final Image image = new Image(imageId, kernelId, ramdiskId, imageManifestLocation, ownerId, arch, ImagePlatform.linux, isPublic, machineType);
        image.setState(ImageState.PENDING);
        getDhtClientFactory().createBlockingWriter().put(getPiIdBuilder().getPId(image), image);

        // if it's public, we add it to the global image index, otherwise to the user record.
        if (isPublic)
            imageServiceHelper.addImageToIndex(image);
        else
            getUserService().addImageToUser(ownerId, imageId);

        // add to task processing queue
        PId decryptImageQueueId = getPiIdBuilder().getPiQueuePId(PiQueue.DECRYPT_IMAGE).forLocalScope(PiQueue.DECRYPT_IMAGE.getNodeScope());
        getTaskProcessingQueueHelper().addUrlToQueue(decryptImageQueueId, image.getUrl(), decrytionRetries, new TaskProcessingQueueContinuation() {
            @Override
            public void receiveResult(String uri, String nodeId) {
                // anycast image to image manager for decryption
                imageServiceHelper.anycast(image, PiTopics.DECRYPT_IMAGE);
            }
        });

        return imageId;
    }

    public boolean deregisterImage(final String ownerId, final String imageId) {
        return deregisterImage(ownerId, imageId, MachineType.MACHINE);
    }

    // this method is only for use by the DeleteUserHelper and allows it to de-register images without knowing the
    // MachineType. Note that it isn't on the ImageService interface
    public boolean deregisterImageWithoutMachineTypeCheck(final String ownerId, final String imageId) {
        return deregisterImage(ownerId, imageId, null);
    }

    public boolean deregisterImage(final String ownerId, final String imageId, final MachineType machineType) {
        LOG.debug(String.format("deregisterImage(%s, %s, %s)", ownerId, imageId, machineType));

        PId imageIdId = getPiIdBuilder().getPId(Image.getUrl(imageId));
        final AtomicBoolean isPublic = new AtomicBoolean(false);
        getDhtClientFactory().createBlockingWriter().update(imageIdId, null, new UpdateResolver<Image>() {
            @Override
            public Image update(Image existingEntity, Image requestedEntity) {
                if (null == existingEntity)
                    throw new NotFoundException(String.format("image %s not found", imageId));
                if (!ownerId.equals(existingEntity.getOwnerId()))
                    throw new NotAuthorizedException(String.format("%s not owner of image %s", ownerId, imageId));
                if (null != machineType)
                    if (!machineType.equals(existingEntity.getMachineType()))
                        throw new NotAuthorizedException(String.format("not allowed to de-register a %s image", existingEntity.getMachineType()));
                existingEntity.setDeleted(true);
                isPublic.set(existingEntity.isPublic());
                return existingEntity;
            }
        });

        if (isPublic.get()) {
            imageServiceHelper.removeImageFromImageIndex(imageId);
        } else {
            getUserService().removeImageFromUser(ownerId, imageId);
        }
        imageServiceHelper.deleteFile(String.format("%s/%s", imagesPath, imageId));
        return true;
    }
}
