/* (c) British Telecommunications plc, 2009, All Rights Reserved */
package com.bt.pi.api.service;

import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentMap;

import javax.annotation.Resource;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.stereotype.Component;

import com.bt.pi.api.entities.ReservationInstances;
import com.bt.pi.app.common.entities.ConsoleOutput;
import com.bt.pi.app.common.entities.Instance;
import com.bt.pi.app.common.entities.Reservation;
import com.bt.pi.app.common.entities.User;
import com.bt.pi.app.instancemanager.handlers.InstanceStateTransition;
import com.bt.pi.app.instancemanager.handlers.TerminateInstanceServiceHelper;
import com.bt.pi.core.id.PId;

@Component
public class InstancesServiceImpl extends ServiceBaseImpl implements InstancesService {
    private static final Log LOG = LogFactory.getLog(InstancesServiceImpl.class);
    @Resource
    private DescribeInstancesServiceHelper describeInstancesServiceHelper;
    @Resource
    private RebootInstanceServiceHelper rebootInstanceServiceHelper;
    @Resource
    private GetConsoleOutputInstanceServiceHelper getConsoleOutputInstanceServiceHelper;
    @Resource
    private RunInstancesServiceHelper runInstancesServiceHelper;
    @Resource
    private TerminateInstanceServiceHelper terminateInstanceServiceHelper;

    public InstancesServiceImpl() {
        super();
        describeInstancesServiceHelper = null;
        rebootInstanceServiceHelper = null;
        getConsoleOutputInstanceServiceHelper = null;
        runInstancesServiceHelper = null;
        terminateInstanceServiceHelper = null;
    }

    public Map<String, Set<Instance>> describeInstances(String ownerId, Collection<String> instanceIds) {
        LOG.debug(String.format("describeInstances(%s, %s)", ownerId, instanceIds));

        PId userId = getPiIdBuilder().getPId(User.getUrl(ownerId));
        Collection<String> instIds = describeInstancesServiceHelper.getInstanceIdsForUser(instanceIds, userId);

        ConcurrentMap<String, Set<Instance>> mapOfReservationInstances = describeInstancesServiceHelper.getInstances(instIds);

        return mapOfReservationInstances;
    }

    public ConsoleOutput getConsoleOutput(String ownerId, String instanceId) {
        return getConsoleOutputInstanceServiceHelper.getConsoleOutput(ownerId, instanceId, getApiApplicationManager().newMessageContext());
    }

    public boolean rebootInstances(String ownerId, Collection<String> instanceIds) {
        return rebootInstanceServiceHelper.rebootInstances(ownerId, instanceIds, getApiApplicationManager());
    }

    public ReservationInstances runInstances(final Reservation reservation) {
        LOG.debug(String.format("runInstances(%s)", reservation));
        return this.runInstancesServiceHelper.runInstances(reservation);
    }

    public Map<String, InstanceStateTransition> terminateInstances(final String ownerId, final Collection<String> instanceIds) {
        LOG.debug(String.format("terminateInstances(%s, %s)", ownerId, instanceIds));
        return terminateInstanceServiceHelper.terminateInstance(ownerId, instanceIds);
    }
}
