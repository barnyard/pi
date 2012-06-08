package com.bt.pi.app.networkmanager;

import com.bt.pi.app.common.entities.watchers.securitygroup.SecurityGroupResourceWatchingStrategy;

public class TestSecurityGroupResourceWatchingStrategy extends SecurityGroupResourceWatchingStrategy {
    @Override
    public long getInitialResourceRefreshIntervalMillis() {
        String property = System.getProperty("securityGroupRefreshRunnerInitialIntervalMillisOverride", null);
        if (property == null)
            return super.getInitialResourceRefreshIntervalMillis();
        else
            return Long.parseLong(property);
    }

    @Override
    public long getInitialConsumerWatcherIntervalMillis() {
        String property = System.getProperty("securityGroupConsumerWatcherInitialIntervalMillisOverride", null);
        if (property == null)
            return super.getInitialConsumerWatcherIntervalMillis();
        else
            return Long.parseLong(property);
    }
}
