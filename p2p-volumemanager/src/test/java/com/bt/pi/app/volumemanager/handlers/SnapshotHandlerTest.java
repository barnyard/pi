package com.bt.pi.app.volumemanager.handlers;

import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import com.bt.pi.app.common.entities.Snapshot;
import com.bt.pi.app.common.resource.PiTopics;
import com.bt.pi.core.application.PubSubMessageContext;
import com.bt.pi.core.entity.EntityMethod;
import com.bt.pi.core.id.KoalaIdFactory;
import com.bt.pi.core.id.PId;

@RunWith(MockitoJUnitRunner.class)
public class SnapshotHandlerTest {
    @InjectMocks
    private SnapshotHandler snapshotHandler = new SnapshotHandler();
    private String nodeIdFull = "1234";
    @Mock
    private Snapshot snapshot;
    @Mock
    private PubSubMessageContext pubSubMessageContext;
    @Mock
    private CreateSnapshotHandler createSnapshotHandler;
    @Mock
    private DeleteSnapshotHandler deleteSnapshotHandler;
    @Mock
    private KoalaIdFactory koalaIdFactory;
    @Mock
    private PId piCreateTopicId;
    @Mock
    private PId piDeleteTopicId;

    @Before
    public void before() {
        when(koalaIdFactory.buildPId(PiTopics.CREATE_SNAPSHOT.getPiLocation())).thenReturn(piCreateTopicId);
        when(piCreateTopicId.forLocalScope(PiTopics.CREATE_SNAPSHOT.getNodeScope())).thenReturn(piCreateTopicId);

        when(koalaIdFactory.buildPId(PiTopics.DELETE_SNAPSHOT.getPiLocation())).thenReturn(piDeleteTopicId);
        when(piDeleteTopicId.forLocalScope(PiTopics.DELETE_SNAPSHOT.getNodeScope())).thenReturn(piDeleteTopicId);
    }

    @Test
    public void testHandleAnycastCreate() {
        // setup
        when(pubSubMessageContext.getTopicPId()).thenReturn(piCreateTopicId);
        when(createSnapshotHandler.createSnapshot(snapshot, pubSubMessageContext, nodeIdFull)).thenReturn(true);

        // act
        boolean result = this.snapshotHandler.handleAnycast(pubSubMessageContext, EntityMethod.CREATE, snapshot, nodeIdFull);

        // assert
        assertTrue(result);
        verify(createSnapshotHandler).createSnapshot(snapshot, pubSubMessageContext, nodeIdFull);
    }

    @Test
    public void testHandleAnycastDelete() {
        // setup
        when(pubSubMessageContext.getTopicPId()).thenReturn(piDeleteTopicId);

        // act
        boolean result = this.snapshotHandler.handleAnycast(pubSubMessageContext, EntityMethod.DELETE, snapshot, nodeIdFull);

        // assert
        assertTrue(result);
        verify(deleteSnapshotHandler).deleteSnapshot(snapshot, nodeIdFull);
    }
}
