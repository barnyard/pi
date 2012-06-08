package com.bt.pi.app.instancemanager.reporting;

import com.bt.pi.core.entity.PiEntityCollection;

public class ZombieInstanceReportEntityCollection extends PiEntityCollection<InstanceReportEntity> {

    public ZombieInstanceReportEntityCollection() {
    }

    @Override
    public String getType() {
        return getClass().getSimpleName();
    }

    @Override
    public String getUriScheme() {
        return getClass().getSimpleName();
    }
}
