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

import com.amazonaws.ec2.doc.x20090404.DescribeSnapshotsDocument;
import com.amazonaws.ec2.doc.x20090404.DescribeSnapshotsResponseDocument;
import com.amazonaws.ec2.doc.x20090404.DescribeSnapshotsResponseType;
import com.amazonaws.ec2.doc.x20090404.DescribeSnapshotsSetItemResponseType;
import com.amazonaws.ec2.doc.x20090404.DescribeSnapshotsSetItemType;
import com.amazonaws.ec2.doc.x20090404.DescribeSnapshotsSetResponseType;
import com.bt.pi.api.service.ElasticBlockStorageService;
import com.bt.pi.app.common.entities.Snapshot;
import com.bt.pi.core.util.MDCHelper;

@Endpoint
public class DescribeSnapshotsHandler extends HandlerBase {

    private static final Log LOG = LogFactory.getLog(DescribeSnapshotsHandler.class);
    private static final String DESCRIBE_SNAPSHOTS = "DescribeSnapshots";
    private ElasticBlockStorageService elasticBlockStorage;

    public DescribeSnapshotsHandler() {
        elasticBlockStorage = null;
    }

    @Resource
    public void setElasticBlockStorage(ElasticBlockStorageService anElasticBlockStorage) {
        elasticBlockStorage = anElasticBlockStorage;
    }

    @PayloadRoot(localPart = DESCRIBE_SNAPSHOTS, namespace = NAMESPACE_20081201)
    public com.amazonaws.ec2.doc.x20081201.DescribeSnapshotsResponseDocument describeSnapshots(com.amazonaws.ec2.doc.x20081201.DescribeSnapshotsDocument requestDocument) {
        return (com.amazonaws.ec2.doc.x20081201.DescribeSnapshotsResponseDocument) callLatest(requestDocument);
    }

    @PayloadRoot(localPart = DESCRIBE_SNAPSHOTS, namespace = NAMESPACE_20090404)
    public DescribeSnapshotsResponseDocument describeSnapshots(DescribeSnapshotsDocument requestDocument) {
        LOG.debug(String.format("describeSnapshots(%s)", requestDocument));
        try {
            DescribeSnapshotsResponseDocument resultDocument = DescribeSnapshotsResponseDocument.Factory.newInstance();
            DescribeSnapshotsResponseType describeSnapshotsResponseType = resultDocument.addNewDescribeSnapshotsResponse();
            DescribeSnapshotsSetResponseType describeSnapshotsSetResponseType = describeSnapshotsResponseType.addNewSnapshotSet();

            List<String> snapshotIds = new ArrayList<String>();
            if (null != requestDocument.getDescribeSnapshots().getSnapshotSet().getItemArray())
                for (DescribeSnapshotsSetItemType snapshotId : requestDocument.getDescribeSnapshots().getSnapshotSet().getItemArray())
                    snapshotIds.add(snapshotId.getSnapshotId());

            String userId = getUserId();
            List<Snapshot> snapshots = elasticBlockStorage.describeSnapshots(userId, snapshotIds);

            for (Snapshot snapshot : snapshots) {
                DescribeSnapshotsSetItemResponseType describeSnapshotsSetItemResponseType = describeSnapshotsSetResponseType.addNewItem();
                describeSnapshotsSetItemResponseType.setProgress(String.valueOf(snapshot.getProgress()));
                describeSnapshotsSetItemResponseType.setSnapshotId(snapshot.getSnapshotId());
                Calendar calendar = Calendar.getInstance();
                calendar.setTimeInMillis(snapshot.getStartTime());
                describeSnapshotsSetItemResponseType.setStartTime(calendar);
                describeSnapshotsSetItemResponseType.setStatus(getConversionUtils().getSnapshotStatusString(snapshot.getStatus()));
                describeSnapshotsSetItemResponseType.setVolumeId(snapshot.getVolumeId());
            }
            describeSnapshotsResponseType.setRequestId(MDCHelper.getTransactionUID());
            LOG.debug(resultDocument);
            return (DescribeSnapshotsResponseDocument) sanitiseXml(resultDocument);
        } catch (Throwable t) {
            throw handleThrowable(t);
        }
    }
}
