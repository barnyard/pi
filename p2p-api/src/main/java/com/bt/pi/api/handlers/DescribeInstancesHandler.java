/* (c) British Telecommunications plc, 2009, All Rights Reserved */
package com.bt.pi.api.handlers;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.Resource;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.xmlbeans.XmlObject;
import org.springframework.ws.server.endpoint.annotation.Endpoint;
import org.springframework.ws.server.endpoint.annotation.PayloadRoot;

import com.amazonaws.ec2.doc.x20090404.DescribeInstancesDocument;
import com.amazonaws.ec2.doc.x20090404.DescribeInstancesItemType;
import com.amazonaws.ec2.doc.x20090404.DescribeInstancesResponseDocument;
import com.amazonaws.ec2.doc.x20090404.DescribeInstancesResponseType;
import com.amazonaws.ec2.doc.x20090404.DescribeInstancesType;
import com.amazonaws.ec2.doc.x20090404.ReservationInfoType;
import com.amazonaws.ec2.doc.x20090404.ReservationSetType;
import com.amazonaws.ec2.doc.x20090404.RunningInstancesItemType;
import com.amazonaws.ec2.doc.x20090404.RunningInstancesSetType;
import com.bt.pi.api.service.InstancesService;
import com.bt.pi.api.utils.ConversionUtils;
import com.bt.pi.app.common.entities.Instance;
import com.bt.pi.core.util.MDCHelper;

/**
 * Web Service handler for DescribeInstances
 */
@Endpoint
public class DescribeInstancesHandler extends HandlerBase {
    private static final Log LOG = LogFactory.getLog(DescribeInstancesHandler.class);
    private static final String DESCRIBE_INSTANCES = "DescribeInstances";
    private InstancesService instancesService;
    private ConversionUtils conversionUtils;

    public DescribeInstancesHandler() {
        instancesService = null;
        conversionUtils = null;
    }

    @Resource
    public void setInstancesService(InstancesService anInstancesService) {
        instancesService = anInstancesService;
    }

    @Resource
    public void setConversionUtils(ConversionUtils aConversionUtils) {
        conversionUtils = aConversionUtils;
    }

    @PayloadRoot(localPart = DESCRIBE_INSTANCES, namespace = NAMESPACE_20081201)
    public com.amazonaws.ec2.doc.x20081201.DescribeInstancesResponseDocument describeInstances(com.amazonaws.ec2.doc.x20081201.DescribeInstancesDocument requestDocument) {
        return (com.amazonaws.ec2.doc.x20081201.DescribeInstancesResponseDocument) callLatest(requestDocument);
    }

    @PayloadRoot(localPart = DESCRIBE_INSTANCES, namespace = NAMESPACE_20090404)
    public DescribeInstancesResponseDocument describeInstances(DescribeInstancesDocument requestDocument) {
        LOG.debug(requestDocument);
        try {
            DescribeInstancesType describeInstances = requestDocument.getDescribeInstances();

            DescribeInstancesResponseDocument resultDocument = DescribeInstancesResponseDocument.Factory.newInstance();
            DescribeInstancesResponseType addNewDescribeInstancesResponse = resultDocument.addNewDescribeInstancesResponse();
            addNewDescribeInstancesResponse.setRequestId(getUserId());

            List<String> instanceIds = new ArrayList<String>();
            if (null != describeInstances.getInstancesSet() && null != describeInstances.getInstancesSet().getItemArray())
                for (DescribeInstancesItemType describeInstancesItemType : describeInstances.getInstancesSet().getItemArray())
                    instanceIds.add(describeInstancesItemType.getInstanceId());

            Map<String, Set<Instance>> mapOfReservationInstances = instancesService.describeInstances(getUserId(), instanceIds);

            ReservationSetType addNewReservationSet = addNewDescribeInstancesResponse.addNewReservationSet();

            for (Set<Instance> setOfInstances : mapOfReservationInstances.values()) {
                conversionUtils.convertReservation(addNewReservationSet, setOfInstances);
            }
            addNewDescribeInstancesResponse.setRequestId(MDCHelper.getTransactionUID());
            LOG.debug(resultDocument);
            return (DescribeInstancesResponseDocument) sanitiseXml(resultDocument);
        } catch (Throwable t) {
            throw handleThrowable(t);
        }
    }

    @Override
    protected void updateResult(XmlObject xmlObject) {
        DescribeInstancesResponseDocument describeInstancesResponseDocument = (DescribeInstancesResponseDocument) xmlObject;
        ReservationInfoType[] itemArray = describeInstancesResponseDocument.getDescribeInstancesResponse().getReservationSet().getItemArray();
        for (ReservationInfoType reservationInfoType : itemArray) {
            RunningInstancesSetType instancesSet = reservationInfoType.getInstancesSet();
            RunningInstancesItemType[] itemArray2 = instancesSet.getItemArray();
            for (RunningInstancesItemType runningInstancesItemType : itemArray2) {
                if (null != runningInstancesItemType.getMonitoring())
                    runningInstancesItemType.unsetMonitoring();
            }
        }
    }
}
