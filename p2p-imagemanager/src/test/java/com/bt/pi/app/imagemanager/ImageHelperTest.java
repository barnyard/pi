package com.bt.pi.app.imagemanager;

import static org.junit.Assert.fail;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.isA;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
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
import com.bt.pi.core.continuation.UpdateResolvingPiContinuation;
import com.bt.pi.core.dht.DhtClientFactory;
import com.bt.pi.core.dht.DhtWriter;
import com.bt.pi.core.id.PId;
import com.bt.pi.core.testing.UpdateResolvingContinuationAnswer;

@RunWith(MockitoJUnitRunner.class)
public class ImageHelperTest {
    @InjectMocks
    private ImageHelper imageHelper = new ImageHelper();
    @Mock
    private DhtClientFactory dhtClientFactory;
    @Mock
    private PiIdBuilder piIdBuilder;
    @Mock
    private Image image;
    private String imageId = "imageId";
    @Mock
    private DhtWriter dhtWriter;
    @Mock
    private PId imagePastryId;

    @SuppressWarnings("unchecked")
    @Before
    public void setUp() throws Exception {
        when(piIdBuilder.getPId(Image.getUrl(imageId))).thenReturn(imagePastryId);
        when(this.dhtClientFactory.createWriter()).thenReturn(dhtWriter);
        doAnswer(new Answer<Object>() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                UpdateResolvingPiContinuation continuation = (UpdateResolvingPiContinuation) invocation.getArguments()[1];
                continuation.update(image, image);
                continuation.handleResult(image);
                return null;
            }
        }).when(dhtWriter).update(eq(imagePastryId), isA(UpdateResolvingPiContinuation.class));
    }

    @Test
    public void testReceiveResult() {
        // act
        this.imageHelper.updateImageState(imageId, ImageState.FAILED);

        // assert
        verify(image).setState(ImageState.FAILED);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void shouldNotSetImageStateIfExistingEntityIsNull() {
        // setup
        UpdateResolvingContinuationAnswer a = new UpdateResolvingContinuationAnswer(null);
        doAnswer(a).when(dhtWriter).update(eq(imagePastryId), isA(UpdateResolvingPiContinuation.class));

        try {
            // act
            this.imageHelper.updateImageState(imageId, ImageState.FAILED);
        } catch (NullPointerException npe) {
            fail("Shouldn't throw an exception if exising entity is null");
        }
        // assert
        verify(image, never()).setState(ImageState.FAILED);
    }
}
