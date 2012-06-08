package com.bt.pi.app.management;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.isA;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import com.bt.pi.app.common.id.PiIdBuilder;
import com.bt.pi.core.application.activation.ApplicationRecord;
import com.bt.pi.core.application.activation.AvailabilityZoneScopedApplicationRecord;
import com.bt.pi.core.application.activation.GlobalScopedApplicationRecord;
import com.bt.pi.core.application.activation.RegionScopedApplicationRecord;
import com.bt.pi.core.dht.BlockingDhtWriter;
import com.bt.pi.core.dht.DhtClientFactory;
import com.bt.pi.core.entity.PiEntity;
import com.bt.pi.core.id.KoalaIdFactory;
import com.bt.pi.core.id.PId;
import com.bt.pi.core.parser.KoalaJsonParser;
import com.bt.pi.core.parser.KoalaPiEntityFactory;

public class ApplicationSeederTest {
    private List<String> resources;
    private PiIdBuilder piIdBuilder;
    private BlockingDhtWriter dhtWriterApplication;
    private static final String MYAPP = "myapp";
    private ApplicationSeeder applicationSeederHandler;
    private DhtClientFactory dhtClientFactory;
    private KoalaIdFactory koalaIdFactory;
    private PId applicationRecordIdInRegion;
    private PId applicationRecordIdInAvz;
    private ApplicationRecord writtenRecord;
    private BlockingDhtWriter applicationExistsWriter;

    @Before
    public void before() throws Exception {
        ArrayList<PiEntity> entities = new ArrayList<PiEntity>();
        entities.add(new GlobalScopedApplicationRecord());
        KoalaPiEntityFactory koalaPiEntityFactory = new KoalaPiEntityFactory();
        koalaPiEntityFactory.setKoalaJsonParser(new KoalaJsonParser());
        koalaPiEntityFactory.setPiEntityTypes(entities);

        koalaIdFactory = new KoalaIdFactory(99, 99);
        koalaIdFactory.setKoalaPiEntityFactory(koalaPiEntityFactory);
        piIdBuilder = new PiIdBuilder();
        piIdBuilder.setKoalaIdFactory(koalaIdFactory);

        applicationRecordIdInRegion = piIdBuilder.getPId(RegionScopedApplicationRecord.getUrl(MYAPP)).forRegion(1);
        applicationRecordIdInAvz = piIdBuilder.getPId(AvailabilityZoneScopedApplicationRecord.getUrl(MYAPP)).forGlobalAvailablityZoneCode(0x0203);

        dhtWriterApplication = mock(BlockingDhtWriter.class);

        applicationExistsWriter = mock(BlockingDhtWriter.class);
        when(applicationExistsWriter.writeIfAbsent(isA(PId.class), (PiEntity) anyObject())).thenReturn(false);

        writtenRecord = null;
        doAnswer(new Answer<Boolean>() {
            @Override
            public Boolean answer(InvocationOnMock invocation) throws Throwable {
                writtenRecord = (ApplicationRecord) invocation.getArguments()[1];
                return true;
            }
        }).when(dhtWriterApplication).writeIfAbsent(eq(applicationRecordIdInRegion), isA(ApplicationRecord.class));

        doAnswer(new Answer<Boolean>() {
            @Override
            public Boolean answer(InvocationOnMock invocation) throws Throwable {
                writtenRecord = (ApplicationRecord) invocation.getArguments()[1];
                return true;
            }
        }).when(dhtWriterApplication).writeIfAbsent(eq(applicationRecordIdInAvz), isA(ApplicationRecord.class));

        when(dhtWriterApplication.getValueWritten()).thenReturn(writtenRecord);

        dhtClientFactory = mock(DhtClientFactory.class);
        when(dhtClientFactory.createBlockingWriter()).thenReturn(dhtWriterApplication);

        resources = new ArrayList<String>();
        resources.add("10.0.0.1-10.0.0.4");
        resources.add("127.0.0.1");
        resources.add("10.19.1.200/24");

        applicationSeederHandler = new ApplicationSeeder();
        applicationSeederHandler.setDhtClientFactory(dhtClientFactory);
        applicationSeederHandler.setPiIdBuilder(piIdBuilder);
    }

