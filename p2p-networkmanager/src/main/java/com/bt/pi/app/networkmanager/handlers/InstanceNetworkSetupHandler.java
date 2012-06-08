/* (c) British Telecommunications plc, 2009, All Rights Reserved */
package com.bt.pi.app.networkmanager.handlers;

import javax.annotation.Resource;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.stereotype.Component;

import com.bt.pi.app.common.entities.Instance;
import com.bt.pi.app.common.id.PiIdBuilder;
import com.bt.pi.app.networkmanager.addressing.PublicIpAddressManager;
import com.bt.pi.app.networkmanager.net.NetworkManager;
import com.bt.pi.core.application.ReceivedMessageContext;
import com.bt.pi.core.continuation.GenericContinuation;
import com.bt.pi.core.continuation.UpdateResolvingPiContinuation;
import com.bt.pi.core.dht.DhtClientFactory;
import com.bt.pi.core.dht.DhtWriter;
import com.bt.pi.core.entity.EntityResponseCode;
import com.bt.pi.core.id.PId;

@Component
public class InstanceNetworkSetupHandler {
    private static final Log LOG = LogFactory.getLog(InstanceNetworkSetupHandler.class);
    private NetworkManager networkManager;
    private PublicIpAddressManager publicIpAddressManager;
    private DhtClientFactory dhtClientFactory;
    private PiIdBuilder piIdBuilder;

    public InstanceNetworkSetupHandler() {
        networkManager = null;
        publicIpAddressManager = null;
        dhtClientFactory = null;
        piIdBuilder = null;
    }

    @Resource
    public void setNetworkManager(NetworkManager aNetworkManager) {
        this.networkManager = aNetworkManager;
    }

    @Resource
    public void setPublicIpAddressManager(PublicIpAddressManager anAddressManager) {
        this.publicIpAddressManager = anAddressManager;
    }

    @Resource
    public void setDhtFactory(DhtClientFactory dhtFactory) {
        dhtClientFactory = dhtFactory;
    }

    @Resource
    public void setPiIdBuilder(PiIdBuilder aPiIdBuilder) {
        this.piIdBuilder = aPiIdBuilder;

    }

    public void handle(final Instance aInstance, final ReceivedMessageContext receivedMessageContext) {
        handle(aInstance, true, receivedMessageContext);
    }

    public void handle(final Instance aInstance, final boolean isInitialInstanceCreation, final ReceivedMessageContext receivedMessageContext) {
        LOG.debug(String.format("handle(%s)", aInstance));
        networkManager.setupNetworkForInstance(aInstance, new GenericContinuation<Instance>() {
            @Override
            public void handleResult(Instance instanceResult) {
                LOG.debug(String.format("Network setup for instance %s returned result %s", aInstance, instanceResult));
                if (instanceResult == null)
                    throw new InstanceNetworkSetupException(String.format("Null instance returned when setting up network for instance %s", aInstance.getInstanceId()));

                publicIpAddressManager.allocatePublicIpAddressForInstance(aInstance.getInstanceId(), new GenericContinuation<String>() {
                    @Override
                    public void handleResult(String addressResult) {
                        LOG.debug(String.format("Public address allocation for instance %s returned result %s", aInstance, addressResult));
                        if (addressResult == null)
                            throw new InstanceNetworkSetupException(String.format("Null address returned when setting up public address for instance %s", aInstance.getInstanceId()));

                        persistInstanceAndSendResponse(aInstance, addressResult, aInstance.getPrivateIpAddress(), aInstance.getPrivateMacAddress(), aInstance.getVlanId(), receivedMessageContext, isInitialInstanceCreation);
                    }
                });
            }
        });
    }

    private void persistInstanceAndSendResponse(final Instance anInstance, final String publicIpAddress, final String privateIpAddress, final String macAddress, final int vlanId, final ReceivedMessageContext receivedMessageContext,
            final boolean isInitialInstanceCreation) {
        LOG.debug(String.format("persistInstance(%s, %s, %s, %s, %d", anInstance.getInstanceId(), publicIpAddress, privateIpAddress, macAddress, vlanId));

        final PId instanceDhtId = piIdBuilder.getPIdForEc2AvailabilityZone(anInstance);

        DhtWriter writer = dhtClientFactory.createWriter();
        writer.update(instanceDhtId, new UpdateResolvingPiContinuation<Instance>() {
            @Override
            public Instance update(Instance existingEntity, Instance requestedEntity) {

                if (isEquals(existingEntity.getPublicIpAddress(), publicIpAddress) && isEquals(existingEntity.getPrivateIpAddress(), privateIpAddress) && isEquals(existingEntity.getPrivateMacAddress(), macAddress)
                        && (existingEntity.getVlanId() == vlanId)) {
                    LOG.debug(String.format("Not persisting instance as no changes found in Instance (%s) publicIpAddress (%s), privateIpAddress (%s) privateMacAddress (%s) vlanId (%d)", existingEntity, publicIpAddress, privateIpAddress, macAddress,
                            vlanId));
                    return null;
                }

                existingEntity.setPublicIpAddress(publicIpAddress);
                existingEntity.setPrivateIpAddress(privateIpAddress);
                existingEntity.setPrivateMacAddress(macAddress);
                existingEntity.setVlanId(vlanId);

                return existingEntity;
            }

            @Override
            public void handleResult(Instance result) {
                Instance tempInstance = result;

                if (result == null)
                    tempInstance = anInstance;

                if (isInitialInstanceCreation) {
                    LOG.debug(String.format("Write of instance %s wrote %s", instanceDhtId, tempInstance));
                    receivedMessageContext.sendResponse(EntityResponseCode.OK, tempInstance);
                } else {
                    LOG.debug(String.format("Not sending response for %s as not initial instance creation", tempInstance.getInstanceId()));
                }
            }

            private boolean isEquals(String left, String right) {
                return StringUtils.equals(left, right);
            }
        });
    }
}
