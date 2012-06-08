package com.bt.pi.app.volumemanager;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import com.bt.pi.app.common.entities.Snapshot;
import com.bt.pi.app.common.entities.SnapshotState;
import com.bt.pi.app.common.entities.User;
import com.bt.pi.app.common.id.PiIdBuilder;
import com.bt.pi.app.common.resource.PiQueue;
import com.bt.pi.core.id.PId;

@RunWith(MockitoJUnitRunner.class)
public class RemoveSnapshotFromUserTaskProcessingQueueContinuationTest {
    private String snapshotId = "snapshotId";
    private String ownerId = "owner";

    private Snapshot snapshot;
    private User user;

    @Mock
    private PiIdBuilder piIdBuilder;
    @InjectMocks
    private RemoveSnapshotFromUserTaskProcessingQueueContinuation continuation = new RemoveSnapshotFromUserTaskProcessingQueueContinuation();

    @Before
    public void setup() {
        snapshot = new Snapshot();
        snapshot.setSnapshotId(snapshotId);
        snapshot.setOwnerId(ownerId);

        user = new User();
        user.setUsername(ownerId);
    }

    @Test
    public void testThatGetOwnerIdReturnsOwnerId() throws Exception {
        // act
        String result = continuation.getOwnerId(snapshot);

        // assert
        assertThat(result, equalTo(ownerId));
    }

    @Test
    public void testThatGetPiQueueForResourceReturnsCorrectQueue() throws Exception {
        // act
        PiQueue result = continuation.getPiQueueForResource();

        // assert
        assertThat(result, equalTo(PiQueue.REMOVE_SNAPSHOT_FROM_USER));
    }

    @Test
    public void testThatRemoveResourceFromUserRemovesSnapshotId() throws Exception {
        // setup
        user.getSnapshotIds().add(snapshotId);

        // act
        boolean result = continuation.removeResourceFromUser(user, snapshot);

        // assert
        assertThat(user.getSnapshotIds().contains(snapshotId), is(false));
        assertThat(result, is(true));
    }

    @Test
    public void testSettingOfResourceToBuried() throws Exception {
        // act
        boolean result = continuation.setResourceStatusToBuried(snapshot);

        // assert
        assertThat(snapshot.getStatus(), equalTo(SnapshotState.BURIED));
        assertThat(result, is(true));
    }

    @Test
    public void testGetResourceId() throws Exception {
        // act
        String result = continuation.getResourceId(snapshot);

        // assert
        assertThat(result, equalTo(snapshotId));
    }

    @Test
    public void testGetResourcePastryId() throws Exception {
        // setup
        String uri = "uri";
        PId pid = mock(PId.class);
        when(piIdBuilder.getPIdForEc2AvailabilityZone(uri)).thenReturn(pid);

        // act
        PId result = continuation.getResourcePastryId(uri);

        // assert
        assertThat(result, equalTo(pid));
    }

    @Test
    public void testThatRemoveResourceFromUserReturnsTrueIfSuccessfullyRemoved() throws Exception {
        // act
        boolean result = continuation.removeResourceFromUser(user, snapshot);

        // assert
        assertThat(result, is(false));
    }

    @Test
    public void testSettingOfResourceToBuriedReturnsFalseIfStatusIsAlreadyBuried() throws Exception {
        // setup
        snapshot.setStatus(SnapshotState.BURIED);

        // act
        boolean result = continuation.setResourceStatusToBuried(snapshot);

        // assert
        assertThat(result, is(false));
    }
}
