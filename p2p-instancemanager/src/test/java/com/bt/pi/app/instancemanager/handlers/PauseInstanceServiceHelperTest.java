package com.bt.pi.app.instancemanager.handlers;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import com.bt.pi.app.common.entities.Instance;
import com.bt.pi.app.common.entities.InstanceAction;
import com.bt.pi.app.common.id.PiIdBuilder;
import com.bt.pi.app.common.resource.PiQueue;
import com.bt.pi.core.application.MessageContext;
import com.bt.pi.core.application.MessageContextFactory;
import com.bt.pi.core.application.watcher.task.TaskProcessingQueueHelper;
import com.bt.pi.core.entity.EntityMethod;
import com.bt.pi.core.id.PId;

@RunWith(MockitoJUnitRunner.class)
public class PauseInstanceServiceHelperTest {
    @InjectMocks
    private PauseInstanceServiceHelper pauseInstanceServiceHelper = new PauseInstanceServiceHelper();
    @Mock
    private Instance instance;
    @Mock
    private MessageContextFactory messageContextFactory;
    @Mock
    private MessageContext messageContext;
    @Mock
    private PId nodePId;
    @Mock
    private PiIdBuilder piIdBuilder;
    private String nodeId = "0123";
    @Mock
    private TaskProcessingQueueHelper taskProcessingQueueHelper;
    private String instanceId = "i-123";
    @Mock
    private PId queuePId;
    private int avzCode = 345;

    @Before
    public void before() {
        when(instance.getNodeId()).thenReturn(nodeId);
        when(instance.getInstanceId()).thenReturn(instanceId);
        when(instance.getUrl()).thenReturn(Instance.getUrl(instanceId));
        when(messageContextFactory.newMessageContext()).thenReturn(messageContext);
        when(piIdBuilder.getNodeIdFromNodeId(nodeId)).thenReturn(nodePId);
        when(piIdBuilder.getGlobalAvailabilityZoneCodeFromEc2Id(instanceId)).thenReturn(avzCode);
        when(piIdBuilder.getPId(PiQueue.PAUSE_INSTANCE.getUrl())).thenReturn(queuePId);
        when(queuePId.forGlobalAvailablityZoneCode(avzCode)).thenReturn(queuePId);
    }

    @Test
    public void shouldSendMessageToInstanceManagerApplicationForPause() {
        // act
        pauseInstanceServiceHelper.pauseInstance(instance);

        // assert
        verify(instance).setActionRequired(InstanceAction.PAUSE);
        verify(messageContext).routePiMessageToApplication(nodePId, EntityMethod.UPDATE, instance, InstanceManagerApplication.APPLICATION_NAME);
    }

    @Test
    public void shouldSendMessageToInstanceManagerApplicationForUnpause() {
        // act
        pauseInstanceServiceHelper.unPauseInstance(instance);

        // assert
        verify(instance).setActionRequired(InstanceAction.UNPAUSE);
        verify(messageContext).routePiMessageToApplication(nodePId, EntityMethod.UPDATE, instance, InstanceManagerApplication.APPLICATION_NAME);
    }

    @Test
    public void shouldAddItemToQueueForPause() {
        // setup

        // act
        pauseInstanceServiceHelper.pauseInstance(instance);

        // assert
        verify(taskProcessingQueueHelper).addUrlToQueue(queuePId, Instance.getUrl(instanceId), PauseInstanceServiceHelper.RETRIES);
    }
}
