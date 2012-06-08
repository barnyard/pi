package com.bt.pi.app.instancemanager.watchers;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import rice.p2p.commonapi.Id;

import com.bt.pi.core.id.KoalaIdFactory;
import com.bt.pi.core.id.PId;
import com.bt.pi.core.past.content.KoalaGCPastMetadata;

@RunWith(MockitoJUnitRunner.class)
public class LocalStorageUserHandlerTest {
    @InjectMocks
    private LocalStorageUserHandler localStorageUserHandler = new LocalStorageUserHandler();
    @Mock
    private KoalaGCPastMetadata metadata;
    @Mock
    private KoalaIdFactory koalaIdFactory;
    @Mock
    private Id id1;
    @Mock
    private PId userPId1;
    @Mock
    private Id id2;
    @Mock
    private PId userPId2;
    @Mock
    private Id id3;
    @Mock
    private PId userPId3;

    @Test
    public void testGetEntityType() {
        assertEquals("User", localStorageUserHandler.getEntityType());
    }

    @Test
    public void shouldStoreUserPIds() {
        // setup
        when(userPId1.toStringFull()).thenReturn("234213423343242343000");
        when(userPId2.toStringFull()).thenReturn("234213423343242343000");
        when(userPId3.toStringFull()).thenReturn("234213423343242343001");
        when(koalaIdFactory.convertToPId(id1)).thenReturn(userPId1);
        when(koalaIdFactory.convertToPId(id2)).thenReturn(userPId2);
        when(koalaIdFactory.convertToPId(id3)).thenReturn(userPId3);

        // act
        this.localStorageUserHandler.doHandle(id1, metadata);
        this.localStorageUserHandler.doHandle(id2, metadata);
        this.localStorageUserHandler.doHandle(id3, metadata);

        // assert
        assertEquals(2, this.localStorageUserHandler.getUserPIds().size());
        assertTrue(this.localStorageUserHandler.getUserPIds().contains(userPId1));
        assertTrue(this.localStorageUserHandler.getUserPIds().contains(userPId2));
    }
}
