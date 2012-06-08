package com.bt.pi.app.networkmanager;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.springframework.context.ApplicationEvent;

import rice.p2p.commonapi.Id;

import com.bt.pi.app.common.entities.SecurityGroup;
import com.bt.pi.app.common.id.PiIdBuilder;
import com.bt.pi.app.networkmanager.handlers.NetworkCleanupHandler;
import com.bt.pi.core.application.activation.ApplicationRecord;
import com.bt.pi.core.application.activation.ApplicationRecordRefreshedEvent;
import com.bt.pi.core.application.resource.ConsumedDhtResourceRegistry;
import com.bt.pi.core.id.PId;

public class NetworkManagerSpringEventHandlerTest {
    private NetworkManagerSpringEventHandler networkManagerSpringEventHandler;
    private ApplicationRecordRefreshedEvent applicationRecordRefreshedEvent;
    private ApplicationRecord applicationRecord;
    private ConsumedDhtResourceRegistry consumedDhtResourceRegistry;
    private List<SecurityGroup> securityGroups;
    private SecurityGroup mySecurityGroup;
    private SecurityGroup otherSecurityGroup;
    private PId mySecurityGroupId;
    private PId otherSecurityGroupId;
    private Id myNodeId;
    private Id otherNodeId;
    private PiIdBuilder piIdBuilder;
    private NetworkManagerApplication networkManagerApplication;
    private NetworkCleanupHandler networkCleanupHandler;

    @Before
    public void before() {
        mySecurityGroupId = mock(PId.class);
        otherSecurityGroupId = mock(PId.class);
        myNodeId = mock(Id.class);
        otherNodeId = mock(Id.class);

        mySecurityGroup = mock(SecurityGroup.class);
        when(mySecurityGroup.getSecurityGroupId()).thenReturn("my:default");
        otherSecurityGroup = mock(SecurityGroup.class);
        when(otherSecurityGroup.getSecurityGroupId()).thenReturn("other:default");

        networkManagerApplication = mock(NetworkManagerApplication.class);
        when(networkManagerApplication.getNodeId()).thenReturn(myNodeId);

        securityGroups = new ArrayList<SecurityGroup>();
        securityGroups.add(mySecurityGroup);
        securityGroups.add(otherSecurityGroup);

        consumedDhtResourceRegistry = mock(ConsumedDhtResourceRegistry.class);
        when(consumedDhtResourceRegistry.getByType(SecurityGroup.class)).thenReturn(securityGroups);

        networkCleanupHandler = mock(NetworkCleanupHandler.class);

        piIdBuilder = mock(PiIdBuilder.class);
        when(piIdBuilder.getPId(mySecurityGroup)).thenReturn(mySecurityGroupId);
        when(piIdBuilder.getPId(otherSecurityGroup)).thenReturn(otherSecurityGroupId);
        when(piIdBuilder.getPId(mySecurityGroup)).thenReturn(mySecurityGroupId);
        when(mySecurityGroupId.forLocalAvailabilityZone()).thenReturn(mySecurityGroupId);
        when(mySecurityGroupId.forLocalRegion()).thenReturn(mySecurityGroupId);
        String mySecurityGroupIdStr = "1234567890123456789012345678901234560008";
        when(mySecurityGroupId.getIdAsHex()).thenReturn(mySecurityGroupIdStr);
        when(piIdBuilder.getPId(otherSecurityGroup)).thenReturn(otherSecurityGroupId);
        when(otherSecurityGroupId.forLocalAvailabilityZone()).thenReturn(otherSecurityGroupId);
        when(otherSecurityGroupId.forLocalRegion()).thenReturn(otherSecurityGroupId);
        String otherSecurityGroupIdStr = "1234567890123456789012345678901234550008";
        when(otherSecurityGroupId.getIdAsHex()).thenReturn(otherSecurityGroupIdStr);

        applicationRecord = mock(ApplicationRecord.class);
        when(applicationRecord.getApplicationName()).thenReturn(NetworkManagerApplication.APPLICATION_NAME);
        when(applicationRecord.getNumCurrentlyActiveNodes()).thenReturn(2);
        when(applicationRecord.getClosestActiveNodeId(rice.pastry.Id.build(mySecurityGroupIdStr))).thenReturn(myNodeId);
        when(applicationRecord.getClosestActiveNodeId(rice.pastry.Id.build(otherSecurityGroupIdStr))).thenReturn(otherNodeId);

        applicationRecordRefreshedEvent = new ApplicationRecordRefreshedEvent(applicationRecord, this);

        networkManagerSpringEventHandler = new NetworkManagerSpringEventHandler();
        networkManagerSpringEventHandler.setConsumedDhtResourceRegistry(consumedDhtResourceRegistry);
        networkManagerSpringEventHandler.setPiIdBuilder(piIdBuilder);
        networkManagerSpringEventHandler.setNetworkManagerApplication(networkManagerApplication);
        networkManagerSpringEventHandler.setNetworkCleanupHandler(networkCleanupHandler);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void shouldIgnoreNonRelevantEvent() {
        // act
        networkManagerSpringEventHandler.onApplicationEvent(mock(ApplicationEvent.class));

        // assert
        verify(consumedDhtResourceRegistry, never()).getByType(any(Class.class));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void shouldIgnoreEventForAnotherApp() {
        // setup
        when(applicationRecord.getApplicationName()).thenReturn("moo");

        // act
        networkManagerSpringEventHandler.onApplicationEvent(applicationRecordRefreshedEvent);

        // assert
        verify(consumedDhtResourceRegistry, never()).getByType(any(Class.class));
    }

    @Test
    public void shouldTearDownNetworkThatWeAreNotResponsibleFor() {
        // act
        networkManagerSpringEventHandler.onApplicationEvent(applicationRecordRefreshedEvent);

        // assert
        verify(networkCleanupHandler).releaseSecurityGroup(otherSecurityGroupId);
        verify(networkCleanupHandler, never()).releaseSecurityGroup(mySecurityGroupId);
    }

    @Test
    public void shouldDoNothingWhenApplicationRecordGivesNoNearestNodes() {
        // setup
        when(applicationRecord.getNumCurrentlyActiveNodes()).thenReturn(0);

        // act
        networkManagerSpringEventHandler.onApplicationEvent(applicationRecordRefreshedEvent);

        // assert
        verify(networkCleanupHandler, never()).releaseSecurityGroup(any(PId.class));
    }
}
