package com.bt.pi.app.networkmanager.addressing;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.isA;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.runners.MockitoJUnitRunner;
import org.mockito.stubbing.Answer;

import com.bt.pi.app.common.entities.InstanceAddress;
import com.bt.pi.app.common.entities.NetworkRule;
import com.bt.pi.app.common.entities.SecurityGroup;
import com.bt.pi.app.common.id.PiIdBuilder;
import com.bt.pi.app.common.net.NetworkCommandRunner;
import com.bt.pi.app.networkmanager.iptables.IpTablesManager;
import com.bt.pi.core.application.resource.ConsumedDhtResourceRegistry;
import com.bt.pi.core.continuation.GenericContinuation;
import com.bt.pi.core.continuation.UpdateResolvingContinuation;
import com.bt.pi.core.entity.PiEntity;
import com.bt.pi.core.id.KoalaIdFactory;
import com.bt.pi.core.id.PId;
import com.bt.pi.core.parser.KoalaJsonParser;
import com.bt.pi.core.parser.KoalaPiEntityFactory;
import com.bt.pi.core.testing.UpdateResolvingContinuationAnswer;

@RunWith(MockitoJUnitRunner.class)
public class AddressAssignmentExecutorTest {
    private static final String ALLOCATED_PUBLIC_ADDRESS_1 = "1.2.3.4";
    private static final String ALLOCATED_PUBLIC_ADDRESS_2 = "5.6.7.8";
    private static final String ALLOCATED_PUBLIC_ADDRESS_3 = "9.10.11.12";
    private static final String VNET_INTERFACE = "eth0";
    private static final String TEST_PUBLIC_IP = "111.222.222.111";
    private String instanceWithNoPublicIp = "i-noPublicIpInstance";
    private String instanceId2 = "i-nstanceId2";
    private String instanceId3 = "i-nstanceId3";
    private String instanceId4 = "i-nstanceId4";
    private AddressAssignmentExecutor addressAssignmentExecutor;
    private KoalaIdFactory koalaIdFactory;
    private PiIdBuilder piIdBuilder;
    private SecurityGroup securityGroup1;
    private SecurityGroup securityGroup2;
    private List<SecurityGroup> securityGroups;
    private UpdateResolvingContinuationAnswer securityGroupAnswer;
    @Mock
    IpTablesManager ipTablesManager;
    @Mock
    ConsumedDhtResourceRegistry consumedDhtResourceRegistry;
    @Mock
    NetworkCommandRunner networkCommandRunner;
    @Mock
    GenericContinuation<Boolean> resultContinuation;

