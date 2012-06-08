package com.bt.pi.app.networkmanager;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import com.bt.pi.app.common.entities.Instance;
import com.bt.pi.app.common.entities.PublicIpAddress;
import com.bt.pi.app.common.entities.SecurityGroup;
import com.bt.pi.app.common.id.PiIdBuilder;
import com.bt.pi.app.common.resource.PiQueue;
import com.bt.pi.app.networkmanager.handlers.AssociateAddressWithInstanceHandler;
import com.bt.pi.app.networkmanager.handlers.DisassociateAddressFromInstanceHandler;
import com.bt.pi.app.networkmanager.handlers.InstanceNetworkRefreshHandler;
import com.bt.pi.app.networkmanager.handlers.InstanceNetworkSetupHandler;
import com.bt.pi.app.networkmanager.handlers.InstanceNetworkTeardownHandler;
import com.bt.pi.app.networkmanager.handlers.SecurityGroupNetworkUpdateHandler;
import com.bt.pi.core.application.ReceivedMessageContext;
import com.bt.pi.core.application.watcher.task.TaskProcessingQueueHelper;
import com.bt.pi.core.entity.EntityMethod;
import com.bt.pi.core.entity.PiEntity;
import com.bt.pi.core.id.PId;

@RunWith(MockitoJUnitRunner.class)
public class NetworkManagerAppDeliveredMessageDispatcherTest {
    @Mock
    private InstanceNetworkSetupHandler instanceNetworkSetupHandler;
    @Mock
    private InstanceNetworkRefreshHandler instanceNetworkRefreshHandler;
    @Mock
    private InstanceNetworkTeardownHandler instanceNetworkTeardownHandler;
    @Mock
    private SecurityGroupNetworkUpdateHandler securityGroupNetworkUpdateHandler;
    @Mock
    AssociateAddressWithInstanceHandler associateAddressWithInstanceHandler;
    @Mock
    DisassociateAddressFromInstanceHandler disassociateAddressFromInstanceHandler;
    @Mock
    ReceivedMessageContext messageContext;
    @Mock
    Instance instance;
    @Mock
    private SecurityGroup securityGroup;
    @Mock
    private PublicIpAddress address;
    @Mock
    private PiIdBuilder piIdBuilder;
    @Mock
    private TaskProcessingQueueHelper taskProcessingQueueHelper;
    @InjectMocks
    private NetworkManagerAppDeliveredMessageDispatcher networkManagerAppDeliveredMessageDispatcher = new NetworkManagerAppDeliveredMessageDispatcher();
    private PiEntity receivedEntity;
    private EntityMethod entityMethod;
    @Mock
    private PId updateSecurityGroupQueueId;
    @Mock
    private PId removeSecurityGroupQueueId;

    @Before
    public void before() {
        entityMethod = EntityMethod.CREATE;
        receivedEntity = instance;
        mockMessageContext();
        when(piIdBuilder.getPiQueuePId(PiQueue.UPDATE_SECURITY_GROUP)).thenReturn(updateSecurityGroupQueueId);
        when(updateSecurityGroupQueueId.forLocalScope(PiQueue.UPDATE_SECURITY_GROUP.getNodeScope())).thenReturn(updateSecurityGroupQueueId);
        when(piIdBuilder.getPiQueuePId(PiQueue.REMOVE_SECURITY_GROUP)).thenReturn(removeSecurityGroupQueueId);
        when(removeSecurityGroupQueueId.forLocalScope(PiQueue.REMOVE_SECURITY_GROUP.getNodeScope())).thenReturn(removeSecurityGroupQueueId);
    }

    private void mockMessageContext() {
        this.messageContext = mock(ReceivedMessageContext.class);
        when(this.messageContext.getMethod()).thenReturn(entityMethod);
        when(this.messageContext.getReceivedEntity()).thenReturn(receivedEntity);
    }

