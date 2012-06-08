package com.bt.pi.api.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.isA;
import static org.mockito.Matchers.isNull;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.runners.MockitoJUnitRunner;
import org.mockito.stubbing.Answer;

import com.bt.pi.api.service.SecurityGroupServiceHelper.SecurityGroupNetworkRuleResolver;
import com.bt.pi.app.common.entities.Instance;
import com.bt.pi.app.common.entities.InstanceState;
import com.bt.pi.app.common.entities.NetworkProtocol;
import com.bt.pi.app.common.entities.NetworkRule;
import com.bt.pi.app.common.entities.NetworkRuleType;
import com.bt.pi.app.common.entities.OwnerIdGroupNamePair;
import com.bt.pi.app.common.entities.SecurityGroup;
import com.bt.pi.app.common.entities.User;
import com.bt.pi.app.common.id.PiIdBuilder;
import com.bt.pi.app.common.resource.PiQueue;
import com.bt.pi.app.common.resource.PiTopics;
import com.bt.pi.core.application.PubSubMessageContext;
import com.bt.pi.core.application.watcher.task.TaskProcessingQueueContinuation;
import com.bt.pi.core.application.watcher.task.TaskProcessingQueueHelper;
import com.bt.pi.core.continuation.UpdateResolver;
import com.bt.pi.core.dht.BlockingDhtReader;
import com.bt.pi.core.dht.BlockingDhtWriter;
import com.bt.pi.core.dht.DhtClientFactory;
import com.bt.pi.core.entity.EntityMethod;
import com.bt.pi.core.entity.PiEntity;
import com.bt.pi.core.id.PId;
import com.bt.pi.core.testing.UpdateResolverAnswer;

@RunWith(MockitoJUnitRunner.class)
public class SecurityGroupServiceImplTest {
    @InjectMocks
    private SecurityGroupServiceImpl securityGroupService = new SecurityGroupServiceImpl();
    private String ownerId = "cloud-user";
    private String groupName = "default";
    private String secondaryGroupName = "secondaryGroupName";
    private String groupDescription = "my default security group";
    private SecurityGroup securityGroup, secondarySecurityGroup;
    private List<NetworkRule> networkRules;
    @Mock
    private PubSubMessageContext pubSubMessageContext;
    private User user;
    @Mock
    private PId securityGroupPiId;
    @Mock
    private PId securityGroup2PiId;
    @Mock
    private PId userPiId;
    @Mock
    private BlockingDhtReader blockingDhtReader;
    @Mock
    private BlockingDhtWriter blockingDhtWriter;
    @Mock
    private DhtClientFactory dhtClientFactory;
    @Mock
    private UserService userService;
    @Mock
    private UserManagementService userManagementService;
    @Mock
    private PiIdBuilder piIdBuilder;
    @Mock
    private ApiApplicationManager apiApplicationManager;
    @InjectMocks
    private SecurityGroupServiceHelper securityGroupServiceHelper = new SecurityGroupServiceHelper();
    @Mock
    TaskProcessingQueueHelper taskProcessingQueueHelper;
    @Mock
    private PId updateSecurityGroupQueueId;
    @Mock
    private PId removeSecurityGroupQueueId;
    private CountDownLatch continuationLatch;

    @Before
    public void before() {
        securityGroup = new SecurityGroup(ownerId, groupName);
        secondarySecurityGroup = new SecurityGroup(ownerId, secondaryGroupName);
        setupUserService();
        setupPiIdBuilder();
        setupDhtReader();
        setupDhtWriter(securityGroup);
        setupApiApplicationManager();
        setupTaskProcessingQueueHelper();
        securityGroupService.setSecurityGroupServiceHelper(this.securityGroupServiceHelper);
    }

