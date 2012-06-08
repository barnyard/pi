/* (c) British Telecommunications plc, 2009, All Rights Reserved */
package com.bt.pi.api.service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.stereotype.Component;

import com.bt.pi.app.common.entities.Instance;
import com.bt.pi.app.common.entities.InstanceState;
import com.bt.pi.app.common.entities.User;
import com.bt.pi.core.conf.Property;
import com.bt.pi.core.continuation.PiContinuation;
import com.bt.pi.core.id.PId;

@Component
public class DescribeInstancesServiceHelper extends ServiceHelperBase {
    private static final Log LOG = LogFactory.getLog(DescribeInstancesServiceHelper.class);
    private static final int SIX_HOURS = 6 * 60 * 60 * 1000;
    private int hideAfterIfTerminatedMillis = SIX_HOURS;

    public DescribeInstancesServiceHelper() {
        super();
    }

    @Property(key = "ec2din.hide.after.if.terminated.millis", defaultValue = "" + SIX_HOURS)
    public void setHideAfterIfTerminatedMillis(int value) {
        this.hideAfterIfTerminatedMillis = value;
    }

    public ConcurrentMap<String, Set<Instance>> getInstances(Collection<String> instanceIds) {
        LOG.debug(String.format("getInstances(%s)", instanceIds));
        final ConcurrentMap<String, Set<Instance>> mapOfReservationInstances = new ConcurrentHashMap<String, Set<Instance>>();

        final List<PId> ids = getPiIdsFromInstanceIds(instanceIds);

        scatterGather(ids, new PiContinuation<Instance>() {
            @Override
            public void handleResult(Instance instance) {
                LOG.debug(String.format("handleResult(%s)", instance));
                if (null == instance)
                    return;
                if (instance.getState().ordinal() > InstanceState.SHUTTING_DOWN.ordinal() && instance.getStateChangeTimestamp() > 0)
                    if ((instance.getStateChangeTimestamp() + hideAfterIfTerminatedMillis) < System.currentTimeMillis())
                        return;
                mapOfReservationInstances.putIfAbsent(instance.getReservationId(), new HashSet<Instance>());
                mapOfReservationInstances.get(instance.getReservationId()).add(instance);
            }
        });
        return mapOfReservationInstances;
    }

    public Collection<String> getInstanceIdsForUser(Collection<String> instanceIds, PId userId) {
        LOG.debug(String.format("getInstanceIdsForUser(%s, %s)", instanceIds, userId));

        Collection<String> instIds = new ArrayList<String>();
        User user = (User) getBlockingDhtCache().get(userId);
        for (String instanceId : user.getInstanceIds()) {
            if (instanceIds.size() == 0 || instanceIds.contains(instanceId))
                instIds.add(instanceId);
        }
        for (String instanceId : user.getTerminatedInstanceIds()) {
            if (instanceIds.size() == 0 || instanceIds.contains(instanceId))
                instIds.add(instanceId);
        }
        return instIds;
    }

    private List<PId> getPiIdsFromInstanceIds(Collection<String> instanceIds) {
        LOG.debug(String.format("getPiIdsFromInstanceIds(%s)", instanceIds));

        final List<PId> ids = new ArrayList<PId>();
        for (final String instanceId : instanceIds) {
            ids.add(getPiIdBuilder().getPIdForEc2AvailabilityZone(Instance.getUrl(instanceId)));
        }
        return ids;
    }
}
