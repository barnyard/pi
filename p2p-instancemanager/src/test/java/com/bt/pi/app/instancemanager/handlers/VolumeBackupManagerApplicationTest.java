package com.bt.pi.app.instancemanager.handlers;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.isA;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Calendar;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.runners.MockitoJUnitRunner;
import org.mockito.stubbing.Answer;

import rice.Continuation;
import rice.p2p.commonapi.Endpoint;
import rice.pastry.PastryNode;

import com.bt.pi.app.common.entities.NodeVolumeBackupRecord;
import com.bt.pi.app.common.id.PiIdBuilder;
import com.bt.pi.core.application.activation.ApplicationRecord;
import com.bt.pi.core.application.activation.RegionScopedApplicationRecord;
import com.bt.pi.core.application.activation.SharedRecordConditionalApplicationActivator;
import com.bt.pi.core.continuation.UpdateResolvingPiContinuation;
import com.bt.pi.core.dht.DhtClientFactory;
import com.bt.pi.core.dht.DhtWriter;
import com.bt.pi.core.dht.cache.BlockingDhtCache;
import com.bt.pi.core.id.KoalaIdFactory;
import com.bt.pi.core.id.PId;
import com.bt.pi.core.scope.NodeScope;

@SuppressWarnings("unchecked")
@RunWith(MockitoJUnitRunner.class)
public class VolumeBackupManagerApplicationTest {
    private static final String NODEID = "nodeid";

    @Mock
    private PId pid;
    @Mock
    private PId appRecordPid;
    @Mock
    private PastryNode pastryNode;
    @Mock
    private KoalaIdFactory koalaIdFactory;
    @Mock
    private PiIdBuilder piIdBuilder;
    @Mock
    private BlockingDhtCache blockingDhtCache;
    @Mock
    private DhtClientFactory dhtClientFactory;
    @Mock
    private DhtWriter dhtWriter;
    @Mock
    private SharedRecordConditionalApplicationActivator activator;
    @Mock
    private VolumeBackupHandler volumeBackupHandler;

    private NodeVolumeBackupRecord nodeVolumeBackupRecord;

    @InjectMocks
    VolumeBackupManagerApplication application = new VolumeBackupManagerApplication() {
        @Override
        public String getNodeIdFull() {
            return NODEID;
        }
    };

