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

import com.bt.pi.app.common.entities.User;
import com.bt.pi.app.common.entities.Volume;
import com.bt.pi.app.common.entities.VolumeState;
import com.bt.pi.app.common.id.PiIdBuilder;
import com.bt.pi.app.common.resource.PiQueue;
import com.bt.pi.core.id.PId;

@RunWith(MockitoJUnitRunner.class)
public class RemoveVolumeFromUserTaskProcessingQueueContinuationTest {
    private String volumeId = "volumeId";
    private String ownerId = "owner";

    private Volume volume;
    private User user;

    @Mock
    private PiIdBuilder piIdBuilder;
    @InjectMocks
    private RemoveVolumeFromUserTaskProcessingQueueContinuation continuation = new RemoveVolumeFromUserTaskProcessingQueueContinuation();

    @Before
    public void setup() {
        volume = new Volume();
        volume.setVolumeId(volumeId);
        volume.setOwnerId(ownerId);

        user = new User();
        user.setUsername(ownerId);
    }

    @Test
    public void testThatGetOwnerIdReturnsOwnerId() throws Exception {
        // act
        String result = continuation.getOwnerId(volume);

        // assert
        assertThat(result, equalTo(ownerId));
    }

    @Test
    public void testThatGetPiQueueForResourceReturnsCorrectQueue() throws Exception {
        // act
        PiQueue result = continuation.getPiQueueForResource();

        // assert
        assertThat(result, equalTo(PiQueue.REMOVE_VOLUME_FROM_USER));
    }

    @Test
    public void testThatRemoveResourceFromUserRemovesSnapshotId() throws Exception {
        // setup
        user.getVolumeIds().add(volumeId);

        // act
        continuation.removeResourceFromUser(user, volume);

        // assert
        assertThat(user.getVolumeIds().contains(volumeId), is(false));
    }

    @Test
    public void testSettingOfResourceToBuried() throws Exception {
        // act
        continuation.setResourceStatusToBuried(volume);

        // assert
        assertThat(volume.getStatus(), equalTo(VolumeState.BURIED));
    }

    @Test
    public void testGetResourceId() throws Exception {
        // act
        String result = continuation.getResourceId(volume);

        // assert
        assertThat(result, equalTo(volumeId));
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
        boolean result = continuation.removeResourceFromUser(user, volume);

        // assert
        assertThat(result, is(false));
    }

    @Test
    public void testSettingOfResourceToBuriedReturnsFalseIfStatusIsAlreadyBuried() throws Exception {
        // setup
        volume.setStatus(VolumeState.BURIED);

        // act
        boolean result = continuation.setResourceStatusToBuried(volume);

        // assert
        assertThat(result, is(false));
    }
}
