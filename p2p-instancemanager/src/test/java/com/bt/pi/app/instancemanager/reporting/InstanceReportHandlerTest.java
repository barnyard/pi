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

public class InstanceReportHandlerTest {
    private InstanceReportHandler instanceReportHandler;

    @Before
    public void setup() {
        instanceReportHandler = new InstanceReportHandler();
    }

    @Test
    public void testGetPiEntityCollection() throws Exception {
        // act
        PiEntityCollection<InstanceReportEntity> piEntityCollection = instanceReportHandler.getPiEntityCollection();

        // assert
        assertThat(piEntityCollection instanceof InstanceReportEntityCollection, is(true));
    }

    @Test
    public void testReportableEntityTypes() throws Exception {
        // act
        instanceReportHandler.setTimeToLive(10);

        // assert
        assertThat(instanceReportHandler.getReportableEntityTypesHandled(), equalTo((Collection<String>) Arrays.asList(new String[] { new InstanceReportEntityCollection().getType() })));
    }

    @Test
    public void testThatChangingTimesTakesEffectInCache() throws Exception {
        // setup
        instanceReportHandler.setTimeToLive(1000);
        instanceReportHandler.setTimeToLive(10);

        InstanceReportEntity instanceReportEntity = newInstanceReportEntity("id", System.currentTimeMillis() - 20000);
        InstanceReportEntity instanceReportEntity2 = newInstanceReportEntity("id2", System.currentTimeMillis());

        // act
        instanceReportHandler.receive(instanceReportEntity);
        instanceReportHandler.receive(instanceReportEntity2);

        // assert
        assertThat(instanceReportHandler.getAllEntities().getEntities().size(), equalTo(1));
        assertThat(instanceReportHandler.getAllEntities().getEntities().contains(instanceReportEntity), is(false));
        assertThat(instanceReportHandler.getAllEntities().getEntities().contains(instanceReportEntity2), is(true));
    }

    private InstanceReportEntity newInstanceReportEntity(String id, long time) {
        InstanceReportEntity instanceReportEntity2 = mock(InstanceReportEntity.class);
        when(instanceReportEntity2.getId()).thenReturn(id);
        when(instanceReportEntity2.getCreationTime()).thenReturn(time);
        return instanceReportEntity2;
    }
}
