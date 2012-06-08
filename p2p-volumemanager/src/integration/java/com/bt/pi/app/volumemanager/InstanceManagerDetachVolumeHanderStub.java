package com.bt.pi.app.volumemanager;

import com.bt.pi.app.instancemanager.handlers.DetachVolumeHandler;

public class InstanceManagerDetachVolumeHanderStub extends DetachVolumeHandler {
    @Override
    protected boolean fileExists(String path) {
        return true;
    }
}
