package com.bt.pi.app.common.entities;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Before;
import org.junit.Test;

import com.bt.pi.app.common.entities.SubnetAllocationRecord;
import com.bt.pi.core.parser.KoalaJsonParser;

public class SubnetAllocationRecordTest {
    private static final String SECURITY_GROUP_ID = "secGroupId";
    private static final Long SUBNET_MASK = new Long(1235443567543L);
    private SubnetAllocationRecord subnetAllocationRecord;

    @Before
    public void before() {
        // setup
        subnetAllocationRecord = createSubnetAllocationRecord();
    }

    private SubnetAllocationRecord createSubnetAllocationRecord() {
        SubnetAllocationRecord aRecord = new SubnetAllocationRecord();
        aRecord.setSecurityGroupId(SECURITY_GROUP_ID);
        aRecord.setSubnetMask(SUBNET_MASK);

        return aRecord;
    }

    @Test
    public void testGettersAndSetters() {

        // assert
        assertEquals(SECURITY_GROUP_ID, subnetAllocationRecord.getSecurityGroupId());
        assertEquals(SUBNET_MASK, subnetAllocationRecord.getSubnetMask());
    }

    @Test
    public void testToString() {
        // act & assert
        assertTrue(subnetAllocationRecord.toString().contains(SECURITY_GROUP_ID));
        assertTrue(subnetAllocationRecord.toString().contains(SUBNET_MASK.toString()));
    }

    @Test
    public void testEquals() {
        SubnetAllocationRecord other = createSubnetAllocationRecord();
        assertEquals(subnetAllocationRecord, other);
    }

    @Test
    public void testHashCode() {
        assertEquals(createSubnetAllocationRecord().hashCode(), createSubnetAllocationRecord().hashCode());
    }

    @Test
    public void testSerialization() {
        // setup
        KoalaJsonParser parser = new KoalaJsonParser();

        // act
        SubnetAllocationRecord reverse = (SubnetAllocationRecord) parser.getObject(parser.getJson(subnetAllocationRecord), SubnetAllocationRecord.class);

        // assert
        assertEquals(subnetAllocationRecord, reverse);
    }

}