    @Before
    public void setup() {
        nodeVolumeBackupRecord = new NodeVolumeBackupRecord();
        nodeVolumeBackupRecord.setNodeId(NODEID);

        final ApplicationRecord applicationRecord = new RegionScopedApplicationRecord(VolumeBackupManagerApplication.APPLICATION_NAME, 1, Arrays.asList(new String[] { "1" }));

        when(appRecordPid.forLocalScope(NodeScope.REGION)).thenReturn(appRecordPid);
        when(koalaIdFactory.buildPId(RegionScopedApplicationRecord.getUrl(application.getApplicationName()))).thenReturn(appRecordPid);

        when(piIdBuilder.getPId(NodeVolumeBackupRecord.getUrl(NODEID))).thenReturn(pid);
        when(blockingDhtCache.get(pid)).thenReturn(nodeVolumeBackupRecord);

        doAnswer(new Answer<Object>() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                ((Continuation) invocation.getArguments()[0]).receiveResult(null);
                return null;
            }

        }).when(volumeBackupHandler).startBackup(isA(Continuation.class));

        when(dhtClientFactory.createWriter()).thenReturn(dhtWriter);
        doAnswer(new Answer<NodeVolumeBackupRecord>() {
            @Override
            public NodeVolumeBackupRecord answer(InvocationOnMock invocation) throws Throwable {
                UpdateResolvingPiContinuation<NodeVolumeBackupRecord> continuation = (UpdateResolvingPiContinuation<NodeVolumeBackupRecord>) invocation.getArguments()[1];
                NodeVolumeBackupRecord updated = continuation.update(nodeVolumeBackupRecord, null);
                if (updated != null)
                    continuation.handleResult(updated);

                return null;
            }
        }).when(dhtWriter).update(eq(pid), isA(UpdateResolvingPiContinuation.class));
        doAnswer(new Answer<ApplicationRecord>() {
            @Override
            public ApplicationRecord answer(InvocationOnMock invocation) throws Throwable {
                UpdateResolvingPiContinuation<ApplicationRecord> continuation = (UpdateResolvingPiContinuation<ApplicationRecord>) invocation.getArguments()[1];
                ApplicationRecord updated = continuation.update(applicationRecord, null);
                if (updated != null)
                    continuation.handleResult(updated);

                return null;
            }
        }).when(dhtWriter).update(eq(appRecordPid), isA(UpdateResolvingPiContinuation.class));

        application.setActivationCheckPeriodSecs(60);
        application.setVolumeBackupCooldownPeriodSecs(86400);
        application.setVolumeBackupManagerAllowedSlotRanges(setupOneHourTimeSlotWithStartingTime(Calendar.getInstance()));
    }

    private String setupOneHourTimeSlotWithStartingTime(Calendar calendar) {
        StringBuilder stringBuilder = new StringBuilder(String.format("%d:00-", calendar.get(Calendar.HOUR_OF_DAY)));
        calendar.add(Calendar.HOUR_OF_DAY, 1);
        stringBuilder.append(String.format("%d:00", calendar.get(Calendar.HOUR_OF_DAY)));
        return stringBuilder.toString();
    }

    @Test
    public void shouldReturnActivationCheckTimeIfLocallyCachedRecordIsNull() {
        // act
        int result = application.getActivationCheckPeriodSecs();

        // assert
        assertEquals(60, result);
    }

    @Test
    public void shouldReturnActivationCheckTimeIfLastBackupWasTooOld() {
        // setup
        application.becomeActive(); // so that the local cached record is set up
        nodeVolumeBackupRecord.setLastBackup(System.currentTimeMillis() - 1500);
        application.setVolumeBackupCooldownPeriodSecs(1);

        // act
        int result = application.getActivationCheckPeriodSecs();

        // assert
        assertEquals(60, result);
    }

    @Test
    public void shouldReturn24HoursIfLastBackupIsNotTooOld() {
        // setup
        application.becomeActive(); // so that the local cached record is set up
        nodeVolumeBackupRecord.setLastBackup(System.currentTimeMillis() - 500);

        // act
        int result = application.getActivationCheckPeriodSecs();

        // assert
        assertEquals(86400, result);
    }

    @Test
    public void shouldOnlyStartApplicationIfLastCompletedTimeIsMoreThanTheCooldownPeriod() {
        // setup
        nodeVolumeBackupRecord.setLastBackup(System.currentTimeMillis() - 1500);
        application.setVolumeBackupCooldownPeriodSecs(1);

        // act
        boolean active = application.becomeActive();

        // assert
        assertTrue(active);
        verify(volumeBackupHandler).startBackup(isA(Continuation.class));
    }

    @Test
    public void shouldNotStartApplicationIfLastCompletedTimeIsLessThanTheCooldownPeriod() {
        // setup
        nodeVolumeBackupRecord.setLastBackup(System.currentTimeMillis() - 500);
        application.setVolumeBackupCooldownPeriodSecs(1000);

        // act
        boolean active = application.becomeActive();

        // assert
        assertFalse(active);
        verify(volumeBackupHandler, never()).startBackup(isA(Continuation.class));
    }

    @Test
    public void shouldUpdateNodeVolumeBackupRecordAndBecomePassiveOnCompletionOfBackup() {
        // setup
        nodeVolumeBackupRecord.setLastBackup(System.currentTimeMillis() - 1500);
        application.setVolumeBackupCooldownPeriodSecs(1);

        // act
        boolean active = application.becomeActive();

        // assert
        assertTrue(active);
        verify(dhtWriter).update(eq(appRecordPid), isA(UpdateResolvingPiContinuation.class));
    }

    @Test
    public void shouldRemoveNodeFromApplicationRecordOnHandlingAnotherNodeDeparture() {
        // act
        application.handleNodeDeparture(NODEID);

        // assert
        verify(activator).deActivateNode(NODEID, application);
    }

    @Test
    public void shouldNotStartApplicationIfNotInAllocatedTimeSlot() {
        // setup
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.HOUR_OF_DAY, 1);
        application.setVolumeBackupManagerAllowedSlotRanges(setupOneHourTimeSlotWithStartingTime(calendar));

        // act
        boolean active = application.becomeActive();

        // assert
        assertFalse(active);
        verify(volumeBackupHandler, never()).startBackup(isA(Continuation.class));
    }

    @Test
    public void shouldCacheNodeVolumeBackupRecordOnStartup() {
        // setup
        Endpoint endpoint = mock(Endpoint.class);
        when(pastryNode.buildEndpoint(application, application.getApplicationName())).thenReturn(endpoint);

        // act
        application.start(pastryNode, null, null, null);

        // assert
        verify(dhtWriter).update(eq(pid), isA(UpdateResolvingPiContinuation.class));
    }
}
