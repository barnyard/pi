package com.bt.pi.app.instancemanager.handlers;

import static org.mockito.Matchers.isA;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.runners.MockitoJUnitRunner;
import org.mockito.stubbing.Answer;

import com.bt.pi.app.common.entities.Snapshot;
import com.bt.pi.app.common.entities.SnapshotState;
import com.bt.pi.core.application.ReceivedMessageContext;
import com.bt.pi.core.cli.commands.CommandExecutionException;
import com.bt.pi.core.entity.EntityResponseCode;
import com.bt.pi.core.util.SerialExecutor;
import com.bt.pi.core.util.common.CommandRunner;

@RunWith(MockitoJUnitRunner.class)
public class CreateSnapshotHandlerTest {
    private static final String LOCAL_VOLS_FOLDER = "var/volumes/local";
    private static final String SNAPSHOTS_FOLDER = "var/snapshots";
    private static final String RSYNC_COMMAND = "rsync -a %s %s";

    @InjectMocks
    private CreateSnapshotHandler createSnapshotHandler = new CreateSnapshotHandler() {
        protected String getAbsoluteLocalVolumesDirectory() {
            return LOCAL_VOLS_FOLDER;
        };
    };

    @Mock
    private CommandRunner commandRunner;
    @Mock
    private Snapshot snapshot;
    private Thread thread;
    @Mock
    private ReceivedMessageContext messageContext;
    private String volumeId = "v-123456";
    private String snapshotId = "snap-12345678";
    @Mock
    private SerialExecutor serialExecutor;

    @Before
    public void setUp() {
        createSnapshotHandler.setRsyncCommand(RSYNC_COMMAND);
        createSnapshotHandler.setSnapshotFolder(SNAPSHOTS_FOLDER);
        createSnapshotHandler.setNfsVolumesDirectory(LOCAL_VOLS_FOLDER);

        when(snapshot.getVolumeId()).thenReturn(volumeId);
        when(snapshot.getSnapshotId()).thenReturn(snapshotId);

        doAnswer(new Answer<Object>() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                Runnable r = (Runnable) invocation.getArguments()[0];
                thread = new Thread(r);
                thread.start();
                return null;
            }
        }).when(serialExecutor).execute(isA(Runnable.class));
    }

    @Test
    public void shouldRunRsyncCommandInCommandRunner() throws Exception {
        // act
        createSnapshotHandler.createSnapshot(snapshot, messageContext);
        thread.join(1000);

        // assert
        verify(commandRunner).runNicely(String.format(RSYNC_COMMAND, LOCAL_VOLS_FOLDER + "/" + volumeId, SNAPSHOTS_FOLDER + "/" + snapshotId));
    }

    @Test
    public void shouldRespondOKToMessageContext() throws Exception {
        // act
        createSnapshotHandler.createSnapshot(snapshot, messageContext);
        thread.join(1000);

        // assert
        verify(messageContext).sendResponse(EntityResponseCode.OK, snapshot);
        verify(snapshot).setStatus(SnapshotState.COMPLETE);
    }

    @Test
    public void shouldSetSnapshotStateToErrorIfExceptionInCommandRunner() throws Exception {
        // setup
        doThrow(new CommandExecutionException()).when(commandRunner).runNicely(isA(String.class));

        // act
        createSnapshotHandler.createSnapshot(snapshot, messageContext);
        thread.join(1000);

        // assert
        verify(messageContext).sendResponse(EntityResponseCode.OK, snapshot);
        verify(snapshot).setStatus(SnapshotState.ERROR);
    }
}
