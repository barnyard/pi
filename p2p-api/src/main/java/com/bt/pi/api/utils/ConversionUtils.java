/* (c) British Telecommunications plc, 2009, All Rights Reserved */
package com.bt.pi.api.utils;

import java.util.Calendar;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.stereotype.Component;

import com.amazonaws.ec2.doc.x20090404.GroupSetType;
import com.amazonaws.ec2.doc.x20090404.InstanceMonitoringStateType;
import com.amazonaws.ec2.doc.x20090404.InstanceStateType;
import com.amazonaws.ec2.doc.x20090404.PlacementResponseType;
import com.amazonaws.ec2.doc.x20090404.ReservationInfoType;
import com.amazonaws.ec2.doc.x20090404.ReservationSetType;
import com.amazonaws.ec2.doc.x20090404.RunningInstancesItemType;
import com.amazonaws.ec2.doc.x20090404.RunningInstancesSetType;
import com.bt.pi.app.common.entities.Instance;
import com.bt.pi.app.common.entities.InstanceState;
import com.bt.pi.app.common.entities.SnapshotState;
import com.bt.pi.app.common.entities.VolumeState;

@Component
public class ConversionUtils {
    private static final Log LOG = LogFactory.getLog(ConversionUtils.class);

    public ConversionUtils() {
    }

    public InstanceStateType getInstanceStateType(InstanceState instanceState) {
        InstanceStateType instanceStateType = InstanceStateType.Factory.newInstance();

        instanceStateType.setCode(instanceState.getCode());
        instanceStateType.setName(instanceState.getDisplayName());

        return instanceStateType;
    }

    public String getSnapshotStatusString(SnapshotState snapshotState) {
        if (SnapshotState.CREATING.equals(snapshotState))
            return SnapshotState.PENDING.toString();
        if (SnapshotState.DELETING.equals(snapshotState))
            return SnapshotState.COMPLETE.toString();
        if (SnapshotState.BURIED.equals(snapshotState))
            return SnapshotState.DELETED.toString();
        return snapshotState.toString();
    }

    public String getVolumeStatusString(VolumeState volumeState) {
        if (VolumeState.AVAILABLE_SNAPSHOTTING.equals(volumeState))
            return VolumeState.AVAILABLE.toString();
        if (VolumeState.IN_USE_SNAPSHOTTING.equals(volumeState))
            return VolumeState.IN_USE.toString();
        if (VolumeState.BURIED.equals(volumeState))
            return VolumeState.DELETED.toString();
        if (VolumeState.FORCE_DETACHING.equals(volumeState))
            return VolumeState.DETACHING.toString();
        return volumeState.toString();
    }

    public void convertReservation(ReservationSetType addNewReservationSet, Set<Instance> instances) {
        LOG.debug(String.format("convertReservation(%s, %s)", addNewReservationSet, instances));
        for (Instance instance : instances) {
            ReservationInfoType reservationInfoType = addNewReservationSet.addNewItem();
            GroupSetType groupSetType = reservationInfoType.addNewGroupSet();
            RunningInstancesSetType runningInstancesSetType = reservationInfoType.addNewInstancesSet();
            groupSetType.addNewItem().setGroupId(instance.getSecurityGroupName());
            reservationInfoType.setReservationId(instance.getReservationId());
            reservationInfoType.setOwnerId(instance.getUserId());
            RunningInstancesItemType runningInstancesItemType = runningInstancesSetType.addNewItem();
            convertInstance(instance, runningInstancesItemType);
        }
    }

    public void convertInstance(Instance instance, RunningInstancesItemType runningInstancesItemType) {

        PlacementResponseType availabilityZone = PlacementResponseType.Factory.newInstance();
        availabilityZone.setAvailabilityZone(instance.getAvailabilityZone());
        runningInstancesItemType.setInstanceType(instance.getInstanceType());

        runningInstancesItemType.setPlacement(availabilityZone);
        if (null != instance.getPlatform())
            runningInstancesItemType.setPlatform(instance.getPlatform().toString());
        runningInstancesItemType.setImageId(instance.getImageId());
        runningInstancesItemType.setRamdiskId(instance.getRamdiskId());
        runningInstancesItemType.setKernelId(instance.getKernelId());
        runningInstancesItemType.setAmiLaunchIndex(String.valueOf(instance.getLaunchIndex()));
        runningInstancesItemType.setDnsName(instance.getPublicIpAddress());
        runningInstancesItemType.setPrivateDnsName(instance.getPrivateIpAddress());
        runningInstancesItemType.setInstanceId(instance.getInstanceId());
        runningInstancesItemType.setInstanceState(getInstanceStateType(instance.getState()));
        runningInstancesItemType.setKeyName(instance.getKeyName());
        runningInstancesItemType.setReason(instance.getReasonForLastStateTransition());
        runningInstancesItemType.setInstanceType(instance.getInstanceType());
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(instance.getLaunchTime());
        runningInstancesItemType.setLaunchTime(calendar);

        InstanceMonitoringStateType instanceMonitoringStateType = InstanceMonitoringStateType.Factory.newInstance();
        instanceMonitoringStateType.setState(String.valueOf(instance.getMonitoring()));
        runningInstancesItemType.setMonitoring(instanceMonitoringStateType);
    }
}
