package com.bt.pi.app.instancemanager.handlers;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.stereotype.Component;

import com.bt.pi.app.common.entities.BlockDeviceMapping;
import com.bt.pi.app.common.entities.Instance;
import com.bt.pi.app.common.entities.InstanceState;
import com.bt.pi.app.common.entities.Volume;
import com.bt.pi.app.common.entities.VolumeState;
import com.bt.pi.app.common.resource.PiQueue;
import com.bt.pi.app.instancemanager.libvirt.LibvirtManagerException;
import com.bt.pi.core.application.ReceivedMessageContext;
import com.bt.pi.core.conf.Property;
import com.bt.pi.core.continuation.PiContinuation;
import com.bt.pi.core.continuation.UpdateResolvingPiContinuation;
import com.bt.pi.core.dht.DhtWriter;
import com.bt.pi.core.entity.EntityResponseCode;
import com.bt.pi.core.id.PId;
import com.bt.pi.core.util.SerialExecutorRunnable;

/*
 * handle the detaching of volumes
 */
@Component("InstanceManager.DetachVolumeHandler")
public class DetachVolumeHandler extends AbstractHandler {
    private static final String FORCE_DETACHING_VOLUME_S_FROM_S = "force detaching volume %s from %s";
    private static final String DEFAULT_DETACH_VOLUME_RETRIES = "5";
    private static final Log LOG = LogFactory.getLog(DetachVolumeHandler.class);
    private static final int ONE = 1;
    private static final int SECOND = 1000;
    private static final String DEFAULT_DETACH_VOLUME_XML = "<disk type='file'><driver name='tap' type='aio' cache='default' /><source file='%s'/><target dev='%s'/></disk>";
    private String detachVolumeXml = DEFAULT_DETACH_VOLUME_XML;
    private int detachVolumeRetries;
    private ConcurrentMap<String, Semaphore> detachVolumeMap;

    public DetachVolumeHandler() {
        super();
        this.detachVolumeRetries = Integer.parseInt(DEFAULT_DETACH_VOLUME_RETRIES);
        detachVolumeMap = new ConcurrentHashMap<String, Semaphore>();
    }

    @Property(key = "detach.volume.retries", defaultValue = DEFAULT_DETACH_VOLUME_RETRIES)
    public void setDetachVolumeRetries(int aDetachVolumeRetries) {
        this.detachVolumeRetries = aDetachVolumeRetries;
    }

    @Property(key = "detach.volume.libvirt.xml", defaultValue = DEFAULT_DETACH_VOLUME_XML)
    public void setDetachVolumeXml(String aDetachVolumeXml) {
        this.detachVolumeXml = aDetachVolumeXml;
    }

    public boolean acquireLock(String aVolumeId, long time, TimeUnit timeUnit) throws InterruptedException {
        Semaphore semaphore = new Semaphore(ONE);
        Semaphore lock = detachVolumeMap.putIfAbsent(aVolumeId, semaphore);
        if (lock == null)
            lock = semaphore;

        return lock.tryAcquire(time, timeUnit);
    }

    public void releaseLock(String aVolumeId) {
        Semaphore lock = detachVolumeMap.get(aVolumeId);
        if (lock == null)
            throw new IllegalArgumentException(String.format("Lock for %s does not exist", aVolumeId));

        lock.release();
    }

    public void detachVolume(final Volume volume, final ReceivedMessageContext receivedMessageContext) {
        LOG.debug(String.format("detachVolume(%s, %s)", volume, receivedMessageContext));
        detachVolumeInThread(volume, receivedMessageContext);
    }

    private void detachVolumeInThread(final Volume volume, final ReceivedMessageContext receivedMessageContext) {
        LOG.debug(String.format("detachVolumeInThread(%s, %s)", volume, receivedMessageContext));
        final AtomicBoolean volumeIsDetached = new AtomicBoolean(false);

        findInstanceForVolume(volume, new PiContinuation<Instance>() {
            @Override
            public void handleResult(Instance resultInstance) {
                if (InstanceState.TERMINATED.equals(resultInstance.getState())) {
                    volumeIsDetached.set(true);
                    cleanVolumeFromFileSystemAndReturnMessage(volume, receivedMessageContext, volumeIsDetached.get());
                } else {
                    getSerialExecutor().execute(new SerialExecutorRunnable(PiQueue.DETACH_VOLUME.getUrl(), volume.getUrl()) {
                        @Override
                        public void run() {
                            try {
                                boolean detached = detachVolumeViaLibvirt(volume);
                                LOG.debug("detached: " + detached);
                                if (VolumeState.FORCE_DETACHING.equals(volume.getStatus())) {
                                    LOG.info(String.format(FORCE_DETACHING_VOLUME_S_FROM_S, volume.getVolumeId(), volume.getInstanceId()));
                                    volumeIsDetached.set(true);
                                } else
                                    volumeIsDetached.set(detached);
                            } catch (LibvirtManagerException lve) {
                                if (VolumeState.FORCE_DETACHING.equals(volume.getStatus())) {
                                    LOG.info(String.format(FORCE_DETACHING_VOLUME_S_FROM_S, volume.getVolumeId(), volume.getInstanceId()));
                                    volumeIsDetached.set(true);
                                }
                            } catch (InterruptedException e) {
                                LOG.warn("Interrupted exception", e);
                                Thread.interrupted();
                            }
                            cleanVolumeFromFileSystemAndReturnMessage(volume, receivedMessageContext, volumeIsDetached.get());
                        }
                    });
                }
            }
        });
    }

