/* (c) British Telecommunications plc, 2009, All Rights Reserved */
package com.bt.pi.api.security;

import org.apache.commons.lang.StringUtils;
import org.joda.time.DateTime;
import org.joda.time.ReadableInstant;
import org.springframework.stereotype.Component;

/**
 * Reads and writes timestamps to and from strings.
 */
@Component
public class CertificateTimestampFormatter {

    private static final String ALIAS_CONTAINS_MORE_THAN_ONE_SEPARATOR = "alias '%s' contains more than one timestamp separator";
    private static final String TIMESTAMP_SEPARATOR = "@";

    public CertificateTimestampFormatter() {
    }

    public ReadableInstant getTimestamp(String alias) {
        int numberOfSeparators = StringUtils.countMatches(alias, TIMESTAMP_SEPARATOR);
        if (numberOfSeparators == 0) {
            throw new IllegalArgumentException(String.format("alias '%s' does not contain a timestamp separator", alias));
        }
        if (numberOfSeparators > 1) {
            throw new IllegalArgumentException(String.format(ALIAS_CONTAINS_MORE_THAN_ONE_SEPARATOR, alias));
        }
        if (StringUtils.isBlank(StringUtils.substringBefore(alias, TIMESTAMP_SEPARATOR))) {
            throw new IllegalArgumentException(String.format("alias '%s' does not contain a userId", alias));
        }

        try {
            return new DateTime(StringUtils.substringAfter(alias, TIMESTAMP_SEPARATOR));
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException(String.format("could not parse timestamp in alias '%s'", alias));
        }
    }

    public String addOrUpdateTimestamp(String alias) {
        if (alias == null) {
            throw new IllegalArgumentException("alias cannot be null");
        }
        if (StringUtils.isBlank(alias)) {
            throw new IllegalArgumentException("alias cannot be blank");
        }

        int numberOfSeparators = StringUtils.countMatches(alias, TIMESTAMP_SEPARATOR);
        if (numberOfSeparators == 0) {
            return formatTimestamp(alias);
        }
        if (numberOfSeparators == 1) {
            return formatTimestamp(StringUtils.substringBeforeLast(alias, TIMESTAMP_SEPARATOR));
        }
        throw new IllegalArgumentException(String.format(ALIAS_CONTAINS_MORE_THAN_ONE_SEPARATOR, alias));
    }

    private String formatTimestamp(String userId) {
        return String.format(userId + TIMESTAMP_SEPARATOR + new DateTime());
    }

}
