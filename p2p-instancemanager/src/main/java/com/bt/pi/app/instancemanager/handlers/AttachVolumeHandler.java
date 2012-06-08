package com.bt.pi.app.instancemanager.handlers;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.stereotype.Component;

import com.bt.pi.app.common.entities.BlockDeviceMapping;
import com.bt.pi.app.common.entities.Instance;
import com.bt.pi.app.common.entities.Volume;
import com.bt.pi.app.common.resource.PiQueue;
import com.bt.pi.core.application.ReceivedMessageContext;
import com.bt.pi.core.conf.Property;
import com.bt.pi.core.continuation.UpdateResolvingPiContinuation;
import com.bt.pi.core.dht.DhtWriter;
import com.bt.pi.core.entity.EntityResponseCode;
import com.bt.pi.core.id.PId;
import com.bt.pi.core.util.SerialExecutorRunnable;

/*
 * handle the attaching of volumes - simple case for now using libvirt to mount a local file
 */
@Component("InstanceManager.AttachVolumeHandler")
public class AttachVolumeHandler extends AbstractHandler {
    private static final Log LOG = LogFactory.getLog(AttachVolumeHandler.class);
    private static final String DEFAULT_ATTACH_VOLUME_XML = "<disk type='file'><driver name='tap' type='aio' cache='default' /><source file='%s'/><target dev='%s'/></disk>";
    private String attachVolumeXml = DEFAULT_ATTACH_VOLUME_XML;

    public AttachVolumeHandler() {
        super();
    }

    @Property(key = "attach.volume.libvirt.xml", defaultValue = DEFAULT_ATTACH_VOLUME_XML)
    public void setAttachVolumeXml(String anAttachVolumeXml) {
        this.attachVolumeXml = anAttachVolumeXml;
    }

    public void attachVolume(final Volume volume, final ReceivedMessageContext receivedMessageContext) {
        LOG.debug(String.format("attachVolume(%s, %s)", volume, receivedMessageContext));
        getSerialExecutor().execute(new SerialExecutorRunnable(PiQueue.ATTACH_VOLUME.getUrl(), volume.getUrl()) {
            @Override
            public void run() {
                try {
                    attachVolumeInThread(volume, receivedMessageContext);
                } catch (Throwable t) {
                    LOG.error(t.getMessage(), t);
                }
            }
        });
    }

    private void attachVolumeInThread(final Volume volume, final ReceivedMessageContext receivedMessageContext) {
        LOG.debug(String.format("attachVolumeInThread(%s, %s)", volume, receivedMessageContext));

        copyVolumeFileFromRemoteToLocal(volume);
        attachVolumeViaLibvirt(volume);

        String instanceId = volume.getInstanceId();
        PId id = getPiIdBuilder().getPIdForEc2AvailabilityZone(Instance.getUrl(instanceId));
        DhtWriter dhtWriter = getDhtClientFactory().createWriter();
        dhtWriter.update(id, new UpdateResolvingPiContinuation<Instance>() {
            @Override
            public Instance update(Instance existingEntity, Instance requestedEntity) {
                existingEntity.getBlockDeviceMappings().add(new BlockDeviceMapping(volume.getVolumeId()));
                return existingEntity;
            }

            @Override
            public void handleResult(Instance result) {
                receivedMessageContext.sendResponse(EntityResponseCode.OK, volume);
            }
        });
    }

    private void copyVolumeFileFromRemoteToLocal(Volume volume) {
        LOG.debug(String.format("copyVolumeFileFromRemoteToLocal(%s)", volume));
        getCommandRunner().runNicely(String.format("%s %s %s", getCopyCommand(), getRelativeNfsVolumeFilename(volume), getRelativeLocalVolumeFilename(volume)));
    }

    private void attachVolumeViaLibvirt(Volume volume) {
        LOG.debug(String.format("attachVolumeViaLibvirt(%s)", volume));
        String domainXml = this.getLibvirtManager().getDomainXml(volume.getInstanceId());
        if (domainXml.contains(volume.getVolumeId())) {
            LOG.warn(String.format("Instance %s already has volume %s attached", volume.getInstanceId(), volume.getVolumeId()));
            return;
        }
        String attachXml = String.format(this.attachVolumeXml, getAbsoluteLocalVolumeFilename(volume.getVolumeId()), cleanDevice(volume.getDevice()));
        this.getLibvirtManager().attachVolume(volume.getInstanceId(), attachXml);
    }
}
