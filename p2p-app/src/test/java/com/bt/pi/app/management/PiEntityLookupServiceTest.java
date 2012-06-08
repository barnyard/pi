package com.bt.pi.app.management;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.isNull;
import static org.mockito.Matchers.notNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;

import com.bt.pi.app.common.id.PiIdLookupService;
import com.bt.pi.core.dht.BlockingDhtReader;
import com.bt.pi.core.dht.DhtClientFactory;
import com.bt.pi.core.entity.PiEntity;
import com.bt.pi.core.id.KoalaIdFactory;
import com.bt.pi.core.id.PId;
import com.bt.pi.core.parser.KoalaJsonParser;

public class PiEntityLookupServiceTest {
    private static final String _789 = "789";
    private static final String _456 = "456";
    private static final String _123 = "123";
    private static final String FUNKY_ENTITY_STRING = "funkyEntity";
    private static final String DUMMY_JSON = "{}";
    private static final String YUMMY_JSON = "{[]}";
    private static final String GUMMY_JSON = "{[]}";
    private static final String ARG = "arg";
    private PiEntityLookupService piEntityLookupService;
    private PiIdLookupService piIdLookupService;
    private KoalaJsonParser koalaJsonParser;
    private DhtClientFactory dhtClientFactory;
    private BlockingDhtReader dhtReader;
    private PiEntity funkyEntity;
    private PiEntity clunkyEntity;
    private PiEntity junkyEntity;
    private PId id;
    private PId otherId;
    private PId anotherId;

    @Before
    public void before() throws Throwable {
        id = mock(PId.class);
        when(id.toStringFull()).thenReturn(_123);
        otherId = mock(PId.class);
        when(otherId.toStringFull()).thenReturn(_456);
        anotherId = mock(PId.class);
        when(anotherId.toStringFull()).thenReturn(_789);
        funkyEntity = mock(PiEntity.class);
        clunkyEntity = mock(PiEntity.class);
        junkyEntity = mock(PiEntity.class);

        piIdLookupService = mock(PiIdLookupService.class);
        when(piIdLookupService.invokeMethod(eq(FUNKY_ENTITY_STRING), (String) isNull(), (String) isNull())).thenReturn(_123);
        when(piIdLookupService.invokeMethod(eq(FUNKY_ENTITY_STRING), (String) notNull(), (String) isNull())).thenReturn(_456);
        when(piIdLookupService.invokeMethod(eq(FUNKY_ENTITY_STRING), (String) notNull(), (String) notNull())).thenReturn(_789);

        dhtReader = mock(BlockingDhtReader.class);
        when(dhtReader.get(eq(id))).thenReturn(funkyEntity);
        when(dhtReader.get(eq(otherId))).thenReturn(clunkyEntity);
        when(dhtReader.get(eq(anotherId))).thenReturn(junkyEntity);

        dhtClientFactory = mock(DhtClientFactory.class);
        when(dhtClientFactory.createBlockingReader()).thenReturn(dhtReader);

        koalaJsonParser = mock(KoalaJsonParser.class);
        when(koalaJsonParser.getJson(funkyEntity)).thenReturn(DUMMY_JSON);
        when(koalaJsonParser.getJson(clunkyEntity)).thenReturn(YUMMY_JSON);
        when(koalaJsonParser.getJson(junkyEntity)).thenReturn(GUMMY_JSON);

        KoalaIdFactory koalaIdFactory = mock(KoalaIdFactory.class);
        when(koalaIdFactory.buildPIdFromHexString(eq(_123))).thenReturn(id);
        when(koalaIdFactory.buildPIdFromHexString(eq(_456))).thenReturn(otherId);
        when(koalaIdFactory.buildPIdFromHexString(eq(_789))).thenReturn(anotherId);

        piEntityLookupService = new PiEntityLookupService();
        piEntityLookupService.setDhtClientFactory(dhtClientFactory);
        piEntityLookupService.setPiIdLookupService(piIdLookupService);
        piEntityLookupService.setKoalaJsonParser(koalaJsonParser);
        piEntityLookupService.setKoalaIdFactory(koalaIdFactory);
    }

    @Test
    public void shouldGetEntityWithNoArgs() {
        // atc
        String res = piEntityLookupService.lookup0(FUNKY_ENTITY_STRING);

        // assert
        assertEquals(DUMMY_JSON, res);
    }

    @Test
    public void shouldGetEntityWithOneArg() {
        // act
        String res = piEntityLookupService.lookup1(FUNKY_ENTITY_STRING, ARG);

        // assert
        assertEquals(YUMMY_JSON, res);
    }

    @Test
    public void shouldGetEntityWithTwoArgs() {
        // act
        String res = piEntityLookupService.lookup2(FUNKY_ENTITY_STRING, ARG, "AVAILABILITY_ZONE");

        // assert
        assertEquals(GUMMY_JSON, res);
    }

    @Test
    public void shouldReturnErrorStringWhenLookupThrowsException() throws Throwable {
        // setup
        when(piIdLookupService.invokeMethod(eq("poop"), (String) any(), (String) any()));

        // act
        String res = piEntityLookupService.lookup0("poop");

        // assert
        assertTrue(res.toLowerCase().contains("error"));
        JSONObject jObject = new JSONObject(res);
        assertNotNull(jObject.get("Error"));
    }
}
