package com.bt.pi.app.instancemanager.watchers;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.anyCollectionOf;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.argThat;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.isA;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.ScheduledExecutorService;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentMatcher;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.runners.MockitoJUnitRunner;
import org.mockito.stubbing.Answer;
import org.springframework.util.ReflectionUtils;

import rice.p2p.commonapi.Id;

import com.bt.pi.app.common.entities.Instance;
import com.bt.pi.app.common.entities.InstanceActivityState;
import com.bt.pi.app.common.entities.InstanceState;
import com.bt.pi.app.common.entities.User;
import com.bt.pi.app.common.id.PiIdBuilder;
import com.bt.pi.app.instancemanager.handlers.PauseInstanceServiceHelper;
import com.bt.pi.app.instancemanager.handlers.TerminateInstanceServiceHelper;
import com.bt.pi.core.application.watcher.service.WatcherService;
import com.bt.pi.core.continuation.PiContinuation;
import com.bt.pi.core.continuation.UpdateResolvingContinuation;
import com.bt.pi.core.continuation.scattergather.ScatterGatherContinuationRunnable;
import com.bt.pi.core.continuation.scattergather.ScatterGatherContinuationRunner;
import com.bt.pi.core.dht.DhtClientFactory;
import com.bt.pi.core.dht.DhtReader;
import com.bt.pi.core.dht.DhtWriter;
import com.bt.pi.core.id.KoalaIdFactory;
import com.bt.pi.core.id.PId;
import com.bt.pi.core.mail.MailSender;
import com.bt.pi.core.util.template.TemplateHelper;

import freemarker.template.TemplateException;

@RunWith(MockitoJUnitRunner.class)
public class UsersInstanceValidationWatcherTest {
    @InjectMocks
    private UsersInstanceValidationWatcher usersInstanceValidationWatcher = new UsersInstanceValidationWatcher();
    @Mock
    private LocalStorageUserHandler localStorageUserHandler;
    @Mock
    private Id id;
    @Mock
    private MailSender mailSender;
    private String subject = "your instances need validating";
    private String emailAddress = "fred@bloggs.com";
    @Mock
    private KoalaIdFactory koalaIdFactory;
    @Mock
    private DhtClientFactory dhtClientFactory;
    @Mock
    private PId userPId1;
    @Mock
    private PId userPId2;
    @Mock
    private DhtReader reader;
    @Mock
    private User user1;
    @Mock
    private User user2;
    private String instanceId1 = "i-00001234";
    @Mock
    private PiIdBuilder piIdBuilder;
    @Mock
    private PId instancePId1;
    private Instance instance1 = new Instance();
    private ScatterGatherContinuationRunner scatterGatherContinuationRunner = new ScatterGatherContinuationRunner();
    @Mock
    private ScheduledExecutorService scheduledExecutorService;
    @Mock
    private DhtWriter writer;
    @Mock
    private PId piNodeId;
    private String[] instanceIds1 = new String[] { instanceId1 };
    private String nodeId = "1234567890";
    private String username = "testuser1";
    private String userPidString = "000012385712578271258975892";
    private String opsWebsiteDnsName = "mydomain.com";
    @Mock
    private TemplateHelper templateHelper;
    private String text = "dummy email text";
    @Mock
    private Executor taskExecutor;
    @Mock
    private WatcherService watcherService;
    @Mock
    private TerminateInstanceServiceHelper terminateInstanceServiceHelper;
    @Mock
    private PauseInstanceServiceHelper pauseInstanceServiceHelper;

