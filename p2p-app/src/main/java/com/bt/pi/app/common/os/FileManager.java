package com.bt.pi.app.common.os;

import java.io.File;
import java.io.IOException;

import org.apache.commons.io.FileUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.stereotype.Component;

@Component
public class FileManager {
    private static final Log LOG = LogFactory.getLog(FileManager.class);

    public FileManager() {
    }

    public void saveFile(String destinationPath, String fileContents) {
        try {
            FileUtils.writeStringToFile(new File(destinationPath), fileContents);
        } catch (IOException e) {
            String errorMessage = "Unable to write file to: " + destinationPath;
            LOG.error(errorMessage, e);
            throw new RuntimeException(errorMessage, e);
        }
    }

    public boolean fileExists(String filePath) {
        return new File(filePath).exists();
    }

    public String copyImage(String from, String to, boolean overwriteIfExits) throws IOException {
        File fromFile = new File(from);
        if (!fileExists(to + File.separator + fromFile.getName()) || overwriteIfExits) {
            FileUtils.copyFileToDirectory(fromFile, new File(to));
        }
        return fromFile.getName();
    }

    public String copyImage(String from, String to) throws IOException {
        return copyImage(from, to, false);
    }

    public void deleteAllFilesInDirectory(String imagesProcessingPath) {
        File imageProcessingDirectory = new File(imagesProcessingPath);

        if (!imageProcessingDirectory.exists()) {
            LOG.warn(String.format("Directory %s doesn't exist", imagesProcessingPath));
            return;
        }

        try {
            FileUtils.cleanDirectory(imageProcessingDirectory);
        } catch (IOException e) {
            LOG.debug("Unable to clean directory:" + imageProcessingDirectory);
        }
    }
}
