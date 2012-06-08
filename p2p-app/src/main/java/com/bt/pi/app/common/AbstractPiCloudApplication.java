package com.bt.pi.app.common;

import java.util.Set;
import java.util.concurrent.TimeUnit;

import javax.annotation.Resource;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.bt.pi.app.common.entities.AvailabilityZone;
import com.bt.pi.app.common.entities.AvailabilityZones;
import com.bt.pi.app.common.id.PiIdBuilder;
import com.bt.pi.app.common.resource.PiTopics;
import com.bt.pi.core.application.KoalaPastryScribeApplicationBase;
import com.bt.pi.core.application.MessageContext;
import com.bt.pi.core.application.PubSubMessageContext;
import com.bt.pi.core.application.ReceivedMessageContext;
import com.bt.pi.core.continuation.GenericContinuation;
import com.bt.pi.core.continuation.PiContinuation;
import com.bt.pi.core.dht.DhtClientFactory;
import com.bt.pi.core.dht.cache.DhtCache;
import com.bt.pi.core.entity.EntityMethod;
import com.bt.pi.core.entity.PiEntity;
import com.bt.pi.core.id.KoalaIdUtils;
import com.bt.pi.core.id.PId;
import com.bt.pi.core.id.SuperNodeIdFactory;
import com.bt.pi.core.scope.NodeScope;
import com.bt.pi.core.util.MDCHelper;

public abstract class AbstractPiCloudApplication extends KoalaPastryScribeApplicationBase {
    public static final String DEFAULT_START_TIMEOUT_MILLIS = "30000";
    public static final String DEFAULT_ACTIVATION_CHECK_PERIOD_SECS = "60";
    private static final Log LOG = LogFactory.getLog(AbstractPiCloudApplication.class);
    private int activationCheckPeriodSecs = Integer.parseInt(DEFAULT_ACTIVATION_CHECK_PERIOD_SECS);
    private long startTimeoutMillis = Long.parseLong(DEFAULT_START_TIMEOUT_MILLIS);
    private String applicationName;
    private DhtCache dhtCache;
    private PiIdBuilder piIdBuilder;
    private DhtClientFactory dhtClientFactory;
    private KoalaIdUtils koalaIdUtils;

    public AbstractPiCloudApplication(String name) {
        applicationName = name;
        dhtCache = null;
        piIdBuilder = null;
        dhtClientFactory = null;
        this.koalaIdUtils = null;
    }

    @Resource
    public void setKoalaIdUtils(KoalaIdUtils aKoalaIdUtils) {
        this.koalaIdUtils = aKoalaIdUtils;
    }

    @Resource
    public final void setPiIdBuilder(PiIdBuilder aPiIdBuilder) {
        piIdBuilder = aPiIdBuilder;
    }

    @Resource
    public final void setDhtClientFactory(DhtClientFactory aDhtClientFactory) {
        this.dhtClientFactory = aDhtClientFactory;
    }

    @Resource(name = "generalCache")
    public void setDhtCache(DhtCache aDhtCache) {
        this.dhtCache = aDhtCache;
    }

    protected DhtCache getDhtCache() {
        return dhtCache;
    }

    protected PiIdBuilder getPiIdBuilder() {
        return piIdBuilder;
    }

    protected DhtClientFactory getDhtClientFactory() {
        return dhtClientFactory;
    }

    @Override
    public String getApplicationName() {
        return this.applicationName;
    }

    public void setActivationCheckPeriodSecs(int value) {
        this.activationCheckPeriodSecs = value;
    }

    public void setStartTimeoutMillis(long value) {
        this.startTimeoutMillis = value;
    }

    @Override
    public int getActivationCheckPeriodSecs() {
        return this.activationCheckPeriodSecs;
    }

    @Override
    public TimeUnit getStartTimeoutUnit() {
        return TimeUnit.MILLISECONDS;
    }

