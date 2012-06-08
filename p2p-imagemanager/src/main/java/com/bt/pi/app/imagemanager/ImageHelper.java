/* (c) British Telecommunications plc, 2009, All Rights Reserved */
package com.bt.pi.app.imagemanager;

import javax.annotation.Resource;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.stereotype.Component;

import com.bt.pi.app.common.entities.Image;
import com.bt.pi.app.common.entities.ImageState;
import com.bt.pi.app.common.id.PiIdBuilder;
import com.bt.pi.core.continuation.UpdateResolvingPiContinuation;
import com.bt.pi.core.dht.DhtClientFactory;

@Component
public class ImageHelper {
    private static final Log LOG = LogFactory.getLog(ImageHelper.class);
    @Resource
    private PiIdBuilder piIdBuilder;
    @Resource
    private DhtClientFactory dhtClientFactory;

    ImageHelper() {
        this.piIdBuilder = null;
        this.dhtClientFactory = null;
    }

    public void updateImageState(final String imageId, final ImageState newState) {
        dhtClientFactory.createWriter().update(piIdBuilder.getPId(Image.getUrl(imageId)), new UpdateResolvingPiContinuation<Image>() {
            @Override
            public Image update(Image existingEntity, Image requestedEntity) {
                if (null == existingEntity) {
                    LOG.warn(String.format("Unable to get image:" + imageId));
                    return null;
                }

                if (newState.equals(existingEntity.getState()))
                    return null;
                existingEntity.setState(newState);
                return existingEntity;
            }

            @Override
            public void handleResult(Image result) {
                if (result != null)
                    LOG.debug(String.format("image %s state updated to %s", result.getImageId(), result.getState()));
            }
        });
    }
}
