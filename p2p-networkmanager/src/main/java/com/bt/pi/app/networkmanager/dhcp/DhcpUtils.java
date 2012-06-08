package com.bt.pi.app.networkmanager.dhcp;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import org.apache.commons.io.FileUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.stereotype.Component;

@Component
public class DhcpUtils {
    private static final Log LOG = LogFactory.getLog(DhcpUtils.class);
    private static final int SHUTDOWN_CHECK_INTERVAL_MILLIS = 250;

    private LinkedBlockingQueue<DhcpRefreshToken> dhcpRefreshQueue;

    public DhcpUtils() {
        dhcpRefreshQueue = new LinkedBlockingQueue<DhcpRefreshToken>();
    }

    public File getFileIfExists(String path) {
        File f = new File(path);
        if (f.exists())
            return f;
        else
            return null;
    }

    public void deleteDhcpFileIfExists(String piDhcpPidFilePath) {
        File pidFile = getFileIfExists(piDhcpPidFilePath);
        if (pidFile != null) {
            LOG.debug(String.format("Removing existing dhcpd pid file %s", piDhcpPidFilePath));
            boolean deleted = pidFile.delete();
            if (!deleted)
                LOG.warn(String.format("Deletion of %s unexpectedly failed", piDhcpPidFilePath));
        }
    }

    public void copyFile(String from, String to) throws IOException {
        FileUtils.copyFile(new File(from), new File(to));
    }

    public void touchFile(String filePath) {
        try {
            FileUtils.touch(new File(filePath));
        } catch (IOException e) {
            throw new RuntimeException(String.format("Failed to touch file %s", filePath), e);
        }
    }

    public void writeConfigToFile(String absoluteDhcpConfigurationFilePath, String dhcpConfiguration) {
        LOG.debug(String.format("Writing dhcp configuration to file %s", absoluteDhcpConfigurationFilePath));
        try {
            FileUtils.writeStringToFile(new File(absoluteDhcpConfigurationFilePath), dhcpConfiguration);
        } catch (IOException e) {
            throw new RuntimeException(String.format("Error writing dhcp config file %s", absoluteDhcpConfigurationFilePath), e);
        }
    }

    public DhcpRefreshToken pollDhcpRefreshQueue() throws InterruptedException {
        return dhcpRefreshQueue.poll(SHUTDOWN_CHECK_INTERVAL_MILLIS, TimeUnit.MILLISECONDS);
    }

    public void addToDhcpRefreshQueue(DhcpRefreshToken dhcpRefreshToken) {
        dhcpRefreshQueue.add(dhcpRefreshToken);
    }
}
