/* (c) British Telecommunications plc, 2009, All Rights Reserved */
package com.bt.pi.api.handlers;

import javax.annotation.Resource;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.ws.server.endpoint.annotation.Endpoint;
import org.springframework.ws.server.endpoint.annotation.PayloadRoot;

import com.amazonaws.ec2.doc.x20090404.BlockDeviceMappingItemType;
import com.amazonaws.ec2.doc.x20090404.GroupItemType;
import com.amazonaws.ec2.doc.x20090404.GroupSetType;
import com.amazonaws.ec2.doc.x20090404.RunInstancesDocument;
import com.amazonaws.ec2.doc.x20090404.RunInstancesResponseDocument;
import com.amazonaws.ec2.doc.x20090404.RunInstancesResponseType;
import com.amazonaws.ec2.doc.x20090404.RunInstancesType;
import com.amazonaws.ec2.doc.x20090404.RunningInstancesItemType;
import com.amazonaws.ec2.doc.x20090404.RunningInstancesSetType;
import com.bt.pi.api.entities.ReservationInstances;
import com.bt.pi.api.service.InstancesService;
import com.bt.pi.api.utils.ConversionUtils;
import com.bt.pi.app.common.entities.BlockDeviceMapping;
import com.bt.pi.app.common.entities.Instance;
import com.bt.pi.app.common.entities.Reservation;
import com.bt.pi.core.conf.Property;
import com.bt.pi.core.util.MDCHelper;

@Endpoint
public class RunInstancesHandler extends HandlerBase {
    protected static final String DEFAULT_INSTANCE_TYPE = "m1.small";
    private static final Log LOG = LogFactory.getLog(RunInstancesHandler.class);
    private static final String OPERATION = "RunInstances";
    private InstancesService instancesService;
    private ConversionUtils conversionUtils;
    private String defaultInstanceType = DEFAULT_INSTANCE_TYPE;

    public RunInstancesHandler() {
        instancesService = null;
        conversionUtils = null;
    }

    @Resource
    public void setConversionUtils(ConversionUtils aConversionUtils) {
        conversionUtils = aConversionUtils;
    }

    @Resource
    public void setInstancesService(InstancesService anInstancesService) {
        instancesService = anInstancesService;
    }

    @PayloadRoot(localPart = OPERATION, namespace = NAMESPACE_20081201)
    public com.amazonaws.ec2.doc.x20081201.RunInstancesResponseDocument runInstances(com.amazonaws.ec2.doc.x20081201.RunInstancesDocument requestDocument) {
        return (com.amazonaws.ec2.doc.x20081201.RunInstancesResponseDocument) callLatest(requestDocument);
    }

    @PayloadRoot(localPart = OPERATION, namespace = NAMESPACE_20090404)
    public RunInstancesResponseDocument runInstances(RunInstancesDocument requestDocument) {
        LOG.debug(requestDocument);
        try {
            Reservation reservation = convertToReservation(requestDocument.getRunInstances());

            ReservationInstances reservationInstances = instancesService.runInstances(reservation);

            RunInstancesResponseDocument resultDocument = RunInstancesResponseDocument.Factory.newInstance();
            RunInstancesResponseType runInstancesResponseType = resultDocument.addNewRunInstancesResponse();

            GroupSetType groupSetType = runInstancesResponseType.addNewGroupSet();
            GroupItemType groupItemType = groupSetType.addNewItem();
            groupItemType.setGroupId(reservationInstances.getReservation().getSecurityGroupName());

            RunningInstancesSetType runningInstancesSetType = runInstancesResponseType.addNewInstancesSet();

            for (Instance instance : reservationInstances.getInstances()) {
                RunningInstancesItemType runningInstancesItemType = runningInstancesSetType.addNewItem();
                conversionUtils.convertInstance(instance, runningInstancesItemType);
            }

            runInstancesResponseType.setOwnerId(reservationInstances.getReservation().getUserId());
            runInstancesResponseType.setReservationId(reservationInstances.getReservation().getReservationId());
            runInstancesResponseType.setRequesterId(getUserId());
            runInstancesResponseType.setRequestId(MDCHelper.getTransactionUID());
            LOG.debug(resultDocument);
            return (RunInstancesResponseDocument) sanitiseXml(resultDocument);
        } catch (Throwable t) {
            throw handleThrowable(t);
        }
    }

    private Reservation convertToReservation(RunInstancesType runInstances) {
        Reservation reservation = new Reservation();
        reservation.setImageId(runInstances.getImageId());
        if (null != runInstances.getPlacement())
            reservation.setAvailabilityZone(runInstances.getPlacement().getAvailabilityZone());
        reservation.setAdditionalInfo(runInstances.getAdditionalInfo());
        reservation.setKernelId(runInstances.getKernelId());
        reservation.setKeyName(runInstances.getKeyName());
        reservation.setMaxCount(runInstances.getMaxCount());
        reservation.setMinCount(runInstances.getMinCount());
        if (null != runInstances.getMonitoring())
            reservation.setMonitoring(runInstances.getMonitoring().getEnabled());
        reservation.setRamdiskId(runInstances.getRamdiskId());
        if (null != runInstances.getUserData()) {
            byte[] bs = Base64.decodeBase64(runInstances.getUserData().getData().getBytes());
            reservation.setUserData(new String(bs));
        }

        String groupName = "default";
        if (null != runInstances.getGroupSet() && runInstances.getGroupSet().getItemArray().length > 0)
            groupName = runInstances.getGroupSet().getItemArray(0).getGroupId();

        reservation.setInstanceType(runInstances.getInstanceType());
        if (null == runInstances.getInstanceType())
            reservation.setInstanceType(defaultInstanceType);

        reservation.setSecurityGroupName(groupName);
        reservation.setUserId(getUserId());
        if (null != runInstances.getBlockDeviceMapping() && runInstances.getBlockDeviceMapping().getItemArray().length > 0)
            for (BlockDeviceMappingItemType blockDeviceMappingItemType : runInstances.getBlockDeviceMapping().getItemArray())
                reservation.getBlockDeviceMappings().add(new BlockDeviceMapping(blockDeviceMappingItemType.getVirtualName(), blockDeviceMappingItemType.getDeviceName()));

        return reservation;
    }

    @Property(key = "default.instance.type", defaultValue = DEFAULT_INSTANCE_TYPE)
    public void setDefaultInstanceType(String property) {
        this.defaultInstanceType = property;
    }
}
