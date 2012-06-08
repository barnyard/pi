package com.bt.pi.api.utils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.when;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import com.bt.pi.app.common.entities.Volume;
import com.bt.pi.app.common.id.PiIdBuilder;
import com.bt.pi.app.common.util.Base62Utils;
import com.bt.pi.core.dht.BlockingDhtReader;
import com.bt.pi.core.dht.DhtClientFactory;
import com.bt.pi.core.id.PId;

@RunWith(PowerMockRunner.class)
@PrepareForTest(Base62Utils.class)
@PowerMockIgnore({ "org.apache.commons.logging.*", "org.apache.log4j.*" })
public class IdFactoryTest {
    @InjectMocks
    private IdFactory idFactory = new IdFactory();
    @Mock
    private DhtClientFactory dhtClientFactory;
    @Mock
    private BlockingDhtReader blockingDhtReader;
    @Mock
    private Volume volume1;
    @Mock
    private PiIdBuilder piIdBuilder;
    @Mock
    private PId dhtId;
    private int globalAvzCode;

    @Before
    public void setUp() throws Exception {

        globalAvzCode = 0x0203;

        when(piIdBuilder.generateBase62Ec2Id(anyString(), anyInt())).thenAnswer(new Answer<String>() {
            @Override
            public String answer(InvocationOnMock invocation) throws Throwable {
                return new PiIdBuilder().generateBase62Ec2Id((String) invocation.getArguments()[0], (Integer) invocation.getArguments()[1]);
            }
        });
        when(piIdBuilder.generateStandardEc2Id(anyString())).thenAnswer(new Answer<String>() {
            @Override
            public String answer(InvocationOnMock invocation) throws Throwable {
                return new PiIdBuilder().generateStandardEc2Id((String) invocation.getArguments()[0]);
            }
        });
        when(this.dhtClientFactory.createBlockingReader()).thenReturn(this.blockingDhtReader);
    }

    @Test
    public void testCreateNewVolumeId() {
        // act
        String result = this.idFactory.createNewVolumeId(globalAvzCode);

        // assert
        assertEquals(12, result.length());
        assertTrue(result.startsWith("vol-"));
    }

    @Test
    public void testCreateNewKernelId() {
        // act
        String result = this.idFactory.createNewKernelId();

        // assert
        assertEquals(12, result.length());
        assertTrue(result.startsWith("pki-"));
    }

    @Test
    public void testCreateNewRamdiskId() {
        // act
        String result = this.idFactory.createNewRamdiskId();

        // assert
        assertEquals(12, result.length());
        assertTrue(result.startsWith("pri-"));
    }

    @Test
    public void testCreateNewVolumeIdAlreadyExists() {
        // setup
        String id1 = "vol-ABCDEFGH";
        String id2 = "vol-ZZZZZZZZ";

        when(piIdBuilder.generateBase62Ec2Id("vol", globalAvzCode)).thenReturn(id1).thenReturn(id2);
        when(this.piIdBuilder.getPIdForEc2AvailabilityZone(Volume.getUrl(id1))).thenReturn(dhtId);
        when(this.piIdBuilder.getPIdForEc2AvailabilityZone(Volume.getUrl(id2))).thenReturn(dhtId);
        when(this.blockingDhtReader.get(eq(dhtId))).thenReturn(volume1).thenReturn(null);

        // act
        String result = this.idFactory.createNewVolumeId(globalAvzCode);

        // assert
        assertEquals(id2, result);
    }

    @Test
    public void testCreateNewVolumeIdAlreadyExistsButDeleted() {
        // setup
        String id1 = "vol-ABCDEFGH";
        String id2 = "vol-ZZZZZZZZ";

        when(piIdBuilder.generateBase62Ec2Id("vol", globalAvzCode)).thenReturn(id1).thenReturn(id2);
        when(volume1.isDeleted()).thenReturn(true);
        when(this.piIdBuilder.getPIdForEc2AvailabilityZone(Volume.getUrl(id1))).thenReturn(dhtId);
        when(this.piIdBuilder.getPIdForEc2AvailabilityZone(Volume.getUrl(id2))).thenReturn(dhtId);
        when(this.blockingDhtReader.get(eq(dhtId))).thenReturn(volume1).thenReturn(null);

        // act
        String result = this.idFactory.createNewVolumeId(globalAvzCode);

        // assert
        assertEquals(id1, result);
    }

    @Test(expected = RetriesExhaustedException.class)
    public void testCreateNewVolumeIdAlreadyExistsRetriesExhausted() {
        // setup
        this.idFactory.setRetries(2);

        String id1 = "vol-ABCDEFGH";
        when(piIdBuilder.generateBase62Ec2Id("vol", globalAvzCode)).thenReturn(id1).thenReturn(id1);
        when(this.piIdBuilder.getPIdForEc2AvailabilityZone(Volume.getUrl(id1))).thenReturn(dhtId);
        when(this.blockingDhtReader.get(eq(dhtId))).thenReturn(volume1);

        // act
        this.idFactory.createNewVolumeId(globalAvzCode);
    }

    @Test
    public void testCreateNewReservationId() {
        // act
        String result = this.idFactory.createNewReservationId();

        // assert
        assertEquals(10, result.length());
        assertTrue(result.startsWith("r-"));
    }

    @Test
    public void testCreateNewInstanceId() {
        // act
        String result = this.idFactory.createNewInstanceId(globalAvzCode);

        // assert
        assertEquals(10, result.length());
        assertTrue(result.startsWith("i-"));
    }

    @Test
    public void testCreateNewImageId() {
        // act
        String result = this.idFactory.createNewImageId();

        // assert
        assertEquals(12, result.length());
        assertTrue(result.startsWith("pmi-"));
    }

    @Test
    public void shouldCreateNewSnapshotId() {
        // act
        String result = this.idFactory.createNewSnapshotId(globalAvzCode);

        // assert
        assertEquals(13, result.length());
        assertTrue(result.startsWith("snap-"));
    }
}
