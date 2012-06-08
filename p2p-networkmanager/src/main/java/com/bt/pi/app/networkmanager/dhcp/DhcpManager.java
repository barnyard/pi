/* (c) British Telecommunications plc, 2009, All Rights Reserved */
package com.bt.pi.app.networkmanager.dhcp;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.annotation.Resource;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.stereotype.Component;

import com.bt.pi.app.common.entities.SecurityGroup;
import com.bt.pi.app.common.net.utils.VlanAddressUtils;
import com.bt.pi.app.networkmanager.NetworkManagerApplication;
import com.bt.pi.core.application.activation.ApplicationRegistry;
import com.bt.pi.core.application.activation.ApplicationStatus;
import com.bt.pi.core.application.activation.UnknownApplicationException;
import com.bt.pi.core.application.resource.ConsumedDhtResourceRegistry;
import com.bt.pi.core.cli.commands.CommandExecutionException;
import com.bt.pi.core.conf.Property;
import com.bt.pi.core.util.common.CommandResult;
import com.bt.pi.core.util.common.CommandRunner;

@Component
public class DhcpManager {
    private static final Log LOG = LogFactory.getLog(DhcpManager.class);
    private static final String APPLICATION_REGISTRY_NOT_FOUND = "Application registry isn't aware of network manager, probably just starting up!";
    private static final String VNET_DHCPDAEMON = "vnet.dhcpdaemon";
    private static final long ONE_THOUSAND = 1000;
    private static final String S_S = "%s/%s";
    private static final String S_SLASH_S = S_S;
    private static final String PI_DHCP_CONF_FILENAME = "pi-dhcp.conf";
    private static final Object PI_DHCP_CONF_BACKUP_FILENAME = "pi-dhcp.conf.2";
    private static final String PI_DHCP_PID_FILENAME = "pi-dhcp.pid";
    private static final String PI_DHCP_LEASES_FILENAME = "pi-dhcp.leases";
    private static final int DHCP_DAEMON_KILL_SLEEP_MILLIS = 500;
    private static final int MAX_SHUTDOWN_WAIT_SECS = 5;
    private static final int DEFAULT_DHCP_DAEMON_RUNNING_CHECK_SECS = 120;
    private CountDownLatch shutdownLatch;
    private volatile boolean isActive;
    private long lastDhcpDaemonRunningCheckTime;
    private int dhcpDaemonRunningCheckFrequencySecs;
    private String dhcpDaemonPath;
    private String netRuntimePath;

    @Resource
    private CommandRunner commandRunner;
    @Resource
    private DhcpConfigurationGenerator dhcpConfigurationGenerator;
    @Resource
    private ConsumedDhtResourceRegistry consumedDhtResourceRegistry;
    @Resource
    private ApplicationRegistry applicationRegistry;
    @Resource
    private DhcpUtils dhcpUtils;

    public DhcpManager() {
        isActive = false;
        shutdownLatch = new CountDownLatch(1);
        dhcpDaemonRunningCheckFrequencySecs = Integer.parseInt(System.getProperty("dhcp.daemon.running.check.freq.sec", Integer.toString(DEFAULT_DHCP_DAEMON_RUNNING_CHECK_SECS)));
        lastDhcpDaemonRunningCheckTime = 0;

        commandRunner = null;
        dhcpConfigurationGenerator = null;
        consumedDhtResourceRegistry = null;
        applicationRegistry = null;
        dhcpUtils = null;
    }

    protected void setDhcpDaemonRunningCheckFrequencySecs(int aDhcpDaemonRunningCheckFrequencySecs) {
        this.dhcpDaemonRunningCheckFrequencySecs = aDhcpDaemonRunningCheckFrequencySecs;
    }

    protected void setLastDhcpDaemonRunningCheckTime(long aLastDhcpDaemonRunningCheckTime) {
        this.lastDhcpDaemonRunningCheckTime = aLastDhcpDaemonRunningCheckTime;
    }

    public void setCommandRunner(CommandRunner aCommandRunner) {
        commandRunner = aCommandRunner;
    }

    @PostConstruct
    public void initialise() {
        LOG.info(String.format("Initialising DHCP..."));
        lastDhcpDaemonRunningCheckTime = System.currentTimeMillis();
        bringUpDhcpDaemon();

        startPollerThread();

        LOG.info(String.format("Done initialising DHCP."));
    }

