/* (c) British Telecommunications plc, 2009, All Rights Reserved */
package com.bt.pi.api.handlers;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;

import javax.annotation.Resource;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.ws.server.endpoint.annotation.Endpoint;
import org.springframework.ws.server.endpoint.annotation.PayloadRoot;

import com.amazonaws.ec2.doc.x20090404.DescribeAddressesDocument;
import com.amazonaws.ec2.doc.x20090404.DescribeAddressesInfoType;
import com.amazonaws.ec2.doc.x20090404.DescribeAddressesItemType;
import com.amazonaws.ec2.doc.x20090404.DescribeAddressesResponseDocument;
import com.amazonaws.ec2.doc.x20090404.DescribeAddressesResponseInfoType;
import com.amazonaws.ec2.doc.x20090404.DescribeAddressesResponseItemType;
import com.amazonaws.ec2.doc.x20090404.DescribeAddressesResponseType;
import com.amazonaws.ec2.doc.x20090404.DescribeAddressesType;
import com.bt.pi.api.service.ElasticIpAddressesService;
import com.bt.pi.app.common.entities.InstanceRecord;
import com.bt.pi.core.util.MDCHelper;

/**
 * Web Service handler for DescribeAddresses
 */
@Endpoint
public class DescribeAddressesHandler extends HandlerBase {
    private static final Log LOG = LogFactory.getLog(DescribeAddressesHandler.class);
    private static final String DESCRIBE_ADDRESSES = "DescribeAddresses";
    private ElasticIpAddressesService elasticIpAddressesService;

    public DescribeAddressesHandler() {
        elasticIpAddressesService = null;
    }

    @Resource
    public void setElasticIpAddressesService(ElasticIpAddressesService anElasticIpAddressesService) {
        elasticIpAddressesService = anElasticIpAddressesService;
    }

    @PayloadRoot(localPart = DESCRIBE_ADDRESSES, namespace = NAMESPACE_20081201)
    public com.amazonaws.ec2.doc.x20081201.DescribeAddressesResponseDocument describeAddresses(com.amazonaws.ec2.doc.x20081201.DescribeAddressesDocument requestDocument) {
        return (com.amazonaws.ec2.doc.x20081201.DescribeAddressesResponseDocument) callLatest(requestDocument);
    }

    @SuppressWarnings("unchecked")
    @PayloadRoot(localPart = DESCRIBE_ADDRESSES, namespace = NAMESPACE_20090404)
    public DescribeAddressesResponseDocument describeAddresses(DescribeAddressesDocument requestDocument) {
        try {
            LOG.debug(requestDocument);

            DescribeAddressesType describeAddresses = requestDocument.getDescribeAddresses();

            DescribeAddressesResponseDocument resultDocument = DescribeAddressesResponseDocument.Factory.newInstance();
            DescribeAddressesResponseType addNewDescribeAddressesResponse = resultDocument.addNewDescribeAddressesResponse();
            DescribeAddressesResponseInfoType addNewAddressesSet = addNewDescribeAddressesResponse.addNewAddressesSet();

            DescribeAddressesInfoType publicIpsSet = describeAddresses.getPublicIpsSet();
            List<String> addresses = new ArrayList<String>();
            if (publicIpsSet != null && publicIpsSet.getItemArray() != null && publicIpsSet.getItemArray().length != 0)
                for (DescribeAddressesItemType describeAddressesItemType : publicIpsSet.getItemArray())
                    addresses.add(describeAddressesItemType.getPublicIp());

            SortedMap<String, InstanceRecord> addressToInstanceRecords = elasticIpAddressesService.describeAddresses(getUserId(), addresses);

            Iterator it = addressToInstanceRecords.entrySet().iterator();
            while (it.hasNext()) {
                Map.Entry pairs = (Map.Entry) it.next();
                String publicIpAddress = (String) pairs.getKey();
                String instanceId = ((InstanceRecord) pairs.getValue()).getInstanceId();
                LOG.debug(String.format("Adding address (%s) and instance (%s) pair", publicIpAddress, instanceId));
                DescribeAddressesResponseItemType addNewItem = addNewAddressesSet.addNewItem();
                addNewItem.setInstanceId(instanceId);
                addNewItem.setPublicIp(publicIpAddress);
            }
            addNewDescribeAddressesResponse.setRequestId(MDCHelper.getTransactionUID());
            LOG.debug(resultDocument);
            return (DescribeAddressesResponseDocument) sanitiseXml(resultDocument);
        } catch (Throwable t) {
            throw handleThrowable(t);
        }
    }
}
