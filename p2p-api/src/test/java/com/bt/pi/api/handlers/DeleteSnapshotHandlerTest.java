package com.bt.pi.api.handlers;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.Before;
import org.junit.Test;
import org.springframework.ws.transport.context.TransportContext;

import com.bt.pi.api.service.ElasticBlockStorageService;

public class DeleteSnapshotHandlerTest extends AbstractHandlerTest {

    private String snapshotId = "snapshotId";
    private DeleteSnapshotHandler handler;
    private ElasticBlockStorageService elasticBlockStorageService;

    @Before
    public void before() {
        super.before();
        handler = new DeleteSnapshotHandler() {
            @Override
            protected TransportContext getTransportContext() {
                return transportContext;
            }
        };
        elasticBlockStorageService = mock(ElasticBlockStorageService.class);
        when(elasticBlockStorageService.deleteSnapshot("userid", snapshotId)).thenReturn(true);
        handler.setElasticBlockStorageService(elasticBlockStorageService);

    }

    @Test
    public void shouldDeleteSnapshot20081201() {
        // setup
        com.amazonaws.ec2.doc.x20081201.DeleteSnapshotDocument requestDocument = com.amazonaws.ec2.doc.x20081201.DeleteSnapshotDocument.Factory.newInstance();
        requestDocument.addNewDeleteSnapshot().setSnapshotId(snapshotId);
        // act
        com.amazonaws.ec2.doc.x20081201.DeleteSnapshotResponseDocument responseDocument = handler.deleteSnapshot(requestDocument);
        // assert
        assertEquals(true, responseDocument.getDeleteSnapshotResponse().getReturn());
    }

    @Test
    public void shouldDeleteSnapshot20090404() {
        // setup
        com.amazonaws.ec2.doc.x20090404.DeleteSnapshotDocument requestDocument = com.amazonaws.ec2.doc.x20090404.DeleteSnapshotDocument.Factory.newInstance();
        requestDocument.addNewDeleteSnapshot().setSnapshotId(snapshotId);
        // act
        com.amazonaws.ec2.doc.x20090404.DeleteSnapshotResponseDocument responseDocument = handler.deleteSnapshot(requestDocument);
        // assert
        assertEquals(true, responseDocument.getDeleteSnapshotResponse().getReturn());
    }

}
