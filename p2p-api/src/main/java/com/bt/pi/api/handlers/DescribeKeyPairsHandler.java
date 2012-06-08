/* (c) British Telecommunications plc, 2009, All Rights Reserved */
package com.bt.pi.api.handlers;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Resource;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.ws.server.endpoint.annotation.Endpoint;
import org.springframework.ws.server.endpoint.annotation.PayloadRoot;

import com.amazonaws.ec2.doc.x20090404.DescribeKeyPairsDocument;
import com.amazonaws.ec2.doc.x20090404.DescribeKeyPairsInfoType;
import com.amazonaws.ec2.doc.x20090404.DescribeKeyPairsItemType;
import com.amazonaws.ec2.doc.x20090404.DescribeKeyPairsResponseDocument;
import com.amazonaws.ec2.doc.x20090404.DescribeKeyPairsResponseInfoType;
import com.amazonaws.ec2.doc.x20090404.DescribeKeyPairsResponseItemType;
import com.amazonaws.ec2.doc.x20090404.DescribeKeyPairsResponseType;
import com.bt.pi.api.service.KeyPairsService;
import com.bt.pi.app.common.entities.KeyPair;
import com.bt.pi.core.util.MDCHelper;

/**
 * Web Service handler for DescribeKeyPairs
 */
@Endpoint
public class DescribeKeyPairsHandler extends HandlerBase {
    private static final Log LOG = LogFactory.getLog(DescribeKeyPairsHandler.class);
    private static final String OPERATION = "DescribeKeyPairs";
    private KeyPairsService keyPairsService;

    public DescribeKeyPairsHandler() {
        keyPairsService = null;
    }

    @Resource
    public void setKeyPairsService(KeyPairsService aKeyPairsService) {
        keyPairsService = aKeyPairsService;
    }

    @PayloadRoot(localPart = OPERATION, namespace = NAMESPACE_20081201)
    public com.amazonaws.ec2.doc.x20081201.DescribeKeyPairsResponseDocument describeKeyPairs(com.amazonaws.ec2.doc.x20081201.DescribeKeyPairsDocument requestDocument) {
        LOG.debug(requestDocument);
        return (com.amazonaws.ec2.doc.x20081201.DescribeKeyPairsResponseDocument) callLatest(requestDocument);
    }

    @PayloadRoot(localPart = OPERATION, namespace = NAMESPACE_20090404)
    public DescribeKeyPairsResponseDocument describeKeyPairs(DescribeKeyPairsDocument requestDocument) {
        LOG.debug(requestDocument);
        try {
            DescribeKeyPairsResponseDocument resultDocument = DescribeKeyPairsResponseDocument.Factory.newInstance();
            DescribeKeyPairsResponseType addNewDescribeKeyPairsResponse = resultDocument.addNewDescribeKeyPairsResponse();
            DescribeKeyPairsResponseInfoType addNewKeySet = addNewDescribeKeyPairsResponse.addNewKeySet();

            DescribeKeyPairsInfoType keySet = requestDocument.getDescribeKeyPairs().getKeySet();

            List<String> keyNames = new ArrayList<String>();
            if (keySet != null && keySet.getItemArray() != null)
                for (DescribeKeyPairsItemType describeKeyPairsItemType : keySet.getItemArray())
                    keyNames.add(describeKeyPairsItemType.getKeyName());

            List<KeyPair> keyPairs = keyPairsService.describeKeyPairs(getUserId(), keyNames);

            for (KeyPair keyPair : keyPairs) {
                DescribeKeyPairsResponseItemType addNewItem = addNewKeySet.addNewItem();
                addNewItem.setKeyFingerprint(keyPair.getKeyFingerprint());
                addNewItem.setKeyName(keyPair.getKeyName());
            }

            addNewDescribeKeyPairsResponse.setRequestId(MDCHelper.getTransactionUID());
            LOG.debug(resultDocument);
            return (DescribeKeyPairsResponseDocument) sanitiseXml(resultDocument);
        } catch (Throwable t) {
            throw handleThrowable(t);
        }
    }
}
