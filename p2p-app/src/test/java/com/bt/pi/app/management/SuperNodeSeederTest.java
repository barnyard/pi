package com.bt.pi.app.management;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.isA;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.when;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import com.bt.pi.core.application.activation.SuperNodeApplicationCheckPoints;
import com.bt.pi.core.continuation.UpdateResolvingPiContinuation;
import com.bt.pi.core.dht.BlockingDhtWriter;
import com.bt.pi.core.dht.DhtClientFactory;
import com.bt.pi.core.id.KoalaIdFactory;
import com.bt.pi.core.id.PId;
import com.bt.pi.core.testing.UpdateResolvingContinuationAnswer;

@RunWith(MockitoJUnitRunner.class)
public class SuperNodeSeederTest {
    @Mock
    private KoalaIdFactory koalaIdFactory;
    @Mock
    private DhtClientFactory dhtClientFactory;
    @Mock
    private BlockingDhtWriter blockingDhtWriter;
    @Mock
    private PId id;

    @InjectMocks
    private SuperNodeSeeder superNodeSeeder = new SuperNodeSeeder();

    @Before
    public void setup() {
        when(dhtClientFactory.createBlockingWriter()).thenReturn(blockingDhtWriter);
        when(koalaIdFactory.buildPId(SuperNodeApplicationCheckPoints.URL)).thenReturn(id);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void shouldCreateASuperNodeApplicationCheckPointsIfItDoesntExist() {
        // setup
        UpdateResolvingContinuationAnswer answer = new UpdateResolvingContinuationAnswer(null);
        doAnswer(answer).when(blockingDhtWriter).update(eq(id), isA(SuperNodeApplicationCheckPoints.class), isA(UpdateResolvingPiContinuation.class));

        // act
        superNodeSeeder.configureNumberOfSuperNodes("some-app", 4, 0);

        // assert
        SuperNodeApplicationCheckPoints result = (SuperNodeApplicationCheckPoints) answer.getResult();
        assertNotNull(result);
        assertEquals(4, result.getNumberOfSuperNodesPerApplication().get("some-app").intValue());
        assertEquals(0, result.getOffsetPerApplication().get("some-app").intValue());
    }

    @SuppressWarnings("unchecked")
    @Test
    public void shouldUpdateSuperNodeApplicationCheckPoints() {
        // setup
        SuperNodeApplicationCheckPoints app = new SuperNodeApplicationCheckPoints();
        app.getNumberOfSuperNodesPerApplication().put("some-app", 6);
        app.getOffsetPerApplication().put("some-app", 2);

        UpdateResolvingContinuationAnswer answer = new UpdateResolvingContinuationAnswer(app);
        doAnswer(answer).when(blockingDhtWriter).update(eq(id), isA(SuperNodeApplicationCheckPoints.class), isA(UpdateResolvingPiContinuation.class));

        // act
        superNodeSeeder.configureNumberOfSuperNodes("some-app", 16, 1);

        // assert
        SuperNodeApplicationCheckPoints result = (SuperNodeApplicationCheckPoints) answer.getResult();
        assertNotNull(result);
        assertEquals(16, result.getNumberOfSuperNodesPerApplication().get("some-app").intValue());
        assertEquals(1, result.getOffsetPerApplication().get("some-app").intValue());
    }
}
