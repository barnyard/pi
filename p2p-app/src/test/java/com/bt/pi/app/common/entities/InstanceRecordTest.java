package com.bt.pi.app.common.entities;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Before;
import org.junit.Test;

import com.bt.pi.core.parser.KoalaJsonParser;

public class InstanceRecordTest {

    private static final String USER_ID = "userId";
    private static final String INSTANCEID = "i-nstanceId";
    private static final long TIME = 1235443567543L;
    private InstanceRecord instanceRecord;

    @Before
    public void before() {
        // setup
        instanceRecord = createInstanceRecord();
    }

    private InstanceRecord createInstanceRecord() {
        InstanceRecord anInstanceRecord = new InstanceRecord();
        anInstanceRecord.setInstanceId(INSTANCEID);
        anInstanceRecord.setOwnerId(USER_ID);
        anInstanceRecord.setCreatedAt(TIME);
        anInstanceRecord.setUpdatedAt(TIME);

        return anInstanceRecord;
    }

    @Test
    public void testGettersAndSetters() {

        // assert
        assertEquals(USER_ID, instanceRecord.getOwnerId());
        assertEquals(INSTANCEID, instanceRecord.getInstanceId());
        assertEquals(TIME, instanceRecord.getCreatedAt());
        assertEquals(TIME, instanceRecord.getUpdatedAt());
    }

    @Test
    public void testToString() {
        // act & assert
        assertTrue(instanceRecord.toString().contains(INSTANCEID));
        assertTrue(instanceRecord.toString().contains(USER_ID));
    }

    @Test
    public void testEquals() {
        InstanceRecord other = createInstanceRecord();
        assertEquals(instanceRecord, other);
    }

    @Test
    public void testHashCode() {
        assertEquals(createInstanceRecord().hashCode(), createInstanceRecord().hashCode());
    }

    @Test
    public void testSerialization() {
        // setup
        KoalaJsonParser parser = new KoalaJsonParser();

        // act
        InstanceRecord reverse = (InstanceRecord) parser.getObject(parser.getJson(instanceRecord), InstanceRecord.class);

        // assert
        assertEquals(instanceRecord, reverse);
    }

    @Test
    public void shouldBeConsumedByOwnerAndInstanceIfBothPresent() {
        assertTrue(instanceRecord.isConsumedBy(USER_ID));
        assertTrue(instanceRecord.isConsumedBy(INSTANCEID));
    }

    @Test
    public void shouldBeConsumedByOwnerIfOnlyOwnerPresent() {
        instanceRecord.setInstanceId(null);

        assertTrue(instanceRecord.isConsumedBy(USER_ID));
        assertFalse(instanceRecord.isConsumedBy(INSTANCEID));
    }

    @Test
    public void shouldBeConsumedByInstanceIfOnlyInstancePresent() {
        instanceRecord.setOwnerId(null);

        assertFalse(instanceRecord.isConsumedBy(USER_ID));
        assertTrue(instanceRecord.isConsumedBy(INSTANCEID));
    }
}
