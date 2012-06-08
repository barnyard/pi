package com.bt.pi.app.common.entities;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.HashSet;
import java.util.Set;

import org.junit.Test;

import com.bt.pi.core.entity.Deletable;

public class ReservationTest {

    private String additionalInfo = "add info";
    private String availabilityZone = "zone";
    private BlockDeviceMapping blockDeviceMapping = new BlockDeviceMapping("vir name", "dev name");
    private Set<String> instanceIds = new HashSet<String>();
    private String instanceType = "small";
    private String keyName = "house keys";
    private int maxCount = 1;
    private int minCount = 1;
    private String reservationId = "r-123";
    private String securityGroupName = "sick group";

    @Test
    public void shouldConstruct() {
        // setup
        // act
        Reservation reservation = new Reservation();
        reservation.setAdditionalInfo(additionalInfo);
        reservation.setAvailabilityZone(availabilityZone);
        reservation.getBlockDeviceMappings().add(blockDeviceMapping);
        reservation.setInstanceIds(instanceIds);
        reservation.setInstanceType(instanceType);
        reservation.setKeyName(keyName);
        reservation.setMaxCount(maxCount);
        reservation.setMinCount(minCount);
        reservation.setReservationId(reservationId);
        reservation.setSecurityGroupName(securityGroupName);
        // assert
        assertEquals(additionalInfo, reservation.getAdditionalInfo());
        assertEquals(availabilityZone, reservation.getAvailabilityZone());
        assertEquals(instanceIds, reservation.getInstanceIds());
        assertEquals(keyName, reservation.getKeyName());
        assertEquals(maxCount, reservation.getMaxCount());
        assertEquals(minCount, reservation.getMinCount());
        assertEquals(reservationId, reservation.getReservationId());
        assertEquals(securityGroupName, reservation.getSecurityGroupName());
        assertEquals(blockDeviceMapping, reservation.getBlockDeviceMappings().get(0));
    }

    @Test
    public void shouldAddInstanceId() {
        // setup
        String anInstanceId = "i-DEADBEEF";
        Reservation reservation = new Reservation();

        // act
        reservation.addInstanceId(anInstanceId);

        // assert
        assertEquals(1, reservation.getInstanceIds().size());
        assertTrue(reservation.getInstanceIds().contains(anInstanceId));
    }

    @Test
    public void reservationShouldAlwaysBeDeletableAndDeleted() throws Exception {
        // setup
        Reservation reservation = new Reservation();
        reservation.setDeleted(false);

        // act
        boolean isDeleted = reservation.isDeleted();

        // assert
        assertTrue(reservation instanceof Deletable);
        assertTrue(isDeleted);
    }
}