    @Test
    public void shouldBeAbleToCreateApplicationRecordInRegionWithAddressRanges() {
        // act
        boolean res = applicationSeederHandler.createRegionScopedApplicationRecord(MYAPP, 1, resources);

        // assert
        assertEquals(MYAPP, writtenRecord.getApplicationName());
        assertEquals(6, writtenRecord.getRequiredActive());
        assertTrue(writtenRecord.getActiveNodeMap().containsKey("10.0.0.1"));
        assertTrue(writtenRecord.getActiveNodeMap().containsKey("10.0.0.2"));
        assertTrue(writtenRecord.getActiveNodeMap().containsKey("10.0.0.3"));
        assertTrue(writtenRecord.getActiveNodeMap().containsKey("10.0.0.4"));
        assertTrue(writtenRecord.getActiveNodeMap().containsKey("127.0.0.1"));
        assertTrue(writtenRecord.getActiveNodeMap().containsKey("10.19.1.200/24"));
        assertTrue(res);
    }

    @Test
    public void shouldBeAbleToCreateApplicationRecordInRegionAndZoneWithAddressRanges() {
        // act
        boolean res = applicationSeederHandler.createAvailabilityZoneScopedApplicationRecord(MYAPP, 2, 3, resources);

        // assert
        assertEquals(MYAPP, writtenRecord.getApplicationName());
        assertEquals(6, writtenRecord.getRequiredActive());
        assertTrue(writtenRecord.getActiveNodeMap().containsKey("10.0.0.1"));
        assertTrue(writtenRecord.getActiveNodeMap().containsKey("10.0.0.2"));
        assertTrue(writtenRecord.getActiveNodeMap().containsKey("10.0.0.3"));
        assertTrue(writtenRecord.getActiveNodeMap().containsKey("10.0.0.4"));
        assertTrue(writtenRecord.getActiveNodeMap().containsKey("127.0.0.1"));
        assertTrue(writtenRecord.getActiveNodeMap().containsKey("10.19.1.200/24"));
        assertTrue(res);
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldFailToCreateRegionScopedApplicationRecordWithoutRegion() {
        // act
        applicationSeederHandler.createRegionScopedApplicationRecord(MYAPP, null, resources);
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldFailToCreateApplicationRecordWithoutRegion() {
        // act
        applicationSeederHandler.createAvailabilityZoneScopedApplicationRecord(MYAPP, null, 3, resources);
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldFailToCreateApplicationRecordWithoutRegionAndZone() {
        // act
        applicationSeederHandler.createAvailabilityZoneScopedApplicationRecord(MYAPP, null, null, resources);
    }

    @Test
    public void shouldBeAbleToCreateApplicationRecordObjectInRegionithNonAddressResources() {
        // setup
        List<String> appResources = new ArrayList<String>();
        appResources.add("bob");
        appResources.add("mary");
        appResources.add("bill");

        // act
        boolean res = applicationSeederHandler.createRegionScopedApplicationRecord(MYAPP, 1, appResources);

        // assert
        assertEquals(MYAPP, writtenRecord.getApplicationName());
        assertEquals(3, writtenRecord.getRequiredActive());
        assertTrue(writtenRecord.getActiveNodeMap().containsKey("bob"));
        assertTrue(writtenRecord.getActiveNodeMap().containsKey("mary"));
        assertTrue(writtenRecord.getActiveNodeMap().containsKey("bill"));
        assertTrue(res);
    }

    @Test
    public void shouldHandleFailureWhenGivenAppRecordWithResourcesAlreadyExists() {
        // setup
        when(dhtClientFactory.createBlockingWriter()).thenReturn(applicationExistsWriter);

        // act
        boolean res = applicationSeederHandler.createRegionScopedApplicationRecord(MYAPP, 1, resources);

        // assert
        assertFalse(res);
        assertEquals(null, writtenRecord);
    }

}