    @Test
    public void shouldDelegateInstanceNetworkSetupRequestOnDeliver() {
        // act
        networkManagerAppDeliveredMessageDispatcher.dispatchToHandler(messageContext, entityMethod, receivedEntity);

        // assert
        verify(instanceNetworkSetupHandler).handle(instance, messageContext);
    }

    @Test
    public void shouldDelegateInstanceNetworkRefreshRequestOnDeliver() {
        // setup
        entityMethod = EntityMethod.UPDATE;
        mockMessageContext();

        // act
        networkManagerAppDeliveredMessageDispatcher.dispatchToHandler(messageContext, entityMethod, receivedEntity);

        // assert
        verify(instanceNetworkRefreshHandler).handle(instance, messageContext);
    }

    @Test
    public void shouldDelegateInstanceNetworkTeardownRequestOnDeliver() {
        // setup
        entityMethod = EntityMethod.DELETE;
        mockMessageContext();

        // act
        networkManagerAppDeliveredMessageDispatcher.dispatchToHandler(messageContext, entityMethod, receivedEntity);

        // assert
        verify(instanceNetworkTeardownHandler).handle(instance, messageContext);
    }

    @Test
    public void shouldDelegateSecurityGroupUpdateOnDeliver() {
        // setup
        entityMethod = EntityMethod.UPDATE;
        receivedEntity = securityGroup;
        mockMessageContext();

        // act
        networkManagerAppDeliveredMessageDispatcher.dispatchToHandler(messageContext, entityMethod, receivedEntity);

        // assert
        verify(securityGroupNetworkUpdateHandler).handle(EntityMethod.UPDATE, securityGroup);
    }

    @Test
    public void shouldRemoveSecurityGroupQueueItemFromQueueOnUpdate() {
        // setup
        securityGroup = new SecurityGroup();
        entityMethod = EntityMethod.UPDATE;
        receivedEntity = securityGroup;
        mockMessageContext();

        // act
        networkManagerAppDeliveredMessageDispatcher.dispatchToHandler(messageContext, entityMethod, receivedEntity);

        // assert
        verify(taskProcessingQueueHelper).removeUrlFromQueue(updateSecurityGroupQueueId, securityGroup.getUrl());
    }

    @Test
    public void shouldRemoveSecurityGroupQueueItemFromQueueOnDelete() {
        // setup
        securityGroup = new SecurityGroup();
        entityMethod = EntityMethod.DELETE;
        receivedEntity = securityGroup;
        mockMessageContext();

        // act
        networkManagerAppDeliveredMessageDispatcher.dispatchToHandler(messageContext, entityMethod, receivedEntity);

        // assert
        verify(taskProcessingQueueHelper).removeUrlFromQueue(removeSecurityGroupQueueId, securityGroup.getUrl());
    }

    @Test
    public void shouldDelegateElasticAddressAssociationOnDeliver() {
        // setup
        entityMethod = EntityMethod.CREATE;
        receivedEntity = address;
        mockMessageContext();

        // act
        networkManagerAppDeliveredMessageDispatcher.dispatchToHandler(messageContext, entityMethod, receivedEntity);

        // assert
        verify(associateAddressWithInstanceHandler).handle(address);
    }

    @Test
    public void shouldDelegateElasticAddressDisassociationOnDeliver() {
        // setup
        entityMethod = EntityMethod.DELETE;
        receivedEntity = address;
        mockMessageContext();

        // act
        networkManagerAppDeliveredMessageDispatcher.dispatchToHandler(messageContext, entityMethod, receivedEntity);

        // assert
        verify(disassociateAddressFromInstanceHandler).handle(address);
    }

    @Test
    public void shouldDelegateSecurityGroupDeleteOnDeliver() {
        // setup
        entityMethod = EntityMethod.DELETE;
        receivedEntity = securityGroup;
        mockMessageContext();

        // act
        networkManagerAppDeliveredMessageDispatcher.dispatchToHandler(messageContext, entityMethod, receivedEntity);

        // assert
        verify(securityGroupNetworkUpdateHandler).handle(EntityMethod.DELETE, securityGroup);
    }
}
