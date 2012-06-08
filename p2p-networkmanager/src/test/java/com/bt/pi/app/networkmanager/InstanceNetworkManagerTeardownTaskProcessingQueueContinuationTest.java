package com.bt.pi.app.networkmanager;

import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.isA;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import com.bt.pi.app.common.entities.Instance;
import com.bt.pi.app.common.entities.SecurityGroup;
import com.bt.pi.app.common.id.PiIdBuilder;
import com.bt.pi.app.common.resource.PiQueue;
import com.bt.pi.core.application.MessageContext;
import com.bt.pi.core.application.MessageContextFactory;
import com.bt.pi.core.application.watcher.task.TaskProcessingQueueHelper;
import com.bt.pi.core.continuation.PiContinuation;
import com.bt.pi.core.dht.DhtClientFactory;
import com.bt.pi.core.dht.DhtReader;
import com.bt.pi.core.entity.EntityMethod;
import com.bt.pi.core.id.PId;
import com.bt.pi.core.testing.GenericContinuationAnswer;

@RunWith(MockitoJUnitRunner.class)
public class InstanceNetworkManagerTeardownTaskProcessingQueueContinuationTest {
    @Mock
    private PiIdBuilder piIdBuilder;
    @Mock
    private MessageContextFactory messageContextFactory;
    @Mock
    private DhtClientFactory dhtClientFactory;
    @Mock
    private TaskProcessingQueueHelper taskProcessingQueueHelper;
    @Mock
    private MessageContext messageContext;
    @Mock
    private PId instancePId;
    @Mock
    private PId securityGroupPId;
    @Mock
    private Instance resultInstance;
    @Mock
    private DhtReader dhtReader;
    @Mock
    private PId instanceNetworkManagerTeardownQueueId;

    @InjectMocks
    private InstanceNetworkManagerTeardownTaskProcessingQueueContinuation c = new InstanceNetworkManagerTeardownTaskProcessingQueueContinuation();

    private String uri = "uri";
    private String nodeId = "nodeId";
    private String instanceId = "instanceA";
    private String instanceNodeId = "nodeA";
    private int globalAvzCode = 1;
    private String userId = "user";
    private String securityGroupName = "security";

    @Before
    public void setup() {
        when(piIdBuilder.getPIdForEc2AvailabilityZone(uri)).thenReturn(instancePId);
        when(piIdBuilder.getGlobalAvailabilityZoneCodeFromEc2Id(instanceId)).thenReturn(globalAvzCode);
        when(piIdBuilder.getPId(SecurityGroup.getUrl(userId, securityGroupName))).thenReturn(securityGroupPId);
        when(piIdBuilder.getPiQueuePId(PiQueue.INSTANCE_NETWORK_MANAGER_TEARDOWN)).thenReturn(instanceNetworkManagerTeardownQueueId);
        when(securityGroupPId.forGlobalAvailablityZoneCode(globalAvzCode)).thenReturn(securityGroupPId);

        when(instanceNetworkManagerTeardownQueueId.forLocalScope(PiQueue.TERMINATE_INSTANCE.getNodeScope())).thenReturn(instanceNetworkManagerTeardownQueueId);
        when(messageContextFactory.newMessageContext()).thenReturn(messageContext);
        when(dhtClientFactory.createReader()).thenReturn(dhtReader);

        when(resultInstance.getInstanceId()).thenReturn(instanceId);
        when(resultInstance.getNodeId()).thenReturn(instanceNodeId);
        when(resultInstance.getUserId()).thenReturn(userId);
        when(resultInstance.getSecurityGroupName()).thenReturn(securityGroupName);
    }

    @Test
    public void shouldAddNodeIfOnQueueItem() {
        // act
        c.receiveResult(uri, nodeId);

        // assert
        verify(taskProcessingQueueHelper).setNodeIdOnUrl(instanceNetworkManagerTeardownQueueId, uri, nodeId);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void shouldSendMessageToTheNodeRunningTheInstance() {
        // setup
        GenericContinuationAnswer<Instance> gca = new GenericContinuationAnswer<Instance>(resultInstance);
        doAnswer(gca).when(dhtReader).getAsync(eq(instancePId), isA(PiContinuation.class));

        // act
        c.receiveResult(uri, nodeId);

        // assert
        verify(messageContext).routePiMessageToApplication(securityGroupPId, EntityMethod.DELETE, resultInstance, NetworkManagerApplication.APPLICATION_NAME);
    }
}
