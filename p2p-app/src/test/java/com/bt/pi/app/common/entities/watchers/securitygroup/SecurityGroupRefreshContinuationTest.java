package com.bt.pi.app.common.entities.watchers.securitygroup;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.isA;
import static org.mockito.Matchers.isNull;
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
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.runners.MockitoJUnitRunner;
import org.mockito.stubbing.Answer;

import rice.Continuation;

import com.bt.pi.app.common.entities.Instance;
import com.bt.pi.app.common.entities.InstanceAddress;
import com.bt.pi.app.common.entities.PublicIpAllocationIndex;
import com.bt.pi.app.common.entities.SecurityGroup;
import com.bt.pi.app.common.entities.util.ResourceRange;
import com.bt.pi.app.common.id.PiIdBuilder;
import com.bt.pi.app.common.net.utils.IpAddressUtils;
import com.bt.pi.core.application.resource.ConsumedDhtResourceRegistry;
import com.bt.pi.core.application.resource.watched.FiniteLifespanConsumerCheckCallback;
import com.bt.pi.core.application.resource.watched.FiniteLifespanConsumerStatusChecker;
import com.bt.pi.core.continuation.UpdateResolvingContinuation;
import com.bt.pi.core.dht.DhtClientFactory;
import com.bt.pi.core.dht.DhtReader;
import com.bt.pi.core.dht.DhtWriter;
import com.bt.pi.core.entity.PiEntity;
import com.bt.pi.core.id.KoalaIdFactory;
import com.bt.pi.core.id.PId;
import com.bt.pi.core.parser.KoalaJsonParser;
import com.bt.pi.core.parser.KoalaPiEntityFactory;
import com.bt.pi.core.testing.GenericContinuationAnswer;
import com.bt.pi.core.testing.UpdateResolvingContinuationAnswer;

@RunWith(MockitoJUnitRunner.class)
public class SecurityGroupRefreshContinuationTest {
    private static final String USERNAME = "username";
    private SecurityGroupRefreshContinuation securityGroupRefreshContinuation;
    private PiIdBuilder piIdBuilder;
    private KoalaIdFactory koalaIdFactory;
    private SecurityGroup securityGroup;
    private UpdateResolvingContinuationAnswer secGroupUpdateResolvingContinuationAnswer;
    private boolean isInstanceActive;
    private boolean isInstanceMissing;
    private PId securityGroupId;
    private String nonRegisteredInstanceId = "i-07VLB13K";
    private String registeredInstanceId = "i-07UxOMDb";
    private InstanceAddress registeredInstanceAddresses;
    private InstanceAddress nonRegisteredInstanceAddresses;
    @Mock
    private ConsumedDhtResourceRegistry consumedDhtResourceRegistry;
    @Mock
    private DhtClientFactory dhtClientFactory;
    @Mock
    private DhtWriter dhtWriter;
    @Mock
    private DhtReader dhtReader;
    @Mock
    private Instance instance;
    @Mock
    private FiniteLifespanConsumerStatusChecker checker;
    private PublicIpAllocationIndex publicIpAllocationIndex;
    private GenericContinuationAnswer<PublicIpAllocationIndex> publicIpIndexAnswer;
    private PId publicIpIndexId;
    private String registeredInstancePublicIp;
    private String nonRegisteredInstancePublicIp;

