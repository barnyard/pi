package com.bt.pi.app.networkmanager.dhcp;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.isA;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.runners.MockitoJUnitRunner;
import org.mockito.stubbing.Answer;

import com.bt.pi.app.common.entities.SecurityGroup;
import com.bt.pi.app.networkmanager.NetworkManagerApplication;
import com.bt.pi.core.application.activation.ApplicationRegistry;
import com.bt.pi.core.application.activation.ApplicationStatus;
import com.bt.pi.core.application.activation.UnknownApplicationException;
import com.bt.pi.core.application.resource.ConsumedDhtResourceRegistry;
import com.bt.pi.core.cli.commands.CommandExecutionException;
import com.bt.pi.core.entity.PiEntity;
import com.bt.pi.core.util.common.CommandResult;
import com.bt.pi.core.util.common.CommandRunner;

@RunWith(MockitoJUnitRunner.class)
public class DhcpManagerTest {
    private static final String KILL_COMMAND = "cat %s/pi-dhcp.pid | xargs kill -9";
    private static final String DHCP_CONFIG_STRING = "some-dhcp-config-string";
    private String dhcpDaemonPath = "/usr/sbin/dhcpd";
    private String netRuntimePath = System.getProperty("java.io.tmpdir");
    private List<SecurityGroup> securityGroups;
    private int failCount = 0;
    private int initialCount = 0;
    private String previousPiEnv;
    private CommandResult psGrepDhcpCommandResult;
    private String dhcpRunningLine;
    private List<String> psGrepDhcpCommandLines;
    private boolean restartInvoked;
    private CountDownLatch restartLatch;
    private LinkedBlockingQueue<DhcpRefreshToken> testQueue = new LinkedBlockingQueue<DhcpRefreshToken>();

    @Mock
    private File pidFileMock;
    @Mock
    private SecurityGroup securityGroup;
    @Mock
    private CommandRunner commandRunner;
    @Mock
    private DhcpConfigurationGenerator dhcpConfigurationGenerator;
    @Mock
    private ConsumedDhtResourceRegistry consumedDhtResourceRegistry;
    @Mock
    private ApplicationRegistry applicationRegistry;
    @Mock
    private DhcpUtils dhcpUtils;

    @InjectMocks
    private DhcpManager dhcpManager = new DhcpManager() {
        @Override
        protected void processRefreshQueue() throws InterruptedException {
            try {
                if (failCount > 0 && initialCount < 1) {
                    throw new RuntimeException("shit happens");
                }
            } finally {
                if (initialCount < 1)
                    failCount--;
                initialCount--;
            }
            super.processRefreshQueue();
        }

        @Override
        protected void restartDhcpDaemon(String dhcpDaemonPath, String netRuntimePath, List<SecurityGroup> securityGroups, boolean killDhcpDaemon) {
            restartInvoked = true;
            super.restartDhcpDaemon(dhcpDaemonPath, netRuntimePath, securityGroups, killDhcpDaemon);
            restartLatch.countDown();
        }
    };

