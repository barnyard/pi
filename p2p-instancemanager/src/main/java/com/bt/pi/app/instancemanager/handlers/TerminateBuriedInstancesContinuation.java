/* (c) British Telecommunications plc, 2009, All Rights Reserved */
package com.bt.pi.app.instancemanager.handlers;

import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.bt.pi.app.common.entities.Instance;
import com.bt.pi.app.common.id.PiIdBuilder;
import com.bt.pi.core.application.MessageContextFactory;

public class TerminateBuriedInstancesContinuation extends TerminateInstancesContinuation {
    private static final Log LOG = LogFactory.getLog(TerminateBuriedInstancesContinuation.class);

    public TerminateBuriedInstancesContinuation(PiIdBuilder aPiIdBuilder, MessageContextFactory aMessageContextFactory, Map<String, InstanceStateTransition> anInstanceStatusMap) {
        super(aPiIdBuilder, aMessageContextFactory, anInstanceStatusMap);
    }

    @Override
    public Instance update(Instance existingEntity, Instance requestedEntity) {
        LOG.debug(String.format("update(Existing Instance - %s,Requested Instance - %s)", existingEntity, requestedEntity));

        if (null == existingEntity)
            return null;

        if (!existingEntity.isBuried()) {
            LOG.debug(String.format("Not terminating instance %s as it is not buried", existingEntity));
            return null;
        }

        return super.update(existingEntity, requestedEntity);
    }
}
