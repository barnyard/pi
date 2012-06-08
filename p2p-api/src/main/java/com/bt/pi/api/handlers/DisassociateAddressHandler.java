/* (c) British Telecommunications plc, 2009, All Rights Reserved */
package com.bt.pi.api.handlers;

import javax.annotation.Resource;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.ws.server.endpoint.annotation.Endpoint;
import org.springframework.ws.server.endpoint.annotation.PayloadRoot;

import com.amazonaws.ec2.doc.x20081201.DisassociateAddressDocument;
import com.amazonaws.ec2.doc.x20081201.DisassociateAddressResponseDocument;
import com.amazonaws.ec2.doc.x20090404.DisassociateAddressResponseType;
import com.bt.pi.api.service.ElasticIpAddressesService;
import com.bt.pi.core.util.MDCHelper;

/**
 * Web Service handler for DisassociateAddress
 */
@Endpoint
public class DisassociateAddressHandler extends HandlerBase {
    private static final Log LOG = LogFactory.getLog(DisassociateAddressHandler.class);
    private static final String DISASSOCIATE_ADDRESS = "DisassociateAddress";
    private ElasticIpAddressesService elasticIpAddressesService;

    public DisassociateAddressHandler() {
        elasticIpAddressesService = null;
    }

    @Resource
    public void setElasticIpAddressesService(ElasticIpAddressesService anElasticIpAddressesService) {
        elasticIpAddressesService = anElasticIpAddressesService;
    }

    @PayloadRoot(localPart = DISASSOCIATE_ADDRESS, namespace = NAMESPACE_20081201)
    public DisassociateAddressResponseDocument disassociateAddress(DisassociateAddressDocument requestDocument) {
        LOG.debug(requestDocument);
        return (DisassociateAddressResponseDocument) callLatest(requestDocument);
    }

    @PayloadRoot(localPart = DISASSOCIATE_ADDRESS, namespace = NAMESPACE_20090404)
    public com.amazonaws.ec2.doc.x20090404.DisassociateAddressResponseDocument disassociateAddress(com.amazonaws.ec2.doc.x20090404.DisassociateAddressDocument requestDocument) {
        LOG.debug(requestDocument);
        try {
            com.amazonaws.ec2.doc.x20090404.DisassociateAddressResponseDocument resultDocument = com.amazonaws.ec2.doc.x20090404.DisassociateAddressResponseDocument.Factory.newInstance();
            boolean result = elasticIpAddressesService.disassociateAddress(getUserId(), requestDocument.getDisassociateAddress().getPublicIp());
            DisassociateAddressResponseType addNewDisassociateAddressResponse = resultDocument.addNewDisassociateAddressResponse();
            addNewDisassociateAddressResponse.setReturn(result);
            addNewDisassociateAddressResponse.setRequestId(MDCHelper.getTransactionUID());
            LOG.debug(resultDocument);
            return (com.amazonaws.ec2.doc.x20090404.DisassociateAddressResponseDocument) sanitiseXml(resultDocument);
        } catch (Throwable t) {
            throw handleThrowable(t);
        }
    }
}