    @SuppressWarnings("unchecked")
    @Before
    public void before() throws Exception {
        previousPiEnv = System.getProperty("koala.env");

        failCount = 0;
        initialCount = 0;

        restartInvoked = false;
        restartLatch = new CountDownLatch(2);

        when(securityGroup.getVlanId()).thenReturn(999L);

        securityGroups = new ArrayList<SecurityGroup>();
        securityGroups.add(securityGroup);

        when(dhcpConfigurationGenerator.generate(eq("pi-dhcp.conf.ftl"), isA(Collection.class))).thenReturn(DHCP_CONFIG_STRING);
        dhcpManager.setNetRuntimePath(netRuntimePath);
        dhcpManager.setVnetDhcpDaemon(dhcpDaemonPath);

        when(consumedDhtResourceRegistry.getByType(eq(SecurityGroup.class))).thenAnswer(new Answer<List<? extends PiEntity>>() {
            @Override
            public List<? extends PiEntity> answer(InvocationOnMock invocation) throws Throwable {
                return DhcpManagerTest.this.securityGroups;
            }
        });

        dhcpRunningLine = "/usr/sbin/dhcpd -cf /opt/koala/cluster/var/run/net/pi-dhcp.conf -lf /opt/koala/cluster/var/run/net/pi-dhcp.leases -pf /opt/koala/cluster/var/run/net/pi-dhcp.pid pibr93 pibr15";
        psGrepDhcpCommandLines = new ArrayList<String>();
        psGrepDhcpCommandLines.add(dhcpRunningLine);

        psGrepDhcpCommandResult = mock(CommandResult.class);
        when(psGrepDhcpCommandResult.getOutputLines()).thenReturn(psGrepDhcpCommandLines);

        when(commandRunner.runInShell("ps -ef | grep dhcp")).thenReturn(psGrepDhcpCommandResult);

        when(applicationRegistry.getApplicationStatus(NetworkManagerApplication.APPLICATION_NAME)).thenReturn(ApplicationStatus.ACTIVE);

        when(dhcpUtils.getFileIfExists(isA(String.class))).thenReturn(pidFileMock);
        doAnswer(new Answer<Object>() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                testQueue.add((DhcpRefreshToken) invocation.getArguments()[0]);
                return null;
            }
        }).when(dhcpUtils).addToDhcpRefreshQueue(isA(DhcpRefreshToken.class));
        doAnswer(new Answer<DhcpRefreshToken>() {
            @Override
            public DhcpRefreshToken answer(InvocationOnMock invocation) throws Throwable {
                return testQueue.poll(250, TimeUnit.MILLISECONDS);
            }
        }).when(dhcpUtils).pollDhcpRefreshQueue();
    }

    @After
    public void after() {
        if (previousPiEnv != null)
            System.setProperty("koala.env", previousPiEnv);
        else
            System.clearProperty("koala.env");
    }

    @Test
    public void shouldRestartDhcpDaemon() throws Exception {
        // act
        dhcpManager.restartDhcpDaemon(dhcpDaemonPath, netRuntimePath, securityGroups);

        // assert
        verify(commandRunner).runInShell(String.format(KILL_COMMAND, netRuntimePath));
        verify(commandRunner).run(String.format("%s" + " -cf %s/pi-dhcp.conf" + " -lf %s/pi-dhcp.leases -pf %s/pi-dhcp.pid" + " pibr999", dhcpDaemonPath, netRuntimePath, netRuntimePath, netRuntimePath));
        verify(dhcpUtils).writeConfigToFile(netRuntimePath + "/pi-dhcp.conf", DHCP_CONFIG_STRING);
    }

    @Test
    public void shouldKillDhcpDaemonIfNotActiveNetworkManager() throws Exception {
        // setup
        when(applicationRegistry.getApplicationStatus(NetworkManagerApplication.APPLICATION_NAME)).thenReturn(ApplicationStatus.PASSIVE);

        // act
        dhcpManager.bringUpDhcpDaemon();

        // assert
        verify(commandRunner).runInShell(String.format(KILL_COMMAND, netRuntimePath));
        verify(commandRunner, never()).run(String.format("%s" + " -cf %s/pi-dhcp.conf" + " -lf %s/pi-dhcp.leases -pf %s/pi-dhcp.pid" + " pibr999", dhcpDaemonPath, netRuntimePath, netRuntimePath, netRuntimePath));
    }

    @Test
    public void shouldKillDhcpDaemonIfNetworkManagerNotInApplicationRegistry() throws Exception {
        // setup
        when(applicationRegistry.getApplicationStatus(NetworkManagerApplication.APPLICATION_NAME)).thenThrow(new UnknownApplicationException(""));

        // act
        dhcpManager.bringUpDhcpDaemon();

        // assert
        verify(commandRunner).runInShell(String.format(KILL_COMMAND, netRuntimePath));
        verify(commandRunner, never()).run(String.format("%s" + " -cf %s/pi-dhcp.conf" + " -lf %s/pi-dhcp.leases -pf %s/pi-dhcp.pid" + " pibr999", dhcpDaemonPath, netRuntimePath, netRuntimePath, netRuntimePath));
    }

    @Test
    public void shouldNotWriteFileIfInIntegrationMode() throws Exception {
        // setup
        System.setProperty("koala.env", "integration");

        // act
        dhcpManager.restartDhcpDaemon(dhcpDaemonPath, netRuntimePath, securityGroups);

        // assert
        verify(dhcpUtils, never()).writeConfigToFile(netRuntimePath + "/pi-dhcp.conf", DHCP_CONFIG_STRING);
    }

    @Test
    public void shouldRestartDhcpDaemonWithKillIfNoPidFile() throws Exception {
        // setup
        when(dhcpUtils.getFileIfExists(isA(String.class))).thenReturn(null);

        // act
        dhcpManager.restartDhcpDaemon(dhcpDaemonPath, netRuntimePath, securityGroups);

        // assert
        verify(commandRunner, never()).runInShell(String.format(KILL_COMMAND, netRuntimePath));
        verify(commandRunner).run(String.format("%s" + " -cf %s/pi-dhcp.conf" + " -lf %s/pi-dhcp.leases -pf %s/pi-dhcp.pid" + " pibr999", dhcpDaemonPath, netRuntimePath, netRuntimePath, netRuntimePath));
        verify(dhcpUtils).writeConfigToFile(netRuntimePath + "/pi-dhcp.conf", DHCP_CONFIG_STRING);
    }

    @Test
    public void dhcpRequestPollerShouldPickUpRequestWhenAvailable() throws Exception {
        // setup
        dhcpManager.initialise();

        // act
        DhcpRefreshToken dhcpRefreshToken = dhcpManager.requestDhcpRefresh(securityGroups);
        dhcpRefreshToken.blockUntilRefreshCompleted();

        // assert
        verify(dhcpUtils, atLeastOnce()).writeConfigToFile(netRuntimePath + "/pi-dhcp.conf", DHCP_CONFIG_STRING);
    }

    @Test
    public void dhcpRequestPollerShouldBlockUntilComplete() throws Exception {
        // setup
        dhcpManager.initialise();

        // act
        DhcpRefreshToken res = dhcpManager.requestDhcpRefresh(securityGroups);
        res.blockUntilRefreshCompleted();

        // assert
        verify(dhcpUtils, atLeastOnce()).writeConfigToFile(netRuntimePath + "/pi-dhcp.conf", DHCP_CONFIG_STRING);
    }

    @Test(expected = DhcpRefreshFailedException.class)
    public void dhcpRequestPollerShouldDelegateExceptionToCallerIfRefreshFails() throws Exception {
        // setup
        doThrow(new CommandExecutionException("oops", Arrays.asList(new String[] { "bang" }))).when(commandRunner).runInShell(String.format(KILL_COMMAND, netRuntimePath));
        dhcpManager.initialise();

        // act
        DhcpRefreshToken dhcpRefreshToken = dhcpManager.requestDhcpRefresh(securityGroups);
        dhcpRefreshToken.blockUntilRefreshCompleted();
    }

    @Test
    public void shouldRestartDhcpDaemonIfInitialQueuePollFails() throws Exception {
        // setup
        this.initialCount = 1;
        this.failCount = 1;
        dhcpManager.initialise();
        DhcpRefreshToken dhcpRefreshToken = new DhcpRefreshToken(dhcpDaemonPath, netRuntimePath, securityGroups);
        testQueue.add(dhcpRefreshToken);

        // act
        Thread.sleep(1000);

        // assert
        verify(dhcpUtils, atLeastOnce()).touchFile(isA(String.class));
        verify(commandRunner, times(2)).run(String.format("%s" + " -cf %s/pi-dhcp.conf" + " -lf %s/pi-dhcp.leases -pf %s/pi-dhcp.pid" + " pibr999", dhcpDaemonPath, netRuntimePath, netRuntimePath, netRuntimePath));
        verify(commandRunner, times(2)).runInShell(String.format(KILL_COMMAND, netRuntimePath));
        verify(dhcpUtils, atLeastOnce()).writeConfigToFile(netRuntimePath + "/pi-dhcp.conf", DHCP_CONFIG_STRING);
    }

    @Test
    public void initialisationShouldKillDhcpdAndDeletePidIfPidFileExists() throws Exception {
        // act
        dhcpManager.initialise();

        // assert
        verify(dhcpUtils).deleteDhcpFileIfExists(netRuntimePath + "/pi-dhcp.pid");
        verify(commandRunner).runInShell(String.format(KILL_COMMAND, netRuntimePath));
        verify(commandRunner).run(String.format("%s" + " -cf %s/pi-dhcp.conf" + " -lf %s/pi-dhcp.leases -pf %s/pi-dhcp.pid" + " pibr999", dhcpDaemonPath, netRuntimePath, netRuntimePath, netRuntimePath));
        verify(dhcpUtils).writeConfigToFile(netRuntimePath + "/pi-dhcp.conf", DHCP_CONFIG_STRING);
    }

    @Test
    public void initialisationShouldNotKillDhcpdOrDeletePidIfPidFileDoesNotExist() throws Exception {
        // setup
        when(dhcpUtils.getFileIfExists(isA(String.class))).thenReturn(null);

        // act
        dhcpManager.initialise();

        // assert
        verify(commandRunner, never()).runInShell(String.format(KILL_COMMAND, netRuntimePath));
        verify(commandRunner).run(String.format("%s" + " -cf %s/pi-dhcp.conf" + " -lf %s/pi-dhcp.leases -pf %s/pi-dhcp.pid" + " pibr999", dhcpDaemonPath, netRuntimePath, netRuntimePath, netRuntimePath));
        verify(dhcpUtils).writeConfigToFile(netRuntimePath + "/pi-dhcp.conf", DHCP_CONFIG_STRING);
    }

    @Test
    public void initialisationShouldNotStartDhcpdIfNoSecGroups() throws Exception {
        // setup
        when(dhcpUtils.getFileIfExists(isA(String.class))).thenReturn(null);
        when(consumedDhtResourceRegistry.getByType(eq(SecurityGroup.class))).thenAnswer(new Answer<List<? extends PiEntity>>() {
            @Override
            public List<? extends PiEntity> answer(InvocationOnMock invocation) throws Throwable {
                return new ArrayList<SecurityGroup>();
            }
        });

        // act
        dhcpManager.initialise();

        // assert
        verify(commandRunner, never()).run(String.format("%s" + " -cf %s/pi-dhcp.conf" + " -lf %s/pi-dhcp.leases -pf %s/pi-dhcp.pid" + " pibr999", dhcpDaemonPath, netRuntimePath, netRuntimePath, netRuntimePath));
        verify(dhcpUtils, never()).writeConfigToFile(netRuntimePath + "/pi-dhcp.conf", DHCP_CONFIG_STRING);
    }

    @Test(expected = DhcpRefreshTimeoutException.class)
    public void dhcpRequestPollerShouldStopOnRequest() throws Exception {
        // setup
        dhcpManager.initialise();

        // act
        dhcpManager.destroy();
        DhcpRefreshToken dhcpRefreshToken = dhcpManager.requestDhcpRefresh(securityGroups);
        dhcpRefreshToken.blockUntilRefreshCompleted(1, TimeUnit.SECONDS);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testDestroy() throws IOException {
        // setup
        this.dhcpManager.initialise();

        // act
        when(dhcpConfigurationGenerator.generate(eq("pi-dhcp.conf.ftl"), isA(Collection.class))).thenReturn(DHCP_CONFIG_STRING + "shutdown");
        this.dhcpManager.destroy();

        // assert
        verify(dhcpUtils).writeConfigToFile(netRuntimePath + "/pi-dhcp.conf", DHCP_CONFIG_STRING + "shutdown");
    }

    @Test
    public void shouldNotCheckIfDhcpdRunningBeforeInitialization() throws Exception {
        // act
        this.dhcpManager.doPeriodicDhcpDaemonCheck();

        // assert
        verify(commandRunner, never()).run(isA(String.class));
    }

    @Test
    public void shouldNotCheckIfDhcpdRunningBeforeTime() throws Exception {
        // setup
        this.dhcpManager.setLastDhcpDaemonRunningCheckTime(System.currentTimeMillis());
        this.dhcpManager.setDhcpDaemonRunningCheckFrequencySecs(10);

        // act
        this.dhcpManager.doPeriodicDhcpDaemonCheck();

        // assert
        verify(commandRunner, never()).run(isA(String.class));
    }

    @Test
    public void shouldDoNothingIfDhcpDaemonRunning() throws Exception {
        // setup
        this.dhcpManager.setLastDhcpDaemonRunningCheckTime(System.currentTimeMillis() - 30000);
        this.dhcpManager.setDhcpDaemonRunningCheckFrequencySecs(10);

        // act
        this.dhcpManager.doPeriodicDhcpDaemonCheck();

        // assert
        verify(commandRunner).runInShell("ps -ef | grep dhcp");
        verify(commandRunner, never()).runInShell(String.format(KILL_COMMAND, netRuntimePath));
        assertFalse(restartInvoked);
    }

    @Test
    public void shouldKillDhcpDaemonIfRunningAndNotActiveNetworkManager() throws Exception {
        // setup
        this.dhcpManager.setLastDhcpDaemonRunningCheckTime(System.currentTimeMillis() - 30000);
        this.dhcpManager.setDhcpDaemonRunningCheckFrequencySecs(10);
        when(applicationRegistry.getApplicationStatus(NetworkManagerApplication.APPLICATION_NAME)).thenReturn(ApplicationStatus.PASSIVE);

        // act
        this.dhcpManager.doPeriodicDhcpDaemonCheck();

        // assert
        verify(commandRunner).runInShell("ps -ef | grep dhcp");
        verify(commandRunner).runInShell(String.format(KILL_COMMAND, netRuntimePath));
        assertFalse(restartInvoked);
    }

    @Test
    public void shouldRestartIfDhcpDaemonNotRunning() throws Exception {
        // setup
        this.dhcpManager.setLastDhcpDaemonRunningCheckTime(System.currentTimeMillis() - 30000);
        this.dhcpManager.setDhcpDaemonRunningCheckFrequencySecs(10);

        psGrepDhcpCommandResult = mock(CommandResult.class);
        when(psGrepDhcpCommandResult.getOutputLines()).thenReturn(new ArrayList<String>());
        when(commandRunner.runInShell("ps -ef | grep dhcp")).thenReturn(psGrepDhcpCommandResult);

        // act
        this.dhcpManager.doPeriodicDhcpDaemonCheck();

        // assert
        verify(commandRunner).runInShell("ps -ef | grep dhcp");
        assertTrue(restartInvoked);
    }

    @Test
    public void shouldCheckForDhcpDaemonAtRegularIntervals() throws InterruptedException {
        // setup
        this.dhcpManager.setDhcpDaemonRunningCheckFrequencySecs(1);

        restartLatch = new CountDownLatch(4);

        psGrepDhcpCommandResult = mock(CommandResult.class);
        when(psGrepDhcpCommandResult.getOutputLines()).thenReturn(new ArrayList<String>());
        when(commandRunner.runInShell("ps -ef | grep dhcp")).thenReturn(psGrepDhcpCommandResult);

        // act
        dhcpManager.initialise();

        // assert
        assertTrue("Remaining: " + restartLatch.getCount(), restartLatch.await(5, TimeUnit.SECONDS));
        verify(commandRunner, times(4)).runInShell(String.format(KILL_COMMAND, netRuntimePath));
        verify(commandRunner, times(4)).run(String.format("%s" + " -cf %s/pi-dhcp.conf" + " -lf %s/pi-dhcp.leases -pf %s/pi-dhcp.pid" + " pibr999", dhcpDaemonPath, netRuntimePath, netRuntimePath, netRuntimePath));
    }

    @Test
    public void shouldRestartDhcpDaemonIfNotPresentIfKillFails() throws InterruptedException {
        // setup
        this.dhcpManager.setDhcpDaemonRunningCheckFrequencySecs(1);

        psGrepDhcpCommandResult = mock(CommandResult.class);
        when(psGrepDhcpCommandResult.getOutputLines()).thenReturn(new ArrayList<String>());
        when(commandRunner.runInShell("ps -ef | grep dhcp")).thenReturn(psGrepDhcpCommandResult);

        doThrow(new CommandExecutionException("oops", Arrays.asList(new String[] { "bang" }))).when(commandRunner).runInShell(String.format(KILL_COMMAND, netRuntimePath));

        // act
        dhcpManager.initialise();

        // assert
        assertTrue(restartLatch.await(5, TimeUnit.SECONDS));
        verify(commandRunner, times(2)).run(String.format("%s" + " -cf %s/pi-dhcp.conf" + " -lf %s/pi-dhcp.leases -pf %s/pi-dhcp.pid" + " pibr999", dhcpDaemonPath, netRuntimePath, netRuntimePath, netRuntimePath));
    }

    @Test
    @Ignore
    public void runPsCommand() {
        // act
        CommandRunner commandRunner = new CommandRunner();
        commandRunner.setExecutor(new Executor() {
            @Override
            public void execute(Runnable runnable) {
                runnable.run();
            }
        });
        for (String line : commandRunner.runInShell("ps -ef | grep dhcp").getOutputLines()) {
            System.out.println(line);
        }
    }

    @SuppressWarnings("unchecked")
    @Test
    public void shouldWriteToBackupFileAfterDhcpDaemonStarts() throws Exception {
        // setup
        when(dhcpConfigurationGenerator.generate(eq("pi-dhcp.conf.ftl"), isA(Collection.class))).thenReturn(DHCP_CONFIG_STRING);

        // act
        dhcpManager.restartDhcpDaemon(dhcpDaemonPath, netRuntimePath, securityGroups);

        // assert
        verify(dhcpConfigurationGenerator).generate(eq("pi-dhcp.conf.ftl"), isA(Collection.class));
        verify(dhcpUtils).writeConfigToFile(netRuntimePath + "/pi-dhcp.conf", DHCP_CONFIG_STRING);
        verify(dhcpUtils).copyFile(netRuntimePath + "/pi-dhcp.conf", netRuntimePath + "/pi-dhcp.conf.2");
    }

    @Test
    public void shouldHandleIOExceptionIfBackupFileDoesntExist() {
        // setup
        // backupFile.delete();
        when(commandRunner.run(String.format("%s" + " -cf %s/pi-dhcp.conf" + " -lf %s/pi-dhcp.leases -pf %s/pi-dhcp.pid" + " pibr999", dhcpDaemonPath, netRuntimePath, netRuntimePath, netRuntimePath))).thenThrow(
                new CommandExecutionException("unable to start dhcp daemon")).thenReturn(mock(CommandResult.class));

        // act
        dhcpManager.restartDhcpDaemon(dhcpDaemonPath, netRuntimePath, securityGroups);
    }

    @Test
    public void shouldTryStartingDhcpFromTheBackupFileIfItFails() throws IOException {
        // setup
        when(commandRunner.run(String.format("%s" + " -cf %s/pi-dhcp.conf" + " -lf %s/pi-dhcp.leases -pf %s/pi-dhcp.pid" + " pibr999", dhcpDaemonPath, netRuntimePath, netRuntimePath, netRuntimePath))).thenThrow(
                new CommandExecutionException("unable to start dhcp daemon")).thenReturn(mock(CommandResult.class));

        // act
        dhcpManager.restartDhcpDaemon(dhcpDaemonPath, netRuntimePath, securityGroups);

        // assert
        verify(dhcpUtils).writeConfigToFile(netRuntimePath + "/pi-dhcp.conf", DHCP_CONFIG_STRING);
    }

    @Test
    public void shouldKillDhcpDaemonBeforeTryingToRecoverByUsingTheBackupFile() throws Exception {
        // setup
        when(commandRunner.run(String.format("%s" + " -cf %s/pi-dhcp.conf" + " -lf %s/pi-dhcp.leases -pf %s/pi-dhcp.pid" + " pibr999", dhcpDaemonPath, netRuntimePath, netRuntimePath, netRuntimePath))).thenThrow(
                new CommandExecutionException("unable to start dhcp daemon")).thenReturn(mock(CommandResult.class));

        // act
        dhcpManager.restartDhcpDaemon(dhcpDaemonPath, netRuntimePath, securityGroups);

        // assert
        verify(commandRunner, times(2)).runInShell(String.format(KILL_COMMAND, netRuntimePath));
    }

    @Test
    public void shouldContinueIfKillCommandFailedBeforeTryingToRecoverByUsingTheBackupFile() throws IOException {
        // setup
        CommandResult commandResult = mock(CommandResult.class);

        when(commandRunner.run(String.format("%s" + " -cf %s/pi-dhcp.conf" + " -lf %s/pi-dhcp.leases -pf %s/pi-dhcp.pid" + " pibr999", dhcpDaemonPath, netRuntimePath, netRuntimePath, netRuntimePath))).thenThrow(
                new CommandExecutionException("unable to start dhcp daemon")).thenReturn(commandResult);

        when(commandRunner.runInShell(String.format(KILL_COMMAND, netRuntimePath))).thenReturn(commandResult).thenThrow(new CommandExecutionException("Unable to kill dhcpd daemon"));

        // act
        dhcpManager.restartDhcpDaemon(dhcpDaemonPath, netRuntimePath, securityGroups);

        // assert
        verify(commandRunner, times(2)).runInShell(String.format(KILL_COMMAND, netRuntimePath));
        verify(dhcpUtils).writeConfigToFile(netRuntimePath + "/pi-dhcp.conf", DHCP_CONFIG_STRING);
    }
}
