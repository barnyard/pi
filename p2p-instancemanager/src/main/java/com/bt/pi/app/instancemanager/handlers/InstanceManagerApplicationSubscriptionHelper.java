package com.bt.pi.app.instancemanager.handlers;

import java.util.concurrent.atomic.AtomicBoolean;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.context.annotation.DependsOn;
import org.springframework.stereotype.Component;

import com.bt.pi.app.common.entities.InstanceTypeConfiguration;
import com.bt.pi.app.common.entities.InstanceTypes;
import com.bt.pi.app.common.id.PiIdBuilder;
import com.bt.pi.app.common.resource.PiTopics;
import com.bt.pi.core.application.KoalaPastryScribeApplication;
import com.bt.pi.core.continuation.PiContinuation;
import com.bt.pi.core.dht.cache.DhtCache;
import com.bt.pi.core.id.PId;

@Component
public class InstanceManagerApplicationSubscriptionHelper {
    private static final Log LOG = LogFactory.getLog(InstanceManagerApplicationSubscriptionHelper.class);
    private KoalaPastryScribeApplication pubSubApp;
    private DhtCache instanceTypesCache;
    private PId instanceTypesId;
    private PiIdBuilder piIdBuilder;
    private AtomicBoolean areResourcesAvailable;
    private AnycastHandler anycastHandler;

    public InstanceManagerApplicationSubscriptionHelper() {
        pubSubApp = null;
        piIdBuilder = null;
        instanceTypesCache = null;
        instanceTypesId = null;
        anycastHandler = null;
        areResourcesAvailable = new AtomicBoolean(false);
    }

    @Resource
    public void setPiIdBuilder(PiIdBuilder aPiIdBuilder) {
        this.piIdBuilder = aPiIdBuilder;
    }

    @Resource(type = InstanceManagerApplication.class)
    public void setPubSubApp(KoalaPastryScribeApplication aPubSubApp) {
        this.pubSubApp = aPubSubApp;
    }

    @Resource(name = "generalCache")
    public void setInstanceTypesCache(DhtCache cache) {
        this.instanceTypesCache = cache;
    }

    @Resource
    public void setAnycastHandler(AnycastHandler aAnycastHandler) {
        this.anycastHandler = aAnycastHandler;
    }

    @PostConstruct
    @DependsOn("piIdBuilder")
    public void setupInstanceTypesId() {
        instanceTypesId = piIdBuilder.getPId(InstanceTypes.URL_STRING);
    }

    public void checkCapacityAndSubscribeUnSubscribe() {
        LOG.debug("checkCapacityAndSubscribeUnSubscribe()");
        instanceTypesCache.get(instanceTypesId, new PiContinuation<InstanceTypes>() {
            @Override
            public void handleResult(InstanceTypes instanceTypes) {
                LOG.debug(String.format("subscribeUnsubscribeForRunInstances(%s)", instanceTypes));
                if (instanceTypes == null) {
                    LOG.warn("InstanceTypes not available in DHT, assuming that it will be soon!");
                    return;
                }
                boolean doUnsubscribe = true;
                for (InstanceTypeConfiguration instanceTypeConfiguration : instanceTypes.getInstanceTypes().values()) {
                    if (instanceTypeConfiguration.isDeprecated())
                        continue;
                    if (anycastHandler.hasEnoughResources(instanceTypeConfiguration)) {
                        doUnsubscribe = false;
                        if (!areResourcesAvailable.getAndSet(true)) {
                            LOG.info(String.format("Instance manager subscribing to topic %s", PiTopics.RUN_INSTANCE.getUrl()));
                            pubSubApp.subscribe(PiTopics.RUN_INSTANCE.getPiLocation(), pubSubApp);
                        }
                        break;
                    }
                }
                if (doUnsubscribe && areResourcesAvailable.getAndSet(false)) {
                    LOG.info(String.format("Instance manager unsubscribing from topic %s", PiTopics.RUN_INSTANCE.getUrl()));
                    pubSubApp.unsubscribe(PiTopics.RUN_INSTANCE.getPiLocation(), pubSubApp);
                }
            }
        });
    }
}
