package com.bt.pi.app.instancemanager.reporting;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Collection;

import org.junit.Before;
import org.junit.Test;

import com.bt.pi.core.entity.PiEntityCollection;

public class ZombieInstanceReportHandlerTest {
    private InstanceReportEntity instanceReportEntity;
    private ZombieInstanceReportHandler zombieInstanceReportHandler;

    @Before
    public void setup() {
        instanceReportEntity = mock(InstanceReportEntity.class);
        when(instanceReportEntity.getId()).thenReturn("id");
        when(instanceReportEntity.getCreationTime()).thenReturn(System.currentTimeMillis());

        zombieInstanceReportHandler = new ZombieInstanceReportHandler();
    }

    @Test
    public void testGetPiEntityCollection() throws Exception {
        // act
        PiEntityCollection<InstanceReportEntity> piEntityCollection = zombieInstanceReportHandler.getPiEntityCollection();

        // assert
        assertThat(piEntityCollection instanceof ZombieInstanceReportEntityCollection, is(true));
    }

    @Test
    public void testReportableEntityTypes() throws Exception {
        // act
        Collection<String> result = zombieInstanceReportHandler.getReportableEntityTypesHandled();

        // assert
        assertThat(result, equalTo((Collection<String>) Arrays.asList(new String[] { new ZombieInstanceReportEntityCollection().getType() })));
    }

    @Test
    public void testThatChangingTimeToLiveStillMaintainsExistingEntities() throws Exception {
        // setup
        zombieInstanceReportHandler.setSize(5);
        zombieInstanceReportHandler.receive(instanceReportEntity);

        // act
        zombieInstanceReportHandler.setTimeToLive(10);

        // assert
        assertThat(zombieInstanceReportHandler.getAllEntities().getEntities().size(), equalTo(1));
        assertThat(zombieInstanceReportHandler.getAllEntities().getEntities().contains(instanceReportEntity), is(true));
    }

    @Test
    public void testThatChangingSizeStillMaintainsExistingEntities() throws Exception {
        // setup
        zombieInstanceReportHandler.setTimeToLive(5);
        zombieInstanceReportHandler.receive(instanceReportEntity);

        // act
        zombieInstanceReportHandler.setSize(5);

        // assert
        assertThat(zombieInstanceReportHandler.getAllEntities().getEntities().size(), equalTo(1));
        assertThat(zombieInstanceReportHandler.getAllEntities().getEntities().contains(instanceReportEntity), is(true));
    }

    @Test
    public void testThatChangingSizeRemovesExtraEntities() throws Exception {
        // setup
        InstanceReportEntity instanceReportEntity2 = mock(InstanceReportEntity.class);
        when(instanceReportEntity2.getId()).thenReturn("id2");
        when(instanceReportEntity2.getCreationTime()).thenReturn(System.currentTimeMillis() + 100);

        zombieInstanceReportHandler.setTimeToLive(500000);
        zombieInstanceReportHandler.receive(instanceReportEntity);
        zombieInstanceReportHandler.receive(instanceReportEntity2);

        // act
        zombieInstanceReportHandler.setSize(1);

        // assert
        assertThat(zombieInstanceReportHandler.getAllEntities().getEntities().size(), equalTo(1));
        assertThat(zombieInstanceReportHandler.getAllEntities().getEntities().contains(instanceReportEntity), is(false));
        assertThat(zombieInstanceReportHandler.getAllEntities().getEntities().contains(instanceReportEntity2), is(true));
    }
}
