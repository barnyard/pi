package com.bt.pi.app.management;

import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.isA;
import static org.mockito.Matchers.isNull;
import static org.mockito.Matchers.notNull;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.Before;
import org.junit.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import com.bt.pi.app.common.id.PiIdLookupService;
import com.bt.pi.core.application.activation.ApplicationRecord;
import com.bt.pi.core.continuation.UpdateResolver;
import com.bt.pi.core.dht.BlockingDhtWriter;
import com.bt.pi.core.dht.DhtClientFactory;
import com.bt.pi.core.entity.PiEntity;
import com.bt.pi.core.id.KoalaIdFactory;
import com.bt.pi.core.id.PId;
import com.bt.pi.core.parser.KoalaJsonParser;

public class PiEntityUpdateServiceTest {
    private static final String _123 = "123";
    private static final String FUNKY_ENTITY_STRING = "funkyEntity";
    private PiEntityUpdateService piEntityUpdateService;
    private PiIdLookupService piIdLookupService;
    private KoalaJsonParser koalaJsonParser;
    private DhtClientFactory dhtClientFactory;
    private BlockingDhtWriter dhtWriter;
    private PId id;

    @SuppressWarnings("unchecked")
    @Before
    public void before() {
        id = mock(PId.class);
        when(id.toStringFull()).thenReturn(_123);

        piIdLookupService = mock(PiIdLookupService.class);
        when(piIdLookupService.lookup(eq(FUNKY_ENTITY_STRING), (String) notNull(), (String) isNull())).thenReturn(_123);

        dhtWriter = mock(BlockingDhtWriter.class);
        doAnswer(new Answer<Object>() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                PiEntity piEntity = ((UpdateResolver<PiEntity>) invocation.getArguments()[2]).update(null, null);
                assertTrue(piEntity instanceof ApplicationRecord);
                return null;
            }
        }).when(dhtWriter).update(eq(id), (PiEntity) isNull(), isA(UpdateResolver.class));

        dhtClientFactory = mock(DhtClientFactory.class);
        when(dhtClientFactory.createBlockingWriter()).thenReturn(dhtWriter);

        koalaJsonParser = new KoalaJsonParser();

        KoalaIdFactory koalaIdFactory = mock(KoalaIdFactory.class);
        when(koalaIdFactory.buildPIdFromHexString(eq(_123))).thenReturn(id);

        piEntityUpdateService = new PiEntityUpdateService();
        piEntityUpdateService.setDhtClientFactory(dhtClientFactory);
        piEntityUpdateService.setKoalaJsonParser(koalaJsonParser);
        piEntityUpdateService.setKoalaIdFactory(koalaIdFactory);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void shouldUpdateEntity() throws Exception {
        String json = "{ \"type\" : \"RegionScopedApplicationRecord\", \"version\" : 257, \"url\" : \"regionapp:pi-api-manager\", \"applicationName\" : \"pi-api-manager\", \"requiredActive\" : 1, \"activeNodeMap\" : { \"1\" : null } }";

        // act
        piEntityUpdateService.update(id.toStringFull(), "com.bt.pi.core.application.activation.RegionScopedApplicationRecord", json);

        // assert
        verify(dhtWriter).update(eq(id), (PiEntity) isNull(), isA(UpdateResolver.class));
    }
}
