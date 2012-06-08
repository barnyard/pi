package com.bt.pi.app.instancemanager.reporting;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.argThat;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.isA;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentMatcher;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import com.bt.pi.app.common.entities.Instance;
import com.bt.pi.app.common.id.PiIdBuilder;
import com.bt.pi.app.instancemanager.images.InstanceImageManager;
import com.bt.pi.core.application.reporter.ReportingApplication;
import com.bt.pi.core.application.resource.ConsumedDhtResourceRegistry;
import com.bt.pi.core.entity.PiEntity;
import com.bt.pi.core.id.PId;
import com.bt.pi.core.node.NodeStartedEvent;

@RunWith(MockitoJUnitRunner.class)
public class NodeInstanceReporterTest {
    @Mock
    private InstanceImageManager instanceImageManager;
    @Mock
    private ConsumedDhtResourceRegistry consumedDhtResourceRegistry;
    @Mock
    private PiIdBuilder piIdBuilder;
    @Mock
    private ReportingApplication reportingApplication;
    @Mock
    private ScheduledExecutorService scheduledExecutorService;

    @InjectMocks
    private NodeInstanceReporter nodeInstanceReporter = new NodeInstanceReporter();

    @Before
    public void setup() {
        PId instanceId1 = mock(PId.class), instanceId2 = mock(PId.class), instanceId3 = mock(PId.class);
        when(piIdBuilder.getPIdForEc2AvailabilityZone(Instance.getUrl("i-1"))).thenReturn(instanceId1);
        when(piIdBuilder.getPIdForEc2AvailabilityZone(Instance.getUrl("i-2"))).thenReturn(instanceId2);
        when(piIdBuilder.getPIdForEc2AvailabilityZone(Instance.getUrl("i-3"))).thenReturn(instanceId3);
        Instance instance1 = new Instance("i-1", "o-1", null);
        Instance instance2 = new Instance("i-2", "o-2", null);
        Instance instance3 = new Instance("i-3", "o-3", null);

        when(instanceImageManager.getRunningInstances()).thenReturn(Arrays.asList(new String[] { "i-1", "i-2", "i-3" }));
        when(consumedDhtResourceRegistry.getCachedEntity(instanceId1)).thenReturn(instance1);
        when(consumedDhtResourceRegistry.getCachedEntity(instanceId2)).thenReturn(instance2);
        when(consumedDhtResourceRegistry.getCachedEntity(instanceId3)).thenReturn(instance3);
    }

    @Test
    public void shouldNotReportAnyInstancesIfNodeNotStarted() throws Exception {
        // act
        nodeInstanceReporter.reportRunningInstances();

        // assert
        verify(reportingApplication, never()).sendReportingUpdateToASuperNode(isA(PiEntity.class));
    }

    @Test
    public void shouldReportAllRunningInstancesToSupernodes() throws Exception {
        // setup
        nodeInstanceReporter.onApplicationEvent(new NodeStartedEvent(this));

        // act
        nodeInstanceReporter.reportRunningInstances();

        // assert
        verify(reportingApplication).sendReportingUpdateToASuperNode(argThat(new ArgumentMatcher<InstanceReportEntityCollection>() {
            @Override
            public boolean matches(Object argument) {
                InstanceReportEntityCollection instanceReportEntityCollection = (InstanceReportEntityCollection) argument;
                assertThat(instanceReportEntityCollection.getEntities().size(), equalTo(3));
                boolean i1 = false, i2 = false, i3 = false;
                for (InstanceReportEntity instanceReportEntity : instanceReportEntityCollection.getEntities()) {
                    if (instanceReportEntity.getInstanceId().equals("i-1") && instanceReportEntity.getOwnerId().equals("o-1"))
                        i1 = true;
                    else if (instanceReportEntity.getInstanceId().equals("i-2") && instanceReportEntity.getOwnerId().equals("o-2"))
                        i2 = true;
                    else if (instanceReportEntity.getInstanceId().equals("i-3") && instanceReportEntity.getOwnerId().equals("o-3"))
                        i3 = true;
                }

                assertThat(i1, is(true));
                assertThat(i2, is(true));
                assertThat(i3, is(true));
                return true;
            }
        }));
    }

    @Test
    public void shouldNotBarfIfRegistryDoesNotContainAnInstanceWhileReportingRunningInstancesToSupernodes() throws Exception {
        // setup
        when(instanceImageManager.getRunningInstances()).thenReturn(Arrays.asList(new String[] { "i-1", "i-2", "i-3", "i-4" }));
        nodeInstanceReporter.onApplicationEvent(new NodeStartedEvent(this));

        // act
        nodeInstanceReporter.reportRunningInstances();

        // assert
        verify(reportingApplication).sendReportingUpdateToASuperNode(argThat(new ArgumentMatcher<InstanceReportEntityCollection>() {
            @Override
            public boolean matches(Object argument) {
                InstanceReportEntityCollection instanceReportEntityCollection = (InstanceReportEntityCollection) argument;
                assertThat(instanceReportEntityCollection.getEntities().size(), equalTo(3));
                boolean i1 = false, i2 = false, i3 = false;
                for (InstanceReportEntity instanceReportEntity : instanceReportEntityCollection.getEntities()) {
                    if (instanceReportEntity.getInstanceId().equals("i-1") && instanceReportEntity.getOwnerId().equals("o-1"))
                        i1 = true;
                    else if (instanceReportEntity.getInstanceId().equals("i-2") && instanceReportEntity.getOwnerId().equals("o-2"))
                        i2 = true;
                    else if (instanceReportEntity.getInstanceId().equals("i-3") && instanceReportEntity.getOwnerId().equals("o-3"))
                        i3 = true;
                }

                assertThat(i1, is(true));
                assertThat(i2, is(true));
                assertThat(i3, is(true));
                return true;
            }
        }));
    }

    @Test
    public void shouldSpinOffSchedulerThreadOnPostConstruct() throws Exception {
        // act
        nodeInstanceReporter.scheduleReportingOfRunningInstances();

        // assert
        verify(scheduledExecutorService).scheduleWithFixedDelay(isA(Runnable.class), eq(0l), eq(900l), eq(TimeUnit.SECONDS));
    }
}

