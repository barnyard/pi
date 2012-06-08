package com.bt.pi.app.instancemanager.handlers;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Map;

import javax.annotation.Resource;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.stereotype.Component;

import rice.Continuation;
import rice.pastry.PastryNode;

import com.bt.pi.app.common.AbstractManagedAddressingPiApplication;
import com.bt.pi.app.common.entities.NodeVolumeBackupRecord;
import com.bt.pi.core.application.KoalaPastryApplicationBase;
import com.bt.pi.core.application.activation.ApplicationRecord;
import com.bt.pi.core.application.activation.RegionScopedApplicationRecord;
import com.bt.pi.core.application.activation.RegionScopedSharedRecordConditionalApplicationActivator;
import com.bt.pi.core.application.activation.SharedRecordConditionalApplicationActivator;
import com.bt.pi.core.conf.Property;
import com.bt.pi.core.continuation.UpdateResolvingPiContinuation;
import com.bt.pi.core.dht.cache.BlockingDhtCache;
import com.bt.pi.core.dht.storage.PersistentDhtStorage;
import com.bt.pi.core.id.PId;
import com.bt.pi.core.past.KoalaDHTStorage;
import com.bt.pi.core.scope.NodeScope;

@Component
public class VolumeBackupManagerApplication extends AbstractManagedAddressingPiApplication {
    public static final String APPLICATION_NAME = "pi-volumebackup-manager";

    private static final Log LOG = LogFactory.getLog(VolumeBackupManagerApplication.class);
    private static final int NUMBER_OF_MILLIS_IN_SECOND = 1000;
    private static final int FIFTY_NINE = 59;
    private static final int TWENTY_THREE = 23;
    private static final String DEFAULT_VOLUME_BACKUP_ACTIVATION_CHECK_SECS = "1800"; // 30 minutes
    private static final String DEFAULT_VOLUME_BACKUP_COOLDOWN_PERIOD_SECS = "86400"; // 60 * 60 * 24
    private static final String DEFAULT_ALLOWED_SLOT_RANGES = "00:00-06:00,22:00-23:59";

    private int volumeBackupCooldownPeriodSecs;
    private NodeVolumeBackupRecord cachedNodeVolumeBackupRecord;
    private Collection<TimeRange> allowedSlots;

    @Resource(type = RegionScopedSharedRecordConditionalApplicationActivator.class)
    private SharedRecordConditionalApplicationActivator applicationActivator;
    @Resource(name = "generalBlockingCache")
    private BlockingDhtCache blockingDhtCache;
    @Resource
    private VolumeBackupHandler volumeBackupHandler;

    public VolumeBackupManagerApplication() {
        super(APPLICATION_NAME);

        cachedNodeVolumeBackupRecord = null;
        allowedSlots = new ArrayList<TimeRange>();
        applicationActivator = null;
        blockingDhtCache = null;
        volumeBackupHandler = null;
    }

    @Property(key = "volumebackupmanager.activation.check.period.secs", defaultValue = DEFAULT_VOLUME_BACKUP_ACTIVATION_CHECK_SECS)
    public void setActivationCheckPeriodSecs(int value) {
        super.setActivationCheckPeriodSecs(value);
    }

    @Property(key = "volumebackupmanager.cooldown.period.secs", defaultValue = DEFAULT_VOLUME_BACKUP_COOLDOWN_PERIOD_SECS)
    public void setVolumeBackupCooldownPeriodSecs(int value) {
        this.volumeBackupCooldownPeriodSecs = value;
    }

    @Property(key = "volumebackupmanager.allowed.slot.ranges", defaultValue = DEFAULT_ALLOWED_SLOT_RANGES)
    public void setVolumeBackupManagerAllowedSlotRanges(String value) {
        allowedSlots.clear();
        populateValidTimeSlotRanges(value);
    }

    @Override
    public void start(PastryNode aPastryNode, KoalaDHTStorage pastImpl, Map<String, KoalaPastryApplicationBase> nodeApplications, PersistentDhtStorage aPersistentDhtStorage) {
        super.start(aPastryNode, pastImpl, nodeApplications, aPersistentDhtStorage);
        cacheNodeVolumeBackupRecord();
    }

    private void cacheNodeVolumeBackupRecord() {
        PId pid = getPiIdBuilder().getPId(NodeVolumeBackupRecord.getUrl(this.getNodeIdFull()));
        if (cachedNodeVolumeBackupRecord == null) {
            getDhtClientFactory().createWriter().update(pid, new UpdateResolvingPiContinuation<NodeVolumeBackupRecord>() {
                @Override
                public NodeVolumeBackupRecord update(NodeVolumeBackupRecord existingEntity, NodeVolumeBackupRecord requestedEntity) {
                    if (existingEntity != null) {
                        cachedNodeVolumeBackupRecord = existingEntity;
                        return null;
                    }

                    cachedNodeVolumeBackupRecord = new NodeVolumeBackupRecord(getNodeIdFull());
                    return cachedNodeVolumeBackupRecord;
                }

                @Override
                public void handleResult(NodeVolumeBackupRecord result) {
                }
            });
        }
    }

    private void populateValidTimeSlotRanges(String value) {
        String[] timeSlotRanges = value.split(",");
        for (String timeSlotRange : timeSlotRanges) {
            String[] range = timeSlotRange.split("-");
            if (range.length != 2)
                throw new IllegalArgumentException(String.format("Not valid timeslot range: %s", timeSlotRange));

            allowedSlots.add(new TimeRange(getTime(range[0]), getTime(range[1])));
        }
    }

