/* (c) British Telecommunications plc, 2009, All Rights Reserved */
package com.bt.pi.api.handlers;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Resource;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.ws.server.endpoint.annotation.Endpoint;
import org.springframework.ws.server.endpoint.annotation.PayloadRoot;

import com.amazonaws.ec2.doc.x20090404.AvailabilityZoneItemType;
import com.amazonaws.ec2.doc.x20090404.AvailabilityZoneSetType;
import com.amazonaws.ec2.doc.x20090404.DescribeAvailabilityZonesDocument;
import com.amazonaws.ec2.doc.x20090404.DescribeAvailabilityZonesResponseDocument;
import com.amazonaws.ec2.doc.x20090404.DescribeAvailabilityZonesResponseType;
import com.amazonaws.ec2.doc.x20090404.DescribeAvailabilityZonesSetItemType;
import com.amazonaws.ec2.doc.x20090404.DescribeAvailabilityZonesSetType;
import com.amazonaws.ec2.doc.x20090404.DescribeAvailabilityZonesType;
import com.bt.pi.api.service.AvailabilityZonesAndRegionsService;
import com.bt.pi.app.common.entities.AvailabilityZone;
import com.bt.pi.app.common.entities.Region;
import com.bt.pi.core.util.MDCHelper;

/**
 * Web Service handler for DescribeAvailabilityZones
 */
@Endpoint
public class DescribeAvailabilityZonesHandler extends HandlerBase {
    private static final Log LOG = LogFactory.getLog(DescribeAvailabilityZonesHandler.class);
    private static final String OPERATION = "DescribeAvailabilityZones";
    private AvailabilityZonesAndRegionsService availilityZonesAndRegionService;

    public DescribeAvailabilityZonesHandler() {
        availilityZonesAndRegionService = null;
    }

    @Resource
    public void setAvailabilityZonesAndRegionsService(AvailabilityZonesAndRegionsService anAvaililityZonesAndRegionService) {
        availilityZonesAndRegionService = anAvaililityZonesAndRegionService;
    }

    @PayloadRoot(localPart = OPERATION, namespace = NAMESPACE_20081201)
    public com.amazonaws.ec2.doc.x20081201.DescribeAvailabilityZonesResponseDocument describeAvailabilityZones(com.amazonaws.ec2.doc.x20081201.DescribeAvailabilityZonesDocument requestDocument) {
        return (com.amazonaws.ec2.doc.x20081201.DescribeAvailabilityZonesResponseDocument) callLatest(requestDocument);
    }

    @PayloadRoot(localPart = OPERATION, namespace = NAMESPACE_20090404)
    public DescribeAvailabilityZonesResponseDocument describeAvailabilityZones(DescribeAvailabilityZonesDocument requestDocument) {
        LOG.debug(requestDocument);
        try {
            DescribeAvailabilityZonesType describeAvailabilityZones = requestDocument.getDescribeAvailabilityZones();
            DescribeAvailabilityZonesSetType availabilityZoneSet = describeAvailabilityZones.getAvailabilityZoneSet();
            DescribeAvailabilityZonesSetItemType[] itemArray = availabilityZoneSet.getItemArray();

            DescribeAvailabilityZonesResponseDocument resultDocument = DescribeAvailabilityZonesResponseDocument.Factory.newInstance();
            DescribeAvailabilityZonesResponseType addNewDescribeAvailabilityZonesResponse = resultDocument.addNewDescribeAvailabilityZonesResponse();
            AvailabilityZoneSetType addNewAvailabilityZoneInfo = addNewDescribeAvailabilityZonesResponse.addNewAvailabilityZoneInfo();

            List<String> availabilityZonesNames = new ArrayList<String>();
            if (null != itemArray)
                for (DescribeAvailabilityZonesSetItemType describeAvailabilityZonesSetItemType : itemArray)
                    availabilityZonesNames.add(describeAvailabilityZonesSetItemType.getZoneName());

            List<AvailabilityZone> availabilityZones = availilityZonesAndRegionService.describeAvailabilityZones(availabilityZonesNames);
            List<Region> regions = availilityZonesAndRegionService.describeRegions(null);

            for (AvailabilityZone availabilityZone : availabilityZones) {
                String regionName = getRegionNameForAvailabilityZone(regions, availabilityZone);

                AvailabilityZoneItemType addNewItem = addNewAvailabilityZoneInfo.addNewItem();
                addNewItem.setZoneName(availabilityZone.getAvailabilityZoneName());
                addNewItem.setRegionName(regionName);
                addNewItem.setZoneState(availabilityZone.getStatus());
            }
            addNewDescribeAvailabilityZonesResponse.setRequestId(MDCHelper.getTransactionUID());
            LOG.debug(resultDocument);
            return (DescribeAvailabilityZonesResponseDocument) sanitiseXml(resultDocument);
        } catch (Throwable t) {
            throw handleThrowable(t);
        }
    }

    private String getRegionNameForAvailabilityZone(List<Region> regions, AvailabilityZone availabilityZone) {
        String regionName = "";
        for (Region region : regions) {
            if (region.getRegionCode() == availabilityZone.getRegionCode()) {
                regionName = region.getRegionName();
                break;
            }
        }
        return regionName;
    }
}
