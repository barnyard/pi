/* (c) British Telecommunications plc, 2009, All Rights Reserved */
package com.bt.pi.api.handlers;

import javax.annotation.Resource;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.ws.server.endpoint.annotation.Endpoint;
import org.springframework.ws.server.endpoint.annotation.PayloadRoot;

import com.amazonaws.ec2.doc.x20090404.CreateKeyPairDocument;
import com.amazonaws.ec2.doc.x20090404.CreateKeyPairResponseDocument;
import com.amazonaws.ec2.doc.x20090404.CreateKeyPairResponseType;
import com.bt.pi.api.service.KeyPairsService;
import com.bt.pi.app.common.entities.KeyPair;
import com.bt.pi.core.util.MDCHelper;

/**
 * Web Service handler for CreateKeyPair
 */
@Endpoint
public class CreateKeyPairHandler extends HandlerBase {
    private static final Log LOG = LogFactory.getLog(CreateKeyPairHandler.class);
    private static final String CREATE_KEY_PAIR = "CreateKeyPair";
    private KeyPairsService keyPairsService;

    public CreateKeyPairHandler() {
        keyPairsService = null;
    }

    @Resource
    public void setKeyPairsService(KeyPairsService aKeyPairsService) {
        keyPairsService = aKeyPairsService;
    }

    @PayloadRoot(localPart = CREATE_KEY_PAIR, namespace = NAMESPACE_20081201)
    public com.amazonaws.ec2.doc.x20081201.CreateKeyPairResponseDocument createKeyPair(com.amazonaws.ec2.doc.x20081201.CreateKeyPairDocument requestDocument) {
        return (com.amazonaws.ec2.doc.x20081201.CreateKeyPairResponseDocument) callLatest(requestDocument);
    }

    @PayloadRoot(localPart = CREATE_KEY_PAIR, namespace = NAMESPACE_20090404)
    public CreateKeyPairResponseDocument createKeyPair(CreateKeyPairDocument requestDocument) {
        LOG.debug(requestDocument);
        try {
            String keyName = requestDocument.getCreateKeyPair().getKeyName();

            CreateKeyPairResponseDocument resultDocument = CreateKeyPairResponseDocument.Factory.newInstance();
            CreateKeyPairResponseType addNewCreateKeyPairResponse = resultDocument.addNewCreateKeyPairResponse();

            KeyPair keyPair = keyPairsService.createKeyPair(getUserId(), keyName);

            addNewCreateKeyPairResponse.setKeyName(keyPair.getKeyName());
            addNewCreateKeyPairResponse.setKeyMaterial(keyPair.getKeyMaterial());
            addNewCreateKeyPairResponse.setKeyFingerprint(keyPair.getKeyFingerprint());
            addNewCreateKeyPairResponse.setRequestId(MDCHelper.getTransactionUID());
            LOG.debug(resultDocument);
            return (CreateKeyPairResponseDocument) sanitiseXml(resultDocument);
        } catch (Throwable t) {
            throw handleThrowable(t);
        }
    }
}