    @Override
    public long getStartTimeout() {
        return this.startTimeoutMillis;
    }

    protected PId getAvailabilityZonesId() {
        return piIdBuilder.getAvailabilityZonesId();
    }

    protected PId getRegionsId() {
        return piIdBuilder.getRegionsId();
    }

    @Override
    public boolean handleAnycast(PubSubMessageContext pubSubMessageContext, EntityMethod entityMethod, PiEntity arg2) {
        return false;
    }

    @Override
    public void deliver(PubSubMessageContext pubSubMessageContext, EntityMethod entityMethod, PiEntity arg2) {
    }

    @Override
    public void deliver(PId arg0, ReceivedMessageContext receivedMessageContext) {
    }

    @Override
    public MessageContext newMessageContext() {
        return super.newMessageContext(MDCHelper.getTransactionUID());
    }

    public PubSubMessageContext newLocalPubSubMessageContext(PiTopics piTopic) {
        LOG.debug(String.format("newLocalPubSubMessageContext(%s)", piTopic));
        return newPubSubMessageContext(getKoalaIdFactory().buildPId(piTopic.getPiLocation().getUrl()).forLocalScope(piTopic.getNodeScope()), MDCHelper.getTransactionUID());
    }

    public PubSubMessageContext newPubSubMessageContextFromGlobalAvzCode(PiTopics piTopic, int globalAvailabilityZoneCode) {
        LOG.debug(String.format("newPubSubMessageContext(%s, %d)", piTopic, globalAvailabilityZoneCode));
        return newPubSubMessageContext(getKoalaIdFactory().buildPId(piTopic.getPiLocation().getUrl()).forScope(piTopic.getPiLocation().getNodeScope(), globalAvailabilityZoneCode), MDCHelper.getTransactionUID());
    }

    public void getAvailabilityZoneByName(final String availabilityZoneName, final GenericContinuation<AvailabilityZone> continuation) {
        PiContinuation<AvailabilityZones> piContinuation = new PiContinuation<AvailabilityZones>() {
            @Override
            public void handleResult(AvailabilityZones result) {
                AvailabilityZone availabilityZone = result.getAvailabilityZoneByName(availabilityZoneName);
                continuation.handleResult(availabilityZone);
            }

            @Override
            public void handleException(Exception e) {
                continuation.handleException(e);
            }
        };
        dhtCache.get(getAvailabilityZonesId(), piContinuation);
    }

    protected boolean iAmAQueueWatchingApplication(int numberOfQueueWatchingApps, int queueWatchingAppsOffset, NodeScope nodeScope) {
        // call supernodeidfactory to get a spread of node ids
        Set<String> superNodeCheckPoints = null;
        if (NodeScope.AVAILABILITY_ZONE.equals(nodeScope))
            superNodeCheckPoints = SuperNodeIdFactory.getSuperNodeCheckPoints(getKoalaIdFactory().getRegion(), getKoalaIdFactory().getAvailabilityZoneWithinRegion(), numberOfQueueWatchingApps, queueWatchingAppsOffset);
        else if (NodeScope.REGION.equals(nodeScope))
            superNodeCheckPoints = SuperNodeIdFactory.getSuperNodeCheckPoints(getKoalaIdFactory().getRegion(), numberOfQueueWatchingApps, queueWatchingAppsOffset);
        else
            superNodeCheckPoints = SuperNodeIdFactory.getSuperNodeCheckPoints(numberOfQueueWatchingApps, queueWatchingAppsOffset);

        LOG.debug(String.format("Queue watching checkpoints for app %s: %s", getApplicationName(), superNodeCheckPoints));
        // am I nearest to any of the resultant list?
        for (String id : superNodeCheckPoints)
            if (koalaIdUtils.isIdClosestToMe(getNodeIdFull(), getLeafNodeHandles(), rice.pastry.Id.build(id), nodeScope))
                return true;
        return false;
    }
}
