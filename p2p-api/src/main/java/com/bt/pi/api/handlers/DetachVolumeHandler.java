/* (c) British Telecommunications plc, 2009, All Rights Reserved */
package com.bt.pi.api.handlers;

import java.util.Calendar;
import java.util.Locale;

import javax.annotation.Resource;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.ws.server.endpoint.annotation.Endpoint;
import org.springframework.ws.server.endpoint.annotation.PayloadRoot;

import com.amazonaws.ec2.doc.x20090404.DetachVolumeDocument;
import com.amazonaws.ec2.doc.x20090404.DetachVolumeResponseDocument;
import com.amazonaws.ec2.doc.x20090404.DetachVolumeResponseType;
import com.bt.pi.api.service.ElasticBlockStorageService;
import com.bt.pi.app.common.entities.Volume;
import com.bt.pi.core.util.MDCHelper;

/**
 * Web Service handler for DetachVolume
 */
@Endpoint("API.DetachVolumeHandler")
public class DetachVolumeHandler extends HandlerBase {
    private static final Log LOG = LogFactory.getLog(DetachVolumeHandler.class);
    private static final String DETACH_VOLUME = "DetachVolume";
    private ElasticBlockStorageService elasticBlockStorageService;

    public DetachVolumeHandler() {
        elasticBlockStorageService = null;
    }

    @Resource
    public void setElasticBlockStorageService(ElasticBlockStorageService anElasticBlockStorageService) {
        elasticBlockStorageService = anElasticBlockStorageService;
    }

    @PayloadRoot(localPart = DETACH_VOLUME, namespace = NAMESPACE_20081201)
    public com.amazonaws.ec2.doc.x20081201.DetachVolumeResponseDocument detachVolume(com.amazonaws.ec2.doc.x20081201.DetachVolumeDocument requestDocument) {
        return (com.amazonaws.ec2.doc.x20081201.DetachVolumeResponseDocument) callLatest(requestDocument);
    }

    @PayloadRoot(localPart = DETACH_VOLUME, namespace = NAMESPACE_20090404)
    public DetachVolumeResponseDocument detachVolume(DetachVolumeDocument requestDocument) {
        LOG.debug(requestDocument);
        try {
            String device = requestDocument.getDetachVolume().getDevice();
            boolean force = requestDocument.getDetachVolume().getForce();
            String instanceId = requestDocument.getDetachVolume().getInstanceId();
            String volumeId = requestDocument.getDetachVolume().getVolumeId();

            Volume volume = elasticBlockStorageService.detachVolume(getUserId(), volumeId, instanceId, device, force);

            DetachVolumeResponseDocument resultDocument = DetachVolumeResponseDocument.Factory.newInstance();
            DetachVolumeResponseType responseType = resultDocument.addNewDetachVolumeResponse();
            Calendar calendar = Calendar.getInstance();
            calendar.setTimeInMillis(volume.getAttachTime());
            responseType.setDevice(volume.getDevice());
            responseType.setInstanceId(volume.getInstanceId());
            responseType.setStatus(volume.getStatus().toString().toLowerCase(Locale.getDefault()));
            responseType.setVolumeId(volume.getVolumeId());
            responseType.setRequestId(MDCHelper.getTransactionUID());
            responseType.setAttachTime(calendar);
            LOG.debug(resultDocument);
            return (DetachVolumeResponseDocument) sanitiseXml(resultDocument);
        } catch (Throwable t) {
            throw handleThrowable(t);
        }
    }
}
