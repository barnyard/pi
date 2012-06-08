package com.bt.pi.app.imagemanager.xml;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.junit.Test;

public class ParseExceptionTest {

    @Test
    public void shouldConstructAnExceptionWitoutAMessageOrCause() {
        ParseException exception = new ParseException();

        assertNull(exception.getMessage());
        assertNull(exception.getCause());
    }

    @Test
    public void shouldConstructAnExceptionWithAMessageAndACause() {
        String message = "some exception message";
        Throwable cause = new Exception();
        ParseException exception = new ParseException(message, cause);

        assertEquals(message, exception.getMessage());
        assertEquals(cause, exception.getCause());
    }
}
