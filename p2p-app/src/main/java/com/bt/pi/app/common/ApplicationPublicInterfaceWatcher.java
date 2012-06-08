package com.bt.pi.app.common;

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.stereotype.Component;

import com.bt.pi.app.common.net.NetworkCommandRunner;
import com.bt.pi.core.application.activation.ApplicationInfo;
import com.bt.pi.core.application.activation.ApplicationRecord;
import com.bt.pi.core.application.activation.ApplicationRegistry;
import com.bt.pi.core.application.activation.ApplicationStatus;
import com.bt.pi.core.conf.Property;

@Component
public class ApplicationPublicInterfaceWatcher {

    private static final String DEFAULT_VNET_PUBLIC_INTERACE = "eth1";
    private static final Log LOG = LogFactory.getLog(ApplicationPublicInterfaceWatcher.class);
    private static final String DEFAULT_WATCHER_INTERVAL = "120";
    private ApplicationRegistry applicationRegistry;
    private NetworkCommandRunner networkCommandRunner;
    private String vnetPublicInterface;
    private ScheduledExecutorService scheduledExecutorService;

    private long applicationPublicInterfaceWatcherIntervalSeconds;
    private ScheduledFuture<?> scheduleWithFixedDelay;

    public ApplicationPublicInterfaceWatcher() {
        applicationPublicInterfaceWatcherIntervalSeconds = Integer.parseInt(DEFAULT_WATCHER_INTERVAL);
        applicationRegistry = null;
        networkCommandRunner = null;
        scheduleWithFixedDelay = null;
    }

    @Resource
    public void setApplicationRegistry(ApplicationRegistry registry) {
        applicationRegistry = registry;
    }

    @Resource
    public void setNetworkCommandRunner(NetworkCommandRunner commandRunner) {
        networkCommandRunner = commandRunner;
    }

    @Resource
    public void setScheduledExecutorService(ScheduledExecutorService aScheduledExecutorService) {
        scheduledExecutorService = aScheduledExecutorService;
    }

    @Property(key = "vnet.public.interface", defaultValue = DEFAULT_VNET_PUBLIC_INTERACE)
    public void setVnetPublicInterface(String value) {
        this.vnetPublicInterface = value;
    }

    @Property(key = "application.public.ip.watcher.interval.seconds", defaultValue = DEFAULT_WATCHER_INTERVAL)
    public void setApplicationPublicIpWatcherIntervalSeconds(int value) {
        applicationPublicInterfaceWatcherIntervalSeconds = value;
    }

    @PostConstruct
    public void startApplicationPublicIpWatcher() {
        LOG.debug(String.format("Scheduling application public ip watcher to run every %d seconds", applicationPublicInterfaceWatcherIntervalSeconds));
        scheduleWithFixedDelay = scheduledExecutorService.scheduleWithFixedDelay(new Runnable() {
            @Override
            public void run() {
                checkApplicationAddresses();
            }
        }, 0, applicationPublicInterfaceWatcherIntervalSeconds, TimeUnit.SECONDS);
    }

    public void checkApplicationAddresses() {
        LOG.debug("Running checkApplicationAddresses()");
        for (String appName : applicationRegistry.getApplicationNames()) {
            ApplicationInfo appInfo = applicationRegistry.getApplicationInfor(appName);
            LOG.debug(String.format("Checking application %s to see if we should add the interface.", appName));
            if (appInfo.getApplication() instanceof AbstractManagedAddressingPiApplication) {
                LOG.debug(String.format("Application %s is an AddressingPiApplication. Current appInfo: %s.", appName, appInfo));
                ApplicationRecord appRecord = appInfo.getCachedApplicationRecord();
                if (appRecord == null) {
                    LOG.debug("Unable to determine ip resource for ");
                } else if (appInfo.getApplicationStatus() == ApplicationStatus.ACTIVE) {
                    String ipAddress = appRecord.getAssociatedResource(appInfo.getApplication().getNodeId());
                    LOG.debug(String.format("Adding address %s as application %s is active.", ipAddress, appRecord.getApplicationName()));
                    networkCommandRunner.addIpAddressAndSendArping(ipAddress, vnetPublicInterface);
                    networkCommandRunner.addDefaultGatewayRouteToDevice(vnetPublicInterface);
                } else {
                    for (String ipAddress : appRecord.getActiveNodeMap().keySet()) {
                        LOG.debug(String.format("Removing address %s as application %s is not active.", ipAddress, appRecord.getApplicationName()));
                        networkCommandRunner.ipAddressDelete(ipAddress, vnetPublicInterface);
                    }
                }
            }
        }
    }

    public void stopWatchingApplicationPublicAddressOnShuttingDown() {
        LOG.debug("Stopping application public ip address watcher");
        if (null != scheduleWithFixedDelay)
            scheduleWithFixedDelay.cancel(true);
    }
}