    protected void bringUpDhcpDaemon() {
        LOG.debug(String.format("Killing existing dhcpd process if present"));
        String piDhcpPidFilePath = String.format(S_SLASH_S, netRuntimePath, PI_DHCP_PID_FILENAME);
        killDhcpDaemon(piDhcpPidFilePath, false);

        dhcpUtils.deleteDhcpFileIfExists(piDhcpPidFilePath);

        ApplicationStatus networkManagerStatus = null;
        try {
            networkManagerStatus = applicationRegistry.getApplicationStatus(NetworkManagerApplication.APPLICATION_NAME);
        } catch (UnknownApplicationException e) {
            LOG.warn(APPLICATION_REGISTRY_NOT_FOUND);
            return;
        }

        List<SecurityGroup> securityGroups = consumedDhtResourceRegistry.getByType(SecurityGroup.class);
        if (securityGroups.size() > 0 && ApplicationStatus.ACTIVE.equals(networkManagerStatus)) {
            LOG.debug(String.format("Starting up dhcpd process"));
            this.restartDhcpDaemon(dhcpDaemonPath, netRuntimePath, securityGroups, false);
        } else {
            LOG.debug(String.format("Network Manager App Status is not active (%s) or No security groups found, dhcpd will be started the next time it is required", networkManagerStatus));
        }
    }

    private void startPollerThread() {
        LOG.info(String.format("Created %s", this.getClass().getSimpleName()));
        isActive = true;
        Thread t = new Thread(new Runnable() {
            @Override
            public void run() {
                processQueuedRestartRequests();
            }
        }, "DHCP-Manager-Worker-Thread");
        t.start();
    }

    private void processQueuedRestartRequests() {
        LOG.info(String.format("Starting to process DHCP refresh requests"));
        while (isActive) {
            try {
                processRefreshQueue();
                doPeriodicDhcpDaemonCheck();
            } catch (Throwable t) {
                LOG.error("Error processing DCHP refresh queue: " + t.getMessage(), t);
            }
        }
        LOG.info(String.format("Stopped processing DHCP refresh requests"));
        shutdownLatch.countDown();
    }

    protected void processRefreshQueue() throws InterruptedException {
        DhcpRefreshToken dhcpRefreshToken = dhcpUtils.pollDhcpRefreshQueue();

        if (dhcpRefreshToken != null) {
            try {
                this.restartDhcpDaemon(dhcpRefreshToken.getDhcpDaemonPath(), dhcpRefreshToken.getNetRuntimePath(), (List<SecurityGroup>) dhcpRefreshToken.getSecurityGroups());
                dhcpRefreshToken.flagCompleted();
            } catch (Throwable t) {
                LOG.info(String.format("DHCP refresh failed with message %s, delegating exception to caller", t.getMessage()));
                dhcpRefreshToken.flagFailed(t);
            }
        }
    }

    protected void doPeriodicDhcpDaemonCheck() {
        if (lastDhcpDaemonRunningCheckTime == 0) {
            LOG.warn(String.format("Skipping dhcp daemon check, prev check time not yet initialized"));
            return;
        }

        long currentTime = System.currentTimeMillis();
        long timeSinceLastCheck = currentTime - lastDhcpDaemonRunningCheckTime;
        if (timeSinceLastCheck > dhcpDaemonRunningCheckFrequencySecs * ONE_THOUSAND) {
            lastDhcpDaemonRunningCheckTime = System.currentTimeMillis();
            // check
            CommandResult commandResult = commandRunner.runInShell("ps -ef | grep dhcp");
            List<String> lines = commandResult.getOutputLines();
            boolean isRunning = false;
            for (String line : lines) {
                if (line.indexOf(PI_DHCP_CONF_FILENAME) > -1) {
                    isRunning = true;
                    break;
                }
            }
            if (!isRunning) {
                LOG.warn(String.format("!!! DHCP daemon not running, going to start it up !!!"));
                bringUpDhcpDaemon();
                return;
            }

            LOG.debug(String.format("DHCP daemon seems to be running"));
            ApplicationStatus networkManagerStatus = null;
            try {
                networkManagerStatus = applicationRegistry.getApplicationStatus(NetworkManagerApplication.APPLICATION_NAME);
            } catch (UnknownApplicationException e) {
                LOG.warn(APPLICATION_REGISTRY_NOT_FOUND);
            }

            if (!ApplicationStatus.ACTIVE.equals(networkManagerStatus)) {
                killDhcpDaemon(String.format(S_SLASH_S, netRuntimePath, PI_DHCP_PID_FILENAME), false);
            }
        }
    }

