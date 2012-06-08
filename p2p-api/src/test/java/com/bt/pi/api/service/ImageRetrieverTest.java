package com.bt.pi.api.service;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.isA;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.runners.MockitoJUnitRunner;
import org.mockito.stubbing.Answer;

import com.bt.pi.app.common.entities.AvailabilityZone;
import com.bt.pi.app.common.entities.AvailabilityZones;
import com.bt.pi.app.common.id.PiIdBuilder;
import com.bt.pi.app.imagemanager.reporting.ImageReportEntity;
import com.bt.pi.app.imagemanager.reporting.ImageReportEntityCollection;
import com.bt.pi.core.application.MessageContext;
import com.bt.pi.core.application.activation.SuperNodeApplicationCheckPoints;
import com.bt.pi.core.application.reporter.ReportingApplication;
import com.bt.pi.core.continuation.PiContinuation;
import com.bt.pi.core.dht.cache.BlockingDhtCache;
import com.bt.pi.core.entity.EntityMethod;
import com.bt.pi.core.id.KoalaIdFactory;
import com.bt.pi.core.id.PId;

@RunWith(MockitoJUnitRunner.class)
public class ImageRetrieverTest {

    private static final String CHECKPOINT_1 = "CHECKPOINT_1";
    private static final String CHECKPOINT_2 = "CHECKPOINT_2";
    @InjectMocks
    private ImageRetriever imageRetriever = spy(new ImageRetriever());

    @Mock
    private BlockingDhtCache blockingDhtCache;
    @Mock
    private KoalaIdFactory koalaIdFactory;
    @Mock
    private PiIdBuilder piIdBuilder;
    @Mock
    private ApiApplicationManager apiApplicationManager;

    private AvailabilityZones availabilityZones;
    private AvailabilityZone availabilityZone1 = new AvailabilityZone("AVZ_1", 1, 1, null);
    private AvailabilityZone availabilityZone2 = new AvailabilityZone("AVZ_2", 2, 1, null);

    @Mock
    private PId availabilityZonesPId;
    @Mock
    private PId checkpointsId;

    @Mock
    private PId supernode1Id;

    @Mock
    private PId supernode2Id;

    @Mock
    private SuperNodeApplicationCheckPoints checkPoints;
    @Mock
    private ImageReportEntity imageInAvz1;
    @Mock
    private ImageReportEntity imageInAvz2;
    private Set<ImageReportEntity> allImages;

    private List<ImageReportEntity> imagesFromSupernode1 = Arrays.asList(new ImageReportEntity[] { imageInAvz1 });

    private List<ImageReportEntity> imagesFromSupernode2 = Arrays.asList(new ImageReportEntity[] { imageInAvz2 });

    @Mock
    private ImageReportEntityCollection collection1;

    @Mock
    private ImageReportEntityCollection collection2;

    @Mock
    private MessageContext messageContext;

    @SuppressWarnings("unchecked")
    @Before
    public void setup() {

        availabilityZones = new AvailabilityZones();
        availabilityZones.addAvailabilityZone(availabilityZone1);
        availabilityZones.addAvailabilityZone(availabilityZone2);
        when(piIdBuilder.getAvailabilityZonesId()).thenReturn(availabilityZonesPId);
        when(blockingDhtCache.get(availabilityZonesPId)).thenReturn(availabilityZones);
        when(koalaIdFactory.buildPId(SuperNodeApplicationCheckPoints.URL)).thenReturn(checkpointsId);
        when(blockingDhtCache.get(checkpointsId)).thenReturn(checkPoints);
        when(checkPoints.getRandomSuperNodeCheckPoint(ReportingApplication.APPLICATION_NAME, 1, 1)).thenReturn(CHECKPOINT_1);
        when(checkPoints.getRandomSuperNodeCheckPoint(ReportingApplication.APPLICATION_NAME, 1, 2)).thenReturn(CHECKPOINT_2);
        when(koalaIdFactory.buildPIdFromHexString(CHECKPOINT_1)).thenReturn(supernode1Id);
        when(koalaIdFactory.buildPIdFromHexString(CHECKPOINT_2)).thenReturn(supernode2Id);
        when(apiApplicationManager.newMessageContext()).thenReturn(messageContext);
        when(collection1.getEntities()).thenReturn(imagesFromSupernode1);
        when(collection2.getEntities()).thenReturn(imagesFromSupernode2);
        allImages = new HashSet<ImageReportEntity>();
        allImages.addAll(imagesFromSupernode1);
        allImages.addAll(imagesFromSupernode2);
        doAnswer(new Answer<Object>() {

            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                @SuppressWarnings("rawtypes")
                PiContinuation continuation = (PiContinuation) invocation.getArguments()[4];
                continuation.receiveResult(collection1);
                return null;
            }
        }).when(messageContext).routePiMessageToApplication(eq(supernode1Id), eq(EntityMethod.GET), isA(ImageReportEntityCollection.class), eq(ReportingApplication.APPLICATION_NAME), isA(PiContinuation.class));

        doAnswer(new Answer<Object>() {

            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                PiContinuation continuation = (PiContinuation) invocation.getArguments()[4];
                continuation.receiveResult(collection2);
                return null;
            }
        }).when(messageContext).routePiMessageToApplication(eq(supernode2Id), eq(EntityMethod.GET), isA(ImageReportEntityCollection.class), eq(ReportingApplication.APPLICATION_NAME), isA(PiContinuation.class));

    }

    @Test
    public void shouldRetrieveAllImages() {
        // act
        Set<ImageReportEntity> images = imageRetriever.retrieveImagesFromSupernodes(new ArrayList<String>());
        // assert
        assertEquals(allImages, images);

    }

    @Test
    public void shouldReturnEmptyListIfNoSupernodesAreSeeded() {
        // setup
        when(checkPoints.getRandomSuperNodeCheckPoint(isA(String.class), isA(Integer.class), isA(Integer.class))).thenReturn(null);
        // act
        Set<ImageReportEntity> images = imageRetriever.retrieveImagesFromSupernodes(new ArrayList<String>());
        // assert
        assertEquals(0, images.size());

    }

}
