/* (c) British Telecommunications plc, 2009, All Rights Reserved */
package com.bt.pi.api.handlers;

import javax.annotation.Resource;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.ws.server.endpoint.annotation.Endpoint;
import org.springframework.ws.server.endpoint.annotation.PayloadRoot;

import com.amazonaws.ec2.doc.x20081201.DeregisterImageDocument;
import com.amazonaws.ec2.doc.x20081201.DeregisterImageResponseDocument;
import com.amazonaws.ec2.doc.x20090404.DeregisterImageResponseType;
import com.bt.pi.api.service.ManagementImageService;
import com.bt.pi.core.util.MDCHelper;

@Endpoint
public class DeregisterImageHandler extends HandlerBase {
    private static final Log LOG = LogFactory.getLog(DeregisterImageHandler.class);
    private static final String OPERATION = "DeregisterImage";
    private ManagementImageService imageService;

    public DeregisterImageHandler() {
        imageService = null;
    }

    @Resource
    public void setImageService(ManagementImageService anImageService) {
        imageService = anImageService;
    }

    @PayloadRoot(localPart = OPERATION, namespace = NAMESPACE_20081201)
    public DeregisterImageResponseDocument deregisterImage(DeregisterImageDocument requestDocument) {
        LOG.debug(requestDocument);
        return (DeregisterImageResponseDocument) callLatest(requestDocument);
    }

    @PayloadRoot(localPart = OPERATION, namespace = NAMESPACE_20090404)
    public com.amazonaws.ec2.doc.x20090404.DeregisterImageResponseDocument deregisterImage(com.amazonaws.ec2.doc.x20090404.DeregisterImageDocument requestDocument) {
        LOG.debug(requestDocument);
        try {
            com.amazonaws.ec2.doc.x20090404.DeregisterImageResponseDocument responseDocument = com.amazonaws.ec2.doc.x20090404.DeregisterImageResponseDocument.Factory.newInstance();
            boolean result = imageService.deregisterImage(getUserId(), requestDocument.getDeregisterImage().getImageId());
            DeregisterImageResponseType addNewDeregisterImageResponse = responseDocument.addNewDeregisterImageResponse();
            addNewDeregisterImageResponse.setReturn(result);
            addNewDeregisterImageResponse.setRequestId(MDCHelper.getTransactionUID());
            LOG.debug(responseDocument);
            return (com.amazonaws.ec2.doc.x20090404.DeregisterImageResponseDocument) sanitiseXml(responseDocument);
        } catch (Throwable t) {
            throw handleThrowable(t);
        }
    }
}
