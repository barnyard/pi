/* (c) British Telecommunications plc, 2009, All Rights Reserved */
package com.bt.pi.api.handlers;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import javax.annotation.Resource;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.ws.server.endpoint.annotation.Endpoint;
import org.springframework.ws.server.endpoint.annotation.PayloadRoot;

import com.amazonaws.ec2.doc.x20090404.AttachmentSetItemResponseType;
import com.amazonaws.ec2.doc.x20090404.AttachmentSetResponseType;
import com.amazonaws.ec2.doc.x20090404.DescribeVolumesDocument;
import com.amazonaws.ec2.doc.x20090404.DescribeVolumesResponseDocument;
import com.amazonaws.ec2.doc.x20090404.DescribeVolumesResponseType;
import com.amazonaws.ec2.doc.x20090404.DescribeVolumesSetItemResponseType;
import com.amazonaws.ec2.doc.x20090404.DescribeVolumesSetItemType;
import com.amazonaws.ec2.doc.x20090404.DescribeVolumesSetResponseType;
import com.bt.pi.api.service.ElasticBlockStorageService;
import com.bt.pi.app.common.entities.Volume;
import com.bt.pi.core.util.MDCHelper;

/**
 * Web Service handler for DescribeVolumes
 */
@Endpoint
public class DescribeVolumesHandler extends HandlerBase {

    private static final Log LOG = LogFactory.getLog(DescribeVolumesHandler.class);
    private static final String DESCRIBE_VOLUMES = "DescribeVolumes";
    private ElasticBlockStorageService elasticBlockStorageService;

    public DescribeVolumesHandler() {
        elasticBlockStorageService = null;
    }

    @Resource
    public void setElasticBlockStorageService(ElasticBlockStorageService anElasticBlockStorageService) {
        elasticBlockStorageService = anElasticBlockStorageService;
    }

    @PayloadRoot(localPart = DESCRIBE_VOLUMES, namespace = NAMESPACE_20081201)
    public com.amazonaws.ec2.doc.x20081201.DescribeVolumesResponseDocument describeVolumes(com.amazonaws.ec2.doc.x20081201.DescribeVolumesDocument requestDocument) {
        return (com.amazonaws.ec2.doc.x20081201.DescribeVolumesResponseDocument) callLatest(requestDocument);
    }

    @PayloadRoot(localPart = DESCRIBE_VOLUMES, namespace = NAMESPACE_20090404)
    public DescribeVolumesResponseDocument describeVolumes(DescribeVolumesDocument requestDocument) {
        LOG.debug(requestDocument);
        try {
            List<String> volumeIds = new ArrayList<String>();
            if (null != requestDocument.getDescribeVolumes().getVolumeSet().getItemArray())
                for (DescribeVolumesSetItemType itemType : requestDocument.getDescribeVolumes().getVolumeSet().getItemArray())
                    volumeIds.add(itemType.getVolumeId());

            List<Volume> volumes = elasticBlockStorageService.describeVolumes(getUserId(), volumeIds);

            DescribeVolumesResponseDocument resultDocument = DescribeVolumesResponseDocument.Factory.newInstance();
            DescribeVolumesResponseType type = resultDocument.addNewDescribeVolumesResponse();
            DescribeVolumesSetResponseType volumesSetResponseType = type.addNewVolumeSet();

            Calendar calendar = Calendar.getInstance();
            for (Volume volume : volumes) {
                DescribeVolumesSetItemResponseType responseItem = volumesSetResponseType.addNewItem();

                AttachmentSetResponseType attachmentSet = responseItem.addNewAttachmentSet();
                calendar.setTimeInMillis(volume.getAttachTime());
                if (null != volume.getInstanceId() && !volume.getInstanceId().equals("")) {
                    AttachmentSetItemResponseType itemResponseType = attachmentSet.addNewItem();
                    itemResponseType.setDevice(volume.getDevice());
                    itemResponseType.setInstanceId(volume.getInstanceId());
                    itemResponseType.setStatus(volume.getAttachedStatus());
                    itemResponseType.setVolumeId(volume.getVolumeId());
                    itemResponseType.setAttachTime(calendar);
                }
                responseItem.setAvailabilityZone(volume.getAvailabilityZone());
                calendar.setTimeInMillis(volume.getCreateTime());
                responseItem.setCreateTime(calendar);
                responseItem.setSize(String.valueOf(volume.getSizeInGigaBytes()));
                responseItem.setSnapshotId(volume.getSnapshotId());
                if (null != volume.getStatus())
                    responseItem.setStatus(getConversionUtils().getVolumeStatusString(volume.getStatus()));
                responseItem.setVolumeId(volume.getVolumeId());
            }
            type.setRequestId(MDCHelper.getTransactionUID());
            LOG.debug(resultDocument);
            return (DescribeVolumesResponseDocument) sanitiseXml(resultDocument);
        } catch (Throwable t) {
            throw handleThrowable(t);
        }
    }
}
