package com.bt.pi.app.management;

import javax.annotation.Resource;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.jmx.export.annotation.ManagedOperation;
import org.springframework.jmx.export.annotation.ManagedOperationParameter;
import org.springframework.jmx.export.annotation.ManagedOperationParameters;
import org.springframework.jmx.export.annotation.ManagedResource;
import org.springframework.stereotype.Component;

import com.bt.pi.app.common.id.PiIdLookupService;
import com.bt.pi.core.dht.BlockingDhtReader;
import com.bt.pi.core.dht.DhtClientFactory;
import com.bt.pi.core.entity.PiEntity;
import com.bt.pi.core.id.KoalaIdFactory;
import com.bt.pi.core.id.PId;
import com.bt.pi.core.parser.KoalaJsonParser;

@Component
@ManagedResource(description = "Helper service for automating pi entity lookups", objectName = "bean:name=piEntityLookupService")
public class PiEntityLookupService {
    private static final Log LOG = LogFactory.getLog(PiEntityLookupService.class);
    private static final String ENTITY_NAME = "entityName";
    private static final String ARG0 = "arg0";
    private static final String ARG_DESCRIPTION = "Url of entity to look up";
    private static final String DESCRIPTION = "PiIdBuilder method name.";
    private PiIdLookupService piIdLookupService;
    private DhtClientFactory dhtClientFactory;
    private KoalaJsonParser koalaJsonParser;
    private KoalaIdFactory koalaIdFactory;

    public PiEntityLookupService() {
        piIdLookupService = null;
        dhtClientFactory = null;
        koalaJsonParser = null;
        koalaIdFactory = null;
    }

    @Resource
    public void setPiIdLookupService(PiIdLookupService aPiIdLookupService) {
        this.piIdLookupService = aPiIdLookupService;
    }

    @Resource
    public void setDhtClientFactory(DhtClientFactory aDhtClientFactory) {
        this.dhtClientFactory = aDhtClientFactory;
    }

    @Resource
    public void setKoalaJsonParser(KoalaJsonParser aKoalaJsonParser) {
        this.koalaJsonParser = aKoalaJsonParser;
    }

    @Resource
    public void setKoalaIdFactory(KoalaIdFactory akoalaIdFactory) {
        this.koalaIdFactory = akoalaIdFactory;
    }

    @ManagedOperation(description = "Look up a named pi entity")
    @ManagedOperationParameters( { @ManagedOperationParameter(name = ENTITY_NAME, description = "Description of the entity to look up") })
    public String lookup0(String entityName) {
        return lookup1(entityName, null);
    }

    @ManagedOperation(description = "Look up a named pi entity with a single argument")
    @ManagedOperationParameters( { @ManagedOperationParameter(name = ENTITY_NAME, description = DESCRIPTION), @ManagedOperationParameter(name = ARG0, description = ARG_DESCRIPTION) })
    public String lookup1(String entityName, String arg0) {
        return lookup2(entityName, arg0, null);
    }

    @ManagedOperation(description = "Look up a named pi entity with a url and a scope argument")
    @ManagedOperationParameters( { @ManagedOperationParameter(name = ENTITY_NAME, description = DESCRIPTION), @ManagedOperationParameter(name = ARG0, description = ARG_DESCRIPTION),
            @ManagedOperationParameter(name = "Scope", description = "The local scope of the entity to look up") })
    public String lookup2(String entityName, String arg0, String scope) {
        LOG.debug(String.format("lookup2(%s, %s, %s)", entityName, arg0, scope));
        String result;
        try {
            String idString = piIdLookupService.invokeMethod(entityName, arg0, scope);
            result = doLookup(idString);
        } catch (Throwable t) {
            result = String.format("{\"Error\":\"Error looking up id: %s \"}", t.getClass());
        }
        return result;
    }

    private String doLookup(String idString) {
        PId id = koalaIdFactory.buildPIdFromHexString(idString);
        BlockingDhtReader reader = dhtClientFactory.createBlockingReader();
        PiEntity res = reader.get(id);
        return koalaJsonParser.getJson(res);
    }
}
