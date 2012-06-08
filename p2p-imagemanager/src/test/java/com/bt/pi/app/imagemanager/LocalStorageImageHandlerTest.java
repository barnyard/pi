package com.bt.pi.app.imagemanager;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.argThat;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.isA;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentMatcher;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.runners.MockitoJUnitRunner;
import org.mockito.stubbing.Answer;

import rice.Continuation;
import rice.p2p.commonapi.Id;
import rice.p2p.commonapi.NodeHandle;

import com.bt.pi.app.common.entities.Image;
import com.bt.pi.app.common.entities.Instance;
import com.bt.pi.app.imagemanager.reporting.ImageReportEntity;
import com.bt.pi.app.imagemanager.reporting.ImageReportEntityCollection;
import com.bt.pi.core.application.reporter.ReportingApplication;
import com.bt.pi.core.dht.DhtClientFactory;
import com.bt.pi.core.dht.DhtReader;
import com.bt.pi.core.entity.PiEntity;
import com.bt.pi.core.id.KoalaIdFactory;
import com.bt.pi.core.id.KoalaIdUtils;
import com.bt.pi.core.id.PId;
import com.bt.pi.core.past.content.KoalaGCPastMetadata;

@RunWith(MockitoJUnitRunner.class)
public class LocalStorageImageHandlerTest {

    @InjectMocks
    private LocalStorageImageHandler handler = spy(new LocalStorageImageHandler());

    @Mock
    private KoalaIdFactory koalaIdFactory;

    @Mock
    private KoalaIdUtils koalaIdUtils;

    @Mock
    private ReportingApplication reportingApplication;

    @Mock
    private DhtClientFactory dhtClientFactory;

    @Mock
    private ImageReportEntity imageReportEntity;

    @Mock
    private ScheduledExecutorService scheduledExecutorService;

    @Mock
    private KoalaGCPastMetadata metadata;

    @Before
    public void setUp() throws Exception {
        when(reportingApplication.getNodeIdFull()).thenReturn("NODE_ID_FULL");
        when(reportingApplication.getLeafNodeHandles()).thenReturn((Collection) new ArrayList<NodeHandle>());
        when(koalaIdUtils.isIdClosestToMe(isA(String.class), isA(Collection.class), isA(Id.class))).thenAnswer(new Answer<Object>() {

            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                System.err.println("Returning true");
                return true;
            }

        });
        when(metadata.getEntityType()).thenReturn(new Image().getType());
        when(metadata.isDeletedAndDeletable()).thenReturn(false);

    }

    @Test
    public void shouldNotHandleEntitiesOtherThanImages() {
        // setup
        when(metadata.getEntityType()).thenReturn(new Instance().getType());
        // act
        handler.handle(mock(Id.class), metadata);
        // assert
        verify(dhtClientFactory, never()).createReader();

    }

    @SuppressWarnings("unchecked")
    @Test
    public void shouldAddImageToReportEntities() {
        // setup
        DhtReader dhtReader = mock(DhtReader.class);
        final Image image = mock(Image.class);
        when(image.getImageId()).thenReturn("IMAGE_ID");
        Id id = mock(Id.class);
        PId pid = mock(PId.class);
        when(koalaIdFactory.convertToPId(id)).thenReturn(pid);
        when(dhtClientFactory.createReader()).thenReturn(dhtReader);
        doAnswer(new Answer<Object>() {

            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                System.err.println("Mocking receive result");
                Continuation continuation = (Continuation) invocation.getArguments()[1];
                continuation.receiveResult(image);
                return null;
            }
        }).when(dhtReader).getAsync(isA(PId.class), isA(Continuation.class));

        // act
        handler.doHandle(id, metadata);

        // assert
        assertTrue(handler.getImageReportEntities().contains(new ImageReportEntity(image)));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void shouldNotAddExistingImageToReportEntities() {
        // setup
        DhtReader dhtReader = mock(DhtReader.class);
        final Image image = mock(Image.class);
        handler.getImageReportEntities().add(new ImageReportEntity(image));
        Id id = mock(Id.class);
        PId pid = mock(PId.class);
        when(dhtClientFactory.createReader()).thenReturn(dhtReader);
        doAnswer(new Answer<Object>() {

            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                System.err.println("Mocking receive result");
                Continuation continuation = (Continuation) invocation.getArguments()[1];
                continuation.receiveResult(image);
                return null;
            }
        }).when(dhtReader).getAsync(isA(PId.class), isA(Continuation.class));
        // act
        handler.doHandle(id, metadata);
        // assert
        assertEquals(1, handler.getImageReportEntities().size());
    }

    @Test
    public void shouldCheckAndReportToSupernodes() {
        // setup
        handler.getImageReportEntities().add(imageReportEntity);

        // act
        handler.checkAndReportToSupernodes();

        // assert
        verify(reportingApplication).sendReportingUpdateToASuperNode(argThat(new ArgumentMatcher<PiEntity>() {

            @Override
            public boolean matches(Object argument) {
                if (!(argument instanceof ImageReportEntityCollection))
                    return false;
                ImageReportEntityCollection instanceReportEntityCollection = (ImageReportEntityCollection) argument;
                return ((List) instanceReportEntityCollection.getEntities()).get(0).equals(imageReportEntity);

            };
        }));
    }

    @Test
    public void shouldScheduleExecutorServiceEveryFiveMins() {
        // act
        handler.scheduleReportToSupernodes();
        // assert
        verify(scheduledExecutorService).scheduleWithFixedDelay(isA(Runnable.class), eq(0L), eq(300L), eq(TimeUnit.SECONDS));
    }

    @Test
    public void shouldReturnImageAsEntityType() {
        // assert
        assertEquals(new Image().getType(), handler.getEntityType());
    }

}
