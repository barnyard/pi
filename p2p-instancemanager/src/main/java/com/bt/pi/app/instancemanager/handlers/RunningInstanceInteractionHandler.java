package com.bt.pi.app.instancemanager.handlers;

import java.io.File;
import java.io.IOException;
import java.util.List;

import javax.annotation.Resource;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.output.LockableFileWriter;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.stereotype.Component;

import com.bt.pi.app.common.entities.ConsoleOutput;
import com.bt.pi.app.common.entities.Instance;
import com.bt.pi.app.common.images.platform.ImagePlatform;
import com.bt.pi.app.common.resource.PiQueue;
import com.bt.pi.app.instancemanager.images.InstanceImageManager;
import com.bt.pi.app.instancemanager.libvirt.LibvirtManager;
import com.bt.pi.core.application.ReceivedMessageContext;
import com.bt.pi.core.conf.Property;
import com.bt.pi.core.entity.EntityResponseCode;
import com.bt.pi.core.id.PId;
import com.bt.pi.core.util.common.CommandResult;

@Component
public class RunningInstanceInteractionHandler extends AbstractHandler {
    private static final String INSTANCE_CONSOLE_LOG_DIR_FORMAT = "%s/%s";
    private static final Log LOG = LogFactory.getLog(RunningInstanceInteractionHandler.class);
    private static final String XM_CONSOLE_COMMAND = "xm console %s";
    private InstanceImageManager instanceImageManager;
    private LibvirtManager libvirtManager;
    private String consoleOutputDirectory;
    private String consoleOutputFileNameFormat;
    private long consoleOutputMaxWaitTime;
    private String consoleOutputCommand;

    public RunningInstanceInteractionHandler() {
        super();
        instanceImageManager = null;
        consoleOutputDirectory = null;
        consoleOutputFileNameFormat = null;
        consoleOutputCommand = null;
        consoleOutputMaxWaitTime = -1;
        libvirtManager = null;
    }

    @Resource
    public void setLibvirtManager(LibvirtManager aLibvirtManager) {
        libvirtManager = aLibvirtManager;
    }

    @Resource
    public void setInstanceImageManager(InstanceImageManager anInstanceImageManager) {
        this.instanceImageManager = anInstanceImageManager;
    }

    @Property(key = "console.output.command", defaultValue = XM_CONSOLE_COMMAND)
    public void setConsoleOutputCommand(String aConsoleOutputCommand) {
        this.consoleOutputCommand = aConsoleOutputCommand;
    }

    @Property(key = "console.output.directory", defaultValue = "/var/log/xen/console")
    public void setConsoleOutputDirectory(String aConsoleOutputDirectory) {
        this.consoleOutputDirectory = aConsoleOutputDirectory;
    }

    @Property(key = "console.output.filename.format", defaultValue = "guest-%s.log")
    public void setConsoleOutputFileNameFormat(String outputFileName) {
        this.consoleOutputFileNameFormat = outputFileName;
    }

    @Property(key = "console.output.max.wait.time.millis", defaultValue = "3000")
    public void setConsoleOutputMaxWaitTime(long millis) {
        this.consoleOutputMaxWaitTime = millis;
    }

    public void rebootInstance(final Instance instance) {
        LOG.debug(String.format("rebootInstance(%s)", instance.getInstanceId()));
        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                try {
                    instanceImageManager.rebootInstance(instance);
                } catch (Throwable t) {
                    LOG.error("Unable to reboot instace. ", t);
                }
            }
        };
        getTaskExecutor().execute(runnable);
    }

    public void respondWithConsoleOutput(final ReceivedMessageContext receivedMessageContext) {
        getTaskExecutor().execute(new Runnable() {
            @Override
            public void run() {
                try {
                    ConsoleOutput cOutput = (ConsoleOutput) receivedMessageContext.getReceivedEntity();
                    receivedMessageContext.sendResponse(setConsoleOutputFromInstance(cOutput), cOutput);
                } catch (Throwable t) {
                    LOG.error("Unable to get console output instace. ", t);
                }
            }
        });
    }

    private EntityResponseCode setConsoleOutputFromInstance(final ConsoleOutput consoleOutput) {
        File instanceConsoleFile = null;
        if (consoleOutput.getImagePlatform().equals(ImagePlatform.windows)) {
            instanceConsoleFile = getWindowsConsoleOutputFile(consoleOutput.getInstanceId());
        } else
            instanceConsoleFile = getVirtualizedConsoleOutputFile(consoleOutput.getInstanceId());

        EntityResponseCode responseCode = EntityResponseCode.OK;
        LOG.debug(String.format("Instance %s console file log: %s", consoleOutput.getInstanceId(), instanceConsoleFile));

        try {
            consoleOutput.setOutput(FileUtils.readFileToString(instanceConsoleFile));
        } catch (IOException e) {
            LOG.warn("Unable to read to file: " + instanceConsoleFile.getAbsolutePath(), e);
            responseCode = EntityResponseCode.ERROR;
        }

        if (EntityResponseCode.ERROR.equals(responseCode)) {
            consoleOutput.setOutput("Unable to retreive latest console output.");
        }

        return responseCode;
    }

    private File getVirtualizedConsoleOutputFile(String instanceId) {
        String outputFileName = String.format(consoleOutputFileNameFormat, instanceId);
        return new File(String.format(INSTANCE_CONSOLE_LOG_DIR_FORMAT, consoleOutputDirectory, outputFileName));
    }

    private File getWindowsConsoleOutputFile(String instanceId) {
        CommandResult commandResult = getCommandRunner().run(String.format(consoleOutputCommand, instanceId), consoleOutputMaxWaitTime, true);
        LOG.debug(String.format("Instance %s console result: %s", instanceId, commandResult));
        File instanceConsoleFile = new File(String.format(INSTANCE_CONSOLE_LOG_DIR_FORMAT, consoleOutputDirectory, String.format(consoleOutputFileNameFormat, instanceId)));
        LockableFileWriter writer = null;
        try {
            writer = new LockableFileWriter(instanceConsoleFile, true);
            writeToFile(commandResult.getOutputLines(), writer);
            writeToFile(commandResult.getErrorLines(), writer);
            writer.flush();
            writer.close();
        } catch (IOException e) {
            LOG.warn("Unable to write to file: " + instanceConsoleFile.getAbsolutePath(), e);
        } finally {
            writer = null;
        }
        return instanceConsoleFile;
    }

    private void writeToFile(List<String> lines, LockableFileWriter writer) throws IOException {
        for (int i = 0; lines != null && i < lines.size(); i++) {
            writer.append(lines.get(i) + "\r\n");
        }
    }

    public void pauseInstance(Instance instance) {
        LOG.debug(String.format("pauseInstance(%s)", instance.getInstanceId()));
        this.libvirtManager.pauseInstance(instance.getInstanceId());
        PId pauseInstanceQueueId = getPiIdBuilder().getPId(PiQueue.PAUSE_INSTANCE.getUrl()).forLocalScope(PiQueue.PAUSE_INSTANCE.getNodeScope());
        getTaskProcessingQueueHelper().removeUrlFromQueue(pauseInstanceQueueId, instance.getUrl());
    }

    public void unPauseInstance(Instance instance) {
        LOG.debug(String.format("unPauseInstance(%s)", instance.getInstanceId()));
        this.libvirtManager.unPauseInstance(instance.getInstanceId());
    }
}
