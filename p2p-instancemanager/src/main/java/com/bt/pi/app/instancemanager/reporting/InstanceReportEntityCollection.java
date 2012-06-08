package com.bt.pi.app.instancemanager.reporting;

import com.bt.pi.core.entity.PiEntityCollection;

public class InstanceReportEntityCollection extends PiEntityCollection<InstanceReportEntity> {
    public InstanceReportEntityCollection() {
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
