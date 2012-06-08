package com.bt.pi.app.instancemanager.reporting;

import static junit.framework.Assert.assertEquals;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.not;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;

import org.junit.Before;
import org.junit.Test;

import com.bt.pi.core.parser.KoalaJsonParser;

public class InstanceReportEntityTest {
    private InstanceReportEntity instanceReportEntity;

    @Before
    public void setup() {
        instanceReportEntity = new InstanceReportEntity();
    }

    @Test
    public void testConstructor() throws Exception {
        // act
        instanceReportEntity = new InstanceReportEntity("instanceId", "ownerId", 2345, "127.0.0.1", "127.0.0.1");

        // assert
        assertThat((String) instanceReportEntity.getId(), equalTo("instanceId"));
        assertThat(instanceReportEntity.getInstanceId(), equalTo("instanceId"));
        assertThat(instanceReportEntity.getOwnerId(), equalTo("ownerId"));
        // assertThat(instanceReportEntity.getLaunchTime(), equalTo(new Date(2345)));
    }

    @Test
    public void testGettersAndSetters() throws Exception {
        // act
        instanceReportEntity.setInstanceId("instanceId");
        instanceReportEntity.setOwnerId("ownerId");

        // assert
        assertThat((String) instanceReportEntity.getId(), equalTo("instanceId"));
        assertThat(instanceReportEntity.getInstanceId(), equalTo("instanceId"));
        assertThat(instanceReportEntity.getOwnerId(), equalTo("ownerId"));

    }

    @Test
    public void equalsShouldOnlyCheckInstanceId() throws Exception {
        // setup
        instanceReportEntity = new InstanceReportEntity("instanceId", "1", 0, "127.0.0.1", "127.0.0.1");
        InstanceReportEntity instanceReportEntity2 = new InstanceReportEntity("instanceId", "2", 1, "127.0.0.1", "127.0.0.1");

        assertThat(instanceReportEntity, equalTo(instanceReportEntity2));
    }

    @Test
    public void equalsShouldOnlyCheckInstanceIdForDifferentInstanceIds() throws Exception {
        // setup
        instanceReportEntity = new InstanceReportEntity("instanceId", "1", 0, "127.0.0.1", "127.0.0.1");
        InstanceReportEntity instanceReportEntity2 = new InstanceReportEntity("instanceId2", "1", 0, "127.0.0.1", "127.0.0.1");

        assertThat(instanceReportEntity, not(equalTo(instanceReportEntity2)));
    }

    @Test
    public void compareToShouldOnlyCheckInstanceId() throws Exception {
        // setup
        instanceReportEntity = new InstanceReportEntity("instanceId", "1", 0, "127.0.0.1", "127.0.0.1");
        InstanceReportEntity instanceReportEntity2 = new InstanceReportEntity("instanceId", "2", 1, "127.0.0.1", "127.0.0.1");

        assertThat(instanceReportEntity.compareTo(instanceReportEntity2), equalTo(0));
    }

    @Test
    public void compareToShouldOnlyCheckInstanceIdForDifferentInstanceIds() throws Exception {
        // setup
        instanceReportEntity = new InstanceReportEntity("instanceId", "1", 0, "127.0.0.1", "127.0.0.1");
        InstanceReportEntity instanceReportEntity2 = new InstanceReportEntity("instanceId2", "1", 0, "127.0.0.1", "127.0.0.1");

        assertThat(instanceReportEntity.compareTo(instanceReportEntity2), not(equalTo(0)));
    }

    @Test
    public void getKeysForMapShouldReturnEmptyArray() throws Exception {
        // act
        Object[] keysForMap = instanceReportEntity.getKeysForMap();

        // assert
        assertThat(keysForMap.length, equalTo(0));
    }

    @Test
    public void getKeysForMapCountShouldReturn0() throws Exception {
        // act
        int keysForMapCount = instanceReportEntity.getKeysForMapCount();

        // assert
        assertThat(keysForMapCount, equalTo(0));
    }

    @Test
    public void getType() throws Exception {
        // act
        String type = instanceReportEntity.getType();

        // asserto
        assertThat(type, equalTo(InstanceReportEntity.class.getSimpleName()));
    }

    @Test
    public void getUrl() throws Exception {
        // act
        String url = instanceReportEntity.getUrl();

        // assert
        assertNull(url);
    }

    @Test
    public void shouldBeAbleToConvertToJsonAndBack() {
        // setup
        instanceReportEntity = new InstanceReportEntity("instanceId", "ownerId", System.currentTimeMillis(), "127.0.0.1", "127.0.0.1");

        // act
        KoalaJsonParser parser = new KoalaJsonParser();
        String json = parser.getJson(instanceReportEntity);
        System.out.println(json);
        InstanceReportEntity entity = (InstanceReportEntity) parser.getObject(json, InstanceReportEntity.class);

        // assert
        assertEquals(instanceReportEntity, entity);
    }
}
