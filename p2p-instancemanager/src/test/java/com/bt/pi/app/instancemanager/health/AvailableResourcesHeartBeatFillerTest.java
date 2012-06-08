package com.bt.pi.app.instancemanager.health;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.when;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import com.bt.pi.app.instancemanager.handlers.SystemResourceState;
import com.bt.pi.core.application.health.entity.HeartbeatEntity;

@RunWith(MockitoJUnitRunner.class)
public class AvailableResourcesHeartBeatFillerTest {

    private static final Long FREE_MEMORY = (long) (4 * 1024);

    private static final Long FREE_DISK = (long) (20 * 1024);

    private static final Long FREE_CORES = (long) 4;

    @InjectMocks
    private AvailableResourcesHeartBeatFiller availableResourcesHeartBeatFiller = new AvailableResourcesHeartBeatFiller();

    @Mock
    private SystemResourceState systemResourceState;

    private HeartbeatEntity heartbeatEntity = new HeartbeatEntity();

    @Before
    public void before() {

    }

    @Test
    public void shouldPopulateAvailableResources() {
        // setup
        when(systemResourceState.getFreeCores()).thenReturn(FREE_CORES.intValue());
        when(systemResourceState.getFreeDiskInMB()).thenReturn(FREE_DISK);
        when(systemResourceState.getFreeMemoryInMB()).thenReturn(FREE_MEMORY);
        // act
        availableResourcesHeartBeatFiller.populate(heartbeatEntity);
        // assert
        assertEquals((Long) FREE_DISK, heartbeatEntity.getAvailableResources().get(AvailableResourcesHeartBeatFiller.FREE_DISK));
        assertEquals(FREE_CORES, heartbeatEntity.getAvailableResources().get(AvailableResourcesHeartBeatFiller.FREE_CORES));
        assertEquals(FREE_MEMORY, heartbeatEntity.getAvailableResources().get(AvailableResourcesHeartBeatFiller.FREE_MEMORY));
    }
}
