package com.bt.pi.app.common.entities;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import com.bt.pi.app.common.entities.MachineType;

public class MachineTypeTest {

    @Test
    public void testGetters() {
        assertEquals("KERNEL", MachineType.KERNEL.toString());
        assertEquals("RAMDISK", MachineType.RAMDISK.toString());
        assertEquals("MACHINE", MachineType.MACHINE.toString());
        assertEquals("pki", MachineType.KERNEL.getImagePrefix());
        assertEquals("pmi", MachineType.MACHINE.getImagePrefix());
        assertEquals("pri", MachineType.RAMDISK.getImagePrefix());
    }
}
