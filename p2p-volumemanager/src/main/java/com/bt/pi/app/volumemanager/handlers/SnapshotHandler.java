/* (c) British Telecommunications plc, 2009, All Rights Reserved */
package com.bt.pi.app.volumemanager.handlers;

import javax.annotation.Resource;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.stereotype.Component;

import com.bt.pi.app.common.entities.Snapshot;
import com.bt.pi.app.common.resource.PiTopics;
import com.bt.pi.core.application.PubSubMessageContext;
import com.bt.pi.core.entity.EntityMethod;

@Component
public class SnapshotHandler extends AbstractHandler {
    private static final Log LOG = LogFactory.getLog(SnapshotHandler.class);
    @Resource
    private CreateSnapshotHandler createSnapshotHandler;
    @Resource
    private DeleteSnapshotHandler deleteSnapshotHandler;

    public SnapshotHandler() {
        this.createSnapshotHandler = null;
        this.deleteSnapshotHandler = null;
    }

    public boolean handleAnycast(PubSubMessageContext pubSubMessageContext, EntityMethod entityMethod, Snapshot snapshot, String nodeIdFull) {
        LOG.debug(String.format("handleAnycast(%s, %s, %s, %s)", pubSubMessageContext, entityMethod, snapshot, nodeIdFull));
        if (getKoalaIdFactory().buildPId(PiTopics.CREATE_SNAPSHOT.getPiLocation()).forLocalScope(PiTopics.CREATE_SNAPSHOT.getNodeScope()).equals(pubSubMessageContext.getTopicPId())) {
            LOG.debug(String.format("Creating snapshot with id %s", snapshot.getSnapshotId()));
            return this.createSnapshotHandler.createSnapshot(snapshot, pubSubMessageContext, nodeIdFull);

        }
        if (getKoalaIdFactory().buildPId(PiTopics.DELETE_SNAPSHOT.getPiLocation()).forLocalScope(PiTopics.DELETE_SNAPSHOT.getNodeScope()).equals(pubSubMessageContext.getTopicPId())) {
            LOG.debug(String.format("Deleting snapshot with id %s", snapshot.getSnapshotId()));
            this.deleteSnapshotHandler.deleteSnapshot(snapshot, nodeIdFull);
            return true;
        }
        return false;
    }
}
