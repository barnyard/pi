package com.bt.pi.app.management;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.stereotype.Component;

import com.bt.pi.core.application.activation.SuperNodeApplicationCheckPoints;
import com.bt.pi.core.continuation.UpdateResolvingPiContinuation;
import com.bt.pi.core.id.PId;

@Component
public class SuperNodeSeeder extends SeederBase {
    private static final Log LOG = LogFactory.getLog(SuperNodeSeeder.class);

    public SuperNodeSeeder() {
    }

    public void configureNumberOfSuperNodes(final String applicationName, final int numberOfSuperNodes, final int offset) {

        final SuperNodeApplicationCheckPoints superNodeApplicationCheckPoints = new SuperNodeApplicationCheckPoints();
        PId id = getKoalaIdFactory().buildPId(superNodeApplicationCheckPoints.getUrl());

        getDhtClientFactory().createBlockingWriter().update(id, superNodeApplicationCheckPoints, new UpdateResolvingPiContinuation<SuperNodeApplicationCheckPoints>() {

            @Override
            public SuperNodeApplicationCheckPoints update(SuperNodeApplicationCheckPoints existingEntity, SuperNodeApplicationCheckPoints requestedEntity) {

                if (existingEntity == null) {
                    requestedEntity.setSuperNodeCheckPointsForApplication(applicationName, numberOfSuperNodes, offset);
                    return requestedEntity;
                }

                existingEntity.setSuperNodeCheckPointsForApplication(applicationName, numberOfSuperNodes, offset);
                return existingEntity;
            }

            @Override
            public void handleResult(SuperNodeApplicationCheckPoints result) {
                LOG.info(String.format("Super node record updated: %s", result));
            }
        });
    }
}
