/* (c) British Telecommunications plc, 2009, All Rights Reserved */
package com.bt.pi.app.instancemanager.handlers;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.context.annotation.DependsOn;
import org.springframework.stereotype.Component;

import com.bt.pi.app.common.entities.Image;
import com.bt.pi.app.common.entities.Instance;
import com.bt.pi.app.common.entities.InstanceTypes;
import com.bt.pi.app.common.entities.KeyPair;
import com.bt.pi.app.common.entities.User;
import com.bt.pi.app.instancemanager.images.PlatformBuilder;
import com.bt.pi.app.instancemanager.images.PlatformBuilderFactory;
import com.bt.pi.app.networkmanager.net.VirtualNetworkBuilder;
import com.bt.pi.core.continuation.GenericContinuation;
import com.bt.pi.core.continuation.PiContinuation;
import com.bt.pi.core.dht.cache.DhtCache;
import com.bt.pi.core.id.PId;

@Component
public class RunInstanceHandler extends AbstractHandler {
    private static final Log LOG = LogFactory.getLog(RunInstanceContinuation.class);
    private PId instanceTypesId;
    @Resource
    private PlatformBuilderFactory platformBuilderFactory;
    @Resource(name = "generalCache")
    private DhtCache dhtCache;
    @Resource
    private VirtualNetworkBuilder virtualNetworkBuilder;
    @Resource
    private SystemResourceState systemResourceState;

    public RunInstanceHandler() {
        super();
        platformBuilderFactory = null;
        dhtCache = null;
        virtualNetworkBuilder = null;
        instanceTypesId = null;
    }

    public void setPlatformBuilderFactory(PlatformBuilderFactory aPlatformBuilderFactory) {
        platformBuilderFactory = aPlatformBuilderFactory;
    }

    public void startInstance(final Instance instance) {
        LOG.debug(String.format("startInstance(%s)", instance));
        int vlanId = instance.getVlanId();
        String instanceId = instance.getInstanceId();

        LOG.debug(String.format("Setting up virtual network for instance (id: %s) with vlanId: %d", instanceId, vlanId));
        virtualNetworkBuilder.setUpVirtualNetworkForInstance(vlanId, instanceId);

        loadImageInformationAndBuildInstance(instance);
    }

    private void loadImageInformationAndBuildInstance(final Instance instance) {
        LOG.debug(String.format("loadImageInformationAndBuildInstance(%s)", instance));
        dhtCache.get(getPiIdBuilder().getPId(Image.getUrl(instance.getImageId())), new PiContinuation<Image>() {
            @Override
            public void handleResult(Image image) {
                LOG.debug(String.format("Image %s received for instance %s.", image, instance));

                final PlatformBuilder platformBuilder = platformBuilderFactory.getFor(image.getPlatform());
                instance.setPlatform(image.getPlatform());

                setDefaultsIfNeeded(instance, image);

                if (StringUtils.isEmpty(instance.getKeyName())) {
                    LOG.debug(String.format("Key not specified for instance %s, building instance with null key", instance.getInstanceId()));
                    buildInstance(platformBuilder, instance, null);

                    return;
                } else {
                    LOG.debug(String.format("Retrieving key %s (user %s) from dht, to embed in instance %s", instance.getKeyName(), instance.getUserId(), instance.getInstanceId()));
                    getDhtClientFactory().createReader().getAsync(getPiIdBuilder().getPId(User.getUrl(instance.getUserId())), new GenericContinuation<User>() {
                        @Override
                        public void handleResult(User result) {
                            if (result == null) {
                                LOG.warn(String.format("Unable to get user id %s while running instance %s", instance.getUserId(), instance.getInstanceId()));
                                return;
                            }

                            KeyPair keyPair = result.getKeyPair(instance.getKeyName());
                            if (keyPair == null) {
                                LOG.debug(String.format("KeyPair %s not found in user record %s, building instance with null key", instance.getKeyName(), instance.getUserId()));
                                buildInstance(platformBuilder, instance, null);
                            } else
                                buildInstance(platformBuilder, instance, keyPair.getKeyMaterial());
                        }
                    });
                }
            }
        });
    }

    private void setDefaultsIfNeeded(final Instance instance, Image image) {
        if (StringUtils.isBlank(instance.getKernelId())) {
            instance.setKernelId(image.getKernelId());
        }
        if (StringUtils.isBlank(instance.getRamdiskId())) {
            instance.setRamdiskId(image.getRamdiskId());
        }
        instance.setSourceImagePath(instance.getImageId());
        instance.setSourceKernelPath(instance.getKernelId());
        instance.setSourceRamdiskPath(instance.getRamdiskId());
    }

    private void buildInstance(final PlatformBuilder platformBuilder, final Instance instance, final String key) {
        getInstanceTypesCache().get(instanceTypesId, new PiContinuation<InstanceTypes>() {
            @Override
            public void handleResult(InstanceTypes result) {
                // new thread for this as it WILL take a long time.
                getSerialExecutor().execute(new BuildInstanceRunner(getConsumedDhtResourceRegistry(), instance, key, getPiIdBuilder(), platformBuilder, systemResourceState, getTaskProcessingQueueHelper()));
            }
        });
    }

    @PostConstruct
    @DependsOn("piIdBuilder")
    public void setupInstanceTypesId() {
        instanceTypesId = getPiIdBuilder().getPId(InstanceTypes.URL_STRING);
    }
}
