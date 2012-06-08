package com.bt.pi.app.common.entities;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.bt.pi.core.entity.PiEntityBase;

public class InstanceTypes extends PiEntityBase {
    public static final String SCHEME = "instancetypes";
    public static final String URL_STRING = SCHEME + ":all";
    public static final String UNKNOWN = "UNKNOWN";
    private static final String TYPE = InstanceTypes.class.getSimpleName();

    private Map<String, InstanceTypeConfiguration> instanceTypes;

    public InstanceTypes() {
        instanceTypes = new ConcurrentHashMap<String, InstanceTypeConfiguration>();
    }

    public InstanceTypeConfiguration getInstanceTypeConfiguration(String instanceType) {
        if (null == instanceType)
            return null;

        return instanceTypes.get(instanceType);
    }

    public void addInstanceType(InstanceTypeConfiguration instanceTypeConfiguration) {
        instanceTypes.put(instanceTypeConfiguration.getInstanceType(), instanceTypeConfiguration);
    }

    @Override
    public String getType() {
        return InstanceTypes.TYPE;
    }

    @Override
    public String getUrl() {
        return InstanceTypes.URL_STRING;
    }

    public Map<String, InstanceTypeConfiguration> getInstanceTypes() {
        return instanceTypes;
    }

    public void setInstanceTypes(Map<String, InstanceTypeConfiguration> aInstanceTypes) {
        this.instanceTypes = aInstanceTypes;
    }

    @Override
    public String getUriScheme() {
        return SCHEME;
    }
}
