/* (c) British Telecommunications plc, 2009, All Rights Reserved */
package com.bt.pi.app.common;

import javax.annotation.PreDestroy;
import javax.annotation.Resource;

import com.bt.pi.app.common.net.iptables.ManagedAddressingApplicationIpTablesManager;

public abstract class AbstractPublicManagedAddressingPiApplication extends AbstractManagedAddressingPiApplication {
    private ManagedAddressingApplicationIpTablesManager ipTablesManager;

    public AbstractPublicManagedAddressingPiApplication(String name) {
        super(name);
        ipTablesManager = null;
    }

    @Resource
    public void setManagedAddressingApplicationIpTablesManager(ManagedAddressingApplicationIpTablesManager anIpTablesManager) {
        ipTablesManager = anIpTablesManager;
    }

    @Override
    public boolean becomeActive() {
        boolean res = super.becomeActive();

        if (res)
            ipTablesManager.enablePiAppChainForApplication(getApplicationName(), getPublicIpAddressForApplicationFromActivationRecord(), getPort());

        return res;
    }

    @Override
    @PreDestroy
    public void becomePassive() {
        super.becomePassive();
        ipTablesManager.disablePiAppChainForApplication(getApplicationName());
    }

    protected abstract int getPort();
}