    @SuppressWarnings("unchecked")
    @Before
    public void before() throws Exception {
        Set<ResourceRange> resourceRanges = new HashSet<ResourceRange>();
        resourceRanges.add(new ResourceRange(IpAddressUtils.ipToLong("1.2.3.4"), IpAddressUtils.ipToLong("1.2.3.44")));
        publicIpAllocationIndex = new PublicIpAllocationIndex();
        publicIpAllocationIndex.setResourceRanges(resourceRanges);
        registeredInstancePublicIp = publicIpAllocationIndex.allocateIpAddressToInstance(registeredInstanceId);
        nonRegisteredInstancePublicIp = publicIpAllocationIndex.allocateIpAddressToInstance(nonRegisteredInstanceId);

        registeredInstanceAddresses = new InstanceAddress();
        registeredInstanceAddresses.setPublicIpAddress(registeredInstancePublicIp);
        nonRegisteredInstanceAddresses = new InstanceAddress();
        nonRegisteredInstanceAddresses.setPublicIpAddress(nonRegisteredInstancePublicIp);

        securityGroup = new SecurityGroup("user-id", "group-name", 10L, "10.0.0.2", null, null, null);
        securityGroup.getInstances().put(registeredInstanceId, registeredInstanceAddresses);
        securityGroup.getInstances().put(nonRegisteredInstanceId, nonRegisteredInstanceAddresses);

        KoalaPiEntityFactory koalaPiEntityFactory = new KoalaPiEntityFactory();// {
        // @Override
        // protected String readFile(String aPiEntitiesJsonFile) throws IOException {
        // return
        // "{\"persistablePiEntityMappings\":[{\"type\" : \"SecurityGroup\", \"typeCode\" : 4, \"scheme\" : \"sg\"}, {\"type\" : \"PublicIpAllocationIndex\", \"typeCode\" : 5, \"scheme\" : \"idx:public-ip-allocations\"}]}";
        // }
        // };
        koalaPiEntityFactory.setKoalaJsonParser(new KoalaJsonParser());
        koalaPiEntityFactory.setPiEntityTypes(Arrays.asList(new PiEntity[] { new SecurityGroup(), new PublicIpAllocationIndex() }));
        // koalaPiEntityFactory.setPersistedEntityMappings();
        koalaIdFactory = new KoalaIdFactory(99, 99);
        koalaIdFactory.setKoalaPiEntityFactory(koalaPiEntityFactory);
        piIdBuilder = new PiIdBuilder();
        piIdBuilder.setKoalaIdFactory(koalaIdFactory);
        securityGroupId = piIdBuilder.getPId(securityGroup).forLocalRegion();
        publicIpIndexId = piIdBuilder.getPId(PublicIpAllocationIndex.URL).forLocalRegion();

        secGroupUpdateResolvingContinuationAnswer = new UpdateResolvingContinuationAnswer(new SecurityGroup(securityGroup));
        doAnswer(secGroupUpdateResolvingContinuationAnswer).when(consumedDhtResourceRegistry).update(eq(securityGroupId), isA(UpdateResolvingContinuation.class));

        publicIpIndexAnswer = new GenericContinuationAnswer<PublicIpAllocationIndex>(publicIpAllocationIndex);
        doAnswer(publicIpIndexAnswer).when(dhtReader).getAsync(eq(publicIpIndexId), isA(Continuation.class));

        when(dhtClientFactory.createWriter()).thenReturn(dhtWriter);
        when(dhtClientFactory.createReader()).thenReturn(dhtReader);

        Set<String> consumers = new HashSet<String>();
        consumers.add(registeredInstanceId);

        when(consumedDhtResourceRegistry.getAllConsumers(securityGroupId)).thenReturn(consumers);

        isInstanceActive = true;
        doAnswer(new Answer<Object>() {
            public Object answer(InvocationOnMock invocation) throws Throwable {
                FiniteLifespanConsumerCheckCallback<Instance> activeCallback = (FiniteLifespanConsumerCheckCallback<Instance>) invocation.getArguments()[0];
                FiniteLifespanConsumerCheckCallback<Instance> inactiveCallback = (FiniteLifespanConsumerCheckCallback<Instance>) invocation.getArguments()[1];
                if (isInstanceActive) {
                    if (activeCallback != null)
                        activeCallback.handleCallback(instance);
                } else {
                    if (inactiveCallback != null)
                        inactiveCallback.handleCallback(isInstanceMissing ? null : instance);
                }
                return null;
            }
        }).when(checker).check((FiniteLifespanConsumerCheckCallback) isNull(), isA(FiniteLifespanConsumerCheckCallback.class));

        securityGroupRefreshContinuation = new SecurityGroupRefreshContinuation(piIdBuilder, dhtClientFactory, consumedDhtResourceRegistry) {
            @Override
            protected FiniteLifespanConsumerStatusChecker createInstanceStatusChecker(String instanceId, PId consumerRecordId) {
                assertEquals(piIdBuilder.getPIdForEc2AvailabilityZone(Instance.getUrl(instanceId)), consumerRecordId);
                return checker;
            }
        };
    }

