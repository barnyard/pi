package com.bt.pi.app.common.id;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.Before;
import org.junit.Test;

import com.bt.pi.app.common.entities.Instance;
import com.bt.pi.core.id.PId;

public class PiIdLookupServiceTest {
    private static final String DUMMY_ID_STRING = "123";
    private PiIdLookupService piIdLookupService;
    private PiIdBuilder piIdBuilder;
    private PId dummyId;

    @Before
    public void before() {
        dummyId = mock(PId.class);
        when(dummyId.toStringFull()).thenReturn(DUMMY_ID_STRING);

        piIdBuilder = mock(PiIdBuilder.class);
        when(piIdBuilder.getRegionsId()).thenReturn(dummyId);
        when(piIdBuilder.getPIdForEc2AvailabilityZone(Instance.getUrl("i-abc"))).thenReturn(dummyId);

        piIdLookupService = new PiIdLookupService();
        piIdLookupService.setPiIdBuilder(piIdBuilder);
    }

    @Test
    public void shouldReturnErrorForNullId() {
        // act
        String res = piIdLookupService.lookup(null);

        // assert
        assertTrue(res.startsWith("ERROR:"));
    }

    @Test
    public void shouldReturnErrorForEmptyId() {
        // act
        String res = piIdLookupService.lookup("  ");

        // assert
        assertTrue(res.startsWith("ERROR:"));
    }

    @Test
    public void shouldReturnErrorForNonExistentIdDesc() {
        // act
        String res = piIdLookupService.lookup("something i dreamt up");

        // assert
        assertTrue(res.startsWith("ERROR:"));
    }

    @Test
    public void shouldReturnErrorForUnexpectedError() {
        // setup
        piIdLookupService.setPiIdBuilder(null);

        // act
        String res = piIdLookupService.lookup("publicIpIndexId");

        // assert
        assertTrue(res.startsWith("ERROR: id lookup unexpectedly failed"));
    }

    @Test
    public void shouldLookUpIdWithNoArgs() {
        // act
        String res = piIdLookupService.lookup("regionsId");

        // assert
        assertEquals(DUMMY_ID_STRING, res);
    }

    @Test
    public void shouldLookUpIdWithNoArgsBySpecifyingBlankArgs() {
        // act
        String res = piIdLookupService.lookup("regionsId", "  ", null);

        // assert
        assertEquals(DUMMY_ID_STRING, res);
    }

    @Test
    public void shouldLookUpIdWithTwoArgs() {
        // act
        String res = piIdLookupService.lookup("pIdForEc2AvailabilityZone", Instance.getUrl("i-abc"), null);

        // assert
        assertEquals(DUMMY_ID_STRING, res);
    }

}
