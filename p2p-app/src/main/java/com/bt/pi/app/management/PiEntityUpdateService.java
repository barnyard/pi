package com.bt.pi.app.management;

import javax.annotation.Resource;
import javax.management.JMException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.jmx.export.annotation.ManagedOperation;
import org.springframework.jmx.export.annotation.ManagedOperationParameter;
import org.springframework.jmx.export.annotation.ManagedOperationParameters;
import org.springframework.jmx.export.annotation.ManagedResource;
import org.springframework.stereotype.Component;

import com.bt.pi.core.continuation.UpdateResolver;
import com.bt.pi.core.dht.BlockingDhtWriter;
import com.bt.pi.core.dht.DhtClientFactory;
import com.bt.pi.core.entity.PiEntity;
import com.bt.pi.core.id.KoalaIdFactory;
import com.bt.pi.core.id.PId;
import com.bt.pi.core.parser.KoalaJsonParser;

@Component
@ManagedResource(description = "Helper service for automating pi entity updates", objectName = "bean:name=piEntityUpdateService")
public class PiEntityUpdateService {
    private static final Log LOG = LogFactory.getLog(PiEntityUpdateService.class);

    private DhtClientFactory dhtClientFactory;
    private KoalaJsonParser koalaJsonParser;
    private KoalaIdFactory koalaIdFactory;

    public PiEntityUpdateService() {
        dhtClientFactory = null;
        koalaJsonParser = null;
        koalaIdFactory = null;
    }

    @Resource
    public void setKoalaJsonParser(KoalaJsonParser aKoalaJsonParser) {
        this.koalaJsonParser = aKoalaJsonParser;
    }

    @Resource
    public void setDhtClientFactory(DhtClientFactory aDhtClientFactory) {
        this.dhtClientFactory = aDhtClientFactory;
    }

    @Resource
    public void setKoalaIdFactory(KoalaIdFactory akoalaIdFactory) {
        this.koalaIdFactory = akoalaIdFactory;
    }

    @SuppressWarnings("unchecked")
    @ManagedOperation(description = "Look up a named pi entity with a single argument")
    @ManagedOperationParameters( { @ManagedOperationParameter(name = "idString", description = "Id of record to update"), @ManagedOperationParameter(name = "entityType", description = "Entity type"),
            @ManagedOperationParameter(name = "json", description = "Json content") })
    public boolean update(String idString, String entityType, String json) throws JMException {
        LOG.debug(String.format("update(%s, %s, %s)", idString, entityType, json));
        PId id = koalaIdFactory.buildPIdFromHexString(idString);

        Class<? extends PiEntity> type;
        try {
            type = (Class<? extends PiEntity>) Class.forName(entityType);
        } catch (ClassNotFoundException e) {
            String message = String.format("Unable to construct class of type %s", entityType);
            LOG.error(message, e);
            throw new JMException(message);
        }
        final PiEntity object = (PiEntity) koalaJsonParser.getObject(json, type);

        BlockingDhtWriter writer = dhtClientFactory.createBlockingWriter();
        writer.update(id, null, new UpdateResolver<PiEntity>() {
            @Override
            public PiEntity update(PiEntity existingEntity, PiEntity requestedEntity) {
                return object;
            }
        });
        return writer.getValueWritten() != null;
    }
}
