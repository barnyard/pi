/* (c) British Telecommunications plc, 2009, All Rights Reserved */
package com.bt.pi.api.handlers;

import javax.annotation.Resource;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.ws.server.endpoint.annotation.Endpoint;
import org.springframework.ws.server.endpoint.annotation.PayloadRoot;

import com.amazonaws.ec2.doc.x20090404.DeleteSnapshotDocument;
import com.amazonaws.ec2.doc.x20090404.DeleteSnapshotResponseDocument;
import com.amazonaws.ec2.doc.x20090404.DeleteSnapshotResponseType;
import com.bt.pi.api.service.ElasticBlockStorageService;
import com.bt.pi.core.util.MDCHelper;

@Endpoint("API.DeleteSnapshotHandler")
public class DeleteSnapshotHandler extends HandlerBase {

    private static final Log LOG = LogFactory.getLog(DeleteSnapshotHandler.class);
    private static final String DELETE_SNAPSHOT = "DeleteSnapshot";
    private ElasticBlockStorageService elasticBlockStorageService;

    public DeleteSnapshotHandler() {
        elasticBlockStorageService = null;
    }

    @Resource
    public void setElasticBlockStorageService(ElasticBlockStorageService anElasticBlockStorageService) {
        elasticBlockStorageService = anElasticBlockStorageService;
    }

    @PayloadRoot(localPart = DELETE_SNAPSHOT, namespace = NAMESPACE_20081201)
    public com.amazonaws.ec2.doc.x20081201.DeleteSnapshotResponseDocument deleteSnapshot(com.amazonaws.ec2.doc.x20081201.DeleteSnapshotDocument requestDocument) {
        return (com.amazonaws.ec2.doc.x20081201.DeleteSnapshotResponseDocument) callLatest(requestDocument);
    }

    @PayloadRoot(localPart = DELETE_SNAPSHOT, namespace = NAMESPACE_20090404)
    public DeleteSnapshotResponseDocument deleteSnapshot(DeleteSnapshotDocument requestDocument) {
        LOG.debug(String.format("deleteSnapshot(%s)", requestDocument));
        try {
            String snapshotId = requestDocument.getDeleteSnapshot().getSnapshotId();
            boolean result = elasticBlockStorageService.deleteSnapshot(getUserId(), snapshotId);

            DeleteSnapshotResponseDocument responseDocument = DeleteSnapshotResponseDocument.Factory.newInstance();
            DeleteSnapshotResponseType addNewDeleteSnapshotResponse = responseDocument.addNewDeleteSnapshotResponse();
            addNewDeleteSnapshotResponse.setReturn(result);
            addNewDeleteSnapshotResponse.setRequestId(MDCHelper.getTransactionUID());
            LOG.debug(responseDocument);
            return (DeleteSnapshotResponseDocument) sanitiseXml(responseDocument);
        } catch (Throwable t) {
            throw handleThrowable(t);
        }
    }
}
