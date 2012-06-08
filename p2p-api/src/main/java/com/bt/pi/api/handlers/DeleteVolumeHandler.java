/* (c) British Telecommunications plc, 2009, All Rights Reserved */
package com.bt.pi.api.handlers;

import javax.annotation.Resource;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.ws.server.endpoint.annotation.Endpoint;
import org.springframework.ws.server.endpoint.annotation.PayloadRoot;

import com.amazonaws.ec2.doc.x20090404.DeleteVolumeDocument;
import com.amazonaws.ec2.doc.x20090404.DeleteVolumeResponseDocument;
import com.amazonaws.ec2.doc.x20090404.DeleteVolumeResponseType;
import com.bt.pi.api.service.ElasticBlockStorageService;
import com.bt.pi.core.util.MDCHelper;

@Endpoint("API.DeleteVolumeHandler")
public class DeleteVolumeHandler extends HandlerBase {

    private static final Log LOG = LogFactory.getLog(DeleteSnapshotHandler.class);
    private static final String DELETE_VOLUME = "DeleteVolume";
    private ElasticBlockStorageService elasticBlockStorageService;

    public DeleteVolumeHandler() {
        elasticBlockStorageService = null;
    }

    @Resource
    public void setElasticBlockStorageService(ElasticBlockStorageService anElasticBlockStorageService) {
        elasticBlockStorageService = anElasticBlockStorageService;
    }

    @PayloadRoot(localPart = DELETE_VOLUME, namespace = NAMESPACE_20081201)
    public com.amazonaws.ec2.doc.x20081201.DeleteVolumeResponseDocument deleteVolume(com.amazonaws.ec2.doc.x20081201.DeleteVolumeDocument requestDocument) {
        return (com.amazonaws.ec2.doc.x20081201.DeleteVolumeResponseDocument) callLatest(requestDocument);
    }

    @PayloadRoot(localPart = DELETE_VOLUME, namespace = NAMESPACE_20090404)
    public DeleteVolumeResponseDocument deleteVolume(DeleteVolumeDocument requestDocument) {
        LOG.debug(String.format("deleteVolume(%s)", requestDocument));
        try {
            String volumeId = requestDocument.getDeleteVolume().getVolumeId();
            boolean result = elasticBlockStorageService.deleteVolume(getUserId(), volumeId);

            DeleteVolumeResponseDocument responseDocument = DeleteVolumeResponseDocument.Factory.newInstance();
            DeleteVolumeResponseType addNewDeleteVolumeResponse = responseDocument.addNewDeleteVolumeResponse();
            addNewDeleteVolumeResponse.setReturn(result);
            addNewDeleteVolumeResponse.setRequestId(MDCHelper.getTransactionUID());
            LOG.debug(responseDocument);
            return (DeleteVolumeResponseDocument) sanitiseXml(responseDocument);
        } catch (Throwable t) {
            throw handleThrowable(t);
        }
    }
}
