/* (c) British Telecommunications plc, 2009, All Rights Reserved */
package com.bt.pi.api.handlers;

import javax.annotation.Resource;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.ws.server.endpoint.annotation.Endpoint;
import org.springframework.ws.server.endpoint.annotation.PayloadRoot;

import com.amazonaws.ec2.doc.x20081201.ReleaseAddressDocument;
import com.amazonaws.ec2.doc.x20081201.ReleaseAddressResponseDocument;
import com.amazonaws.ec2.doc.x20090404.ReleaseAddressResponseType;
import com.bt.pi.api.service.ElasticIpAddressesService;
import com.bt.pi.core.util.MDCHelper;

@Endpoint
public class ReleaseAddressHandler extends HandlerBase {
    private static final Log LOG = LogFactory.getLog(ReleaseAddressHandler.class);
    private static final String OPERATION = "ReleaseAddress";
    private ElasticIpAddressesService elasticIpAddressesService;

    public ReleaseAddressHandler() {
        elasticIpAddressesService = null;
    }

    @Resource
    public void setElasticIpAddressesService(ElasticIpAddressesService anElasticIpAddressesService) {
        elasticIpAddressesService = anElasticIpAddressesService;
    }

    @PayloadRoot(localPart = OPERATION, namespace = NAMESPACE_20081201)
    public ReleaseAddressResponseDocument releaseAddress(ReleaseAddressDocument requestDocument) {
        return (ReleaseAddressResponseDocument) callLatest(requestDocument);
    }

    @PayloadRoot(localPart = OPERATION, namespace = NAMESPACE_20090404)
    public com.amazonaws.ec2.doc.x20090404.ReleaseAddressResponseDocument releaseAddress(com.amazonaws.ec2.doc.x20090404.ReleaseAddressDocument requestDocument) {
        LOG.debug(requestDocument);
        try {
            com.amazonaws.ec2.doc.x20090404.ReleaseAddressResponseDocument resultDocument = com.amazonaws.ec2.doc.x20090404.ReleaseAddressResponseDocument.Factory.newInstance();
            boolean result = elasticIpAddressesService.releaseAddress(getUserId(), requestDocument.getReleaseAddress().getPublicIp());
            ReleaseAddressResponseType addNewReleaseAddressResponse = resultDocument.addNewReleaseAddressResponse();
            addNewReleaseAddressResponse.setReturn(result);
            addNewReleaseAddressResponse.setRequestId(MDCHelper.getTransactionUID());
            LOG.debug(resultDocument);
            return (com.amazonaws.ec2.doc.x20090404.ReleaseAddressResponseDocument) sanitiseXml(resultDocument);
        } catch (Throwable t) {
            throw handleThrowable(t);
        }
    }
}
