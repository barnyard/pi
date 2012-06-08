/* (c) British Telecommunications plc, 2009, All Rights Reserved */
package com.bt.pi.api.handlers;

import javax.annotation.Resource;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.ws.server.endpoint.annotation.Endpoint;
import org.springframework.ws.server.endpoint.annotation.PayloadRoot;

import com.amazonaws.ec2.doc.x20090404.DeleteKeyPairDocument;
import com.amazonaws.ec2.doc.x20090404.DeleteKeyPairResponseDocument;
import com.amazonaws.ec2.doc.x20090404.DeleteKeyPairResponseType;
import com.bt.pi.api.service.KeyPairsService;
import com.bt.pi.core.util.MDCHelper;

/**
 * Web Service handler for DeleteKeyPair
 */
@Endpoint
public class DeleteKeyPairHandler extends HandlerBase {
    private static final Log LOG = LogFactory.getLog(DeleteKeyPairHandler.class);
    private static final String OPERATION = "DeleteKeyPair";
    private KeyPairsService keyPairsService;

    public DeleteKeyPairHandler() {
        keyPairsService = null;
    }

    @Resource
    public void setKeyPairsService(KeyPairsService aKeyPairsService) {
        keyPairsService = aKeyPairsService;
    }

    @PayloadRoot(localPart = OPERATION, namespace = NAMESPACE_20081201)
    public com.amazonaws.ec2.doc.x20081201.DeleteKeyPairResponseDocument deleteKeyPair(com.amazonaws.ec2.doc.x20081201.DeleteKeyPairDocument requestDocument) {
        LOG.debug(requestDocument);
        return (com.amazonaws.ec2.doc.x20081201.DeleteKeyPairResponseDocument) callLatest(requestDocument);
    }

    @PayloadRoot(localPart = OPERATION, namespace = NAMESPACE_20090404)
    public DeleteKeyPairResponseDocument deleteKeyPair(DeleteKeyPairDocument requestDocument) {
        LOG.debug(requestDocument);
        try {
            String keyName = requestDocument.getDeleteKeyPair().getKeyName();
            DeleteKeyPairResponseDocument resultDocument = DeleteKeyPairResponseDocument.Factory.newInstance();
            DeleteKeyPairResponseType addNewDeleteKeyPairResponse = resultDocument.addNewDeleteKeyPairResponse();
            boolean result = keyPairsService.deleteKeyPair(getUserId(), keyName);
            addNewDeleteKeyPairResponse.setReturn(result);
            addNewDeleteKeyPairResponse.setRequestId(MDCHelper.getTransactionUID());
            LOG.debug(resultDocument);
            return (DeleteKeyPairResponseDocument) sanitiseXml(resultDocument);
        } catch (Throwable t) {
            throw handleThrowable(t);
        }
    }
}
