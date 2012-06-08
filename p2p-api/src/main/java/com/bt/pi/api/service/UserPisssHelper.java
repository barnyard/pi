package com.bt.pi.api.service;

import java.util.Set;

import javax.annotation.Resource;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.stereotype.Component;

import com.bt.pi.app.common.id.PiIdBuilder;
import com.bt.pi.core.application.MessageContext;
import com.bt.pi.core.continuation.PiContinuation;
import com.bt.pi.core.entity.EntityMethod;
import com.bt.pi.core.entity.PiEntity;
import com.bt.pi.core.id.PId;
import com.bt.pi.sss.PisssApplicationManager;
import com.bt.pi.sss.entities.BucketCollectionEntity;

@Component
public class UserPisssHelper {

    private static final Log LOG = LogFactory.getLog(UserPisssHelper.class);

    @Resource
    private ApiApplicationManager apiApplicationManager;

    @Resource
    private PiIdBuilder piIdBuilder;

    public UserPisssHelper() {

    }

    public void deleteBucketsFromUser(String username, Set<String> bucketNames) {
        LOG.debug(String.format("deleteBucketsFromUser(%s,%s)", username, bucketNames));
        if (bucketNames == null || bucketNames.isEmpty())
            return;
        BucketCollectionEntity bucketCollectionEntity = new BucketCollectionEntity();
        bucketCollectionEntity.setOwner(username);
        bucketCollectionEntity.setBucketNames(bucketNames);
        routeMessageToPisss(bucketCollectionEntity);

    }

    protected void routeMessageToPisss(BucketCollectionEntity bucketCollectionEntity) {
        LOG.debug(String.format("routeMessageToPisss(%s)", bucketCollectionEntity));
        MessageContext messageContext = apiApplicationManager.getMessageContext();
        PId pisssIdForRegion = piIdBuilder.getNodeIdFromNodeId(apiApplicationManager.getNodeIdFull());
        messageContext.routePiMessageToApplication(pisssIdForRegion, EntityMethod.DELETE, bucketCollectionEntity, PisssApplicationManager.APPLICATION_NAME, new PiContinuation<PiEntity>() {

            @Override
            public void handleResult(PiEntity result) {
                LOG.debug("BucketCollectionEntity routed: " + result);
            }
        });
    }

}
