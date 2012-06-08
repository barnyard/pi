package com.bt.pi.integration.util;

import java.io.File;
import java.io.IOException;

import com.bt.pi.app.common.entities.BlockDeviceMapping;
import com.bt.pi.app.instancemanager.handlers.VolumeBackupHandler;

public class StubVolumeBackupHandler extends VolumeBackupHandler {
    @Override
    protected File createTempFile(BlockDeviceMapping blockDevice) throws IOException {
        return new File("tmp/" + blockDevice.getVolumeId());
    }

    @Override
    protected boolean existsFile(String localVolumeFilename) {
        return true;
    }

    @Override
    public String getAbsoluteLocalVolumeFilename(String volumeId) {
        return super.getAbsoluteLocalVolumeFilename(volumeId);
    }
}
