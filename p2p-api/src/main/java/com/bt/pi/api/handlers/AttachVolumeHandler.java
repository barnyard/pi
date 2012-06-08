/* (c) British Telecommunications plc, 2009, All Rights Reserved */
package com.bt.pi.api.handlers;

import java.util.Calendar;
import java.util.Locale;

import javax.annotation.Resource;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.ws.server.endpoint.annotation.Endpoint;
import org.springframework.ws.server.endpoint.annotation.PayloadRoot;

import com.amazonaws.ec2.doc.x20090404.AttachVolumeDocument;
import com.amazonaws.ec2.doc.x20090404.AttachVolumeResponseDocument;
import com.amazonaws.ec2.doc.x20090404.AttachVolumeResponseType;
import com.bt.pi.api.service.ElasticBlockStorageService;
import com.bt.pi.app.common.entities.Volume;
import com.bt.pi.core.util.MDCHelper;

/**
 * Web Service handler for AttachVolume
 */
@Endpoint("API.AttachVolumeHandler")
public class AttachVolumeHandler extends HandlerBase {
    private static final Log LOG = LogFactory.getLog(AttachVolumeHandler.class);
    private static final String ATTACH_VOLUME = "AttachVolume";
    private ElasticBlockStorageService elasticBlockStorageService;

    public AttachVolumeHandler() {
        elasticBlockStorageService = null;
    }

    @Resource
    public void setElasticBlockStorageService(ElasticBlockStorageService anElasticBlockStorageService) {
        elasticBlockStorageService = anElasticBlockStorageService;
    }

    @PayloadRoot(localPart = ATTACH_VOLUME, namespace = NAMESPACE_20081201)
    public com.amazonaws.ec2.doc.x20081201.AttachVolumeResponseDocument attachVolume(com.amazonaws.ec2.doc.x20081201.AttachVolumeDocument requestDocument) {
        return (com.amazonaws.ec2.doc.x20081201.AttachVolumeResponseDocument) callLatest(requestDocument);
    }

    @PayloadRoot(localPart = ATTACH_VOLUME, namespace = NAMESPACE_20090404)
    public AttachVolumeResponseDocument attachVolume(AttachVolumeDocument requestDocument) {
        LOG.debug(requestDocument);
        try {
            String device = requestDocument.getAttachVolume().getDevice();
            String instanceId = requestDocument.getAttachVolume().getInstanceId();
            String volumeId = requestDocument.getAttachVolume().getVolumeId();

            Volume volume = elasticBlockStorageService.attachVolume(getUserId(), volumeId, instanceId, device);

            AttachVolumeResponseDocument resultDocument = AttachVolumeResponseDocument.Factory.newInstance();
            Calendar calendar = Calendar.getInstance();
            calendar.setTimeInMillis(volume.getAttachTime());
            AttachVolumeResponseType addNewAttachVolumeResponse = resultDocument.addNewAttachVolumeResponse();
            addNewAttachVolumeResponse.setDevice(volume.getDevice());
            addNewAttachVolumeResponse.setInstanceId(volume.getInstanceId());
            addNewAttachVolumeResponse.setStatus(volume.getStatus().toString().toLowerCase(Locale.getDefault()));
            addNewAttachVolumeResponse.setVolumeId(volume.getVolumeId());
            addNewAttachVolumeResponse.setRequestId(MDCHelper.getTransactionUID());
            addNewAttachVolumeResponse.setAttachTime(calendar);
            LOG.debug(resultDocument);
            return (AttachVolumeResponseDocument) sanitiseXml(resultDocument);
        } catch (Throwable t) {
            throw handleThrowable(t);
        }
    }
}
