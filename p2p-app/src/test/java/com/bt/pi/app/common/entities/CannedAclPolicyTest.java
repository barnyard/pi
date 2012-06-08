package com.bt.pi.app.common.entities;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import org.junit.Test;

public class CannedAclPolicyTest {

    @Test
    public void testCannedAclPolicyGet() {
        // act/assert
        for (CannedAclPolicy cannedAclPolicy : CannedAclPolicy.values()) {
            CannedAclPolicy result = CannedAclPolicy.get(cannedAclPolicy.getName());
            assertEquals(cannedAclPolicy, result);
        }
    }

    @Test
    public void testCannedAclPolicyValueOf() {
        // act/assert
        for (CannedAclPolicy cannedAclPolicy : CannedAclPolicy.values()) {
            CannedAclPolicy result = CannedAclPolicy.valueOf(cannedAclPolicy.toString());
            assertEquals(cannedAclPolicy, result);
        }
    }

    @Test
    public void testCannedAclPolicyGetInvalid() {
        // act
        CannedAclPolicy result = CannedAclPolicy.get("fred");

        // assert
        assertNull(result);
    }

    @Test
    public void testCannedAclPolicyGetNull() {
        // act
        CannedAclPolicy result = CannedAclPolicy.get(null);

        // assert
        assertNull(result);
    }

    @Test
    public void testCannedAclPolicyAwsExecRead() {
        // act
        CannedAclPolicy result = CannedAclPolicy.get("aws-exec-read");

        // assert
        assertNotNull(result);
        assertEquals(CannedAclPolicy.AWS_EXEC_READ, result);
    }
}
