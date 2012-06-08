/* (c) British Telecommunications plc, 2009, All Rights Reserved */
package com.bt.pi.api.handlers;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.annotation.Resource;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.ws.server.endpoint.annotation.Endpoint;
import org.springframework.ws.server.endpoint.annotation.PayloadRoot;

import com.amazonaws.ec2.doc.x20090404.InstanceStateType;
import com.amazonaws.ec2.doc.x20090404.TerminateInstancesDocument;
import com.amazonaws.ec2.doc.x20090404.TerminateInstancesInfoType;
import com.amazonaws.ec2.doc.x20090404.TerminateInstancesItemType;
import com.amazonaws.ec2.doc.x20090404.TerminateInstancesResponseDocument;
import com.amazonaws.ec2.doc.x20090404.TerminateInstancesResponseInfoType;
import com.amazonaws.ec2.doc.x20090404.TerminateInstancesResponseItemType;
import com.amazonaws.ec2.doc.x20090404.TerminateInstancesResponseType;
import com.amazonaws.ec2.doc.x20090404.TerminateInstancesType;
import com.bt.pi.api.service.InstancesService;
import com.bt.pi.app.instancemanager.handlers.InstanceStateTransition;
import com.bt.pi.core.util.MDCHelper;

@Endpoint
public class TerminateInstancesHandler extends HandlerBase {
    private static final Log LOG = LogFactory.getLog(TerminateInstancesHandler.class);
    private static final String TERMINATE_INSTANCES = "TerminateInstances";
    private InstancesService instancesService;

    public TerminateInstancesHandler() {
        instancesService = null;
    }

    @Resource
    public void setInstancesService(InstancesService anInstancesService) {
        instancesService = anInstancesService;
    }

    @PayloadRoot(localPart = TERMINATE_INSTANCES, namespace = NAMESPACE_20081201)
    public com.amazonaws.ec2.doc.x20081201.TerminateInstancesResponseDocument terminateInstances(com.amazonaws.ec2.doc.x20081201.TerminateInstancesDocument requestDocument) {
        return (com.amazonaws.ec2.doc.x20081201.TerminateInstancesResponseDocument) callLatest(requestDocument);
    }

    @PayloadRoot(localPart = TERMINATE_INSTANCES, namespace = NAMESPACE_20090404)
    public TerminateInstancesResponseDocument terminateInstances(TerminateInstancesDocument requestDocument) {
        LOG.debug(requestDocument);
        try {

            TerminateInstancesType terminateInstances = requestDocument.getTerminateInstances();

            TerminateInstancesResponseDocument resultDocument = TerminateInstancesResponseDocument.Factory.newInstance();
            TerminateInstancesResponseType addNewTerminateInstancesResponse = resultDocument.addNewTerminateInstancesResponse();
            addNewTerminateInstancesResponse.setRequestId(getUserId());

            TerminateInstancesResponseInfoType responseInstancesSet = null;
            TerminateInstancesInfoType instancesSet = terminateInstances.getInstancesSet();
            if (null != instancesSet) {
                List<String> instanceIds = new ArrayList<String>();
                for (TerminateInstancesItemType item : instancesSet.getItemArray()) {
                    String instanceId = item.getInstanceId();
                    instanceIds.add(instanceId);
                }

                Map<String, InstanceStateTransition> instanceStatusMap = instancesService.terminateInstances(getUserId(), instanceIds);

                responseInstancesSet = addNewTerminateInstancesResponse.addNewInstancesSet();
                for (TerminateInstancesItemType item : instancesSet.getItemArray()) {
                    String instanceId = item.getInstanceId();
                    LOG.info(String.format("Terminating instance: %s. InstanceStatusMap %s.", instanceId, instanceStatusMap));
                    TerminateInstancesResponseItemType addNewItem = responseInstancesSet.addNewItem();
                    InstanceStateTransition transition = instanceStatusMap.get(instanceId);
                    addNewItem.setInstanceId(instanceId);
                    InstanceStateType addNewPreviousState = addNewItem.addNewPreviousState();
                    addNewPreviousState.setCode(transition.getPreviousState().getCode());
                    addNewPreviousState.setName(transition.getPreviousState().getDisplayName());
                    InstanceStateType addNewShutdownState = addNewItem.addNewShutdownState();
                    addNewShutdownState.setCode(transition.getNextState().getCode());
                    addNewShutdownState.setName(transition.getNextState().getDisplayName());
                }
            }
            addNewTerminateInstancesResponse.setRequestId(MDCHelper.getTransactionUID());
            LOG.debug(resultDocument);
            return (TerminateInstancesResponseDocument) sanitiseXml(resultDocument);
        } catch (Throwable t) {
            throw handleThrowable(t);
        }
    }
}
