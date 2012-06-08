package com.bt.pi.app.common;

import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.isA;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import rice.p2p.commonapi.Id;

import com.bt.pi.app.common.net.NetworkCommandRunner;
import com.bt.pi.core.application.activation.ActivationAwareApplication;
import com.bt.pi.core.application.activation.ApplicationInfo;
import com.bt.pi.core.application.activation.ApplicationRecord;
import com.bt.pi.core.application.activation.ApplicationRegistry;
import com.bt.pi.core.application.activation.ApplicationStatus;
import com.bt.pi.core.application.activation.TimeStampedPair;

public class ApplicationPublicInterfaceWatcherTest {

    private static final String PUBLIC_INTERFACE = "ethWon";

    private static final String APP_NAME_2 = "app2";
    private static final String APP_NAME_1 = "app1";
    private NetworkCommandRunner networkCommandRunner;
    private ApplicationRegistry applicationRegistry;
    private ApplicationPublicInterfaceWatcher applicationPublicInterfaceWatcher;
    private ApplicationInfo appInfo1;
    private ApplicationInfo appInfo2;
    private String IP_ADDRESS = "1.2.3.4";

    private ApplicationRecord appRecord;

    private ScheduledExecutorService scheduledExecutorService;

    @Before
    public void before() {
        Set<String> applicationNames = new HashSet<String>();
        applicationNames.add(APP_NAME_1);
        applicationNames.add(APP_NAME_2);

        AbstractManagedAddressingPiApplication abstractManagedAddressingPiApplication = mock(AbstractManagedAddressingPiApplication.class);
        ActivationAwareApplication activationAwareApplication = mock(ActivationAwareApplication.class);

        appRecord = mock(ApplicationRecord.class);
        when(appRecord.getAssociatedResource((Id) anyObject())).thenReturn(IP_ADDRESS);

        appInfo1 = new ApplicationInfo(abstractManagedAddressingPiApplication);
        appInfo1.setCachedApplicationRecord(appRecord);

        appInfo2 = new ApplicationInfo(activationAwareApplication);
        appInfo1.setCachedApplicationRecord(appRecord);

        scheduledExecutorService = mock(ScheduledExecutorService.class);

        applicationRegistry = mock(ApplicationRegistry.class);
        when(applicationRegistry.getApplicationNames()).thenReturn(applicationNames);
        when(applicationRegistry.getApplicationInfor(eq(APP_NAME_1))).thenReturn(appInfo1);
        when(applicationRegistry.getApplicationInfor(eq(APP_NAME_2))).thenReturn(appInfo2);

        networkCommandRunner = mock(NetworkCommandRunner.class);

        applicationPublicInterfaceWatcher = new ApplicationPublicInterfaceWatcher();
        applicationPublicInterfaceWatcher.setApplicationRegistry(applicationRegistry);
        applicationPublicInterfaceWatcher.setNetworkCommandRunner(networkCommandRunner);
        applicationPublicInterfaceWatcher.setVnetPublicInterface(PUBLIC_INTERFACE);
        applicationPublicInterfaceWatcher.setScheduledExecutorService(scheduledExecutorService);
    }

    @Test
    public void testRunnerProcessesApplication() {
        appInfo1.setApplicationStatus(ApplicationStatus.ACTIVE);

        // act
        applicationPublicInterfaceWatcher.checkApplicationAddresses();

        verify(networkCommandRunner, times(1)).addDefaultGatewayRouteToDevice(eq(PUBLIC_INTERFACE));
        verify(networkCommandRunner, times(1)).addIpAddressAndSendArping(eq(IP_ADDRESS), eq(PUBLIC_INTERFACE));
    }

    @Test
    public void shouldOnlyProcessAbstractManagedAddressingPiApplication() {
        appInfo1.setApplicationStatus(ApplicationStatus.ACTIVE);
        appInfo2.setApplicationStatus(ApplicationStatus.ACTIVE);

        // act
        applicationPublicInterfaceWatcher.checkApplicationAddresses();

        verify(networkCommandRunner, times(1)).addDefaultGatewayRouteToDevice(eq(PUBLIC_INTERFACE));
        verify(networkCommandRunner, times(1)).addIpAddressAndSendArping(eq(IP_ADDRESS), eq(PUBLIC_INTERFACE));
        verify(networkCommandRunner, never()).ipAddressDelete(eq(IP_ADDRESS), eq(PUBLIC_INTERFACE));
    }

