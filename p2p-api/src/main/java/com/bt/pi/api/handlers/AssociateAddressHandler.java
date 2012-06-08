/* (c) British Telecommunications plc, 2009, All Rights Reserved */
package com.bt.pi.api.handlers;

import javax.annotation.Resource;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.ws.server.endpoint.annotation.Endpoint;
import org.springframework.ws.server.endpoint.annotation.PayloadRoot;

import com.amazonaws.ec2.doc.x20090404.AssociateAddressResponseDocument;
import com.amazonaws.ec2.doc.x20090404.AssociateAddressResponseType;
import com.bt.pi.api.service.ElasticIpAddressesService;
import com.bt.pi.core.util.MDCHelper;

/**
 * Web Service handler for AssociateAddress
 */
@Endpoint
public class AssociateAddressHandler extends HandlerBase {
    private static final Log LOG = LogFactory.getLog(AssociateAddressHandler.class);
    private static final String ASSOCIATE_ADDRESS = "AssociateAddress";
    private ElasticIpAddressesService elasticIpAddressesService;

    public AssociateAddressHandler() {
        elasticIpAddressesService = null;
    }

    @Resource
    public void setElasticIpAddressesService(ElasticIpAddressesService anElasticIpAddressesService) {
        elasticIpAddressesService = anElasticIpAddressesService;
    }

    @PayloadRoot(localPart = ASSOCIATE_ADDRESS, namespace = NAMESPACE_20081201)
    public com.amazonaws.ec2.doc.x20081201.AssociateAddressResponseDocument associateAddress(com.amazonaws.ec2.doc.x20081201.AssociateAddressDocument requestDocument) {
        LOG.debug(requestDocument);
        return (com.amazonaws.ec2.doc.x20081201.AssociateAddressResponseDocument) callLatest(requestDocument);
    }

    @PayloadRoot(localPart = ASSOCIATE_ADDRESS, namespace = NAMESPACE_20090404)
    public com.amazonaws.ec2.doc.x20090404.AssociateAddressResponseDocument associateAddress(com.amazonaws.ec2.doc.x20090404.AssociateAddressDocument requestDocument) {
        LOG.debug(requestDocument);
        try {
            String ip = requestDocument.getAssociateAddress().getPublicIp();
            String instanceId = requestDocument.getAssociateAddress().getInstanceId();

            AssociateAddressResponseDocument resultDocument = AssociateAddressResponseDocument.Factory.newInstance();
            AssociateAddressResponseType addNewAssociateAddressResponse = resultDocument.addNewAssociateAddressResponse();

            boolean result = elasticIpAddressesService.associateAddress(getUserId(), ip, instanceId);
            addNewAssociateAddressResponse.setReturn(result);
            addNewAssociateAddressResponse.setRequestId(MDCHelper.getTransactionUID());
            LOG.debug(resultDocument);
            return (AssociateAddressResponseDocument) sanitiseXml(resultDocument);
        } catch (Throwable t) {
            LOG.error("exception!!!!: " + t);
            throw handleThrowable(t);
        }
    }
}
