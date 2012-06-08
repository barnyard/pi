package com.bt.pi.app.networkmanager.handlers;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.isA;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.runners.MockitoJUnitRunner;
import org.mockito.stubbing.Answer;

import com.bt.pi.app.common.entities.Instance;
import com.bt.pi.app.common.entities.InstanceAddress;
import com.bt.pi.app.common.entities.InstanceState;
import com.bt.pi.app.common.entities.PublicIpAllocationIndex;
import com.bt.pi.app.common.entities.SecurityGroup;
import com.bt.pi.app.common.entities.util.ResourceRange;
import com.bt.pi.app.common.id.PiIdBuilder;
import com.bt.pi.app.common.net.utils.IpAddressUtils;
import com.bt.pi.core.application.ReceivedMessageContext;
import com.bt.pi.core.application.resource.ConsumedDhtResourceRegistry;
import com.bt.pi.core.continuation.PiContinuation;
import com.bt.pi.core.continuation.UpdateResolvingContinuation;
import com.bt.pi.core.continuation.UpdateResolvingPiContinuation;
import com.bt.pi.core.dht.DhtClientFactory;
import com.bt.pi.core.dht.DhtReader;
import com.bt.pi.core.dht.DhtWriter;
import com.bt.pi.core.dht.cache.DhtCache;
import com.bt.pi.core.entity.PiEntity;
import com.bt.pi.core.id.KoalaIdFactory;
import com.bt.pi.core.id.PId;
import com.bt.pi.core.parser.KoalaJsonParser;
import com.bt.pi.core.parser.KoalaPiEntityFactory;
import com.bt.pi.core.testing.GenericContinuationAnswer;
import com.bt.pi.core.testing.UpdateResolvingContinuationAnswer;

@RunWith(MockitoJUnitRunner.class)
public class InstanceNetworkRefreshHandlerTest {
    private static final String INSTANCE_ID = "i-07YQGJLf";
    @InjectMocks
    private InstanceNetworkRefreshHandler instanceNetworkRefreshHandler = new InstanceNetworkRefreshHandler();
    private Instance instance;
    @Mock
    private ReceivedMessageContext messageContext;
    @Mock
    private ConsumedDhtResourceRegistry consumedDhtResourceRegistry;
    @Mock
    private InstanceNetworkSetupHandler instanceNetworkSetupHandler;
    @Mock
    private DhtCache dhtCache;
    @Mock
    private DhtClientFactory dhtClientFactory;
    @Mock
    private DhtWriter dhtWriter;
    @Mock
    private DhtReader dhtReader;

    private PiIdBuilder piIdBuilder;
    private Set<String> consumers;
    private PId securityGroupRecordId;
    private PId instanceRecordId;
    private SecurityGroup securityGroup;
    private InstanceAddress instanceAddresses;
    private UpdateResolvingContinuationAnswer instanceRecordUpdateAnswer;
    private GenericContinuationAnswer<PublicIpAllocationIndex> publicIpIndexReadAnswer;
    private PublicIpAllocationIndex publicIpIndexRecord;
    private String instancePublicIp;
    private PId publicIpIndexRecordId;