    @PreDestroy
    public void destroy() {
        LOG.info(String.format("Destroying %s", this.getClass().getSimpleName()));
        isActive = false;
        boolean shutDownCleanly;
        try {
            shutDownCleanly = shutdownLatch.await(MAX_SHUTDOWN_WAIT_SECS, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            throw new RuntimeException(e.getMessage(), e);
        }

        if (!shutDownCleanly)
            LOG.warn(String.format("%s did not shut down within %s seconds", this.getClass().getSimpleName(), MAX_SHUTDOWN_WAIT_SECS));

        List<SecurityGroup> securityGroups = consumedDhtResourceRegistry.getByType(SecurityGroup.class);
        generateDhcpConfigurationFile(netRuntimePath, securityGroups);
    }

    protected void restartDhcpDaemon(String aDhcpDaemonPath, String aNetRuntimePath, List<SecurityGroup> securityGroups) {
        restartDhcpDaemon(aDhcpDaemonPath, aNetRuntimePath, securityGroups, true);
    }

    protected void restartDhcpDaemon(String aDhcpDaemonPath, String aNetRuntimePath, List<SecurityGroup> securityGroups, boolean killDhcpDaemon) {
        LOG.debug(String.format("restartDhcpDaemon(%s, %s, %s, %s)", aDhcpDaemonPath, aNetRuntimePath, securityGroups, killDhcpDaemon));

        generateDhcpConfigurationFile(aNetRuntimePath, securityGroups);

        String[] interfaces = getInterfaces(securityGroups);
        if (interfaces.length == 0) {
            LOG.info(String.format("Did not get any interfaces, not restartind DHCP daemon"));
            return;
        }

        StringBuilder interfacesParamsBuilder = new StringBuilder();
        String sep = "";
        for (String currentInterface : interfaces) {
            interfacesParamsBuilder.append(sep);
            interfacesParamsBuilder.append(currentInterface);
            sep = " ";
        }
        String interfacesParams = interfacesParamsBuilder.toString();

        String piDhcpPidFilePath = String.format(S_SLASH_S, aNetRuntimePath, PI_DHCP_PID_FILENAME);
        if (killDhcpDaemon)
            killDhcpDaemon(piDhcpPidFilePath, true);

        String piDhcpLeasesFilePath = String.format(S_SLASH_S, aNetRuntimePath, PI_DHCP_LEASES_FILENAME);
        dhcpUtils.touchFile(piDhcpLeasesFilePath);

        String piDhcpConfFilePath = String.format(S_SLASH_S, aNetRuntimePath, PI_DHCP_CONF_FILENAME);
        String piDhcpConfBackupFilePath = String.format(S_SLASH_S, aNetRuntimePath, PI_DHCP_CONF_BACKUP_FILENAME);

        try {
            startDhcpDaemon(aDhcpDaemonPath, interfacesParams, piDhcpPidFilePath, piDhcpLeasesFilePath, piDhcpConfFilePath);
            LOG.debug(String.format("Copying %s to %s", piDhcpConfFilePath, piDhcpConfBackupFilePath));
            dhcpUtils.copyFile(piDhcpConfFilePath, piDhcpConfBackupFilePath);
        } catch (CommandExecutionException e) {
            LOG.error("Error starting dhcp daemon", e);
            recoverDhcpConfigurationAndStartDaemon(aDhcpDaemonPath, aNetRuntimePath, interfacesParams, piDhcpPidFilePath, piDhcpLeasesFilePath, piDhcpConfFilePath, piDhcpConfBackupFilePath);
        } catch (IOException e) {
            LOG.error(String.format("Unable to backup %s to %s", piDhcpConfFilePath, piDhcpConfBackupFilePath));
        }
    }

    private void recoverDhcpConfigurationAndStartDaemon(String aDhcpDaemonPath, String aNetRuntimePath, String interfacesParams, String piDhcpPidFilePath, String piDhcpLeasesFilePath, String piDhcpConfFilePath, String piDhcpConfBackupFilePath) {
        LOG.debug(String.format("Trying to recover last backed up file: %s", piDhcpConfBackupFilePath));
        try {
            killDhcpDaemon(piDhcpPidFilePath, false);
            dhcpUtils.copyFile(piDhcpConfBackupFilePath, piDhcpConfFilePath);
            startDhcpDaemon(aDhcpDaemonPath, interfacesParams, piDhcpPidFilePath, piDhcpLeasesFilePath, piDhcpConfFilePath);
        } catch (IOException ioe) {
            LOG.error(String.format("Unable to recover %s from %s", piDhcpConfFilePath, piDhcpConfBackupFilePath), ioe);
        }
    }

    private void startDhcpDaemon(String aDhcpDaemonPath, String interfacesParams, String piDhcpPidFilePath, String piDhcpLeasesFilePath, String piDhcpConfFilePath) {
        String startCommand = String.format("%s -cf %s -lf %s -pf %s %s", aDhcpDaemonPath, piDhcpConfFilePath, piDhcpLeasesFilePath, piDhcpPidFilePath, interfacesParams);
        LOG.info(String.format("Starting DHCP daemon with %s", startCommand));
        commandRunner.run(startCommand);
    }

    private void killDhcpDaemon(String piDhcpPidFilePath, boolean failOnError) {
        LOG.debug(String.format("Going to kill dhcp daemon with PID as given by %s", piDhcpPidFilePath));
        if (dhcpUtils.getFileIfExists(piDhcpPidFilePath) != null) {
            String killCommand = String.format("cat %s | xargs kill -9", piDhcpPidFilePath);
            LOG.info(String.format("Stopping DHCP daemon via '%s'", killCommand));
            try {
                commandRunner.runInShell(killCommand);
            } catch (CommandExecutionException e) {
                if (failOnError) {
                    throw e;
                } else {
                    LOG.warn(String.format("Failed to kill dhcp daemon!"));
                }
            }
            LOG.debug(String.format("Sleeping to allow dhcpd kill command to complete"));
            try {
                Thread.sleep(DHCP_DAEMON_KILL_SLEEP_MILLIS);
            } catch (InterruptedException e) {
                throw new RuntimeException(e.getMessage(), e);
            }
        } else {
            LOG.debug(String.format("PID file %s not found, not killing dhcpd", piDhcpPidFilePath));
        }
    }

    private void generateDhcpConfigurationFile(String aNetRuntimePath, List<SecurityGroup> securityGroups) {
        String dhcpConfiguration = dhcpConfigurationGenerator.generate(String.format("%s.ftl", PI_DHCP_CONF_FILENAME), securityGroups);
        LOG.debug(String.format("Generated DHCP configuration:\n%s\n", dhcpConfiguration));

        if ("integration".equals(System.getProperty("koala.env"))) {
            LOG.warn("Running Integration tests therefore not writing dhcp conf file");
        } else {
            dhcpUtils.writeConfigToFile(String.format(S_S, aNetRuntimePath, PI_DHCP_CONF_FILENAME), dhcpConfiguration);
        }
    }

    private String[] getInterfaces(List<SecurityGroup> securityGroups) {
        List<String> interfacesList = new ArrayList<String>();

        for (SecurityGroup securityGroup : securityGroups) {
            interfacesList.add(VlanAddressUtils.getBridgeNameForVlan(securityGroup.getVlanId()));
        }

        return interfacesList.toArray(new String[interfacesList.size()]);
    }

    @Property(key = VNET_DHCPDAEMON)
    public void setVnetDhcpDaemon(String value) {
        this.dhcpDaemonPath = value;
    }

    @Property(key = "net.runtime.path")
    public void setNetRuntimePath(String value) {
        this.netRuntimePath = value;
    }

    public DhcpRefreshToken requestDhcpRefresh(List<SecurityGroup> securityGroups) {
        LOG.debug(String.format("requestDhcpRefresh(%s)", securityGroups));
        DhcpRefreshToken dhcpRefreshToken = new DhcpRefreshToken(dhcpDaemonPath, netRuntimePath, securityGroups);
        dhcpUtils.addToDhcpRefreshQueue(dhcpRefreshToken);
        return dhcpRefreshToken;
    }
}