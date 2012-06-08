/* (c) British Telecommunications plc, 2009, All Rights Reserved */
package com.bt.pi.app.imagemanager;

import java.io.File;

import javax.annotation.Resource;

import org.apache.commons.io.FileUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Component;

import com.bt.pi.app.common.entities.Image;
import com.bt.pi.app.common.entities.ImageState;
import com.bt.pi.app.common.entities.User;
import com.bt.pi.app.common.id.PiIdBuilder;
import com.bt.pi.app.common.resource.PiQueue;
import com.bt.pi.app.imagemanager.crypto.ImageCryptographer;
import com.bt.pi.core.application.watcher.task.TaskProcessingQueueHelper;
import com.bt.pi.core.conf.Property;
import com.bt.pi.core.continuation.PiContinuation;
import com.bt.pi.core.dht.cache.DhtCache;
import com.bt.pi.core.id.PId;

@Component
public class DecryptionHandler {
    private static final String S_S = "%s/%s";
    private static final Log LOG = LogFactory.getLog(DecryptionHandler.class);
    private static final String SLASH = "/";
    private static final String DEFAULT_IMAGES_PATH = "var/images";
    private String imagesPath = DEFAULT_IMAGES_PATH;
    @Resource
    private TaskProcessingQueueHelper taskProcessingQueueHelper;
    @Resource
    private ThreadPoolTaskExecutor taskExecutor;
    @Resource
    private ImageCryptographer imageCryptographer;
    @Resource(name = "userCache")
    private DhtCache dhtCache;
    @Resource
    private PiIdBuilder piIdBuilder;
    @Resource
    private ImageHelper imageHelper;

    DecryptionHandler() {
        this.taskProcessingQueueHelper = null;
        this.taskExecutor = null;
        this.imageCryptographer = null;
        this.dhtCache = null;
        this.piIdBuilder = null;
        this.imageHelper = null;
    }

    public void decrypt(final Image image, final ImageManagerApplication imageManager) {
        LOG.debug(String.format("decrypt(%s, %s)", image, imageManager));

        final PId decryptImageQueueId = piIdBuilder.getPiQueuePId(PiQueue.DECRYPT_IMAGE).forLocalScope(PiQueue.DECRYPT_IMAGE.getNodeScope());
        this.taskProcessingQueueHelper.setNodeIdOnUrl(decryptImageQueueId, image.getUrl(), imageManager.getNodeIdFull());

        dhtCache.getReadThrough(piIdBuilder.getPId(User.getUrl(image.getOwnerId())), new PiContinuation<User>() {
            @Override
            public void handleResult(final User user) {
                LOG.debug(String.format("handleResult(%s)", user.getUsername()));
                Thread thread = taskExecutor.createThread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            String imageFilePath = String.format(S_S, imagesPath, image.getImageId());
                            if (new File(imageFilePath).exists()) {
                                LOG.info(String.format("%s already exists so no need to decrypt", imageFilePath));
                            } else {
                                LOG.info(String.format("%s not found so will attempt to decrypt from bucket", imageFilePath));
                                String manifestFileLocation = image.getManifestLocation();
                                File decryptedImageFile = imageCryptographer.decrypt(manifestFileLocation.split(SLASH)[0], manifestFileLocation.split(SLASH)[1], user);
                                boolean renamed = decryptedImageFile.renameTo(new File(imageFilePath));
                                // This rename seems to overwrite an existing file, but will return false if the files
                                // are
                                // on different file systems. For this reason we create the preceding tmp file in the
                                // images
                                // directory
                                if (renamed)
                                    LOG.info(String.format("file %s renamed to %s", decryptedImageFile.getAbsolutePath(), imageFilePath));
                                else {
                                    LOG.warn(String.format("file %s NOT renamed to %s, assume someone else has beaten me to it", decryptedImageFile.getAbsolutePath(), imageFilePath));
                                    FileUtils.deleteQuietly(decryptedImageFile);
                                }

                            }
                            imageHelper.updateImageState(image.getImageId(), ImageState.AVAILABLE);
                            taskProcessingQueueHelper.removeUrlFromQueue(decryptImageQueueId, image.getUrl());
                        } catch (Throwable t) {
                            LOG.error(t);
                        }
                    }
                });
                thread.setPriority(Thread.MIN_PRIORITY);
                thread.start();
            }
        });
    }

    @Property(key = "image.path", defaultValue = DEFAULT_IMAGES_PATH)
    public void setImagesPath(String value) {
        this.imagesPath = value;
    }

    protected void setThreadPoolTaskExecutor(ThreadPoolTaskExecutor taskExecutor2) {
        this.taskExecutor = taskExecutor2;
    }
}
