package com.bt.pi.app.management;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.isA;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.Before;
import org.junit.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import com.bt.pi.app.common.entities.InstanceTypeConfiguration;
import com.bt.pi.app.common.entities.InstanceTypes;
import com.bt.pi.app.common.id.PiIdBuilder;
import com.bt.pi.core.continuation.UpdateResolver;
import com.bt.pi.core.dht.BlockingDhtWriter;
import com.bt.pi.core.dht.DhtClientFactory;
import com.bt.pi.core.entity.PiEntity;
import com.bt.pi.core.id.KoalaIdFactory;
import com.bt.pi.core.id.PId;
import com.bt.pi.core.parser.KoalaJsonParser;
import com.bt.pi.core.parser.KoalaPiEntityFactory;

public class InstanceSeederTest {

    private AtomicBoolean writeSucceeded;
    @SuppressWarnings("unchecked")
    private Answer entityWrittenAnswer;
    private KoalaIdFactory koalaIdFactory;
    private PiIdBuilder piIdBuilder;
    private DhtClientFactory dhtClientFactory;
    private BlockingDhtWriter dhtWriterInstanceType;
    private PiEntity writtenEntity;
    private InstanceSeeder instanceSeeder;
    private PId instanceTypeId;

    @Before
    public void before() throws Exception {
        KoalaPiEntityFactory koalaPiEntityFactory = new KoalaPiEntityFactory();// {
        // @Override
        // protected String readFile(String aPiEntitiesJsonFile) throws IOException {
        // return
        // "{\"persistablePiEntityMappings\":[{\"type\" : \"InstanceTypes\", \"typeCode\" : 13, \"scheme\" : \"instancetypes\"}]}";
        // }
        // };
        koalaPiEntityFactory.setKoalaJsonParser(new KoalaJsonParser());
        koalaPiEntityFactory.setPiEntityTypes(Arrays.asList(new PiEntity[] { new InstanceTypes() }));
        // / koalaPiEntityFactory.setPersistedEntityMappings();

        koalaIdFactory = new KoalaIdFactory(99, 99);
        koalaIdFactory.setKoalaPiEntityFactory(koalaPiEntityFactory);
        piIdBuilder = new PiIdBuilder();
        piIdBuilder.setKoalaIdFactory(koalaIdFactory);

        instanceTypeId = piIdBuilder.getPId(new InstanceTypes());

        writtenEntity = null;
        writeSucceeded = new AtomicBoolean(true);

        dhtWriterInstanceType = mock(BlockingDhtWriter.class);

        entityWrittenAnswer = new Answer<Boolean>() {
            @Override
            public Boolean answer(InvocationOnMock invocation) throws Throwable {
                if (writeSucceeded.get()) {
                    writtenEntity = (PiEntity) invocation.getArguments()[1];
                }
                return writeSucceeded.get();
            }
        };

        dhtClientFactory = mock(DhtClientFactory.class);
        when(dhtClientFactory.createBlockingWriter()).thenReturn(dhtWriterInstanceType);

        instanceSeeder = new InstanceSeeder();
        instanceSeeder.setPiIdBuilder(piIdBuilder);
        instanceSeeder.setDhtClientFactory(dhtClientFactory);
    }

    @Test
    public void shouldConfigureInstanceTypes() throws Exception {
        // setup
        doAnswer(entityWrittenAnswer).when(dhtWriterInstanceType).writeIfAbsent(isA(PId.class), isA(InstanceTypes.class));
        InstanceTypeConfiguration type1 = new InstanceTypeConfiguration("m1.small", 1, 512, 10);
        InstanceTypeConfiguration type2 = new InstanceTypeConfiguration("m2.small", 2, 512, 5);
        InstanceTypeConfiguration type3 = new InstanceTypeConfiguration("m3.small", 3, 512, 1);

        String names = "m1.small;m2.small;m3.small";
        String cores = "1;2;3";
        String memory = "512;512;512";
        String disk = "10;5;1";

        // act
        boolean result = instanceSeeder.configureInstanceTypes(names, cores, memory, disk);

        // assert
        assertTrue(result);
        assertThatPiEntityIsCorrect(type1, type2, type3);
    }

    private void assertThatPiEntityIsCorrect(InstanceTypeConfiguration type1, InstanceTypeConfiguration type2, InstanceTypeConfiguration type3) {
        assertTrue(writtenEntity instanceof InstanceTypes);
        assertEquals(type1, ((InstanceTypes) writtenEntity).getInstanceTypeConfiguration(type1.getInstanceType()));
        assertEquals(type2, ((InstanceTypes) writtenEntity).getInstanceTypeConfiguration(type2.getInstanceType()));
        assertEquals(type3, ((InstanceTypes) writtenEntity).getInstanceTypeConfiguration(type3.getInstanceType()));
    }

    @Test
    public void shouldNotConfigureInstanceTypesIfAlreadyExists() throws Exception {
        // setup
        writeSucceeded.set(false);

        String names = "m1.small;m2.small;m3.small";
        String cores = "1;2;3";
        String memory = "512;512;512";
        String disk = "10;5;1";

        // act
        boolean result = instanceSeeder.configureInstanceTypes(names, cores, memory, disk);

        // assert
        assertFalse(result);
        assertEquals(null, writtenEntity);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void shouldNotConfigureInstanceTypesIfArgumentNumbersMismatch() throws Exception {
        // setup
        writeSucceeded.set(false);

        String names = "m1.small;m2.small;m3.small";
        String cores = "1;2;3";
        String memory = "512;512;512";
        String disk = "10;5;1;4";

        // act
        boolean result = instanceSeeder.configureInstanceTypes(names, cores, memory, disk);

        // assert
        assertFalse(result);
        verify(dhtWriterInstanceType, never()).update(eq(instanceTypeId), isA(InstanceTypes.class), isA(UpdateResolver.class));
        assertEquals(null, writtenEntity);
    }
}
