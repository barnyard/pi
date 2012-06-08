/* (c) British Telecommunications plc, 2009, All Rights Reserved */
package com.bt.pi.api.service;

import java.util.Collection;
import java.util.Map;
import java.util.Set;

import com.bt.pi.api.entities.ReservationInstances;
import com.bt.pi.app.common.entities.ConsoleOutput;
import com.bt.pi.app.common.entities.Instance;
import com.bt.pi.app.common.entities.Reservation;
import com.bt.pi.app.instancemanager.handlers.InstanceStateTransition;

/**
 * Interface for all instance related API calls - Describe Instances - Reboot Instances - Run Instances - Terminate
 * Instances - Get Console Output
 */
public interface InstancesService {

    boolean rebootInstances(String ownerId, Collection<String> instanceIds);

    // TODO: should we send each request itself, or send a group one and process a list with more information?
    Map<String, InstanceStateTransition> terminateInstances(String ownerId, Collection<String> instanceIds);

    Map<String, Set<Instance>> describeInstances(String ownerId, Collection<String> instanceIds);

    ReservationInstances runInstances(Reservation reservation);

    ConsoleOutput getConsoleOutput(String ownerId, String instanceId);
}
