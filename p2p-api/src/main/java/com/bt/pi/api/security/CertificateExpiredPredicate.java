/* (c) British Telecommunications plc, 2009, All Rights Reserved */
package com.bt.pi.api.security;

import org.joda.time.DateTime;
import org.joda.time.Duration;
import org.joda.time.Interval;
import org.joda.time.Period;
import org.joda.time.ReadableInstant;
import org.springframework.stereotype.Component;

import com.bt.pi.core.conf.Property;

@Component
public class CertificateExpiredPredicate {

    private Period maxAge;

    public CertificateExpiredPredicate() {
        maxAge = null;
    }

    /**
     * See {@link http://en.wikipedia.org/wiki/ISO_8601#Time_intervals} for valid formats
     * 
     * @param iso8601Duration
     *            duration according to ISO8601, for example "PT30M" for 30 minutes
     */
    @Property(key = "certificate.cache.max.age", defaultValue = "PT30M")
    public void setMaxAge(String iso8601Duration) {
        maxAge = new Period(iso8601Duration);
    }

    /**
     * @return true if the timestamp is older than maxAge (default 30 minutes)
     */
    public boolean isExpired(ReadableInstant timestamp) {
        final ReadableInstant now = new DateTime();
        Duration timestampAge = new Interval(timestamp, now).toDuration();
        Duration lifetime = maxAge.toDurationTo(now);
        if (timestampAge.isLongerThan(lifetime)) {
            return true;
        }
        return false;
    }

}
