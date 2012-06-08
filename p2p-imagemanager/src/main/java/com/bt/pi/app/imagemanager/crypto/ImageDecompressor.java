package com.bt.pi.app.imagemanager.crypto;

import java.io.File;

import javax.annotation.Resource;

import org.apache.commons.io.FileUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.stereotype.Component;

import com.bt.pi.core.cli.commands.CommandExecutionException;
import com.bt.pi.core.util.common.CommandRunner;

@Component
public class ImageDecompressor {
    private static final Log LOG = LogFactory.getLog(ImageDecompressor.class);
    private CommandRunner commandRunner;

    public ImageDecompressor() {
        this.commandRunner = null;
    }

    public void unZipImage(String decryptedImageName, String tarredImageName) {
        LOG.info(String.format("unZipImage(%s, %s)", decryptedImageName, tarredImageName));
        commandRunner.runInShell(String.format("/bin/gunzip -c %s > %s", decryptedImageName, tarredImageName));
        LOG.debug(String.format("file %s unzipped to %s, size = %d", decryptedImageName, tarredImageName, new File(tarredImageName).length()));
    }

    public File untarImage(String tarredImageName, String imageName) {
        LOG.info(String.format("untarImage(%s, %s)", tarredImageName, imageName));
        File untarredFile = new File(imageName);
        try {
            commandRunner.runInShell(String.format("/bin/tar xfO %s > %s", tarredImageName, imageName));
            LOG.debug(String.format("file %s untarred to %s, size = %d", tarredImageName, imageName, untarredFile.length()));
            return untarredFile;
        } catch (CommandExecutionException ex) {
            // DL: delete file if the untar fails
            FileUtils.deleteQuietly(untarredFile);
            throw ex;
        }
    }

    @Resource
    public void setCommandRunner(CommandRunner aCommandRunner) {
        this.commandRunner = aCommandRunner;
    }
}