    @SuppressWarnings("unchecked")
    @Test
    public void shouldPurgeInstanceFromSecGroupWhenInstanceInactive() {
        // setup
        isInstanceActive = false;

        // act
        securityGroupRefreshContinuation.handleResult(securityGroup);

        // asset
        SecurityGroup res = (SecurityGroup) secGroupUpdateResolvingContinuationAnswer.getResult();
        assertEquals(1, res.getInstances().size());
        assertNotNull(res.getInstances().get(registeredInstanceId));
        verify(consumedDhtResourceRegistry).refresh(eq(securityGroupId), isA(Continuation.class));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void shouldPurgeInstanceFromSecGroupWhenInstanceMissingInDht() {
        // setup
        isInstanceActive = false;
        isInstanceMissing = true;

        // act
        securityGroupRefreshContinuation.handleResult(securityGroup);

        // asset
        SecurityGroup res = (SecurityGroup) secGroupUpdateResolvingContinuationAnswer.getResult();
        assertEquals(1, res.getInstances().size());
        assertNotNull(res.getInstances().get(registeredInstanceId));
        verify(consumedDhtResourceRegistry).refresh(eq(securityGroupId), isA(Continuation.class));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void shouldNotPurgeInstanceFromSecGroupWhenInstanceAactive() {
        // setup
        isInstanceActive = true;

        // act
        securityGroupRefreshContinuation.handleResult(securityGroup);

        // asset
        assertNull(secGroupUpdateResolvingContinuationAnswer.getResult());
        verify(consumedDhtResourceRegistry, never()).refresh(eq(securityGroupId), isA(Continuation.class));
    }

    @Test
    public void shouldCorrectSecurityGroupPublicIpFromPublicIpIndex() {
        // setup
        String elasticIp = publicIpAllocationIndex.allocateElasticIpAddressToUser(USERNAME);
        publicIpAllocationIndex.assignElasticIpAddressToInstance(elasticIp, registeredInstanceId, USERNAME);

        // act
        securityGroupRefreshContinuation.handleResult(securityGroup);

        // assert
        assertEquals(elasticIp, ((SecurityGroup) secGroupUpdateResolvingContinuationAnswer.getResult()).getInstances().get(registeredInstanceId).getPublicIpAddress());
        assertEquals(nonRegisteredInstancePublicIp, ((SecurityGroup) secGroupUpdateResolvingContinuationAnswer.getResult()).getInstances().get(nonRegisteredInstanceId).getPublicIpAddress());
    }

    @Test
    public void shouldDoNothingToSecurityGroupPublicIpWhenNoMatchFoundInPublicIndex() {
        // setup
        publicIpAllocationIndex.freeResourceFor(registeredInstanceId);

        // act
        securityGroupRefreshContinuation.handleResult(securityGroup);

        // assert
        assertNull(secGroupUpdateResolvingContinuationAnswer.getResult());
    }

    @Test
    public void shouldAssignAddressToSecGroupInstanceWithNoPublicaddress() {
        // setup
        securityGroup.getInstances().get(registeredInstanceId).setPublicIpAddress(null);

        String elasticIp = publicIpAllocationIndex.allocateElasticIpAddressToUser(USERNAME);
        publicIpAllocationIndex.assignElasticIpAddressToInstance(elasticIp, registeredInstanceId, USERNAME);

        // act
        securityGroupRefreshContinuation.handleResult(securityGroup);

        // assert
        assertEquals(elasticIp, ((SecurityGroup) secGroupUpdateResolvingContinuationAnswer.getResult()).getInstances().get(registeredInstanceId).getPublicIpAddress());
    }
}