    private void cleanVolumeFromFileSystemAndReturnMessage(final Volume volume, final ReceivedMessageContext receivedMessageContext, final boolean volumeIsDetached) {
        LOG.debug(String.format("cleanVolumeFromFileSystemAndReturnMessage(%s, %s, %s)", volume, receivedMessageContext, volumeIsDetached));
        getSerialExecutor().execute(new SerialExecutorRunnable(PiQueue.DETACH_VOLUME.getUrl(), volume.getUrl()) {
            @Override
            public void run() {
                if (volumeIsDetached) {
                    boolean lockAcquired = false;
                    try {
                        lockAcquired = acquireLock(volume.getVolumeId(), ONE, TimeUnit.MILLISECONDS);
                        if (lockAcquired) {
                            copyVolumeFileFromLocalToRemote(volume);
                            deleteVolumeFileFromLocal(volume);
                        } else {
                            LOG.debug(String.format("Not detaching volume %s currently since a backup is in progress", volume.getVolumeId()));
                            return;
                        }
                    } catch (InterruptedException e) {
                        LOG.error(e.getMessage(), e);
                        Thread.interrupted();
                    } finally {
                        if (lockAcquired)
                            releaseLock(volume.getVolumeId());
                    }
                }

                PId id = getPiIdBuilder().getPIdForEc2AvailabilityZone(volume);
                getDhtClientFactory().createReader().getAsync(id, new PiContinuation<Volume>() {
                    @Override
                    public void handleResult(final Volume result) {
                        if (volumeIsDetached) {
                            result.setStatus(VolumeState.AVAILABLE);
                            removeBlockDeviceMappingsFromInstance(result, receivedMessageContext);
                        } else {
                            if (null != receivedMessageContext) {
                                result.setStatus(VolumeState.IN_USE);
                                receivedMessageContext.sendResponse(EntityResponseCode.OK, result);
                            }
                        }
                    }
                });
            }
        });
    }

    private void removeBlockDeviceMappingsFromInstance(final Volume volume, final ReceivedMessageContext receivedMessageContext) {
        LOG.debug(String.format("removeBlockDeviceMappingsFromInstance(%s, %s)", volume, receivedMessageContext));
        String instanceId = volume.getInstanceId();
        PId id = getPiIdBuilder().getPIdForEc2AvailabilityZone(Instance.getUrl(instanceId));
        DhtWriter dhtWriter = getDhtClientFactory().createWriter();
        dhtWriter.update(id, new UpdateResolvingPiContinuation<Instance>() {
            @Override
            public Instance update(Instance existingEntity, Instance requestedEntity) {
                List<BlockDeviceMapping> toBeRemoved = new ArrayList<BlockDeviceMapping>();
                for (BlockDeviceMapping blockDeviceMapping : existingEntity.getBlockDeviceMappings()) {
                    if (volume.getVolumeId().equals(blockDeviceMapping.getVolumeId())) {
                        toBeRemoved.add(blockDeviceMapping);
                    }
                }
                existingEntity.getBlockDeviceMappings().removeAll(toBeRemoved);
                return existingEntity;
            }

            @Override
            public void handleResult(Instance result) {
                if (null != receivedMessageContext) {
                    receivedMessageContext.sendResponse(EntityResponseCode.OK, volume);
                }
            }
        });
    }

    private boolean detachVolumeViaLibvirt(Volume volume) throws InterruptedException {
        LOG.debug(String.format("detachVolumeViaLibvirt(%s)", volume));
        String absoluteLocalVolumeFilename = getAbsoluteLocalVolumeFilename(volume.getVolumeId());
        String libvirtXml = String.format(this.detachVolumeXml, absoluteLocalVolumeFilename, cleanDevice(volume.getDevice()));

        int retryCount = 0;

        LOG.debug(String.format("Trying to detach volume: %s from Instance: %s on path: %s", volume.getVolumeId(), volume.getInstanceId(), absoluteLocalVolumeFilename));

        while (this.getLibvirtManager().volumeExists(volume.getInstanceId(), absoluteLocalVolumeFilename) && retryCount < this.detachVolumeRetries) {
            try {
                this.getLibvirtManager().detachVolume(volume.getInstanceId(), libvirtXml);
            } catch (LibvirtManagerException e) {
                LOG.warn(String.format("Error detaching volume %s from instance %s", volume.getVolumeId(), volume.getInstanceId()), e);
            }
            Thread.sleep(SECOND);
            retryCount++;
        }

        return !this.getLibvirtManager().volumeExists(volume.getInstanceId(), absoluteLocalVolumeFilename);
    }

    // we try to be idempotent here in case the file is missing
    private void copyVolumeFileFromLocalToRemote(Volume volume) {
        LOG.debug(String.format("copyVolumeFileFromLocalToRemote(%s)", volume));
        String localFilename = getRelativeLocalVolumeFilename(volume);
        if (fileExists(localFilename))
            getCommandRunner().run(String.format("%s %s %s", getCopyCommand(), localFilename, getRelativeNfsVolumeFilename(volume)));
    }

    // we try to be idempotent here in case the file is missing
    private void deleteVolumeFileFromLocal(Volume volume) {
        LOG.debug(String.format("deleteVolumeFileFromLocal(%s)", volume));
        String localFilename = getRelativeLocalVolumeFilename(volume);
        if (fileExists(localFilename))
            getCommandRunner().run(String.format("%s %s", getDeleteCommand(), localFilename));
    }

    // protected for testing override
    protected boolean fileExists(String path) {
        return new File(path).exists();
    }

    private void findInstanceForVolume(final Volume sparseVolume, PiContinuation<Instance> continuation) {
        LOG.debug(String.format("findInstanceForVolume(%s, %s)", sparseVolume, continuation));
        PId instancePId = getPiIdBuilder().getPIdForEc2AvailabilityZone(Instance.getUrl(sparseVolume.getInstanceId()));
        getDhtClientFactory().createReader().getAsync(instancePId, continuation);
    }
}
