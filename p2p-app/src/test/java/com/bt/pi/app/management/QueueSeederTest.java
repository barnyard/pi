package com.bt.pi.app.management;

import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.isA;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.Before;
import org.junit.Test;

import com.bt.pi.app.common.id.PiIdBuilder;
import com.bt.pi.app.common.resource.PiQueue;
import com.bt.pi.core.dht.BlockingDhtWriter;
import com.bt.pi.core.dht.DhtClientFactory;
import com.bt.pi.core.entity.TaskProcessingQueue;
import com.bt.pi.core.id.PId;

public class QueueSeederTest {
    private QueueSeeder queueSeeder;
    private PiIdBuilder piIdBuilder;
    private DhtClientFactory dhtClientFactory;
    private BlockingDhtWriter writer;
    private PId generalQueueId;
    private PId regionQueueId;
    private PId avzQueueId;

    @Before
    public void setUp() throws Exception {
        generalQueueId = mock(PId.class);
        regionQueueId = mock(PId.class);
        avzQueueId = mock(PId.class);
        this.queueSeeder = new QueueSeeder();

        this.piIdBuilder = mock(PiIdBuilder.class);
        this.queueSeeder.setPiIdBuilder(this.piIdBuilder);

        this.dhtClientFactory = mock(DhtClientFactory.class);
        this.queueSeeder.setDhtClientFactory(this.dhtClientFactory);

        writer = mock(BlockingDhtWriter.class);
        when(this.dhtClientFactory.createBlockingWriter()).thenReturn(writer);
    }

    @Test
    public void testAddQueue() {
        // setup
        PiQueue piQueue = PiQueue.CREATE_VOLUME;
        when(this.piIdBuilder.getPiQueuePId(eq(piQueue))).thenReturn(generalQueueId);
        when(generalQueueId.forLocalScope(piQueue.getNodeScope())).thenReturn(generalQueueId);
        when(writer.writeIfAbsent(eq(generalQueueId), isA(TaskProcessingQueue.class))).thenReturn(true);

        // act
        boolean result = this.queueSeeder.addQueue(piQueue);

        // assert
        assertTrue(result);
    }

    @Test
    public void testAddQueuesForRegion() {
        // setup
        when(this.piIdBuilder.getPId(isA(String.class))).thenReturn(generalQueueId);
        when(generalQueueId.forRegion(0x22)).thenReturn(generalQueueId);
        when(this.piIdBuilder.getPId(eq(PiQueue.ASSOCIATE_ADDRESS.getUrl()))).thenReturn(regionQueueId);
        when(regionQueueId.forRegion(0x22)).thenReturn(regionQueueId);
        when(this.piIdBuilder.getPId(eq(PiQueue.CREATE_VOLUME.getUrl()))).thenReturn(avzQueueId);
        when(avzQueueId.forRegion(0x22)).thenReturn(regionQueueId);
        when(writer.writeIfAbsent(eq(generalQueueId), isA(TaskProcessingQueue.class))).thenReturn(true);
        when(writer.writeIfAbsent(eq(regionQueueId), isA(TaskProcessingQueue.class))).thenReturn(true);
        when(writer.writeIfAbsent(eq(avzQueueId), isA(TaskProcessingQueue.class))).thenReturn(false);

        // act
        boolean result = this.queueSeeder.addQueuesForRegion(0x22);

        // assert
        verify(writer).writeIfAbsent(eq(regionQueueId), isA(TaskProcessingQueue.class));
        verify(writer, never()).writeIfAbsent(eq(avzQueueId), isA(TaskProcessingQueue.class));
        assertTrue(result);
    }

    @Test
    public void testAddQueuesForAvz() {
        // setup
        when(this.piIdBuilder.getPId(isA(String.class))).thenReturn(generalQueueId);
        when(generalQueueId.forGlobalAvailablityZoneCode(0x2211)).thenReturn(generalQueueId);
        when(this.piIdBuilder.getPId(eq(PiQueue.ASSOCIATE_ADDRESS.getUrl()))).thenReturn(regionQueueId);
        when(regionQueueId.forGlobalAvailablityZoneCode(0x2211)).thenReturn(regionQueueId);
        when(this.piIdBuilder.getPId(eq(PiQueue.CREATE_VOLUME.getUrl()))).thenReturn(avzQueueId);
        when(avzQueueId.forGlobalAvailablityZoneCode(0x2211)).thenReturn(avzQueueId);

        when(writer.writeIfAbsent(eq(generalQueueId), isA(TaskProcessingQueue.class))).thenReturn(true);
        when(writer.writeIfAbsent(eq(regionQueueId), isA(TaskProcessingQueue.class))).thenReturn(false);
        when(writer.writeIfAbsent(eq(avzQueueId), isA(TaskProcessingQueue.class))).thenReturn(true);

        // act
        boolean result = this.queueSeeder.addQueuesForAvailabilityZone(0x2211);

        // assert
        verify(writer).writeIfAbsent(eq(avzQueueId), isA(TaskProcessingQueue.class));
        verify(writer, never()).writeIfAbsent(eq(regionQueueId), isA(TaskProcessingQueue.class));
        assertTrue(result);
    }
}
