/* (c) British Telecommunications plc, 2009, All Rights Reserved */
package com.bt.pi.app.instancemanager.images;

import java.io.IOException;

import javax.annotation.Resource;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.stereotype.Component;

import com.bt.pi.app.common.os.FileManager;
import com.bt.pi.core.conf.Property;

@Component
public class FileImageLoader implements ImageLoader {
    private static final String S_SLASH_S = "%s/%s";
    private static Log logger = LogFactory.getLog(FileImageLoader.class);
    private FileManager fileManager;
    private String imagePath;

    public FileImageLoader() {
        this.fileManager = null;
    }

    @Resource
    public void setFileManager(FileManager aFileManager) {
        fileManager = aFileManager;
    }

    @Property(key = "image.path", defaultValue = "var/images")
    public void setImagePath(String value) {
        this.imagePath = value;
    }

    @Override
    public String saveImage(String imageId, String piCacheDirectory) {
        String path = String.format(S_SLASH_S, this.imagePath, imageId);
        try {
            logger.debug(String.format("saveImage(%s, %s)", path, piCacheDirectory));
            String copiedFileName = fileManager.copyImage(path, piCacheDirectory);
            return String.format(S_SLASH_S, piCacheDirectory, copiedFileName);
        } catch (IOException e) {
            throw new ImageLoaderException("Unable to copy file from:" + path + " to:" + piCacheDirectory, e);
        }
    }
}
