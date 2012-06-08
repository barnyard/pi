/* (c) British Telecommunications plc, 2009, All Rights Reserved */
package com.bt.pi.app.volumemanager.handlers;

import javax.annotation.Resource;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.stereotype.Component;

import com.bt.pi.app.common.entities.Volume;
import com.bt.pi.app.common.resource.PiTopics;
import com.bt.pi.core.application.PubSubMessageContext;
import com.bt.pi.core.entity.EntityMethod;

@Component
public class VolumeHandler extends AbstractHandler {
    private static final Log LOG = LogFactory.getLog(VolumeHandler.class);
    @Resource
    private CreateVolumeHandler createHandler;
    @Resource
    private DeleteVolumeHandler deleteHandler;
    @Resource
    private DetachVolumeHandler detachHandler;
    @Resource
    private AttachVolumeHandler attachHandler;

    public VolumeHandler() {
        this.createHandler = null;
        this.deleteHandler = null;
        this.detachHandler = null;
        this.attachHandler = null;
    }

    public boolean handleAnycast(PubSubMessageContext pubSubMessageContext, EntityMethod entityMethod, Volume volume, String nodeIdFull) {
        LOG.debug(String.format("handleAnycast(%s, %s, %s)", pubSubMessageContext, volume, nodeIdFull));
        // TODO: clean up these topics and have a check inside the pubsubMessage Context
        if (getKoalaIdFactory().buildPId(PiTopics.CREATE_VOLUME.getPiLocation()).forLocalScope(PiTopics.CREATE_VOLUME.getNodeScope()).equals(pubSubMessageContext.getTopicPId())) {
            return this.createHandler.createVolume(volume, nodeIdFull);
        }
        if (getKoalaIdFactory().buildPId(PiTopics.DELETE_VOLUME.getPiLocation()).forLocalScope(PiTopics.DELETE_VOLUME.getNodeScope()).equals(pubSubMessageContext.getTopicPId())) {
            this.deleteHandler.deleteVolume(volume, nodeIdFull);
            return true;
        }
        if (getKoalaIdFactory().buildPId(PiTopics.ATTACH_VOLUME.getPiLocation()).forLocalScope(PiTopics.ATTACH_VOLUME.getNodeScope()).equals(pubSubMessageContext.getTopicPId())) {
            this.attachHandler.attachVolume(volume, pubSubMessageContext, nodeIdFull);
            return true;
        }
        if (getKoalaIdFactory().buildPId(PiTopics.DETACH_VOLUME.getPiLocation()).forLocalScope(PiTopics.DETACH_VOLUME.getNodeScope()).equals(pubSubMessageContext.getTopicPId())) {
            this.detachHandler.detachVolume(volume, pubSubMessageContext, nodeIdFull);
            return true;
        }
        return false;
    }
}
