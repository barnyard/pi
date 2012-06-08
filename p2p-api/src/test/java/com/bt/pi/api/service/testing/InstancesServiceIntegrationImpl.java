package com.bt.pi.api.service.testing;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.bt.pi.api.entities.ReservationInstances;
import com.bt.pi.api.service.InstancesService;
import com.bt.pi.api.service.ServiceBaseImpl;
import com.bt.pi.app.common.entities.ConsoleOutput;
import com.bt.pi.app.common.entities.Instance;
import com.bt.pi.app.common.entities.InstanceState;
import com.bt.pi.app.common.entities.Reservation;
import com.bt.pi.app.common.images.platform.ImagePlatform;
import com.bt.pi.app.instancemanager.handlers.InstanceStateTransition;

public class InstancesServiceIntegrationImpl extends ServiceBaseImpl implements InstancesService {
    private static final Log LOG = LogFactory.getLog(InstancesServiceIntegrationImpl.class);
    private static final String INSTANCE_ID = "i-123";
    private static final String DNS = "dns-";

    public boolean rebootInstances(String ownerId, Collection<String> instanceIds) {
        if (instanceIds.size() > 0 && ((List<String>) instanceIds).get(0).equals(INSTANCE_ID))
            return true;
        return false;
    }

    public Map<String, InstanceStateTransition> terminateInstances(String string, Collection<String> instanceIds) {
        LOG.debug(String.format("terminateInstances(%s, %s", string, instanceIds));
        Map<String, InstanceStateTransition> instanceMap = new HashMap<String, InstanceStateTransition>();
        for (String instanceId : instanceIds) {
            instanceMap.put(instanceId, new InstanceStateTransition(InstanceState.RUNNING, InstanceState.SHUTTING_DOWN));
        }
        return instanceMap;
    }

    public Map<String, Set<Instance>> describeInstances(String string, Collection<String> instanceIds) {
        Set<Instance> setOfInstances = new HashSet<Instance>();
        if (instanceIds.size() == 0) {
            addInstance(setOfInstances, INSTANCE_ID);
        } else {
            for (String instanceId : instanceIds) {
                addInstance(setOfInstances, instanceId);
            }
        }

        Map<String, Set<Instance>> mapOfInstances = new HashMap<String, Set<Instance>>();
        mapOfInstances.put("r-123", setOfInstances);
        return mapOfInstances;
    }

    private void addInstance(Set<Instance> setOfInstances, String id) {
        Instance instance = new Instance(id, "userid", "default");
        instance.setPublicIpAddress(String.format("dns-%s.com", id));
        instance.setImageId(String.format("emi-%s", id));
        instance.setKernelId(String.format("kernel-%s", id));
        instance.setKeyName(String.format("key-%s", id));
        instance.setAvailabilityZone(String.format("zone-%s", id));
        instance.setPlatform(ImagePlatform.linux);
        instance.setReservationId(String.format("r-%s", id));
        instance.setSecurityGroupName(String.format("g-%s", id));
        instance.setUserId(String.format("owner-%s", id));
        instance.setInstanceType(String.format("type-%s", id));
        instance.setLaunchIndex(Integer.parseInt(id.split("-")[1]));
        instance.setPrivateIpAddress(String.format("private.dns-%s.com", id));
        instance.setRamdiskId(String.format("ramdisk-%s", id));
        instance.setState(InstanceState.RUNNING);
        if ("i-666".equals(id))
            instance.setState(InstanceState.CRASHED);
        instance.setReasonForLastStateTransition(String.format("reason-%s", id));
        instance.setMonitoring(true);
        instance.setLaunchTime(1256834063405l);
        setOfInstances.add(instance);
    }

    public ReservationInstances runInstances(Reservation reservation) {

        ReservationInstances reservationInstances = new ReservationInstances();
        Instance instance = new Instance();

        String imageId = reservation.getImageId();
        String index = imageId.split("-")[1];

        reservation.setReservationId("r-" + index);
        reservation.setUserId("owner-" + index);
        reservation.setSecurityGroupName("default-" + index);

        instance.setSecurityGroupName("default-" + index);
        instance.setReservationId("r-" + index);
        instance.setUserId("owner-" + index);
        instance.setInstanceId("i-" + index);

        instance.setLaunchTime(System.currentTimeMillis());
        instance.setAvailabilityZone("zone-" + index);

        instance.setRamdiskId("ramdisk-" + index);

        instance.setLaunchIndex(1);
        instance.setPublicIpAddress(DNS + index + ".com");
        instance.setImageId(imageId);
        instance.setInstanceType("large-" + index);
        instance.setKernelId("kernel-" + index);
        instance.setKeyName("key-" + index);
        instance.setPlatform(ImagePlatform.windows);
        instance.setPrivateIpAddress(DNS + index + ".private.com");
        instance.setState(InstanceState.PENDING);

        // addNewItem.addNewProductCodes().addNewItem().setProductCode("product-" + index);
        // addNewItem.addNewMonitoring().setState("monitoring-state-" + index);

        instance.setMonitoring(true);
        reservationInstances.getInstances().add(instance);
        reservationInstances.setReservation(reservation);
        return reservationInstances;

    }

    public ConsoleOutput getConsoleOutput(String ownerId, String instanceId) {
        return new ConsoleOutput("Did you really mean to do rm -rf?", 1864445563437L, "bob", ImagePlatform.windows);
    }
}
