package com.bt.pi.app.common.os;

import static org.junit.Assert.assertEquals;

import java.io.File;

import org.apache.commons.io.FileUtils;
import org.junit.Test;

public class FileManagerTest {
    private FileManager fileManager = new FileManager();
    private String data = "the cat sat on the mat";

    @Test
    public void shouldCopyFile() throws Exception {
        // setup
        File from = File.createTempFile("unittesting", null);
        from.deleteOnExit();
        String to = System.getProperty("java.io.tmpdir");
        FileUtils.writeStringToFile(from, data);

        // act
        String copiedPath = fileManager.copyImage(from.getPath(), to);

        // assert
        File dest = new File(to + "/" + from.getName());
        assertEquals(dest.getName(), copiedPath);
        assertEquals(data, FileUtils.readFileToString(dest));
        dest.delete();
    }

    @Test
    public void shouldSaveFile() throws Exception {
        // setup
        File to = File.createTempFile("unittesting", null);
        to.deleteOnExit();

        // act
        fileManager.saveFile(to.getPath(), data);

        // assert
        assertEquals(data, FileUtils.readFileToString(to));
    }

    @Test
    public void shouldDeleteAllFilesinDirectory() throws Exception {
        // setup
        String imagesProcessingPath = "image_processing";
        String tmpDir = String.format("%s/%s", System.getProperty("java.io.tmpdir"), imagesProcessingPath);

        File imageProcessingDirectory = new File(tmpDir);
        FileUtils.forceMkdir(imageProcessingDirectory);

        for (int i = 0; i < 10; i++) {
            FileUtils.writeStringToFile(new File(String.format("%s/unittest%d", tmpDir, i)), "Hello World");
        }

        // act
        fileManager.deleteAllFilesInDirectory(tmpDir);

        // assert
        File[] listFiles = imageProcessingDirectory.listFiles();
        assertEquals(0, listFiles.length);

        FileUtils.forceDelete(imageProcessingDirectory);
    }
}
