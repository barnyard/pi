package com.bt.pi.app.common.testing;

import java.util.HashMap;
import java.util.Map;

import com.bt.pi.app.common.os.DeviceUtils;

public class StubDeviceUtils extends DeviceUtils {
    private boolean deviceAlwaysExists = false;
    private Map<String, Boolean> results = new HashMap<String, Boolean>();

    @Override
    public boolean deviceExists(String devName) {
        if (deviceAlwaysExists)
            return true;

        return super.deviceExists(devName);
    }

    public void addAddress(String address, boolean result) {
        results.put(address, result);
    }

    public void clear() {
        results.clear();
    }

    @Override
    public boolean deviceHasAddress(String devName, String address) {
        if (results.containsKey(address))
            return results.get(address);
        if (deviceAlwaysExists)
            return true;

        return super.deviceHasAddress(devName, address);
    }

    public void setDeviceAlwaysExists(boolean deviceAlwaysExists) {
        this.deviceAlwaysExists = deviceAlwaysExists;
    }
}