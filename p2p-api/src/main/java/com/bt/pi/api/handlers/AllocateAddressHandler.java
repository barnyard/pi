/* (c) British Telecommunications plc, 2009, All Rights Reserved */
package com.bt.pi.api.handlers;

import javax.annotation.Resource;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.ws.server.endpoint.annotation.Endpoint;
import org.springframework.ws.server.endpoint.annotation.PayloadRoot;

import com.amazonaws.ec2.doc.x20090404.AllocateAddressResponseDocument;
import com.amazonaws.ec2.doc.x20090404.AllocateAddressResponseType;
import com.bt.pi.api.service.ElasticIpAddressesService;
import com.bt.pi.core.util.MDCHelper;

/**
 * Web Service handler for AllocateAddress
 */
@Endpoint
public class AllocateAddressHandler extends HandlerBase {
    private static final Log LOG = LogFactory.getLog(AllocateAddressHandler.class);
    private static final String ALLOCATE_ADDRESS = "AllocateAddress";
    @Resource
    private ElasticIpAddressesService elasticIpAddressesService;

    public AllocateAddressHandler() {
        elasticIpAddressesService = null;
    }

    @PayloadRoot(localPart = ALLOCATE_ADDRESS, namespace = NAMESPACE_20081201)
    public com.amazonaws.ec2.doc.x20081201.AllocateAddressResponseDocument allocateAddress(com.amazonaws.ec2.doc.x20081201.AllocateAddressDocument requestDocument) {
        LOG.debug(requestDocument);
        return (com.amazonaws.ec2.doc.x20081201.AllocateAddressResponseDocument) callLatest(requestDocument);
    }

    @PayloadRoot(localPart = ALLOCATE_ADDRESS, namespace = NAMESPACE_20090404)
    public com.amazonaws.ec2.doc.x20090404.AllocateAddressResponseDocument allocateAddress(com.amazonaws.ec2.doc.x20090404.AllocateAddressDocument requestDocument) {
        LOG.debug(requestDocument);
        try {
            AllocateAddressResponseDocument resultDocument = AllocateAddressResponseDocument.Factory.newInstance();
            AllocateAddressResponseType addNewAllocateAddressResponse = resultDocument.addNewAllocateAddressResponse();
            String newIpAddress = elasticIpAddressesService.allocateAddress(getUserId());
            addNewAllocateAddressResponse.setPublicIp(newIpAddress);
            addNewAllocateAddressResponse.setRequestId(MDCHelper.getTransactionUID());
            LOG.debug(resultDocument);
            return (AllocateAddressResponseDocument) sanitiseXml(resultDocument);
        } catch (Throwable t) {
            throw handleThrowable(t);
        }
    }
}
