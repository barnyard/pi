package com.bt.pi.api.handlers;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.Before;
import org.junit.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.ws.transport.context.TransportContext;

import com.amazonaws.ec2.doc.x20081201.CreateSnapshotDocument;
import com.amazonaws.ec2.doc.x20081201.CreateSnapshotResponseDocument;
import com.amazonaws.ec2.doc.x20081201.CreateSnapshotType;
import com.bt.pi.api.service.ElasticBlockStorageService;
import com.bt.pi.api.utils.ConversionUtils;
import com.bt.pi.app.common.entities.Snapshot;
import com.bt.pi.app.common.entities.SnapshotState;

public class CreateSnapshotHandlerTest extends AbstractHandlerTest {

    private CreateSnapshotHandler createSnapshotHandler;
    private String volumeId = "v-123";
    private ElasticBlockStorageService elasticBlockStorageService;

    @Before
    public void setUp() throws Exception {
        super.before();
        this.createSnapshotHandler = new CreateSnapshotHandler() {
            @Override
            protected TransportContext getTransportContext() {
                return transportContext;
            }
        };
        elasticBlockStorageService = mock(ElasticBlockStorageService.class);
        Snapshot value = new Snapshot();
        value.setVolumeId(volumeId);
        value.setSnapshotId("snap-123");
        value.setStatus(SnapshotState.PENDING);
        value.setProgress(10.0);
        value.setStartTime(System.currentTimeMillis());
        when(elasticBlockStorageService.createSnapshot("userid", "v-123", null)).thenReturn(value);
        createSnapshotHandler.setElasticBlockStorageService(elasticBlockStorageService);
        ReflectionTestUtils.setField(createSnapshotHandler, "conversionUtils", new ConversionUtils());
    }

    @Test
    public void testCreateSnapshot() {
        // setup
        CreateSnapshotDocument requestDocument = CreateSnapshotDocument.Factory.newInstance();
        CreateSnapshotType addNewCreateSnapshot = requestDocument.addNewCreateSnapshot();
        addNewCreateSnapshot.setVolumeId(volumeId);

        // act
        CreateSnapshotResponseDocument result = this.createSnapshotHandler.createSnapshot(requestDocument);

        // assert
        assertEquals(volumeId, result.getCreateSnapshotResponse().getVolumeId());
        assertEquals("10.0", result.getCreateSnapshotResponse().getProgress());
        assertEquals("snap-123", result.getCreateSnapshotResponse().getSnapshotId());
        assertEquals(SnapshotState.PENDING.toString(), result.getCreateSnapshotResponse().getStatus());
        assertDateEquals(result.getCreateSnapshotResponse().getStartTime());
    }
}
