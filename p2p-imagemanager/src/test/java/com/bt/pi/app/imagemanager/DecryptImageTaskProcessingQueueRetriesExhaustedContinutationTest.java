package com.bt.pi.app.imagemanager;

import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.isA;
import static org.mockito.Mockito.doAnswer;
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

import com.bt.pi.app.common.entities.Image;
import com.bt.pi.app.common.entities.ImageState;
import com.bt.pi.app.common.id.PiIdBuilder;
import com.bt.pi.core.continuation.PiContinuation;
import com.bt.pi.core.dht.DhtClientFactory;
import com.bt.pi.core.dht.DhtReader;
import com.bt.pi.core.id.PId;

@RunWith(MockitoJUnitRunner.class)
public class DecryptImageTaskProcessingQueueRetriesExhaustedContinutationTest {
    @InjectMocks
    private DecryptImageTaskProcessingQueueRetriesExhaustedContinutation decryptImageTaskProcessingQueueRetriesExhaustedContinutation = new DecryptImageTaskProcessingQueueRetriesExhaustedContinutation();
    @Mock
    private DhtClientFactory dhtClientFactory;
    @Mock
    private PiIdBuilder piIdBuilder;
    private String nodeId = "jughsa;dghjsg";
    private String uri = "img:123";
    @Mock
    private PId reservationPastryId;
    @Mock
    private DhtReader dhtReader;
    @Mock
    private Image image;
    private String imageId = "imageId";
    @Mock
    private ImageHelper imageHelper;

    @SuppressWarnings("unchecked")
    @Before
    public void setUp() throws Exception {
        when(this.piIdBuilder.getPId(uri)).thenReturn(reservationPastryId);
        when(this.dhtClientFactory.createReader()).thenReturn(dhtReader);
        doAnswer(new Answer<Object>() {
            @SuppressWarnings("rawtypes")
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                PiContinuation continuation = (PiContinuation) invocation.getArguments()[1];
                continuation.handleResult(image);
                return null;
            }
        }).when(dhtReader).getAsync(eq(reservationPastryId), isA(PiContinuation.class));

        when(image.getImageId()).thenReturn(imageId);
    }

    @Test
    public void testReceiveResult() {
        // act
        this.decryptImageTaskProcessingQueueRetriesExhaustedContinutation.receiveResult(uri, nodeId);

        // assert
        verify(imageHelper).updateImageState(imageId, ImageState.FAILED);
    }
}
