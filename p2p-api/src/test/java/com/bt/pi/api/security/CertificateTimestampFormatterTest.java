package com.bt.pi.api.security;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static org.junit.matchers.JUnitMatchers.containsString;

import org.joda.time.DateTime;
import org.joda.time.DateTimeUtils;
import org.joda.time.ReadableInstant;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.bt.pi.api.security.CertificateTimestampFormatter;

public class CertificateTimestampFormatterTest {

    private final CertificateTimestampFormatter formatter = new CertificateTimestampFormatter();
    private ReadableInstant now;

    // reading timestamp

    @Before
    public void fixTime() {
        // Fixes the time given back as "now" to Joda Time to a particular value
        now = new DateTime(2001, 2, 3, 4, 5, 6, 789);
        DateTimeUtils.setCurrentMillisFixed(now.getMillis());
    }

    @After
    public void resumeTime() {
        // reset joda-time to return system's time as "now"
        DateTimeUtils.setCurrentMillisSystem();
    }

    @Test
    public void shouldThrowIllegalArgumentExceptionIfNoSeparatorInAlias() {
        try {
            formatter.getTimestamp("aliasValue");
            fail("Should have thrown an IllegalArgumentException");
        } catch (IllegalArgumentException e) {
            assertThat(e.getMessage(), containsString("aliasValue"));
            assertThat(e.getMessage(), containsString("does not contain a timestamp separator"));
        }
    }

    @Test
    public void shouldThrowIllegalArgumentExceptionIfMoreThanOneSeparatorInAlias() {
        try {
            formatter.getTimestamp("aliasValue@foo@bar");
            fail("Should have thrown an IllegalArgumentException");
        } catch (IllegalArgumentException e) {
            assertThat(e.getMessage(), containsString("aliasValue@foo@bar"));
            assertThat(e.getMessage(), containsString("contains more than one timestamp separator"));
        }
    }

    @Test
    public void shouldThrowIllegalArgumentExceptionIfTimestampCannotBeParsed() {
        try {
            formatter.getTimestamp("aliasValue@iamnotatimestamp");
            fail("Should have thrown an IllegalArgumentException");
        } catch (IllegalArgumentException e) {
        }
    }

    @Test
    public void shouldThrowIllegalArgumentExceptionForEmptyUserId() {
        try {
            formatter.getTimestamp("@2009-12-07T12:34:56.789Z");
            fail("Should have thrown an IllegalArgumentException");
        } catch (IllegalArgumentException e) {
            assertThat(e.getMessage(), containsString("@2009-12-07T12:34:56.789Z"));
            assertThat(e.getMessage(), containsString("does not contain a userId"));
        }
    }

    @Test
    public void shouldThrowIllegalArgumentExceptionForEmptyTimestamp() {
        try {
            formatter.getTimestamp("aliasValue@");
            fail("Should have thrown an IllegalArgumentException");
        } catch (IllegalArgumentException e) {
        }
    }

    @Test
    public void shouldThrowExceptionWhenTimestampCannotBeParsed() {
        try {
            formatter.getTimestamp("aliasValue@iamnotatimestamp");
            fail("Should have thrown an IllegalArgumentException");
        } catch (IllegalArgumentException e) {
            assertThat(e.getMessage(), containsString("aliasValue@iamnotatimestamp"));
            assertThat(e.getMessage(), containsString("could not parse timestamp"));
        }
    }

    @Test
    public void shouldReturnTimestampFromISO8601FormattedAlias() {
        ReadableInstant timestamp = formatter.getTimestamp("aliasValue@2009-12-07T12:34:56.789Z");
        ReadableInstant expected = new DateTime(2009, 12, 7, 12, 34, 56, 789);
        assertEquals(expected, timestamp);
    }

    @Test
    public void shouldReturnTimestampWithZeroedFieldsFromPartialTimestamp() {
        ReadableInstant timestamp = formatter.getTimestamp("aliasValue@2009-12-07");
        ReadableInstant expected = new DateTime(2009, 12, 7, 0, 0, 0, 0);
        assertEquals(expected, timestamp);
    }

    // adding/updating timestamp

    @Test
    public void shouldThrowIllegalArgumentExceptionIfAliasIsNull() {
        try {
            formatter.addOrUpdateTimestamp(null);
            fail("Should have thrown an IllegalArgumentException");
        } catch (IllegalArgumentException e) {
            assertThat(e.getMessage(), containsString("alias cannot be null"));
        }
    }

    @Test
    public void shouldThrowIllegalArgumentExceptionIfAliasIsBlank() {
        try {
            formatter.addOrUpdateTimestamp("");
            fail("Should have thrown an IllegalArgumentException");
        } catch (IllegalArgumentException e) {
            assertThat(e.getMessage(), containsString("alias cannot be blank"));
        }
    }

    @Test
    public void shouldAddTimestampToAliasWithoutOne() {
        assertEquals("foo@2001-02-03T04:05:06.789Z", formatter.addOrUpdateTimestamp("foo"));
    }

    @Test
    public void shouldUpdateTimestampOnAliasThatHasOne() {
        assertEquals("foo@2001-02-03T04:05:06.789Z", formatter.addOrUpdateTimestamp("foo@2007-08-09T10:11:12.345Z"));
    }

    @Test
    public void shouldThrowIllegalArgumentExceptionIfAliasHasMoreThanOneTimestampSeparator() {
        try {
            formatter.addOrUpdateTimestamp("foo@123@456");
            fail("Should have thrown an IllegalArgumentException");
        } catch (IllegalArgumentException e) {
            assertThat(e.getMessage(), containsString("foo@123@456"));
            assertThat(e.getMessage(), containsString("contains more than one timestamp separator"));
        }
    }
}