    private Calendar getTime(String slot) {
        String[] split = slot.split(":");
        if (split.length != 2)
            throw new IllegalArgumentException(String.format("Each time slot should be in the format hh:mm: %s", slot));

        int hour = Integer.parseInt(split[0]);
        int minute = Integer.parseInt(split[1]);
        if (hour < 0 || hour > TWENTY_THREE)
            throw new IllegalArgumentException(String.format("The hours value should be between 00 and 23: %d", split[0]));
        if (minute < 0 || minute > FIFTY_NINE)
            throw new IllegalArgumentException(String.format("The minutes value should be between 00 and 59: %d", split[1]));

        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.HOUR_OF_DAY, hour);
        calendar.set(Calendar.MINUTE, minute);
        return calendar;
    }

    @Override
    public int getActivationCheckPeriodSecs() {
        if (hasEnoughTimeElapsedSinceLastBackup())
            return super.getActivationCheckPeriodSecs();
        else
            return volumeBackupCooldownPeriodSecs;
    }

    private boolean hasEnoughTimeElapsedSinceLastBackup() {
        if (cachedNodeVolumeBackupRecord == null)
            return true;
        if ((System.currentTimeMillis() - cachedNodeVolumeBackupRecord.getLastBackup()) > (((long) volumeBackupCooldownPeriodSecs) * NUMBER_OF_MILLIS_IN_SECOND))
            return true;

        return false;
    }

    @Override
    public boolean becomeActive() {
        LOG.debug("becomeActive()");
        final PId pid = getPiIdBuilder().getPId(NodeVolumeBackupRecord.getUrl(this.getNodeIdFull()));

        if (!isCurrentlyInAllowedTimeRange())
            return false;

        cachedNodeVolumeBackupRecord = blockingDhtCache.get(pid);

        if (hasEnoughTimeElapsedSinceLastBackup()) {
            LOG.debug("Starting up application:" + APPLICATION_NAME);
            volumeBackupHandler.startBackup(new Continuation<Object, Exception>() {
                @Override
                public void receiveException(Exception exception) {
                }

                @Override
                public void receiveResult(Object result) {
                    getDhtClientFactory().createWriter().update(pid, new UpdateResolvingPiContinuation<NodeVolumeBackupRecord>() {
                        @Override
                        public NodeVolumeBackupRecord update(NodeVolumeBackupRecord existingEntity, NodeVolumeBackupRecord requestedEntity) {
                            if (existingEntity != null) {
                                existingEntity.setLastBackup(System.currentTimeMillis());
                                return existingEntity;
                            }
                            return null;
                        }

                        @Override
                        public void handleResult(NodeVolumeBackupRecord result) {
                            LOG.debug(String.format("NodeVolumeBackupRecord's last backup timestamp updated: %s", result));
                            cachedNodeVolumeBackupRecord = result;
                            becomePassive();
                        }
                    });
                }
            });

            return true;
        }

        LOG.debug("Not starting up application:" + APPLICATION_NAME);
        return false;
    }

    private boolean isCurrentlyInAllowedTimeRange() {
        Calendar calendar = Calendar.getInstance();
        for (TimeRange timeRange : allowedSlots) {
            clearCalendar(calendar);
            if (calendar.after(timeRange.start) && calendar.before(timeRange.end))
                return true;
        }
        return false;
    }

    @Override
    public void becomePassive() {
        PId appRecordId = getKoalaIdFactory().buildPId(RegionScopedApplicationRecord.getUrl(getApplicationName())).forLocalScope(NodeScope.REGION);
        getDhtClientFactory().createWriter().update(appRecordId, new UpdateResolvingPiContinuation<ApplicationRecord>() {
            @Override
            public ApplicationRecord update(ApplicationRecord existingEntity, ApplicationRecord requestedEntity) {
                LOG.debug(String.format("Removing %s from %s application Record.", getNodeIdFull(), getApplicationName()));
                boolean b = existingEntity.removeActiveNode(getNodeIdFull());
                LOG.debug("Remove successful: " + b);
                return existingEntity;
            }

            @Override
            public void handleResult(ApplicationRecord result) {
                LOG.debug(String.format("Result of deactivating node %s is: %s", getNodeIdFull(), result));
            }
        });
    }

    @Override
    public void handleNodeDeparture(String nodeId) {
        removeNodeIdFromApplicationRecord(nodeId);
    }

    @Override
    protected SharedRecordConditionalApplicationActivator getActivatorFromApplication() {
        return applicationActivator;
    }

    private Calendar clearCalendar(Calendar calendar) {
        calendar.set(Calendar.YEAR, 0);
        calendar.set(Calendar.MONTH, 1);
        calendar.set(Calendar.DAY_OF_MONTH, 1);
        return calendar;
    }

    private class TimeRange {
        private Calendar start;
        private Calendar end;

        public TimeRange(Calendar aStart, Calendar aEnd) {
            if (!aStart.before(aEnd))
                throw new IllegalArgumentException(String.format("Start time should be before end time: %s, %s", aStart, aEnd));

            start = clearCalendar(aStart);
            end = clearCalendar(aEnd);
        }
    }
}
