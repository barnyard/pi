package com.bt.pi.app.imagemanager.reporting;

import com.bt.pi.core.entity.PiEntityCollection;

public class ImageReportEntityCollection extends PiEntityCollection<ImageReportEntity> {

    public ImageReportEntityCollection() {
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
