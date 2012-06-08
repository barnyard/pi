package com.bt.pi.app.instancemanager.reporting;

import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;

import com.bt.pi.app.common.entities.Instance;
import com.bt.pi.app.common.id.PiIdBuilder;
import com.bt.pi.app.instancemanager.images.InstanceImageManager;
import com.bt.pi.core.application.reporter.ReportingApplication;
import com.bt.pi.core.application.resource.ConsumedDhtResourceRegistry;
import com.bt.pi.core.conf.Property;
import com.bt.pi.core.node.NodeStartedEvent;

@Component
public class NodeInstanceReporter implements ApplicationListener<NodeStartedEvent> {
    private static final Log LOG = LogFactory.getLog(NodeInstanceReporter.class);
    private static final String DEFAULT_INSTANCE_PUBLISH_DELAY_SECONDS = "900";

    private AtomicBoolean nodeStarted;
    private long publishIntervalSeconds;

    @Resource
    private PiIdBuilder piIdBuilder;
    @Resource
    private ConsumedDhtResourceRegistry consumedDhtResourceRegistry;
    @Resource
    private InstanceImageManager instanceImageManager;
    @Resource
    private ReportingApplication reportingApplication;
    @Resource
    private ScheduledExecutorService scheduledExecutorService;

    public NodeInstanceReporter() {
        nodeStarted = new AtomicBoolean(false);
        publishIntervalSeconds = Integer.parseInt(DEFAULT_INSTANCE_PUBLISH_DELAY_SECONDS);

        consumedDhtResourceRegistry = null;
        instanceImageManager = null;
        reportingApplication = null;
        scheduledExecutorService = null;
    }

    @Property(key = "instance.report.publishintervalsize", defaultValue = DEFAULT_INSTANCE_PUBLISH_DELAY_SECONDS)
    public void setPublishIntervalSeconds(int aPublishIntervalSeconds) {
        publishIntervalSeconds = aPublishIntervalSeconds;
    }

    @PostConstruct
    public void scheduleReportingOfRunningInstances() {
        scheduledExecutorService.scheduleWithFixedDelay(new Runnable() {
            @Override
            public void run() {
                reportRunningInstances();
            }
        }, 0, publishIntervalSeconds, TimeUnit.SECONDS);
    }

    public void reportRunningInstances() {
        if (nodeStarted.get()) {
            Collection<String> runningInstanceIds = instanceImageManager.getRunningInstances();
            Collection<InstanceReportEntity> instanceReportEntities = new ArrayList<InstanceReportEntity>();
            for (String runningInstanceId : runningInstanceIds) {
                Instance runningInstance = consumedDhtResourceRegistry.getCachedEntity(piIdBuilder.getPIdForEc2AvailabilityZone(Instance.getUrl(runningInstanceId)));
                if (runningInstance == null)
                    LOG.warn(String.format("Unable to find instance details for running instance %s in the consumed dht resource registry", runningInstanceId));
                else {
                    instanceReportEntities.add(new InstanceReportEntity(runningInstance));
                }
            }

            InstanceReportEntityCollection instanceReportEntityCollection = new InstanceReportEntityCollection();
            instanceReportEntityCollection.setEntities(instanceReportEntities);

            reportingApplication.sendReportingUpdateToASuperNode(instanceReportEntityCollection);
        }
    }

    @Override
    public void onApplicationEvent(NodeStartedEvent event) {
        nodeStarted.set(true);
    }
}
