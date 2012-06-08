/* (c) British Telecommunications plc, 2009, All Rights Reserved */
package com.bt.pi.api.handlers;

import javax.annotation.Resource;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.ws.server.endpoint.annotation.Endpoint;
import org.springframework.ws.server.endpoint.annotation.PayloadRoot;

import com.amazonaws.ec2.doc.x20081201.RegisterImageDocument;
import com.amazonaws.ec2.doc.x20081201.RegisterImageResponseDocument;
import com.amazonaws.ec2.doc.x20090404.RegisterImageResponseType;
import com.bt.pi.api.service.ManagementImageService;
import com.bt.pi.core.util.MDCHelper;

@Endpoint
public class RegisterImageHandler extends HandlerBase {
    private static final Log LOG = LogFactory.getLog(RegisterImageHandler.class);
    private static final String OPERATION = "RegisterImage";
    private ManagementImageService imageService;

    public RegisterImageHandler() {
        imageService = null;
    }

    @Resource
    public void setImageService(ManagementImageService anIageService) {
        imageService = anIageService;
    }

    @PayloadRoot(localPart = OPERATION, namespace = NAMESPACE_20081201)
    public RegisterImageResponseDocument registerImage(RegisterImageDocument requestDocument) {
        return (RegisterImageResponseDocument) callLatest(requestDocument);
    }

    @PayloadRoot(localPart = OPERATION, namespace = NAMESPACE_20090404)
    public com.amazonaws.ec2.doc.x20090404.RegisterImageResponseDocument registerImage(com.amazonaws.ec2.doc.x20090404.RegisterImageDocument requestDocument) {
        LOG.debug(requestDocument);
        try {
            com.amazonaws.ec2.doc.x20090404.RegisterImageResponseDocument resultDocument = com.amazonaws.ec2.doc.x20090404.RegisterImageResponseDocument.Factory.newInstance();
            String imageId = imageService.registerImage(getUserId(), requestDocument.getRegisterImage().getImageLocation());
            RegisterImageResponseType addNewRegisterImageResponse = resultDocument.addNewRegisterImageResponse();
            addNewRegisterImageResponse.setImageId(imageId);
            addNewRegisterImageResponse.setRequestId(MDCHelper.getTransactionUID());
            LOG.debug(resultDocument);
            return (com.amazonaws.ec2.doc.x20090404.RegisterImageResponseDocument) sanitiseXml(resultDocument);
        } catch (Throwable t) {
            throw handleThrowable(t);
        }
    }
}
