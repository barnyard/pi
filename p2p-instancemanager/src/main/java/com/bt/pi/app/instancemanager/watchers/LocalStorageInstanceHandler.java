package com.bt.pi.app.instancemanager.watchers;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import rice.p2p.commonapi.Id;

import com.bt.pi.app.common.entities.Instance;
import com.bt.pi.app.common.entities.InstanceState;
import com.bt.pi.app.instancemanager.reporting.InstanceReportEntity;
import com.bt.pi.app.instancemanager.reporting.ZombieInstanceReportEntityCollection;
import com.bt.pi.core.application.storage.LocalStorageScanningHandlerBase;
import com.bt.pi.core.continuation.UpdateResolvingPiContinuation;
import com.bt.pi.core.past.content.KoalaGCPastMetadata;

@Component
public class LocalStorageInstanceHandler extends LocalStorageScanningHandlerBase {
    private static final Log LOG = LogFactory.getLog(LocalStorageInstanceHandler.class);
    private static final long DISPATCH_DELAY_MILLIS = 1000 * 60 * 5; // five mins
    private static final String INSTANCE_ENTITY_TYPE = new Instance().getType();

    private List<InstanceReportEntity> zombieInstanceReportEntities;

    public LocalStorageInstanceHandler() {
        this.zombieInstanceReportEntities = new ArrayList<InstanceReportEntity>();
    }

    // for unit testing
    List<InstanceReportEntity> getZombieInstanceReportEntities() {
        return zombieInstanceReportEntities;
    }

    @Override
    public void doHandle(Id id, KoalaGCPastMetadata metadata) {
        LOG.debug(String.format("doHandle(%s, %s)", id.toStringFull(), metadata));

        getDhtClientFactory().createWriter().update(getKoalaIdFactory().convertToPId(id), new UpdateResolvingPiContinuation<Instance>() {
            @Override
            public Instance update(Instance existingEntity, Instance requestedEntity) {
                if (null == existingEntity)
                    return null;
                checkForZombieInstance(existingEntity);
                // force an update to make the metadata get refreshed so that the instance will be garbage collected
                if (existingEntity.isDeleted())
                    return existingEntity;
                return null;
            }

            @Override
            public void handleResult(Instance result) {
                if (null != result)
                    LOG.debug(String.format("instance %s updated to force dht garbage collection", result.getInstanceId()));
            }
        });
    }

    private void checkForZombieInstance(Instance instance) {
        LOG.debug(String.format("checkForZombieInstance(%s)", instance));
        if (instance.getState().ordinal() >= InstanceState.FAILED.ordinal())
            return;
        if (instance.isBuried())
            synchronized (zombieInstanceReportEntities) {
                this.zombieInstanceReportEntities.add(new InstanceReportEntity(instance));
            }
    }

    @Scheduled(fixedDelay = DISPATCH_DELAY_MILLIS)
    public void checkAndDispatchToSupernode() {
        LOG.debug(String.format("checkAndDispatchToSupernode()"));
        synchronized (zombieInstanceReportEntities) {
            if (zombieInstanceReportEntities.isEmpty())
                return;
            try {
                ZombieInstanceReportEntityCollection instanceReportEntityCollection = new ZombieInstanceReportEntityCollection();
                List<InstanceReportEntity> copy = new ArrayList<InstanceReportEntity>(this.zombieInstanceReportEntities);
                instanceReportEntityCollection.setEntities(copy);
                getReportingApplication().sendReportingUpdateToASuperNode(instanceReportEntityCollection);
                this.zombieInstanceReportEntities.clear();
            } catch (Throwable t) {
                LOG.error(t);
            }
        }
    }

    @Override
    protected String getEntityType() {
        return INSTANCE_ENTITY_TYPE;
    }
}