    private void setupTaskProcessingQueueHelper() {
        this.continuationLatch = new CountDownLatch(1);

        doAnswer(new Answer<Object>() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                ((TaskProcessingQueueContinuation) invocation.getArguments()[3]).receiveResult(null, null);
                continuationLatch.countDown();
                return null;
            }
        }).when(taskProcessingQueueHelper).addUrlToQueue(isA(PId.class), isA(String.class), anyInt(), isA(TaskProcessingQueueContinuation.class));
    }

    private void setupUserService() {
        user = new User(ownerId, "accesskey", "secretkey");
        user.getSecurityGroupIds().add("default");
        user.getSecurityGroupIds().add(secondaryGroupName);
        when(userManagementService.getUser(ownerId)).thenReturn(user);
    }

    private void setupPiIdBuilder() {
        when(piIdBuilder.getPId(SecurityGroup.getUrl(ownerId, groupName))).thenReturn(securityGroupPiId);
        when(piIdBuilder.getPId(SecurityGroup.getUrl(ownerId, secondaryGroupName))).thenReturn(securityGroup2PiId);
        when(securityGroupPiId.forLocalRegion()).thenReturn(securityGroupPiId);
        when(securityGroup2PiId.forLocalRegion()).thenReturn(securityGroup2PiId);
        when(piIdBuilder.getPId(User.getUrl(ownerId))).thenReturn(userPiId);
        when(piIdBuilder.getPiQueuePId(PiQueue.UPDATE_SECURITY_GROUP)).thenReturn(updateSecurityGroupQueueId);
        when(updateSecurityGroupQueueId.forLocalScope(PiQueue.UPDATE_SECURITY_GROUP.getNodeScope())).thenReturn(updateSecurityGroupQueueId);
        when(piIdBuilder.getPiQueuePId(PiQueue.REMOVE_SECURITY_GROUP)).thenReturn(removeSecurityGroupQueueId);
        when(removeSecurityGroupQueueId.forLocalScope(PiQueue.REMOVE_SECURITY_GROUP.getNodeScope())).thenReturn(removeSecurityGroupQueueId);
        securityGroupService.setPiIdBuilder(piIdBuilder);
    }

    @SuppressWarnings("unchecked")
    private void setupDhtWriter(PiEntity existing) {
        final UpdateResolverAnswer ans = new UpdateResolverAnswer(existing);
        doAnswer(ans).when(blockingDhtWriter).update(isA(PId.class), (PiEntity) any(), isA(UpdateResolver.class));
        when(blockingDhtWriter.getValueWritten()).thenAnswer(new Answer<PiEntity>() {
            public PiEntity answer(InvocationOnMock invocation) throws Throwable {
                return ans.getResult();
            }
        });
        when(dhtClientFactory.createBlockingWriter()).thenReturn(blockingDhtWriter);
    }

    private void setupDhtReader() {
        when(dhtClientFactory.createBlockingReader()).thenReturn(blockingDhtReader);
        when(blockingDhtReader.get(securityGroupPiId)).thenReturn(securityGroup);
        when(blockingDhtReader.get(securityGroup2PiId)).thenReturn(secondarySecurityGroup);
    }

    private void setupApiApplicationManager() {
        when(apiApplicationManager.newLocalPubSubMessageContext(PiTopics.NETWORK_MANAGERS_IN_REGION)).thenReturn(pubSubMessageContext);
    }

    @Before
    public void setupNetworkRules() {
        networkRules = new ArrayList<NetworkRule>();
        networkRules.add(createNetworkRule(40, 20));
        networkRules.add(createNetworkRule(800, 800));
        networkRules.add(createNetworkRule(1300, 1200));
    }

    private NetworkRule createNetworkRule(int maxPort, int minPort) {
        NetworkRule networkRule = new NetworkRule();
        networkRule.setDestinationSecurityGroupName("aDestinationSecurityGroupName");
        networkRule.setNetworkProtocol(NetworkProtocol.TCP);
        networkRule.setNetworkRuleType(NetworkRuleType.FIREWALL_OPEN);
        networkRule.setOwnerIdGroupNamePair(new OwnerIdGroupNamePair[] {});
        networkRule.setPortRangeMax(maxPort);
        networkRule.setPortRangeMin(minPort);
        networkRule.setSourceNetworks(new String[] {});
        return networkRule;
    }

    @Test
    public void testCreateSecurityGroup() {
        // setup
        setupDhtWriter(null);

        // act
        boolean result = securityGroupService.createSecurityGroup(ownerId, groupName, groupDescription);

        // assert
        assertTrue(result);
    }

    @Test
    public void testCreateSecurityGroupWhenGroupAlreadyDeleted() {
        // setup
        securityGroup.setDeleted(true);
        setupDhtWriter(securityGroup);

        // act
        boolean result = securityGroupService.createSecurityGroup(ownerId, groupName, groupDescription);

        // assert
        assertTrue(result);
    }

    @Test
    public void testCreateSecurityGroupUpdateUserSecurityGroupCollection() {
        // setup
        user.getSecurityGroupIds().remove(groupName);
        doAnswer(new Answer<Object>() {
            public Object answer(InvocationOnMock invocation) throws Throwable {
                user.getSecurityGroupIds().add((String) invocation.getArguments()[1]);
                return null;
            }
        }).when(userService).addSecurityGroupToUser(ownerId, groupName);
        when(blockingDhtWriter.getValueWritten()).thenReturn(user);

        // act
        boolean result = securityGroupService.createSecurityGroup(ownerId, groupName, groupDescription);

        // assert
        assertTrue(result);
        assertEquals(2, user.getSecurityGroupIds().size());
    }

    @Test
    public void testDescribeSecurityGroups() {
        // setup
        List<String> securityGroupsIds = new ArrayList<String>();

        // act
        List<SecurityGroup> securityGroups = securityGroupService.describeSecurityGroups(ownerId, securityGroupsIds);

        // assert
        assertEquals(2, securityGroups.size());
        assertEquals("default", securityGroups.get(0).getOwnerIdGroupNamePair().getGroupName());
        assertEquals(secondaryGroupName, securityGroups.get(1).getOwnerIdGroupNamePair().getGroupName());
    }

    @Test
    public void testDescribeSecurityGroupForASpecificSecurityGroup() {
        // setup
        List<String> securityGroupsIds = new ArrayList<String>();
        securityGroupsIds.add("default");

        // act
        List<SecurityGroup> describeSecurityGroups = securityGroupService.describeSecurityGroups(ownerId, securityGroupsIds);

        // assert
        assertEquals(1, describeSecurityGroups.size());
    }

    @Test
    public void testIgnoreAnyInvalidSecurityGroupNames() {
        // setup
        List<String> securityGroupNames = new ArrayList<String>();
        securityGroupNames.add("default");
        securityGroupNames.add("InvalidSecurityGroupName");

        // act
        List<SecurityGroup> describeSecurityGroups = securityGroupService.describeSecurityGroups(ownerId, securityGroupNames);
        assertEquals(1, describeSecurityGroups.size());
    }

    @Test
    public void testDescribeSecurityGroupsNull() {
        // setup

        // act
        List<SecurityGroup> securityGroups = securityGroupService.describeSecurityGroups(ownerId, null);

        // assert
        assertEquals(2, securityGroups.size());
        assertEquals("default", securityGroups.get(0).getOwnerIdGroupNamePair().getGroupName());
    }

    @Test
    public void testAuthoriseIngressAddsNewRulesToSecurityGroupWithNoRules() throws Exception {
        // setup
        doAnswer(new Answer<Object>() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                SecurityGroup updated = (SecurityGroup) ((SecurityGroupNetworkRuleResolver) invocation.getArguments()[2]).update(securityGroup, securityGroup);
                List<NetworkRule> updatedRules = new ArrayList<NetworkRule>(updated.getNetworkRules());
                assertEquals(networkRules.size(), updatedRules.size());
                assertTrue(updatedRules.containsAll(networkRules));
                return null;
            }
        }).when(blockingDhtWriter).update(eq(securityGroupPiId), (SecurityGroup) isNull(), isA(SecurityGroupNetworkRuleResolver.class));
        when(blockingDhtWriter.getValueWritten()).thenReturn(securityGroup);

        // act
        boolean result = securityGroupService.authoriseIngress(ownerId, groupName, networkRules);

        // assert
        assertTrue(this.continuationLatch.await(250, TimeUnit.MILLISECONDS));
        assertTrue(result);
        verify(blockingDhtWriter).update(eq(securityGroupPiId), (SecurityGroup) isNull(), isA(SecurityGroupNetworkRuleResolver.class));
        verify(pubSubMessageContext).publishContent(EntityMethod.UPDATE, securityGroup);
        verify(taskProcessingQueueHelper).addUrlToQueue(eq(updateSecurityGroupQueueId), eq(securityGroup.getUrl()), anyInt(), isA(TaskProcessingQueueContinuation.class));
        assertThatSecurityGroupFieldsAreNullForNetworkRulesUpdate();
    }

    private void assertThatSecurityGroupFieldsAreNullForNetworkRulesUpdate() {
        assertNull(securityGroup.getDescription());
        assertNull(securityGroup.getDnsAddress());
        assertNull(securityGroup.getInstances());
        assertNull(securityGroup.getNetmask());
        assertNull(securityGroup.getNetworkAddress());
        assertNull(securityGroup.getVlanId());

        assertNotNull(securityGroup.getSecurityGroupId());
        assertNotNull(securityGroup.getOwnerIdGroupNamePair());
        assertNotNull(securityGroup.getNetworkRules());
    }

    @Test
    public void testAuthoriseIngressAddsNewRulesToSecurityGroupWithExistingRules() throws Exception {
        // setup
        securityGroup.addNetworkRule(createNetworkRule(24000, 23998));
        securityGroup.addNetworkRule(createNetworkRule(4880, 4880));

        doAnswer(new Answer<Object>() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                SecurityGroup updated = (SecurityGroup) ((SecurityGroupNetworkRuleResolver) invocation.getArguments()[2]).update(securityGroup, securityGroup);
                List<NetworkRule> updatedRules = new ArrayList<NetworkRule>(updated.getNetworkRules());
                assertEquals(networkRules.size() + 2, updatedRules.size());
                assertTrue(updatedRules.containsAll(networkRules));
                return null;
            }
        }).when(blockingDhtWriter).update(eq(securityGroupPiId), (SecurityGroup) isNull(), isA(SecurityGroupNetworkRuleResolver.class));
        when(blockingDhtWriter.getValueWritten()).thenReturn(securityGroup);

        // act
        boolean result = securityGroupService.authoriseIngress(ownerId, groupName, networkRules);

        // assert
        assertTrue(this.continuationLatch.await(250, TimeUnit.MILLISECONDS));
        assertTrue(result);
        verify(blockingDhtWriter).update(eq(securityGroupPiId), (SecurityGroup) isNull(), isA(SecurityGroupNetworkRuleResolver.class));
        verify(pubSubMessageContext).publishContent(EntityMethod.UPDATE, securityGroup);
        assertThatSecurityGroupFieldsAreNullForNetworkRulesUpdate();
    }

    @Test
    public void testAuthoriseIngressAddsNewRulesToSecurityGroupWithExistingOverlappingRules() throws Exception {
        // setup
        securityGroup.addNetworkRule(createNetworkRule(24000, 23998));
        securityGroup.addNetworkRule(networkRules.get(0));

        doAnswer(new Answer<Object>() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                SecurityGroup updated = (SecurityGroup) ((SecurityGroupNetworkRuleResolver) invocation.getArguments()[2]).update(securityGroup, securityGroup);
                List<NetworkRule> updatedRules = new ArrayList<NetworkRule>(updated.getNetworkRules());
                assertEquals(networkRules.size() + 1, updatedRules.size());
                assertTrue(updatedRules.containsAll(networkRules));
                return null;
            }
        }).when(blockingDhtWriter).update(eq(securityGroupPiId), (SecurityGroup) isNull(), isA(SecurityGroupNetworkRuleResolver.class));
        when(blockingDhtWriter.getValueWritten()).thenReturn(securityGroup);

        // act
        boolean result = securityGroupService.authoriseIngress(ownerId, groupName, networkRules);

        // assert
        assertTrue(this.continuationLatch.await(250, TimeUnit.MILLISECONDS));
        assertTrue(result);
        verify(blockingDhtWriter).update(eq(securityGroupPiId), (SecurityGroup) isNull(), isA(SecurityGroupNetworkRuleResolver.class));
        verify(pubSubMessageContext).publishContent(EntityMethod.UPDATE, securityGroup);
        assertThatSecurityGroupFieldsAreNullForNetworkRulesUpdate();
    }

    @Test(expected = NotFoundException.class)
    public void testAuthoriseIngressFailsWhenSecurityGroupDoesNotExist() throws Exception {
        // setup
        setupDhtWriter(null);
        doAnswer(new Answer<Object>() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                PiEntity updated = ((SecurityGroupNetworkRuleResolver) invocation.getArguments()[2]).update(null, securityGroup);
                assertNull(updated);
                return null;
            }
        }).when(blockingDhtWriter).update(eq(securityGroupPiId), (SecurityGroup) isNull(), isA(SecurityGroupNetworkRuleResolver.class));

        // act
        securityGroupService.authoriseIngress(ownerId, groupName, networkRules);
    }

    @Test
    public void testRevokeIngressRemovesNewRulesToSecurityGroupWithExistingRules() throws Exception {
        // setup
        securityGroup.addNetworkRule(createNetworkRule(24000, 23998));
        securityGroup.addNetworkRule(createNetworkRule(4880, 4880));

        doAnswer(new Answer<Object>() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                SecurityGroup updated = (SecurityGroup) ((SecurityGroupNetworkRuleResolver) invocation.getArguments()[2]).update(securityGroup, securityGroup);
                List<NetworkRule> updatedRules = new ArrayList<NetworkRule>(updated.getNetworkRules());
                assertEquals(2, updatedRules.size());
                assertFalse("Rules should not be present.", updatedRules.containsAll(networkRules));
                return null;
            }
        }).when(blockingDhtWriter).update(eq(securityGroupPiId), (SecurityGroup) isNull(), isA(SecurityGroupNetworkRuleResolver.class));
        when(blockingDhtWriter.getValueWritten()).thenReturn(securityGroup);

        // act
        boolean result = securityGroupService.revokeIngress(ownerId, groupName, networkRules);

        // assert
        assertTrue(this.continuationLatch.await(250, TimeUnit.MILLISECONDS));
        assertTrue(result);
        verify(blockingDhtWriter).update(eq(securityGroupPiId), (SecurityGroup) isNull(), isA(SecurityGroupNetworkRuleResolver.class));
        verify(pubSubMessageContext).publishContent(EntityMethod.UPDATE, securityGroup);
        assertThatSecurityGroupFieldsAreNullForNetworkRulesUpdate();
    }

    @Test(expected = NotFoundException.class)
    public void testRevokeIngressForNonExistentGroup() throws Exception {
        when(blockingDhtWriter.getValueWritten()).thenReturn(null);

        // act
        securityGroupService.revokeIngress(ownerId, groupName, networkRules);
    }

    @Test
    public void shouldReturnFalseWhenSecurityGroupNameIsNull() {
        assertFalse(securityGroupService.deleteSecurityGroup(ownerId, null));
    }

    @Test
    public void shouldRetrunFalseWhenSecurityGroupNameIsBlank() {
        assertFalse(securityGroupService.deleteSecurityGroup(ownerId, ""));
    }

    @Test
    public void shouldReturnFalseWhenSecurityGroupNameNotInGroup() {
        assertFalse(securityGroupService.deleteSecurityGroup(ownerId, "unknownGroup"));
    }

    @Test
    public void shouldDeleteSecurityGroup() throws InterruptedException {
        // setup
        setupDhtWriter(secondarySecurityGroup);
        secondarySecurityGroup.addNetworkRule(createNetworkRule(0, 0));
        secondarySecurityGroup.addNetworkRule(createNetworkRule(80, 80));

        doAnswer(new Answer<Object>() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                user.getSecurityGroupIds().remove(invocation.getArguments()[1]);
                return null;
            }
        }).when(userService).removeSecurityGroupFromUser(ownerId, secondaryGroupName);

        assertEquals(2, user.getSecurityGroupIds().size());

        // act
        boolean result = securityGroupService.deleteSecurityGroup(ownerId, secondaryGroupName);

        // assert
        assertTrue(this.continuationLatch.await(250, TimeUnit.MILLISECONDS));
        assertTrue("Group was not deleted.", result);
        assertEquals(0, secondarySecurityGroup.getNetworkRules().size());
        assertTrue(secondarySecurityGroup.isDeleted());
        verify(pubSubMessageContext).publishContent(EntityMethod.DELETE, secondarySecurityGroup);
        assertEquals(1, user.getSecurityGroupIds().size());
    }

    @Test
    public void shouldHandleSecurityGroupNotFoundWhenDeletingSecurityGroup() throws InterruptedException {
        // setup
        setupDhtWriter(secondarySecurityGroup);
        secondarySecurityGroup.addNetworkRule(createNetworkRule(0, 0));
        secondarySecurityGroup.addNetworkRule(createNetworkRule(80, 80));

        doAnswer(new Answer<Object>() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                user.getSecurityGroupIds().remove(invocation.getArguments()[1]);
                return null;
            }
        }).when(userService).removeSecurityGroupFromUser(ownerId, secondaryGroupName);

        assertEquals(2, user.getSecurityGroupIds().size());

        when(blockingDhtReader.get(securityGroup2PiId)).thenReturn(null);

        // act
        boolean result = securityGroupService.deleteSecurityGroup(ownerId, secondaryGroupName);

        // assert
        assertTrue(result);
        verify(taskProcessingQueueHelper, never()).addUrlToQueue(isA(PId.class), isA(String.class), anyInt(), isA(TaskProcessingQueueContinuation.class));
        verify(pubSubMessageContext, never()).publishContent(EntityMethod.DELETE, secondarySecurityGroup);
        assertEquals(1, user.getSecurityGroupIds().size());
    }

    @Test
    public void shouldAddTaskToQueueOnDeleteSecurityGroup() throws InterruptedException {
        // setup

        // act
        securityGroupService.deleteSecurityGroup(ownerId, secondaryGroupName);

        // assert
        assertTrue(this.continuationLatch.await(250, TimeUnit.MILLISECONDS));
        verify(taskProcessingQueueHelper).addUrlToQueue(eq(removeSecurityGroupQueueId), eq(secondarySecurityGroup.getUrl()), eq(5), isA(TaskProcessingQueueContinuation.class));
    }

    @Test(expected = IllegalStateException.class)
    public void shouldThrowIllegalStateExceptionIfSecurityGroupHasInstanceWhichIsShuttingDownButNotTerminated() {
        // setup
        setupInstance("i-shutdown", InstanceState.SHUTTING_DOWN);
        secondarySecurityGroup.addInstance("i-shutdown", null, null, null);
        setupDhtReader();

        // act
        boolean res = securityGroupService.deleteSecurityGroup(ownerId, secondaryGroupName);

        // assert
        assertFalse(res);
    }

    private void setupInstance(String instanceId, InstanceState instanceState) {
        Instance i = new Instance();
        i.setInstanceId(instanceId);
        i.setState(instanceState);

        PId piId = mock(PId.class);

        when(securityGroupServiceHelper.getInstancePiId(instanceId)).thenReturn(piId);
        when(blockingDhtReader.get(piId)).thenReturn(i);

        secondarySecurityGroup.addInstance(instanceId, "172.0.0.1", "10.10.10.10", "macAddress");
    }
}
