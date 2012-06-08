package com.bt.pi.app.imagemanager;

import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.isA;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
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
import com.bt.pi.app.common.id.PiIdBuilder;
import com.bt.pi.core.continuation.PiContinuation;
import com.bt.pi.core.dht.DhtClientFactory;
import com.bt.pi.core.dht.DhtReader;
import com.bt.pi.core.id.PId;

@RunWith(MockitoJUnitRunner.class)
public class DecryptImageTaskProcessingQueueContinuationTest {
    @InjectMocks
    private DecryptImageTaskProcessingQueueContinuation decryptImageTaskProcessingQueueContinuation = new DecryptImageTaskProcessingQueueContinuation();
    private String nodeId = "nodeId";
    private String url = "url";
    @Mock
    private PiIdBuilder piIdBuilder;
    @Mock
    private PId id;
    @Mock
    private DecryptionHandler decryptionHandler;
    @Mock
    private DhtClientFactory dhtClientFactory;
    @Mock
    private DhtReader reader;
    @Mock
    private Image image;
    @Mock
    private ImageManagerApplication imageManager;

    @SuppressWarnings("unchecked")
    @Before
    public void setUp() throws Exception {
        when(this.piIdBuilder.getPId(url)).thenReturn(id);
        when(this.dhtClientFactory.createReader()).thenReturn(reader);
        this.image = mock(Image.class);
        doAnswer(new Answer<Object>() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                @SuppressWarnings("rawtypes")
                PiContinuation piContinuation = (PiContinuation) invocation.getArguments()[1];
                piContinuation.receiveResult(image);
                return null;
            }
        }).when(this.reader).getAsync(eq(id), isA(PiContinuation.class));
    }

    @Test
    public void testReceiveResult() {
        // act
        this.decryptImageTaskProcessingQueueContinuation.receiveResult(url, nodeId);

        // assert
        verify(this.decryptionHandler).decrypt(image, imageManager);
    }
}
