package com.bt.pi.integration.applications;

import java.net.UnknownHostException;

public class NodePhysicalHealthChecker extends com.bt.pi.core.application.health.NodePhysicalHealthChecker {
    private int port;

    @Override
    protected String getHostname() throws UnknownHostException {
        String result = super.getHostname() + port;
        // System.err.println("hostname: " + result);
        return result;
    }

    public void setPort(int port) {
        this.port = port;
    }
}