    @SuppressWarnings({ "unchecked", "rawtypes" })
    @Before
    public void before() throws IOException, TemplateException {
        usersInstanceValidationWatcher.setSubject(subject);
        usersInstanceValidationWatcher.setValidationMillis(28L * 24L * 60L * 60L * 1000L);
        usersInstanceValidationWatcher.setOpsWebsiteDnsName(opsWebsiteDnsName);

        when(user1.getEmailAddress()).thenReturn(emailAddress);
        instance1.setInstanceId(instanceId1);
        instance1.setNodeId(nodeId);
        instance1.setState(InstanceState.RUNNING);
        when(user1.getInstanceIds()).thenReturn(instanceIds1);
        when(user1.getUsername()).thenReturn(username);
        when(userPId1.toStringFull()).thenReturn(userPidString);

        scatterGatherContinuationRunner.setScheduledExecutorService(scheduledExecutorService);
        doAnswer(new Answer<Object>() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                ScatterGatherContinuationRunnable scatterGatherContinuationRunnable = (ScatterGatherContinuationRunnable) invocation.getArguments()[0];
                scatterGatherContinuationRunnable.run();
                return null;
            }
        }).when(scheduledExecutorService).execute(isA(ScatterGatherContinuationRunnable.class));

        setField(usersInstanceValidationWatcher, "scatterGatherContinuationRunner", scatterGatherContinuationRunner);

        when(koalaIdFactory.convertToPId(id)).thenReturn(userPId1);
        when(dhtClientFactory.createReader()).thenReturn(reader);
        when(dhtClientFactory.createWriter()).thenReturn(writer);
        when(piIdBuilder.getPIdForEc2AvailabilityZone(eq(Instance.getUrl(instanceId1)))).thenReturn(instancePId1);
        when(piIdBuilder.getNodeIdFromNodeId(nodeId)).thenReturn(piNodeId);
        when(piIdBuilder.getPId(user1)).thenReturn(userPId1);
        when(piIdBuilder.getPId(user2)).thenReturn(userPId2);

        doAnswer(new Answer<Object>() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                PiContinuation piContinuation = (PiContinuation) invocation.getArguments()[1];
                piContinuation.handleResult(user1);
                return null;
            }
        }).when(reader).getAsync(eq(userPId1), isA(PiContinuation.class));

        doAnswer(new Answer<Object>() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                PiContinuation piContinuation = (PiContinuation) invocation.getArguments()[1];
                piContinuation.handleResult(user2);
                return null;
            }
        }).when(reader).getAsync(eq(userPId2), isA(PiContinuation.class));

        doAnswer(new Answer<Object>() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                UpdateResolvingContinuation piContinuation = (UpdateResolvingContinuation) invocation.getArguments()[1];
                Object updated = piContinuation.update(instance1, null);
                piContinuation.receiveResult(updated);
                return null;
            }
        }).when(writer).update(eq(instancePId1), isA(UpdateResolvingContinuation.class));

        when(templateHelper.generate(isA(String.class), isA(Map.class))).thenReturn(text);

        doAnswer(new Answer<Object>() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                ((Runnable) invocation.getArguments()[0]).run();
                return null;
            }
        }).when(taskExecutor).execute(isA(Runnable.class));
        when(localStorageUserHandler.getUserPIds()).thenReturn(new HashSet<PId>(Arrays.asList(new PId[] { userPId1 })));
    }

    private void setField(Object target, String fieldName, Object value) {
        Field field = ReflectionUtils.findField(target.getClass(), fieldName);
        ReflectionUtils.makeAccessible(field);
        ReflectionUtils.setField(field, target, value);
    }

    @Test
    public void shouldAddToWatcherService() {
        // setup
        long initialIntervalMillis = 34;
        long repeatingIntervalMillis = 78;
        this.usersInstanceValidationWatcher.setInitialIntervalMillis(initialIntervalMillis);
        this.usersInstanceValidationWatcher.setRepeatingIntervalMillis(repeatingIntervalMillis);

        // act
        this.usersInstanceValidationWatcher.postConstruct();

        // assert
        verify(watcherService, atLeast(1)).replaceTask(UsersInstanceValidationWatcher.class.getSimpleName(), usersInstanceValidationWatcher, initialIntervalMillis, repeatingIntervalMillis);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void shouldSendEmailToUserWithExpiredInstanceActivityState() {
        // setup
        instance1.setInstanceActivityState(InstanceActivityState.GREEN);
        setField(instance1, "instanceActivityStateChangeTimestamp", 1000);

        // act
        usersInstanceValidationWatcher.run();

        // assert
        verify(mailSender).send(emailAddress, subject, text);
        assertEquals(InstanceActivityState.AMBER, instance1.getInstanceActivityState());
        assertTrue(System.currentTimeMillis() - 500 < instance1.getInstanceActivityStateChangeTimestamp());
        verify(templateHelper).generate(eq("instancevalidation.email.ftl"), argThat(new ArgumentMatcher<Map<String, Object>>() {
            @Override
            public boolean matches(Object argument) {
                Map<String, Object> map = (Map<String, Object>) argument;
                return map.get("user").equals(user1) && map.get("userPid").equals(userPidString) && map.get("ops_website_dns_name").equals(opsWebsiteDnsName);
            }
        }));
    }

    @Test
    public void shouldIgnoreInstanceThatHasBeenGreenForLessThan28Days() {
        // setup
        instance1.setInstanceActivityState(InstanceActivityState.GREEN);
        setField(instance1, "instanceActivityStateChangeTimestamp", System.currentTimeMillis() - (27L * 24L * 60L * 60L * 1000L));
        long timestamp = instance1.getInstanceActivityStateChangeTimestamp();

        // act
        usersInstanceValidationWatcher.run();

        // assert
        verify(mailSender, never()).send(anyString(), anyString(), anyString());
        assertEquals(InstanceActivityState.GREEN, instance1.getInstanceActivityState());
        assertEquals(timestamp, instance1.getInstanceActivityStateChangeTimestamp());
    }

    @Test
    public void shouldIgnoreInstanceThatIsNotRunning() {
        // setup
        instance1.setInstanceActivityState(InstanceActivityState.GREEN);
        instance1.setState(InstanceState.FAILED);
        setField(instance1, "instanceActivityStateChangeTimestamp", System.currentTimeMillis() - (30L * 24L * 60L * 60L * 1000L));
        long timestamp = instance1.getInstanceActivityStateChangeTimestamp();

        // act
        usersInstanceValidationWatcher.run();

        // assert
        verify(mailSender, never()).send(anyString(), anyString(), anyString());
        assertEquals(InstanceActivityState.GREEN, instance1.getInstanceActivityState());
        assertEquals(timestamp, instance1.getInstanceActivityStateChangeTimestamp());
    }

    @Test
    public void shouldPauseInstanceThatHasBeenAmberForMoreThan16Days() {
        // setup
        instance1.setInstanceActivityState(InstanceActivityState.AMBER);
        setField(instance1, "instanceActivityStateChangeTimestamp", System.currentTimeMillis() - (17L * 24L * 60L * 60L * 1000L));

        // act
        usersInstanceValidationWatcher.run();

        // assert
        verify(mailSender, never()).send(anyString(), anyString(), anyString());
        assertEquals(InstanceActivityState.RED, instance1.getInstanceActivityState());
        assertTrue(System.currentTimeMillis() - 100 < instance1.getInstanceActivityStateChangeTimestamp());
        verify(pauseInstanceServiceHelper).pauseInstance(instance1);
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    @Test
    public void shouldPauseMultipleInstances() {
        // setup
        when(localStorageUserHandler.getUserPIds()).thenReturn(new HashSet<PId>(Arrays.asList(new PId[] { userPId1 })));
        instance1.setInstanceActivityState(InstanceActivityState.AMBER);
        setField(instance1, "instanceActivityStateChangeTimestamp", System.currentTimeMillis() - (17L * 24L * 60L * 60L * 1000L));

        final Instance instance2 = new Instance();
        final String instanceId2 = "i-2222222";
        instance2.setNodeId(nodeId);
        instance2.setInstanceActivityState(InstanceActivityState.AMBER);
        instance2.setInstanceId(instanceId2);
        instance2.setState(InstanceState.RUNNING);
        setField(instance2, "instanceActivityStateChangeTimestamp", System.currentTimeMillis() - (17L * 24L * 60L * 60L * 1000L));
        String[] instanceIds2 = new String[] { instanceId2 };
        when(user2.getInstanceIds()).thenReturn(instanceIds2);
        PId instancePId2 = mock(PId.class);
        when(piIdBuilder.getPIdForEc2AvailabilityZone(eq(Instance.getUrl(instanceId2)))).thenReturn(instancePId2);
        doAnswer(new Answer<Object>() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                UpdateResolvingContinuation piContinuation = (UpdateResolvingContinuation) invocation.getArguments()[1];
                Object updated = piContinuation.update(instance2, null);
                piContinuation.receiveResult(updated);
                return null;
            }
        }).when(writer).update(eq(instancePId2), isA(UpdateResolvingContinuation.class));
        when(localStorageUserHandler.getUserPIds()).thenReturn(new HashSet(Arrays.asList(new PId[] { userPId1, userPId2 })));

        // act
        usersInstanceValidationWatcher.run();

        // assert
        verify(mailSender, never()).send(anyString(), anyString(), anyString());
        assertEquals(InstanceActivityState.RED, instance1.getInstanceActivityState());
        assertTrue(System.currentTimeMillis() - 100 < instance1.getInstanceActivityStateChangeTimestamp());
        verify(pauseInstanceServiceHelper).pauseInstance(instance1);
        verify(pauseInstanceServiceHelper).pauseInstance(instance2);

        assertEquals(InstanceActivityState.RED, instance2.getInstanceActivityState());
        assertTrue(System.currentTimeMillis() - 100 < instance2.getInstanceActivityStateChangeTimestamp());
    }

    @Test
    public void shouldIgnoreInstanceThatHasBeenAmberForLessThan16Days() {
        // setup
        instance1.setInstanceActivityState(InstanceActivityState.AMBER);
        setField(instance1, "instanceActivityStateChangeTimestamp", System.currentTimeMillis() - (15L * 24L * 60L * 60L * 1000L));
        long timestamp = instance1.getInstanceActivityStateChangeTimestamp();

        // act
        usersInstanceValidationWatcher.run();

        // assert
        verify(mailSender, never()).send(anyString(), anyString(), anyString());
        assertEquals(InstanceActivityState.AMBER, instance1.getInstanceActivityState());
        assertEquals(timestamp, instance1.getInstanceActivityStateChangeTimestamp());
        verify(pauseInstanceServiceHelper, never()).pauseInstance(instance1);
    }

    @Test
    public void shouldTerminateInstanceThatHasBeenRedForMoreThan5Days() {
        // setup
        instance1.setInstanceActivityState(InstanceActivityState.RED);
        setField(instance1, "instanceActivityStateChangeTimestamp", System.currentTimeMillis() - (6L * 24L * 60L * 60L * 1000L));
        long timestamp = instance1.getInstanceActivityStateChangeTimestamp();

        // act
        usersInstanceValidationWatcher.run();

        // assert
        verify(mailSender, never()).send(anyString(), anyString(), anyString());
        assertEquals(InstanceActivityState.RED, instance1.getInstanceActivityState());
        assertEquals(timestamp, instance1.getInstanceActivityStateChangeTimestamp());
        verify(terminateInstanceServiceHelper).terminateInstance(username, Arrays.asList(new String[] { instanceId1 }));
    }

    @Test
    public void shouldIgnoreInstanceThatHasBeenRedForLessThan5Days() {
        // setup
        instance1.setInstanceActivityState(InstanceActivityState.RED);
        setField(instance1, "instanceActivityStateChangeTimestamp", System.currentTimeMillis() - (4L * 24L * 60L * 60L * 1000L));
        long timestamp = instance1.getInstanceActivityStateChangeTimestamp();

        // act
        usersInstanceValidationWatcher.run();

        // assert
        verify(mailSender, never()).send(anyString(), anyString(), anyString());
        assertEquals(InstanceActivityState.RED, instance1.getInstanceActivityState());
        assertEquals(timestamp, instance1.getInstanceActivityStateChangeTimestamp());
        verify(terminateInstanceServiceHelper, never()).terminateInstance(eq(username), anyCollectionOf(String.class));
    }
}
