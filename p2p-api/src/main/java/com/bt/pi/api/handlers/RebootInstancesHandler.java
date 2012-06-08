/* (c) British Telecommunications plc, 2009, All Rights Reserved */
package com.bt.pi.api.handlers;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Resource;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.ws.server.endpoint.annotation.Endpoint;
import org.springframework.ws.server.endpoint.annotation.PayloadRoot;

import com.amazonaws.ec2.doc.x20090404.RebootInstancesDocument;
import com.amazonaws.ec2.doc.x20090404.RebootInstancesItemType;
import com.amazonaws.ec2.doc.x20090404.RebootInstancesResponseDocument;
import com.amazonaws.ec2.doc.x20090404.RebootInstancesResponseType;
import com.bt.pi.api.service.InstancesService;
import com.bt.pi.core.util.MDCHelper;

/**
 * Web Service handler for RebootInstances
 */
@Endpoint
public class RebootInstancesHandler extends HandlerBase {
    private static final Log LOG = LogFactory.getLog(RebootInstancesHandler.class);
    private static final String OPERATION = "RebootInstances";
    private InstancesService instancesService;

    public RebootInstancesHandler() {
        instancesService = null;
    }

    @Resource
    public void setInstancesService(InstancesService anInstancesService) {
        instancesService = anInstancesService;
    }

    @PayloadRoot(localPart = OPERATION, namespace = NAMESPACE_20081201)
    public com.amazonaws.ec2.doc.x20081201.RebootInstancesResponseDocument rebootInstances(com.amazonaws.ec2.doc.x20081201.RebootInstancesDocument requestDocument) {
        return (com.amazonaws.ec2.doc.x20081201.RebootInstancesResponseDocument) callLatest(requestDocument);
    }

    @PayloadRoot(localPart = OPERATION, namespace = NAMESPACE_20090404)
    public RebootInstancesResponseDocument rebootInstances(RebootInstancesDocument requestDocument) {
        LOG.debug(requestDocument);
        try {
            List<String> instanceIds = new ArrayList<String>();
            if (null != requestDocument.getRebootInstances().getInstancesSet().getItemArray())
                for (RebootInstancesItemType rebootInstancesItemType : requestDocument.getRebootInstances().getInstancesSet().getItemArray())
                    instanceIds.add(rebootInstancesItemType.getInstanceId());

            boolean result = instancesService.rebootInstances(getUserId(), instanceIds);

            RebootInstancesResponseDocument resultDocument = RebootInstancesResponseDocument.Factory.newInstance();
            RebootInstancesResponseType addNewRebootInstancesResponse = resultDocument.addNewRebootInstancesResponse();
            addNewRebootInstancesResponse.setReturn(result);
            addNewRebootInstancesResponse.setRequestId(MDCHelper.getTransactionUID());
            LOG.debug(resultDocument);
            return (RebootInstancesResponseDocument) sanitiseXml(resultDocument);
        } catch (Throwable t) {
            throw handleThrowable(t);
        }
    }
}
