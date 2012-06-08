package com.bt.pi.app.management;

import java.util.Locale;

import org.apache.commons.lang.RandomStringUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.stereotype.Component;

import com.bt.pi.app.common.entities.Image;
import com.bt.pi.app.common.entities.ImageIndex;
import com.bt.pi.app.common.entities.ImageState;
import com.bt.pi.app.common.entities.MachineType;
import com.bt.pi.app.common.entities.User;
import com.bt.pi.app.common.images.platform.ImagePlatform;
import com.bt.pi.core.continuation.UpdateResolver;
import com.bt.pi.core.continuation.UpdateResolvingPiContinuation;
import com.bt.pi.core.dht.BlockingDhtWriter;
import com.bt.pi.core.id.PId;

@Component
public class ImageSeeder extends SeederBase {
    private static final Log LOG = LogFactory.getLog(ImageSeeder.class);
    private static final String DASH = "-";
    private static final int EIGHT = 8;

    public ImageSeeder() {
        super();
    }

    public String createImage(String imageId, int regionCode, String kernelId, String ramDiskId, String manifestLocation, String ownerId, String architecture, String platform, Boolean isPublic, String machineType) {
        LOG.debug(String.format("createImage(%s, %s, %s, %s, %s, %s, %s, %s, %s)", imageId, regionCode, kernelId, ramDiskId, manifestLocation, ownerId, architecture, isPublic, machineType));
        MachineType type = MachineType.valueOf(machineType.toUpperCase(Locale.UK));
        ImagePlatform plat = ImagePlatform.valueOf(platform);
        String imageIdToStore = StringUtils.isBlank(imageId) ? type.getImagePrefix() + DASH + RandomStringUtils.random(EIGHT, HEX_ID_SET) : imageId;
        Image image = new Image(imageIdToStore, kernelId, ramDiskId, manifestLocation, ownerId, architecture, plat, isPublic, type);
        image.setState(ImageState.AVAILABLE);

        if (isPublic)
            addImageToImageIndex(imageIdToStore, regionCode);
        else
            addImageToUser(ownerId, imageIdToStore);

        boolean written = storeImage(image);

        if (written) {
            return image.getImageId();
        } else
            return null;
    }

    private void addImageToUser(final String ownerId, final String imageIdToStore) {
        LOG.debug(String.format("addImageToUser(%s, %s)", ownerId, imageIdToStore));
        getDhtClientFactory().createBlockingWriter().update(getPiIdBuilder().getPId(User.getUrl(ownerId)), null, new UpdateResolver<User>() {
            @Override
            public User update(User existingEntity, User requestedEntity) {
                if (null == existingEntity)
                    throw new IllegalArgumentException(String.format("user %s does not exist for private image", ownerId));
                existingEntity.getImageIds().add(imageIdToStore);
                return existingEntity;
            }
        });
    }

    public void updateImagePlatform(final String imageId, final String platform) {
        LOG.debug(String.format("updateImagePlatform(%s, %s)", imageId, platform));

        if (StringUtils.isEmpty(imageId) || StringUtils.isEmpty(platform))
            throw new IllegalArgumentException("ImageId or Platform is null or empty");

        final ImagePlatform imagePlatform = ImagePlatform.valueOf(platform);
        final PId piImageId = getPiIdBuilder().getPId(Image.getUrl(imageId));

        getDhtClientFactory().createBlockingWriter().update(piImageId, null, new UpdateResolvingPiContinuation<Image>() {

            @Override
            public Image update(Image existingEntity, Image requestedEntity) {
                if (existingEntity == null)
                    return null;

                existingEntity.setPlatform(imagePlatform);
                return existingEntity;
            }

            @Override
            public void handleResult(Image result) {
                if (result == null) {
                    throw new IllegalArgumentException("Unable to update image platform for:" + imageId);
                }

                LOG.debug(String.format("Updated image platform for image id: %s to %s", imageId, platform));
            }
        });
    }

    private boolean storeImage(Image image) {
        LOG.debug(String.format("storeImage(%s)", image));
        PId id = getPiIdBuilder().getPId(image);
        BlockingDhtWriter dhtWriter = getDhtClientFactory().createBlockingWriter();
        return dhtWriter.writeIfAbsent(id, image);
    }

    private void addImageToImageIndex(final String imageId, int regionId) {
        LOG.debug(String.format("addImageToImageIndex(%s)", imageId));
        PId imageIdIndexId = getPiIdBuilder().getPId(ImageIndex.URL).forRegion(regionId);
        ImageIndex imageIndex = new ImageIndex();
        imageIndex.getImages().add(imageId);
        BlockingDhtWriter dhtWriter = getDhtClientFactory().createBlockingWriter();
        dhtWriter.update(imageIdIndexId, imageIndex, new UpdateResolver<ImageIndex>() {
            @Override
            public ImageIndex update(ImageIndex existingEntity, ImageIndex requestedEntity) {
                if (null == existingEntity)
                    return requestedEntity;
                existingEntity.getImages().addAll(requestedEntity.getImages());
                return existingEntity;
            }
        });
    }
}
