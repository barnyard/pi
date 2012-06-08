package com.bt.pi.app.common.entities;

import com.bt.pi.core.application.activation.AvailabilityZoneScopedApplicationRecord;
import com.bt.pi.core.application.activation.GlobalScopedApplicationRecord;
import com.bt.pi.core.application.activation.RegionScopedApplicationRecord;

public enum ResourceSchemes {
    BUCKET_META_DATA("bucketMetaData"),
    IMAGE_INDEX(ResourceSchemes.IDX_STR),
    PUBLIC_IP_ADDRESS_INDEX(ResourceSchemes.IDX_STR),
    VLAN_ALLOCATION_INDEX(ResourceSchemes.IDX_STR),
    SUBNET_ALLOCATION_INDEX(ResourceSchemes.IDX_STR),
    SECURITY_GROUP("sg"),
    INSTANCE("inst"),
    USER("user"),
    USER_ACCESS_KEY("uak"),
    TOPIC("topic"),
    VOLUME("vol"),
    QUEUE("queue"),
    AVAILABILITY_ZONES("avz"),
    REGIONS("rgn"),
    PI_CERT("pcrt"),
    ADDRESS("addr"),
    VIRTUAL_NETWORK("vlan"),
    IMG("img"),
    AVZ_APP(AvailabilityZoneScopedApplicationRecord.URI_SCHEME),
    REGION_APP(RegionScopedApplicationRecord.URI_SCHEME),
    GLOBAL_APP(GlobalScopedApplicationRecord.URI_SCHEME);

    private static final String IDX_STR = "idx";
    private String name;

    private ResourceSchemes(String aName) {
        this.name = aName;
    }

    public String toString() {
        return this.name;
    }
}
