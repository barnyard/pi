package com.bt.pi.api.handlers;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.List;

import org.apache.log4j.AppenderSkeleton;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.spi.LoggingEvent;
import org.apache.xmlbeans.XmlObject;
import org.junit.Test;
import org.mockito.Mockito;

import com.amazonaws.ec2.doc.x20090404.DescribeImagesDocument;
import com.amazonaws.ec2.doc.x20090404.DescribeImagesResponseDocument;
import com.bt.pi.api.ApiException;
import com.bt.pi.api.ApiServerException;
import com.bt.pi.api.utils.XmlMappingException;

public class HandlerBaseTest {
    private HandlerBase handler;

    @Test
    public void testCallLatest() throws Exception {
        // setup
        this.handler = new HandlerBase() {
            @SuppressWarnings("unused")
            public DescribeImagesResponseDocument describeImages(DescribeImagesDocument describeImagesDocument) {
                DescribeImagesResponseDocument result = DescribeImagesResponseDocument.Factory.newInstance();
                result.addNewDescribeImagesResponse();
                return result;
            }
        };
        XmlObject request = XmlObject.Factory.parse("<DescribeImages xmlns=\"" + HandlerBase.NAMESPACE_20081201 + "\"/>");

        // act
        XmlObject result = this.handler.callLatest(request);

        // assert
        assertTrue(result instanceof com.amazonaws.ec2.doc.x20081201.DescribeImagesResponseDocument);
    }

    @Test(expected = XmlMappingException.class)
    public void testCallLatestXmlException() throws Exception {
        // setup
        this.handler = new HandlerBase() {
        };
        XmlObject request = Mockito.mock(XmlObject.class);
        Mockito.when(request.toString()).thenReturn("invalid xml");

        // act
        try {
            this.handler.callLatest(request);
        } catch (XmlMappingException e) {
            // assert
            assertEquals("error mapping request/response to/from 2009-04-04/2008-12-01 version - XmlException - error: Unexpected element: CDATA", e.getMessage());
            throw e;
        }
    }

    @Test(expected = RuntimeException.class)
    public void testCallLatestInvocationTargetException() throws Exception {
        // setup
        final String message = "shit happens";
        this.handler = new HandlerBase() {
            @SuppressWarnings("unused")
            public DescribeImagesResponseDocument describeImages(DescribeImagesDocument describeImagesDocument) {
                throw new RuntimeException(message);
            }
        };
        XmlObject request = XmlObject.Factory.parse("<DescribeImages xmlns=\"" + HandlerBase.NAMESPACE_20081201 + "\"/>");

        // act
        try {
            this.handler.callLatest(request);
        } catch (RuntimeException e) {
            // assert
            assertEquals(message, e.getMessage());
            throw e;
        }
    }

    private static class TestAppender extends AppenderSkeleton {

        private Level level;
        private Object message;
        private List<String> stackTrace;

        @Override
        protected void append(LoggingEvent arg0) {
            if (!arg0.getLoggerName().equals("com.bt.pi.api.handlers.HandlerBase"))
                return;
            this.level = arg0.getLevel();
            this.message = arg0.getMessage();

            String[] trace = arg0.getThrowableStrRep();
            if (trace != null)
                this.stackTrace = Arrays.asList(trace);
        }

        public Level getLevel() {
            return level;
        }

        public Object getMessage() {
            return message;
        }

        public List<String> getStackTrace() {
            return stackTrace;
        }

        @Override
        public void close() {
        }

        @Override
        public boolean requiresLayout() {
            return false;
        }
    }

    @SuppressWarnings("serial")
    @Test
    public void testHandleThrowableApiException() {
        // setup
        this.handler = new HandlerBase() {
        };
        String message = "oops";
        Throwable t = new ApiException(message) {
        };
        TestAppender appender = new TestAppender();

        Logger.getLogger(HandlerBase.class).addAppender(appender);

        // act
        RuntimeException result = this.handler.handleThrowable(t);

        // assert
        assertEquals(t, result);
        assertEquals(Level.WARN, appender.getLevel());
        assertEquals(HandlerBase.CLIENT_ERROR, appender.getMessage());
        assertTrue(appender.getStackTrace().get(0).contains(message));
    }

    @Test
    public void testHandleThrowableIllegalArgumentException() {
        // setup
        this.handler = new HandlerBase() {
        };
        String message = "oops";
        Throwable t = new IllegalArgumentException(message);
        TestAppender appender = new TestAppender();

        Logger.getLogger(HandlerBase.class).addAppender(appender);

        // act
        RuntimeException result = this.handler.handleThrowable(t);

        // assert
        assertTrue(result instanceof ApiException);
        assertEquals(message, result.getMessage());
        assertEquals(Level.WARN, appender.getLevel());
        assertEquals(HandlerBase.CLIENT_ERROR, appender.getMessage());
        assertTrue(appender.getStackTrace().get(0).contains(message));
    }

    @Test
    public void testHandleThrowableRuntimeException() {
        // setup
        this.handler = new HandlerBase() {
        };
        Throwable t = new NullPointerException();
        TestAppender appender = new TestAppender();

        Logger.getLogger(HandlerBase.class).addAppender(appender);

        // act
        RuntimeException result = this.handler.handleThrowable(t);

        // assert
        assertTrue(result instanceof ApiServerException);
        assertEquals(Level.ERROR, appender.getLevel());
        assertEquals(HandlerBase.SERVICE_ERROR, appender.getMessage());
    }
}
