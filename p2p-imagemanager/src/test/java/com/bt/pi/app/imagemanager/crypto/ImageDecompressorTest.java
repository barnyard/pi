package com.bt.pi.app.imagemanager.crypto;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.Executor;

import org.apache.commons.io.FileUtils;
import org.junit.Before;
import org.junit.Test;

import com.bt.pi.core.util.common.CommandLineParser;
import com.bt.pi.core.util.common.CommandRunner;

public class ImageDecompressorTest {

    private static final String INPUT_TEST_TAR_TGZ = "./src/test/resources/test-tar.tgz";
    private static final String INPUT_TEST_TAR = "./src/test/resources/test-tar.tar";
    private static final String INPUT_TEST_FILE = "./src/test/resources/test-file.txt";

    private ImageDecompressor imageDecompressor;
    private CommandRunner commandRunner;

    @Before
    public void setUp() throws Exception {
        File inputTarFile = new File(INPUT_TEST_TAR_TGZ);
        File outputTarFile = createTempFile();
        FileUtils.copyFile(inputTarFile, outputTarFile);
        this.imageDecompressor = new ImageDecompressor();
        this.commandRunner = new CommandRunner();
        this.commandRunner.setCommandLineParser(new CommandLineParser());
        this.commandRunner.setExecutor(new Executor() {
            @Override
            public void execute(Runnable command) {
                command.run();
            }
        });
        this.commandRunner.setNicelyCommandPrefix("nice -n +10 ionice -c3");

        this.imageDecompressor.setCommandRunner(commandRunner);
    }

    @Test
    public void unzipImageShouldUnZipACompressedImageFileOk() throws Exception {
        // setup
        File result = createTempFile();
        result.delete();

        // act
        this.imageDecompressor.unZipImage(INPUT_TEST_TAR_TGZ, result.getAbsolutePath());

        // assert
        assertTrue(new File(result.getAbsolutePath()).exists());
        assertEquals(getFileCheckSum(new File(INPUT_TEST_TAR)), getFileCheckSum(result));
    }

    private long getFileCheckSum(File file) throws IOException {
        return FileUtils.checksumCRC32(file);
    }

    private File createTempFile() throws Exception {
        File result = File.createTempFile("unittesting", null);
        result.deleteOnExit();
        return result;
    }

    @Test
    public void untarImageShouldUnTarATarredImageFileOk() throws Exception {
        // setup
        File tmpFile = createTempFile();
        tmpFile.delete();

        // act
        File result = this.imageDecompressor.untarImage(INPUT_TEST_TAR, tmpFile.getAbsolutePath());

        // assert
        assertTrue(new File(result.getAbsolutePath()).exists());
        assertEquals(getFileCheckSum(new File(INPUT_TEST_FILE)), getFileCheckSum(result));
    }
}
