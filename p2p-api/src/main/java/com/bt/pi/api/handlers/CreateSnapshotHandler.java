/* (c) British Telecommunications plc, 2009, All Rights Reserved */
package com.bt.pi.api.handlers;

import java.util.Calendar;

import javax.annotation.Resource;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.ws.server.endpoint.annotation.Endpoint;
import org.springframework.ws.server.endpoint.annotation.PayloadRoot;

import com.amazonaws.ec2.doc.x20090404.CreateSnapshotDocument;
import com.amazonaws.ec2.doc.x20090404.CreateSnapshotResponseDocument;
import com.amazonaws.ec2.doc.x20090404.CreateSnapshotResponseType;
import com.bt.pi.api.service.ElasticBlockStorageService;
import com.bt.pi.app.common.entities.Snapshot;
import com.bt.pi.core.util.MDCHelper;

/**
 * Web Service handler for CreateSnapshot
 */
@Endpoint("API.CreateSnaphotHandler")
public class CreateSnapshotHandler extends HandlerBase {
    private static final Log LOG = LogFactory.getLog(CreateSnapshotHandler.class);
    private static final String CREATE_SNAPSHOT = "CreateSnapshot";
    private ElasticBlockStorageService elasticBlockStorageService;

    public CreateSnapshotHandler() {
        elasticBlockStorageService = null;
    }

    @Resource
    public void setElasticBlockStorageService(ElasticBlockStorageService anElasticBlockStorageService) {
        elasticBlockStorageService = anElasticBlockStorageService;
    }

    @PayloadRoot(localPart = CREATE_SNAPSHOT, namespace = NAMESPACE_20081201)
    public com.amazonaws.ec2.doc.x20081201.CreateSnapshotResponseDocument createSnapshot(com.amazonaws.ec2.doc.x20081201.CreateSnapshotDocument requestDocument) {
        LOG.debug(requestDocument);
        return (com.amazonaws.ec2.doc.x20081201.CreateSnapshotResponseDocument) callLatest(requestDocument);
    }

    @PayloadRoot(localPart = CREATE_SNAPSHOT, namespace = NAMESPACE_20090404)
    public CreateSnapshotResponseDocument createSnapshot(CreateSnapshotDocument requestDocument) {
        LOG.debug(requestDocument);
        try {
            String volumeId = requestDocument.getCreateSnapshot().getVolumeId();

            CreateSnapshotResponseDocument resultDocument = CreateSnapshotResponseDocument.Factory.newInstance();
            CreateSnapshotResponseType addNewCreateSnapshotResponse = resultDocument.addNewCreateSnapshotResponse();

            Snapshot snapshot = elasticBlockStorageService.createSnapshot(getUserId(), volumeId, null);

            addNewCreateSnapshotResponse.setVolumeId(snapshot.getVolumeId());
            addNewCreateSnapshotResponse.setProgress(String.valueOf(snapshot.getProgress()));
            if (snapshot.getStartTime() != 0L) {
                Calendar instance = Calendar.getInstance();
                instance.setTimeInMillis(snapshot.getStartTime());
                addNewCreateSnapshotResponse.setStartTime(instance);
            }
            addNewCreateSnapshotResponse.setSnapshotId(snapshot.getSnapshotId());
            addNewCreateSnapshotResponse.setStatus(getConversionUtils().getSnapshotStatusString(snapshot.getStatus()));
            addNewCreateSnapshotResponse.setRequestId(MDCHelper.getTransactionUID());
            LOG.debug(resultDocument);
            return (CreateSnapshotResponseDocument) sanitiseXml(resultDocument);
        } catch (Throwable t) {
            throw handleThrowable(t);
        }
    }
}
