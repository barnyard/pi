/* (c) British Telecommunications plc, 2009, All Rights Reserved */
package com.bt.pi.api.handlers;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Resource;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.ws.server.endpoint.annotation.Endpoint;
import org.springframework.ws.server.endpoint.annotation.PayloadRoot;

import com.amazonaws.ec2.doc.x20090404.DescribeRegionsDocument;
import com.amazonaws.ec2.doc.x20090404.DescribeRegionsResponseDocument;
import com.amazonaws.ec2.doc.x20090404.DescribeRegionsResponseType;
import com.amazonaws.ec2.doc.x20090404.DescribeRegionsSetItemType;
import com.amazonaws.ec2.doc.x20090404.RegionItemType;
import com.amazonaws.ec2.doc.x20090404.RegionSetType;
import com.bt.pi.api.service.AvailabilityZonesAndRegionsService;
import com.bt.pi.app.common.entities.Region;
import com.bt.pi.core.util.MDCHelper;

/**
 * Web Service handler for DescribeRegions
 */
@Endpoint
public class DescribeRegionsHandler extends HandlerBase {

    private static final String DESCRIBE_REGIONS = "DescribeRegions";
    private static final String METHOD_ENTRY_LOG_MESSAGE = "describeRegions(%s)";
    private static final Log LOG = LogFactory.getLog(DescribeRegionsHandler.class);
    private AvailabilityZonesAndRegionsService availabilityZonesAndRegionsService;

    public DescribeRegionsHandler() {
        availabilityZonesAndRegionsService = null;
    }

    @Resource
    protected void setAvailabilityZonesAndRegions(AvailabilityZonesAndRegionsService anAvailabilityZonesAndRegions) {
        availabilityZonesAndRegionsService = anAvailabilityZonesAndRegions;
    }

    @PayloadRoot(localPart = DESCRIBE_REGIONS, namespace = NAMESPACE_20081201)
    public com.amazonaws.ec2.doc.x20081201.DescribeRegionsResponseDocument describeRegions(com.amazonaws.ec2.doc.x20081201.DescribeRegionsDocument requestDocument) {
        LOG.debug(String.format(METHOD_ENTRY_LOG_MESSAGE, requestDocument));
        return (com.amazonaws.ec2.doc.x20081201.DescribeRegionsResponseDocument) callLatest(requestDocument);
    }

    @PayloadRoot(localPart = DESCRIBE_REGIONS, namespace = NAMESPACE_20090404)
    public DescribeRegionsResponseDocument describeRegions(DescribeRegionsDocument requestDocument) {
        LOG.debug(String.format(METHOD_ENTRY_LOG_MESSAGE, requestDocument));
        try {
            DescribeRegionsResponseDocument resultDocument = DescribeRegionsResponseDocument.Factory.newInstance();
            DescribeRegionsResponseType describeRegionsResponseType = resultDocument.addNewDescribeRegionsResponse();

            DescribeRegionsSetItemType[] describeRegionsSetItemTypes = requestDocument.getDescribeRegions().getRegionSet().getItemArray();
            List<String> regionNames = new ArrayList<String>();

            for (DescribeRegionsSetItemType describeRegionsSetItemType : describeRegionsSetItemTypes)
                regionNames.add(describeRegionsSetItemType.getRegionName());

            List<Region> regionsResult = availabilityZonesAndRegionsService.describeRegions(regionNames);

            RegionSetType regionSetType = describeRegionsResponseType.addNewRegionInfo();
            if (regionsResult.size() > 0) {
                for (Region region : regionsResult) {
                    RegionItemType regionItemType = regionSetType.addNewItem();
                    regionItemType.setRegionName(region.getRegionName());
                    regionItemType.setRegionEndpoint(region.getRegionEndpoint());
                }
            }
            describeRegionsResponseType.setRequestId(MDCHelper.getTransactionUID());
            LOG.debug(resultDocument);
            return (DescribeRegionsResponseDocument) sanitiseXml(resultDocument);
        } catch (Throwable t) {
            throw handleThrowable(t);
        }
    }
}
