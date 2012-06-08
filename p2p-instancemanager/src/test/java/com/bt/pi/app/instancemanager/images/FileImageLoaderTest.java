package com.bt.pi.app.instancemanager.images;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Matchers;

import com.bt.pi.app.common.os.FileManager;

public class FileImageLoaderTest {
    FileImageLoader fileImageLoader;
    FileManager fileManager;

    @Before
    public void setUp() throws Exception {
        fileManager = mock(FileManager.class);

        fileImageLoader = new FileImageLoader();
        fileImageLoader.setFileManager(fileManager);
    }

    @Test
    public void shouldLoadFileFromDisk() throws Exception {
        // setup
        fileImageLoader.setImagePath("/etc");
        when(fileManager.copyImage(Matchers.isA(String.class), Matchers.isA(String.class))).thenReturn("image.path");

        String imagePath = "image.path";
        String piCacheDirectory = "/opt/cache";

        // act

        String savedImagePath = fileImageLoader.saveImage(imagePath, piCacheDirectory);

        // assert
        assertEquals(String.format("%s/image.path", piCacheDirectory), savedImagePath);
        verify(fileManager).copyImage("/etc/" + imagePath, piCacheDirectory);
    }

    @Test(expected = ImageLoaderException.class)
    public void shouldThrowImageLoaderExceptionIfFailedToCopy() throws IOException {
        // setup
        String imagePath = "image.path";
        String piCacheDirectory = "/opt/cache";

        when(fileManager.copyImage(Matchers.isA(String.class), Matchers.isA(String.class))).thenThrow(new IOException());

        // act
        fileImageLoader.saveImage(imagePath, piCacheDirectory);
    }
}
