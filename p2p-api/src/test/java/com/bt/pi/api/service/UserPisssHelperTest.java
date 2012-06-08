package com.bt.pi.api.service;

import static org.mockito.Matchers.isA;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.HashSet;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import rice.p2p.commonapi.Id;

import com.bt.pi.app.common.id.PiIdBuilder;
import com.bt.pi.core.application.MessageContext;
import com.bt.pi.core.parser.KoalaJsonParser;
import com.bt.pi.sss.entities.BucketCollectionEntity;

@RunWith(MockitoJUnitRunner.class)
public class UserPisssHelperTest {

    @InjectMocks
    private UserPisssHelper userPisssHelper = spy(new UserPisssHelper());

    @Mock
    private ApiApplicationManager apiApplicationManager;

    @Mock
    private PiIdBuilder piIdBuilder;

    @Mock
    private Id nodePastryId;

    private KoalaJsonParser koalaJsonParser = new KoalaJsonParser();

    private String nodeId = "12345678";

    @Test
    public void shouldNotProcessMethodIfNoBucketNames() {
        // act
        userPisssHelper.deleteBucketsFromUser(null, null);
        // assert
        verify(piIdBuilder, never()).getNodeIdFromNodeId(isA(String.class));
    }

    @Test
    public void shouldDeliverDeleteBucketsMessage() throws Exception {
        // setup
        when(apiApplicationManager.getNodeIdFull()).thenReturn(nodeId);
        when(apiApplicationManager.getNodeId()).thenReturn(nodePastryId);
        when(nodePastryId.toStringFull()).thenReturn(nodeId);
        MessageContext messageContext = mock(MessageContext.class);
        when(apiApplicationManager.getMessageContext()).thenReturn(messageContext);

        // act
        userPisssHelper.deleteBucketsFromUser("username", new HashSet<String>(Arrays.asList("bucket1", "bucket2")));
        // assert
        verify(piIdBuilder).getNodeIdFromNodeId(nodeId);
        verify(userPisssHelper).routeMessageToPisss(isA(BucketCollectionEntity.class));
    }
}
