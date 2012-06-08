package com.bt.pi.app.management;

import javax.annotation.Resource;

import com.bt.pi.app.common.id.PiIdBuilder;
import com.bt.pi.core.dht.DhtClientFactory;
import com.bt.pi.core.id.KoalaIdFactory;

public abstract class SeederBase {
    protected static final char[] HEX_ID_SET = new char[] { '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F' };
    protected static final String DASH = "-";
    protected static final long DEFAULT_INACTIVE_RESOURCE_CONSUMER_TIMEOUT_SEC = 24 * 3600;
    protected static final long DEFAULT_INACTIVE_RESOURCE_CONSUMER_FOR_PUBLIC_IP_ALLOCATION_INDEX_TIMEOUT_SEC = -1L;
    protected static final String NOT = "NOT";
    protected static final String SEMICOLON = ";";
    protected static final String SLASH = "/";

    private DhtClientFactory dhtClientFactory;
    private PiIdBuilder piIdBuilder;
    private KoalaIdFactory koalaIdFactory;

    public SeederBase() {
        dhtClientFactory = null;
        piIdBuilder = null;
        koalaIdFactory = null;
    }

    protected DhtClientFactory getDhtClientFactory() {
        return dhtClientFactory;
    }

    protected PiIdBuilder getPiIdBuilder() {
        return piIdBuilder;
    }

    protected KoalaIdFactory getKoalaIdFactory() {
        return koalaIdFactory;
    }

    @Resource
    public void setPiIdBuilder(PiIdBuilder aPiIdBuilder) {
        this.piIdBuilder = aPiIdBuilder;
    }

    @Resource
    public void setDhtClientFactory(DhtClientFactory aDhtClientFactory) {
        this.dhtClientFactory = aDhtClientFactory;
    }

    @Resource
    public void setKoalaIdFactory(KoalaIdFactory aKoalaIdFactory) {
        this.koalaIdFactory = aKoalaIdFactory;
    }
}
