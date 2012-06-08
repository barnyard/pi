/* (c) British Telecommunications plc, 2010, All Rights Reserved */
package com.bt.pi.app.imagemanager.crypto;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.security.GeneralSecurityException;

import javax.annotation.Resource;
import javax.crypto.Cipher;

import org.apache.commons.io.FileUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.stereotype.Component;

import com.bt.pi.app.common.os.FileManager;
import com.bt.pi.core.conf.Property;

@Component
public class ImageFileDecryptor {
    private static final String S_S = "%s/%s";
    private static final String ERROR_CLOSING_STREAMS = "error closing streams";
    private static final int BYTE_ARRAY_SIZE = 8192;
    private static final String DEFAULT_IMAGE_PROCESSING_PATH = "var/image_processing";
    private static final String DEFAULT_IMAGES_PATH = "var/images";
    private static final Log LOG = LogFactory.getLog(ImageFileDecryptor.class);
    private ImageDecompressor imageDecompressor;
    private String imageProcessingPath = DEFAULT_IMAGE_PROCESSING_PATH;
    private String imagesPath = DEFAULT_IMAGES_PATH;
    private FileManager fileManager;

    public ImageFileDecryptor() {
        this.imageDecompressor = null;
        this.fileManager = null;
    }

    @Property(key = "image.processing.path", defaultValue = DEFAULT_IMAGE_PROCESSING_PATH)
    public void setImageProcessingPath(String value) {
        this.imageProcessingPath = value;
    }

    @Property(key = "image.path", defaultValue = DEFAULT_IMAGES_PATH)
    public void setImagesPath(String value) {
        this.imagesPath = value;
    }

    @Resource
    public void setImageDecompressor(ImageDecompressor aImageDecompressor) {
        this.imageDecompressor = aImageDecompressor;
    }

    @Resource
    public void setFileManager(FileManager aFileManager) {
        this.fileManager = aFileManager;
    }

    public File decryptImage(final File encryptedImageFile, final Cipher cipher) {
        LOG.info(String.format("decryptImage(%s, %s)", encryptedImageFile, cipher));

        BufferedOutputStream out = null;
        BufferedInputStream in = null;
        File decryptedImageFile = null;
        File unzippedImageFile = null;
        File untarredImageFile = null;
        try {
            decryptedImageFile = File.createTempFile("decryptedImageFile", null, new File(this.imageProcessingPath));
            out = new BufferedOutputStream(new FileOutputStream(decryptedImageFile));
            in = new BufferedInputStream(new FileInputStream(encryptedImageFile));

            int bytesRead = 0;
            byte[] bytes = new byte[BYTE_ARRAY_SIZE];

            while ((bytesRead = in.read(bytes)) > 0) {
                byte[] outBytes = cipher.update(bytes, 0, bytesRead);
                out.write(outBytes);
            }
            byte[] outBytes = cipher.doFinal();
            out.write(outBytes);
            out.close();

            LOG.debug(String.format("decrypted to %s, size = %d", decryptedImageFile.getAbsolutePath(), decryptedImageFile.length()));
            unzippedImageFile = File.createTempFile("unzippedImageFile", null, new File(this.imageProcessingPath));
            this.imageDecompressor.unZipImage(decryptedImageFile.getAbsolutePath(), unzippedImageFile.getAbsolutePath());

            untarredImageFile = File.createTempFile("untarredImageFile", null, new File(this.imageProcessingPath));
            File finalImageFile = this.imageDecompressor.untarImage(unzippedImageFile.getAbsolutePath(), untarredImageFile.getAbsolutePath());

            LOG.debug(String.format("copying file %s to %s", finalImageFile.getAbsoluteFile(), this.imagesPath));
            return new File(String.format(S_S, this.imagesPath, this.fileManager.copyImage(finalImageFile.getAbsolutePath(), this.imagesPath)));

        } catch (FileNotFoundException fnfe) {
            throw createImageCryptoException("FileNotFoundException raised when de-crypting the image", fnfe);
        } catch (IOException ioe) {
            throw createImageCryptoException("IOException when writing out to the output stream", ioe);
        } catch (GeneralSecurityException e) {
            throw createImageCryptoException("GeneralSecurityException when decrypting image file", e);
        } finally {
            tryClosingStreams(out, in);

            FileUtils.deleteQuietly(encryptedImageFile);
            FileUtils.deleteQuietly(decryptedImageFile);
            FileUtils.deleteQuietly(unzippedImageFile);
            FileUtils.deleteQuietly(untarredImageFile);
        }
    }

    private void tryClosingStreams(BufferedOutputStream out, BufferedInputStream in) {
        if (in != null) {
            try {
                in.close();
            } catch (IOException e) {
                LOG.error(ERROR_CLOSING_STREAMS, e);
            }
        }
        if (out != null) {
            try {
                out.close();
            } catch (IOException e) {
                LOG.error(ERROR_CLOSING_STREAMS, e);
            }
        }
    }

    private ImageCryptoException createImageCryptoException(String message, Exception e) {
        LOG.error(message, e);
        return new ImageCryptoException(message, e);
    }
}