    @SuppressWarnings("unchecked")
    @Before
    public void before() throws Exception {
        Set<ResourceRange> resourceRanges = new HashSet<ResourceRange>();
        resourceRanges.add(new ResourceRange(IpAddressUtils.ipToLong("1.2.3.4"), IpAddressUtils.ipToLong("1.2.3.44")));
        publicIpIndexRecord = new PublicIpAllocationIndex();
        publicIpIndexRecord.setResourceRanges(resourceRanges);
        instancePublicIp = publicIpIndexRecord.allocateIpAddressToInstance(INSTANCE_ID);

        KoalaPiEntityFactory koalaPiEntityFactory = new KoalaPiEntityFactory();
        koalaPiEntityFactory.setKoalaJsonParser(new KoalaJsonParser());
        koalaPiEntityFactory.setPiEntityTypes(Arrays.asList(new PiEntity[] { new SecurityGroup(), new PublicIpAllocationIndex(), new Instance() }));
        KoalaIdFactory koalaIdFactory = new KoalaIdFactory(99, 99);
        koalaIdFactory.setKoalaPiEntityFactory(koalaPiEntityFactory);
        piIdBuilder = new PiIdBuilder();
        piIdBuilder.setKoalaIdFactory(koalaIdFactory);
        instanceRecordId = piIdBuilder.getPIdForEc2AvailabilityZone(Instance.getUrl(INSTANCE_ID));
        securityGroupRecordId = piIdBuilder.getPId(SecurityGroup.getUrl("userId", "default")).forLocalRegion();
        publicIpIndexRecordId = piIdBuilder.getPId(PublicIpAllocationIndex.URL).forLocalRegion();

        instance = new Instance(INSTANCE_ID, "userId", "default");
        instance.setPublicIpAddress(instancePublicIp);

        instanceAddresses = new InstanceAddress();
        instanceAddresses.setPublicIpAddress(instancePublicIp);
        securityGroup = new SecurityGroup("userId", "default", 10L, "10.0.0.2", "255.255.255.240", null, null);
        securityGroup.getInstances().put(INSTANCE_ID, instanceAddresses);

        consumers = new HashSet<String>();

        when(consumedDhtResourceRegistry.getAllConsumers(eq(securityGroupRecordId))).thenReturn(consumers);
        when(consumedDhtResourceRegistry.getCachedEntity(eq(securityGroupRecordId))).thenReturn(securityGroup);

        instanceRecordUpdateAnswer = new UpdateResolvingContinuationAnswer(instance);
        when(dhtClientFactory.createReader()).thenReturn(dhtReader);
        when(dhtClientFactory.createWriter()).thenReturn(dhtWriter);
        doAnswer(instanceRecordUpdateAnswer).when(dhtWriter).update(eq(instanceRecordId), isA(UpdateResolvingContinuation.class));

        publicIpIndexReadAnswer = new GenericContinuationAnswer<PublicIpAllocationIndex>(publicIpIndexRecord);
        doAnswer(publicIpIndexReadAnswer).when(dhtCache).get(eq(publicIpIndexRecordId), isA(PiContinuation.class));

        instanceNetworkRefreshHandler.setPiIdBuilder(piIdBuilder);
    }

    @Test
    public void shouldNotSetupNetworkWhenInstanceAlreadyRegisteredWithResourceManager() {
        // setup
        consumers.add(INSTANCE_ID);

        // act
        instanceNetworkRefreshHandler.handle(instance, messageContext);

        // assert
        verify(instanceNetworkSetupHandler, never()).handle(any(Instance.class), anyBoolean(), any(ReceivedMessageContext.class));
    }

