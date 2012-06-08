/* (c) British Telecommunications plc, 2009, All Rights Reserved */
package com.bt.pi.api.service;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.stereotype.Component;

import com.bt.pi.app.common.entities.ConsoleOutput;
import com.bt.pi.app.common.entities.Instance;
import com.bt.pi.app.instancemanager.handlers.InstanceManagerApplication;
import com.bt.pi.core.application.MessageContext;
import com.bt.pi.core.continuation.PiContinuation;
import com.bt.pi.core.entity.EntityMethod;
import com.bt.pi.core.id.PId;

@Component
public class GetConsoleOutputInstanceServiceHelper extends ServiceHelperBase {
    private static final int TWENTY = 20;
    private static final Log LOG = LogFactory.getLog(GetConsoleOutputInstanceServiceHelper.class);

    public GetConsoleOutputInstanceServiceHelper() {
    }

    public ConsoleOutput getConsoleOutput(String ownerId, String instanceId, MessageContext messageContext) {
        LOG.debug(String.format("getConsoleOutput(%s, %s, %s)", ownerId, instanceId, messageContext));
        PId piInstanceId = getPiIdBuilder().getPIdForEc2AvailabilityZone(Instance.getUrl(instanceId));
        Instance instance = (Instance) getDhtClientFactory().createBlockingReader().get(piInstanceId);
        final AtomicBoolean exceptionEncountered = new AtomicBoolean(false);
        final CountDownLatch latch = new CountDownLatch(1);

        if (instance == null) {
            throw new NotFoundException(String.format("Instance %s does not exist in the system.", instanceId));
        }

        if (!instance.getUserId().equals(ownerId)) {
            throw new NotAuthorizedException(String.format("User: %s is not authorized to access instance: %s", ownerId, instanceId));
        }
        final ConsoleOutput output = new ConsoleOutput(instanceId, instance.getPlatform());
        PiContinuation<ConsoleOutput> outputcoContinuation = new PiContinuation<ConsoleOutput>() {
            @Override
            public void handleResult(ConsoleOutput result) {
                output.setInstanceId(result.getInstanceId());
                output.setOutput(result.getOutput());
                output.setTimestamp(result.getTimestamp());
                latch.countDown();
            }

            @Override
            public void handleException(Exception e) {
                super.handleException(e);
                exceptionEncountered.set(true);
                latch.countDown();
            }
        };

        messageContext.routePiMessageToApplication(getPiIdBuilder().getNodeIdFromNodeId(instance.getNodeId()), EntityMethod.GET, new ConsoleOutput(null, System.currentTimeMillis(), instanceId, instance.getPlatform()),
                InstanceManagerApplication.APPLICATION_NAME, outputcoContinuation);

        try {
            if (!latch.await(TWENTY, TimeUnit.SECONDS) || exceptionEncountered.get()) {
                output.setOutput("Console output is currently not available.");
                output.setTimestamp(System.currentTimeMillis());
            }
        } catch (InterruptedException e1) {
            LOG.error("Unable to wait on latch.", e1);
        }
        return output;
    }
}
