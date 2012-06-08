package com.bt.pi.app.common.entities;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Before;
import org.junit.Test;

import com.bt.pi.app.common.entities.VlanAllocationRecord;
import com.bt.pi.core.parser.KoalaJsonParser;

public class VlanAllocationRecordTest {
    private static final String SECURITY_GROUP_ID = "secGroupId";
    private VlanAllocationRecord vlanAllocationRecord;

    @Before
    public void before() {
        // setup
        vlanAllocationRecord = createVlanAllocationRecord();
    }

    private VlanAllocationRecord createVlanAllocationRecord() {
        VlanAllocationRecord aRecord = new VlanAllocationRecord();
        aRecord.setSecurityGroupId(SECURITY_GROUP_ID);

        return aRecord;
    }

    @Test
    public void testGettersAndSetters() {
        // assert
        assertEquals(SECURITY_GROUP_ID, vlanAllocationRecord.getSecurityGroupId());
    }

    @Test
    public void testToString() {
        // act & assert
        assertTrue(vlanAllocationRecord.toString().contains(SECURITY_GROUP_ID));
        assertTrue(vlanAllocationRecord.toString().contains("Heartbeat"));
    }

    @Test
    public void testEquals() {
        VlanAllocationRecord other = createVlanAllocationRecord();
        assertEquals(vlanAllocationRecord, other);
    }

    @Test
    public void testHashCode() {
        assertEquals(createVlanAllocationRecord().hashCode(), createVlanAllocationRecord().hashCode());
    }

    @Test
    public void testSerialization() {
        // setup
        KoalaJsonParser parser = new KoalaJsonParser();

        // act
        VlanAllocationRecord reverse = (VlanAllocationRecord) parser.getObject(parser.getJson(vlanAllocationRecord), VlanAllocationRecord.class);

        // assert
        assertEquals(vlanAllocationRecord, reverse);
    }

}
