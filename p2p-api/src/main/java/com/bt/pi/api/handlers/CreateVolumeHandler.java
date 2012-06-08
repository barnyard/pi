/* (c) British Telecommunications plc, 2009, All Rights Reserved */
package com.bt.pi.api.handlers;

import java.util.Calendar;

import javax.annotation.Resource;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.ws.server.endpoint.annotation.Endpoint;
import org.springframework.ws.server.endpoint.annotation.PayloadRoot;

import com.amazonaws.ec2.doc.x20090404.CreateVolumeDocument;
import com.amazonaws.ec2.doc.x20090404.CreateVolumeResponseDocument;
import com.amazonaws.ec2.doc.x20090404.CreateVolumeResponseType;
import com.bt.pi.api.service.ElasticBlockStorageService;
import com.bt.pi.app.common.entities.Volume;
import com.bt.pi.core.conf.Property;
import com.bt.pi.core.util.MDCHelper;

/**
 * Web Service handler for CreateVolume
 */
@Endpoint("API.CreateVolumeHandler")
public class CreateVolumeHandler extends HandlerBase {
    protected static final String DEFAULT_MAX_VOLUME_SIZE_IN_GIGABYTES = "100";
    private static final Log LOG = LogFactory.getLog(CreateVolumeHandler.class);
    private static final String CREATE_VOLUME = "CreateVolume";
    private ElasticBlockStorageService elasticBlockStorageService;
    private int maxVolumeSizeGigaBytes = Integer.parseInt(DEFAULT_MAX_VOLUME_SIZE_IN_GIGABYTES);

    public CreateVolumeHandler() {
        elasticBlockStorageService = null;
    }

    @Resource
    public void setElasticBlockStorageService(ElasticBlockStorageService anElasticBlockStorageService) {
        elasticBlockStorageService = anElasticBlockStorageService;
    }

    @PayloadRoot(localPart = CREATE_VOLUME, namespace = NAMESPACE_20081201)
    public com.amazonaws.ec2.doc.x20081201.CreateVolumeResponseDocument createVolume(com.amazonaws.ec2.doc.x20081201.CreateVolumeDocument requestDocument) {
        LOG.debug(requestDocument);
        return (com.amazonaws.ec2.doc.x20081201.CreateVolumeResponseDocument) callLatest(requestDocument);
    }

    // TODO: add try/catch/throwable to all of these service methods
    @PayloadRoot(localPart = CREATE_VOLUME, namespace = NAMESPACE_20090404)
    public CreateVolumeResponseDocument createVolume(CreateVolumeDocument requestDocument) {
        LOG.debug(requestDocument);
        try {
            String availabilityZone = requestDocument.getCreateVolume().getAvailabilityZone();
            String sizeInGigaBytes = requestDocument.getCreateVolume().getSize();
            String snapshotId = requestDocument.getCreateVolume().getSnapshotId();
            int size = 0;
            if (StringUtils.isBlank(snapshotId))
                size = validateSize(sizeInGigaBytes);

            Volume volume = elasticBlockStorageService.createVolume(getUserId(), size, availabilityZone, snapshotId);

            CreateVolumeResponseDocument resultDocument = CreateVolumeResponseDocument.Factory.newInstance();
            CreateVolumeResponseType addNewCreateVolumeResponse = resultDocument.addNewCreateVolumeResponse();

            addNewCreateVolumeResponse.setAvailabilityZone(volume.getAvailabilityZone());
            Calendar instance = Calendar.getInstance();
            instance.setTimeInMillis(volume.getCreateTime());
            addNewCreateVolumeResponse.setCreateTime(instance);
            addNewCreateVolumeResponse.setSize(String.valueOf(volume.getSizeInGigaBytes()));
            addNewCreateVolumeResponse.setSnapshotId(volume.getSnapshotId());
            addNewCreateVolumeResponse.setVolumeId(volume.getVolumeId());
            addNewCreateVolumeResponse.setStatus(getConversionUtils().getVolumeStatusString(volume.getStatus()));
            addNewCreateVolumeResponse.setRequestId(MDCHelper.getTransactionUID());
            LOG.debug(resultDocument);
            return (CreateVolumeResponseDocument) sanitiseXml(resultDocument);
        } catch (Throwable t) {
            throw handleThrowable(t);
        }
    }

    private int validateSize(String sizeInGigaBytes) {
        int result = 0;
        try {
            result = Integer.parseInt(sizeInGigaBytes);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("size not a number");
        }
        if (result < 1)
            throw new IllegalArgumentException("size must be greater than zero");
        if (result > this.maxVolumeSizeGigaBytes)
            throw new IllegalArgumentException("size must be less than " + this.maxVolumeSizeGigaBytes);
        return result;
    }

    @Property(key = "volume.max.size.in.gigabytes", defaultValue = DEFAULT_MAX_VOLUME_SIZE_IN_GIGABYTES)
    public void setMaxVolumeSizeInGigaBytes(int newMax) {
        this.maxVolumeSizeGigaBytes = newMax;
    }
}