    @Test
    public void shouldDoNothingWhenInstanceNoLongerActive() {
        // setup
        instance.setLastHeartbeatTimestamp(0L);

        // act
        instanceNetworkRefreshHandler.handle(instance, messageContext);

        // assert
        verify(instanceNetworkSetupHandler, never()).handle(any(Instance.class), anyBoolean(), any(ReceivedMessageContext.class));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void shouldForwardToSetupHandlerWhenActiveInstanceNotRegisteredWithResourceManager() {
        // setup
        doAnswer(new GenericContinuationAnswer<Instance>(instance)).when(dhtReader).getAsync(eq(piIdBuilder.getPIdForEc2AvailabilityZone(instance)), isA(PiContinuation.class));

        // act
        instanceNetworkRefreshHandler.handle(instance, messageContext);

        // assert
        verify(instanceNetworkSetupHandler).handle(instance, false, messageContext);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void shouldNotForwardToSetupHandlerWhenActiveInstanceNotRegisteredWithResourceManagerBecauseInstanceIsShuttingDown() {
        // setup
        Instance instanceReadFromDht = new Instance();
        instanceReadFromDht.setState(InstanceState.SHUTTING_DOWN);
        doAnswer(new GenericContinuationAnswer<Instance>(instanceReadFromDht)).when(dhtReader).getAsync(eq(piIdBuilder.getPIdForEc2AvailabilityZone(instance)), isA(PiContinuation.class));

        // act
        instanceNetworkRefreshHandler.handle(instance, messageContext);

        // assert
        verify(instanceNetworkSetupHandler, never()).handle(instance, false, messageContext);
    }

    @Test
    public void shouldCorrectInstancePublicIpWhenDifferentFromThatInSecurityGroupAndPublicIpIndexConfirmsSecGroupIsRight() {
        // setup
        consumers.add(INSTANCE_ID);
        instance.setPublicIpAddress("9.8.7.6");

        // act
        instanceNetworkRefreshHandler.handle(instance, messageContext);

        // assert
        assertEquals(instancePublicIp, ((Instance) instanceRecordUpdateAnswer.getResult()).getPublicIpAddress());
    }

    @SuppressWarnings("unchecked")
    @Test
    public void shouldClearOutPrivateIpAddressForInstanceFromSecurityGroupIfNotInValidSubnet() throws Exception {
        // setup
        String privateIpAddress = "10.2.3.4";
        instance.setPrivateIpAddress(privateIpAddress);
        instanceAddresses.setPrivateIpAddress(privateIpAddress);

        doAnswer(new Answer<Object>() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                Object updated = ((UpdateResolvingPiContinuation) invocation.getArguments()[1]).update(securityGroup, securityGroup);
                if (updated != null)
                    ((UpdateResolvingPiContinuation) invocation.getArguments()[1]).handleResult(securityGroup);
                return null;
            }
        }).when(consumedDhtResourceRegistry).update(eq(securityGroupRecordId), isA(UpdateResolvingPiContinuation.class));
        doAnswer(new GenericContinuationAnswer<Instance>(instance)).when(dhtReader).getAsync(eq(piIdBuilder.getPIdForEc2AvailabilityZone(instance)), isA(PiContinuation.class));

        // act
        instanceNetworkRefreshHandler.handle(instance, messageContext);

        // assert
        assertNull(securityGroup.getInstances().get(instance.getInstanceId()).getPrivateIpAddress());
        verify(instanceNetworkSetupHandler).handle(instance, false, messageContext);
    }

    @Test
    public void shouldCorrectInstancePublicIpWhenNullAndPublicIpIndexAgreesWithSecGroup() {
        // setup
        consumers.add(INSTANCE_ID);
        instance.setPublicIpAddress(null);

        // act
        instanceNetworkRefreshHandler.handle(instance, messageContext);

        // assert
        assertEquals(instancePublicIp, ((Instance) instanceRecordUpdateAnswer.getResult()).getPublicIpAddress());
    }

    @Test
    public void shouldLeaveInstancePublicIpAloneWhenDifferentFromThatInSecurityGroupAndPublicIpIndexDisagreesWithSecGroup() {
        // setup
        consumers.add(INSTANCE_ID);
        instance.setPublicIpAddress("9.8.7.6");
        String elasticIp = publicIpIndexRecord.allocateElasticIpAddressToUser("username");
        publicIpIndexRecord.assignElasticIpAddressToInstance(elasticIp, INSTANCE_ID, "username");

        // act
        instanceNetworkRefreshHandler.handle(instance, messageContext);

        // assert
        assertNull(instanceRecordUpdateAnswer.getResult());
    }

    @Test
    public void shouldLeaveInstancePublicIpAloneWhenDifferentFromThatInSecurityGroupAndPublicIpIndexHasNoAssignmentForThatInstance() {
        // setup
        consumers.add(INSTANCE_ID);
        instance.setPublicIpAddress("9.8.7.6");
        publicIpIndexRecord.freeResourceFor(INSTANCE_ID);

        // act
        instanceNetworkRefreshHandler.handle(instance, messageContext);

        // assert
        assertNull(instanceRecordUpdateAnswer.getResult());
    }

    @Test
    public void shouldLeaveInstancePublicIpAloneWhenSecurityGroupNotInResourceManager() {
        // setup
        consumers.add(INSTANCE_ID);

        // act
        instanceNetworkRefreshHandler.handle(instance, messageContext);

        // assert
        assertNull(instanceRecordUpdateAnswer.getResult());
    }

    @Test
    public void shouldLeaveInstancePublicIpAloneWhenNoMatchingAssignmentInSecurityGroup() {
        // setup
        consumers.add(INSTANCE_ID);
        when(consumedDhtResourceRegistry.getCachedEntity(securityGroupRecordId)).thenReturn(null);

        // act
        instanceNetworkRefreshHandler.handle(instance, messageContext);

        // assert
        assertNull(instanceRecordUpdateAnswer.getResult());
    }
}