    @Test
    public void shouldRemoveInterfaceForInactiveApps() {
        appInfo1.setApplicationStatus(ApplicationStatus.PASSIVE);
        appInfo2.setApplicationStatus(ApplicationStatus.PASSIVE);
        Map<String, TimeStampedPair<String>> ipMapping = new HashMap<String, TimeStampedPair<String>>();
        ipMapping.put(IP_ADDRESS, null);
        String ipAddress2 = "5.6.7.8";
        ipMapping.put(ipAddress2, null);

        when(appRecord.getActiveNodeMap()).thenReturn(ipMapping);
        // act
        applicationPublicInterfaceWatcher.checkApplicationAddresses();

        verify(networkCommandRunner, never()).addDefaultGatewayRouteToDevice(anyString());
        verify(networkCommandRunner, never()).ipAddressAdd(anyString(), anyString());
        verify(networkCommandRunner, times(1)).ipAddressDelete(eq(IP_ADDRESS), eq(PUBLIC_INTERFACE));
        verify(networkCommandRunner, times(1)).ipAddressDelete(eq(ipAddress2), eq(PUBLIC_INTERFACE));
    }

    @Test
    public void shouldNotRunIfIPisNull() {
        appInfo1.setApplicationStatus(ApplicationStatus.PASSIVE);
        appInfo2.setApplicationStatus(ApplicationStatus.PASSIVE);

        // act
        applicationPublicInterfaceWatcher.checkApplicationAddresses();

        verify(networkCommandRunner, never()).addDefaultGatewayRouteToDevice(anyString());
        verify(networkCommandRunner, never()).ipAddressAdd(anyString(), anyString());
    }

    @Test
    public void shouldStartSchedulerOnPostConstruct() throws SecurityException, NoSuchMethodException {

        // act
        Method method = ApplicationPublicInterfaceWatcher.class.getMethod("startApplicationPublicIpWatcher");

        // assert
        assertTrue(method.getAnnotations()[0] instanceof javax.annotation.PostConstruct);
    }

    @Test
    public void shouldScheduleApplicationPublicInterfaceWatcherOnPostContruct() {
        // setup

        // act
        applicationPublicInterfaceWatcher.startApplicationPublicIpWatcher();

        // assert
        verify(scheduledExecutorService).scheduleWithFixedDelay(isA(Runnable.class), eq(0L), eq(120L), eq(TimeUnit.SECONDS));
    }

    @Test
    public void shouldRunCheckApplicationAddressesFromScheduledTask() throws InterruptedException {
        // setup
        scheduledExecutorService = Executors.newScheduledThreadPool(1);
        applicationPublicInterfaceWatcher.setScheduledExecutorService(scheduledExecutorService);
        applicationPublicInterfaceWatcher.setApplicationPublicIpWatcherIntervalSeconds(1);
        appInfo1.setApplicationStatus(ApplicationStatus.ACTIVE);

        // act
        applicationPublicInterfaceWatcher.startApplicationPublicIpWatcher();
        Thread.sleep(1 * 1000);

        // assert
        verify(networkCommandRunner, times(1)).addDefaultGatewayRouteToDevice(eq(PUBLIC_INTERFACE));
        verify(networkCommandRunner, Mockito.atLeast(1)).addIpAddressAndSendArping(eq(IP_ADDRESS), eq(PUBLIC_INTERFACE));
    }

    @Test
    public void shouldCancelApplicationPublicInterfaceWatcherOnShuttingDown() throws InterruptedException {
        scheduledExecutorService = Executors.newScheduledThreadPool(1);
        applicationPublicInterfaceWatcher.setScheduledExecutorService(scheduledExecutorService);
        applicationPublicInterfaceWatcher.setApplicationPublicIpWatcherIntervalSeconds(1);
        appInfo1.setApplicationStatus(ApplicationStatus.ACTIVE);

        // act
        applicationPublicInterfaceWatcher.startApplicationPublicIpWatcher();
        Thread.sleep(1 * 1000);
        // NOTE: If the task is not cancelled then it will re-add the default gateway/ip which will fail the verify
        applicationPublicInterfaceWatcher.stopWatchingApplicationPublicAddressOnShuttingDown();
        Thread.sleep(2 * 1000);

        // assert
        verify(networkCommandRunner, times(1)).addDefaultGatewayRouteToDevice(eq(PUBLIC_INTERFACE));
        verify(networkCommandRunner, times(1)).addIpAddressAndSendArping(eq(IP_ADDRESS), eq(PUBLIC_INTERFACE));
    }

}
