package com.bt.pi.app.common.entities;

public enum MachineType {
    MACHINE("MACHINE", "pmi"), KERNEL("KERNEL", "pki"), RAMDISK("RAMDISK", "pri");

    private String machineType;
    private String imagePrefix;

    private MachineType(String type, String prefix) {
        machineType = type;
        imagePrefix = prefix;
    }

    public String getMachineType() {
        return machineType;
    }

    public String getImagePrefix() {
        return imagePrefix;
    }
}
