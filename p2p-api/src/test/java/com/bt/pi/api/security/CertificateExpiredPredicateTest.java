package com.bt.pi.api.security;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.joda.time.DateTime;
import org.joda.time.DateTimeUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.bt.pi.api.security.CertificateExpiredPredicate;

public class CertificateExpiredPredicateTest {

    private CertificateExpiredPredicate predicate;

    @Before
    public void before() {
        DateTimeUtils.setCurrentMillisFixed(new DateTime(2009, 1, 1, 13, 0, 0, 0).getMillis());
        predicate = new CertificateExpiredPredicate();
        predicate.setMaxAge("PT30M");
    }

    @After
    public void after() {
        DateTimeUtils.setCurrentMillisSystem();
    }

    @Test
    public void timestampOfNowShouldNotBeExpired() {
        assertFalse(predicate.isExpired(new DateTime()));
    }

    @Test
    public void timestampExactlyAsOldAsDefaultMaxAgeShouldBeNotExpired() {
        assertFalse(predicate.isExpired(new DateTime(2009, 1, 1, 12, 30, 0, 0)));
    }

    @Test
    public void timestampOlderThanDefaultMaxAgeShouldBeExpired() {
        assertTrue(predicate.isExpired(new DateTime(2009, 1, 1, 12, 29, 0, 0)));
    }

    @Test
    public void shouldBeAbleToSetMaxAgeAsArbitraryPeriod() {
        predicate.setMaxAge("PT1H5M30S");
        assertFalse(predicate.isExpired(new DateTime()));
        assertFalse(predicate.isExpired(new DateTime(2009, 1, 1, 11, 54, 30, 0)));
        assertTrue(predicate.isExpired(new DateTime(2009, 1, 1, 11, 54, 0, 0)));
    }

    @Test
    public void shouldThrowIllegalArgumentExceptionForInvalidMaxAge() {
        try {
            predicate.setMaxAge("xxx");
            fail("should have thrown an IllegalArgumentException");
        } catch (IllegalArgumentException e) {
        }
    }
}
