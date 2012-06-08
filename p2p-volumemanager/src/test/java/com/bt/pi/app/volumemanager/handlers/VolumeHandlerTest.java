package com.bt.pi.app.volumemanager.handlers;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import rice.p2p.scribe.Topic;

import com.bt.pi.app.common.entities.Volume;
import com.bt.pi.app.common.entities.VolumeState;
import com.bt.pi.app.common.resource.PiTopics;
import com.bt.pi.core.application.PubSubMessageContext;
import com.bt.pi.core.entity.EntityMethod;
import com.bt.pi.core.id.KoalaIdFactory;
import com.bt.pi.core.id.PId;
import com.bt.pi.core.parser.KoalaPiEntityFactory;

@RunWith(MockitoJUnitRunner.class)
public class VolumeHandlerTest {
    @InjectMocks
    private VolumeHandler volumeHandler = new VolumeHandler();
    @Mock
    private CreateVolumeHandler createHandler;
    @Mock
    private DeleteVolumeHandler deleteHandler;
    @Mock
    private DetachVolumeHandler detachHandler;
    @Mock
    private AttachVolumeHandler attachHandler;
    private KoalaIdFactory koalaIdFactory;
    private String nodeIdFull = "99998877";
    @Mock
    private PubSubMessageContext pubSubMessageContext;
    @Mock
    private Volume volume;
    private Topic createTopic;
    private Topic deleteTopic;
    private Topic attachTopic;
    private Topic detachTopic;

    @Before
    public void before() {
        this.koalaIdFactory = new KoalaIdFactory(255, 255);
        koalaIdFactory.setKoalaPiEntityFactory(new KoalaPiEntityFactory());
        this.volumeHandler.setKoalaIdFactory(koalaIdFactory);
        createTopic = new Topic(koalaIdFactory.buildId(koalaIdFactory.buildPId(PiTopics.CREATE_VOLUME.getPiLocation()).forLocalScope(PiTopics.CREATE_VOLUME.getNodeScope())));
        deleteTopic = new Topic(koalaIdFactory.buildId(koalaIdFactory.buildPId(PiTopics.DELETE_VOLUME.getPiLocation()).forLocalScope(PiTopics.DELETE_VOLUME.getNodeScope())));
        attachTopic = new Topic(koalaIdFactory.buildId(koalaIdFactory.buildPId(PiTopics.ATTACH_VOLUME.getPiLocation()).forLocalScope(PiTopics.ATTACH_VOLUME.getNodeScope())));
        detachTopic = new Topic(koalaIdFactory.buildId(koalaIdFactory.buildPId(PiTopics.DETACH_VOLUME.getPiLocation()).forLocalScope(PiTopics.DETACH_VOLUME.getNodeScope())));
    }

    @Test
    public void testHandleAnycastCreateVolume() {
        // setup
        when(pubSubMessageContext.getTopicPId()).thenReturn(koalaIdFactory.convertToPId(createTopic.getId()));
        when(volume.getStatus()).thenReturn(VolumeState.CREATING);
        when(this.createHandler.createVolume(volume, nodeIdFull)).thenReturn(true);

        // act
        boolean result = volumeHandler.handleAnycast(pubSubMessageContext, EntityMethod.CREATE, volume, nodeIdFull);

        // assert
        assertTrue(result);
        verify(this.createHandler).createVolume(volume, nodeIdFull);
    }

    @Test
    public void testHandleAnycastCreateVolumeAlreadyDoingOne() {
        // setup
        when(pubSubMessageContext.getTopicPId()).thenReturn(koalaIdFactory.convertToPId(createTopic.getId()));
        when(volume.getStatus()).thenReturn(VolumeState.CREATING);
        when(this.createHandler.createVolume(volume, nodeIdFull)).thenReturn(false);

        // act
        boolean result = volumeHandler.handleAnycast(pubSubMessageContext, EntityMethod.CREATE, volume, nodeIdFull);

        // assert
        assertFalse(result);
    }

    @Test
    public void testHandleAnycastDeleteVolume() {
        // setup
        when(pubSubMessageContext.getTopicPId()).thenReturn(koalaIdFactory.convertToPId(deleteTopic.getId()));
        when(volume.getStatus()).thenReturn(VolumeState.AVAILABLE);

        // act
        boolean result = volumeHandler.handleAnycast(pubSubMessageContext, EntityMethod.DELETE, volume, nodeIdFull);

        // assert
        assertTrue(result);
        verify(this.deleteHandler).deleteVolume(volume, nodeIdFull);
    }

    @Test
    public void testHandleAnycastAttachVolume() {
        // setup
        when(pubSubMessageContext.getTopicPId()).thenReturn(koalaIdFactory.convertToPId(attachTopic.getId()));

        // act
        boolean result = volumeHandler.handleAnycast(pubSubMessageContext, EntityMethod.UPDATE, volume, nodeIdFull);

        // assert
        assertTrue(result);
        verify(this.attachHandler).attachVolume(volume, pubSubMessageContext, nodeIdFull);
    }

    @Test
    public void testDeliverDetachVolumePhase1() {
        // setup
        when(volume.getStatus()).thenReturn(VolumeState.DETACHING);
        when(pubSubMessageContext.getTopicPId()).thenReturn(koalaIdFactory.convertToPId(detachTopic.getId()));

        // act
        boolean result = volumeHandler.handleAnycast(pubSubMessageContext, EntityMethod.UPDATE, volume, nodeIdFull);

        // assert
        assertTrue(result);
        verify(this.detachHandler).detachVolume(volume, pubSubMessageContext, nodeIdFull);
    }

    @Test
    public void testHandleAnycastWrongTopic() {
        // setup
        PId anotherTopicPId = mock(PId.class);
        when(pubSubMessageContext.getTopicPId()).thenReturn(anotherTopicPId);
        when(volume.getStatus()).thenReturn(VolumeState.AVAILABLE);

        // act
        boolean result = volumeHandler.handleAnycast(pubSubMessageContext, EntityMethod.UPDATE, volume, nodeIdFull);

        // assert
        assertFalse(result);
    }
}