    @Before
    public void before() throws Exception {
        KoalaPiEntityFactory koalaPiEntityFactory = new KoalaPiEntityFactory();
        koalaPiEntityFactory.setKoalaJsonParser(new KoalaJsonParser());
        koalaPiEntityFactory.setPiEntityTypes(Arrays.asList(new PiEntity[] { new SecurityGroup() }));
        koalaIdFactory = new KoalaIdFactory(99, 99);
        koalaIdFactory.setKoalaPiEntityFactory(koalaPiEntityFactory);
        piIdBuilder = new PiIdBuilder();
        piIdBuilder.setKoalaIdFactory(koalaIdFactory);

        securityGroup1 = new SecurityGroup("bozo", "default", 10L, "172.0.0.0", "255.255.255.240", "147.149.2.5", new HashSet<NetworkRule>());
        securityGroup1.getInstances().put(instanceWithNoPublicIp, new InstanceAddress("172.0.0.3", null, "aa:aa:aa:aa:aa:aa"));
        securityGroup1.getInstances().put(instanceId2, new InstanceAddress("172.0.0.2", ALLOCATED_PUBLIC_ADDRESS_1, "aa:aa:aa:aa:aa:aa"));

        securityGroup2 = new SecurityGroup("nutter", "default", 11L, "172.0.0.16", "255.255.255.240", "147.149.2.5", new HashSet<NetworkRule>());
        securityGroup2.getInstances().put(instanceId3, new InstanceAddress("172.0.0.18", ALLOCATED_PUBLIC_ADDRESS_2, "aa:aa:aa:aa:aa:aa"));

        SecurityGroup securityGroup3 = new SecurityGroup("abc", "default", 12L, "172.0.0.32", null, null, new HashSet<NetworkRule>());
        securityGroup3.getInstances().put(instanceId4, new InstanceAddress("172.0.0.45", ALLOCATED_PUBLIC_ADDRESS_3, "d0:0d:4a:1d:08:f7"));

        securityGroups = new ArrayList<SecurityGroup>();
        securityGroups.add(securityGroup1);
        securityGroups.add(securityGroup2);
        securityGroups.add(securityGroup3);

        when(consumedDhtResourceRegistry.getByType(eq(SecurityGroup.class))).thenAnswer(new Answer<List<? extends PiEntity>>() {
            @Override
            public List<? extends PiEntity> answer(InvocationOnMock invocation) throws Throwable {
                return securityGroups;
            }
        });

        securityGroupAnswer = new UpdateResolvingContinuationAnswer(securityGroup1);

        addressAssignmentExecutor = new AddressAssignmentExecutor();
        addressAssignmentExecutor.setIpTablesManager(ipTablesManager);
        addressAssignmentExecutor.setNetworkCommandRunner(networkCommandRunner);
        addressAssignmentExecutor.setConsumedDhtResourceRegistry(consumedDhtResourceRegistry);
        addressAssignmentExecutor.setPiIdBuilder(piIdBuilder);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testAssignAddress() throws Exception {
        // setup
        doAnswer(securityGroupAnswer).when(consumedDhtResourceRegistry).update(eq(piIdBuilder.getPId(securityGroup1).forLocalRegion()), isA(UpdateResolvingContinuation.class));

        // act
        addressAssignmentExecutor.assignPublicIpAddressToInstance(TEST_PUBLIC_IP, instanceWithNoPublicIp, resultContinuation);

        // assert
        verify(this.networkCommandRunner).addIpAddressAndSendArping(TEST_PUBLIC_IP, VNET_INTERFACE);
        verify(this.ipTablesManager).refreshIpTables();
        // check security group set with ip.
        assertEquals(TEST_PUBLIC_IP, securityGroup1.getInstances().get(instanceWithNoPublicIp).getPublicIpAddress());
        verify(resultContinuation).handleResult(true);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testAssignAddressThrowsWhenNoAddressAssigned() throws Exception {
        // setup
        securityGroupAnswer.forceUpdateResult(null);
        doAnswer(securityGroupAnswer).when(consumedDhtResourceRegistry).update(eq(piIdBuilder.getPId(securityGroup1).forLocalRegion()), isA(UpdateResolvingContinuation.class));

        // act
        addressAssignmentExecutor.assignPublicIpAddressToInstance(TEST_PUBLIC_IP, instanceWithNoPublicIp, resultContinuation);

        // assert
        verify(this.networkCommandRunner, never()).ipAddressAdd(TEST_PUBLIC_IP, VNET_INTERFACE);
        verify(this.ipTablesManager, never()).refreshIpTables();
        verify(resultContinuation).handleResult(false);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testAssignAddressReturnsFalseResultWhenNoInstanceInSecGroups() throws Exception {
        // act
        addressAssignmentExecutor.assignPublicIpAddressToInstance(TEST_PUBLIC_IP, "weird-instance", resultContinuation);

        // assert
        verify(this.networkCommandRunner, never()).ipAddressAdd(TEST_PUBLIC_IP, VNET_INTERFACE);
        verify(this.ipTablesManager, never()).refreshIpTables();
        verify(this.consumedDhtResourceRegistry, never()).update(isA(PId.class), isA(UpdateResolvingContinuation.class));
        verify(resultContinuation).handleResult(false);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testUnassignAddress() throws Exception {
        // setup
        doAnswer(securityGroupAnswer).when(consumedDhtResourceRegistry).update(eq(piIdBuilder.getPId(securityGroup1.getUrl()).forLocalRegion()), isA(UpdateResolvingContinuation.class));

        // act
        addressAssignmentExecutor.unassignPublicIpAddressFromInstance(instanceId2, securityGroup1.getSecurityGroupId(), resultContinuation);

        // assert
        verify(networkCommandRunner).ipAddressDelete(ALLOCATED_PUBLIC_ADDRESS_1, VNET_INTERFACE);
        verify(this.ipTablesManager).refreshIpTables();
        assertEquals(null, securityGroup1.getInstances().get(instanceId2).getPublicIpAddress());
        verify(resultContinuation).handleResult(true);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testUnassignAddressForInstanceWithNoAddress() throws Exception {
        // setup
        doAnswer(securityGroupAnswer).when(consumedDhtResourceRegistry).update(eq(piIdBuilder.getPId(securityGroup1.getUrl()).forLocalRegion()), isA(UpdateResolvingContinuation.class));

        // act
        addressAssignmentExecutor.unassignPublicIpAddressFromInstance(instanceWithNoPublicIp, securityGroup1.getSecurityGroupId(), resultContinuation);

        // assert
        verify(networkCommandRunner, never()).ipAddressDelete(anyString(), anyString());
        verify(this.ipTablesManager, never()).refreshIpTables();
        verify(resultContinuation).handleResult(true);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testUnassignAddressWhenSecGroupUpdateFails() throws Exception {
        // setup
        doAnswer(securityGroupAnswer).when(consumedDhtResourceRegistry).update(eq(piIdBuilder.getPId(securityGroup1.getUrl()).forLocalRegion()), isA(UpdateResolvingContinuation.class));
        securityGroupAnswer.forceUpdateResult(null);

        // act
        addressAssignmentExecutor.unassignPublicIpAddressFromInstance(instanceId2, securityGroup1.getSecurityGroupId(), resultContinuation);

        // assert
        verify(networkCommandRunner, never()).ipAddressDelete(anyString(), anyString());
        verify(this.ipTablesManager, never()).refreshIpTables();
        verify(resultContinuation).handleResult(false);
    }
}
