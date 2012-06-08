package com.bt.pi.app.imagemanager.crypto;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.util.List;

import javax.annotation.Resource;

import org.apache.commons.io.FileUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.stereotype.Component;

import com.bt.pi.app.common.entities.User;
import com.bt.pi.app.imagemanager.xml.Manifest;
import com.bt.pi.core.conf.Property;
import com.bt.pi.sss.client.PisssClient;

@Component
public class ImageAssembler {
    private static final Log LOG = LogFactory.getLog(ImageAssembler.class);
    private static final String DEFAULT_IMAGE_PROCESSING_PATH = "var/image_processing";
    private String imagesProcessingPath = DEFAULT_IMAGE_PROCESSING_PATH;
    @Resource
    private PisssClient pisssClient;

    public ImageAssembler() {
        this.pisssClient = null;
    }

    public File assembleParts(Manifest manifest, String bucketName, User user) throws IOException {
        LOG.debug(String.format("assembleParts(%s, %s, %s)", manifest, bucketName, user));
        List<String> parts = manifest.getPartFilenames();
        File outputFile = null;
        FileChannel out = null;
        try {
            outputFile = File.createTempFile("imageConstruction", null, new File(this.imagesProcessingPath));
            out = new FileOutputStream(outputFile).getChannel();
            for (String partName : parts) {
                File tmpFile = File.createTempFile("imageConstructionPart", null, new File(this.imagesProcessingPath));
                String filename = tmpFile.getAbsolutePath();
                LOG.debug(String.format("fetching file %s from bucket %s into tmp file %s", partName, bucketName, filename));
                File fileFromBucket = null;
                try {
                    fileFromBucket = pisssClient.getFileFromBucket(bucketName, partName, user, filename);
                    FileChannel in = new FileInputStream(fileFromBucket).getChannel();
                    in.transferTo(0, in.size(), out);
                    in.close();
                } finally {
                    FileUtils.deleteQuietly(fileFromBucket);
                    FileUtils.deleteQuietly(tmpFile);
                }
            }
        } catch (IOException ex) {
            // DL: Deleting output file if part of the assembly fails
            closeFileChannel(out);
            FileUtils.deleteQuietly(outputFile);
            throw ex;
        } finally {
            closeFileChannel(out);
        }
        return outputFile;
    }

    private void closeFileChannel(FileChannel out) throws IOException {
        if (null != out && out.isOpen()) {
            out.close();
        }
    }

    @Property(key = "image.processing.path", defaultValue = DEFAULT_IMAGE_PROCESSING_PATH)
    public void setImageProcessingPath(String value) {
        this.imagesProcessingPath = value;
    }
}
