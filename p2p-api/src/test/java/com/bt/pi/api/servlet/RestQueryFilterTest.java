package com.bt.pi.api.servlet;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.isA;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.output.ByteArrayOutputStream;
import org.apache.xmlbeans.XmlObject;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.w3c.dom.Document;

import com.bt.pi.api.utils.HttpUtils;
import com.bt.pi.api.utils.QueryParameterUtils;
import com.bt.pi.api.utils.SoapEnvelopeUtils;
import com.bt.pi.api.utils.SoapRequestFactory;
import com.bt.pi.api.utils.XmlFormattingException;
import com.bt.pi.api.utils.XmlUtils;
import com.sun.net.httpserver.Filter.Chain;
import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpContext;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpPrincipal;

public class RestQueryFilterTest {
    private static final String SOAP_NS = "http://schemas.xmlsoap.org/soap/envelope/";
    private static final String ENVELOPE = "<soap:Envelope xmlns:soap=\"" + SOAP_NS + "\">" + "<soap:Body><Payload xmlns=\"http://ec2.amazonaws.com/doc/2009-04-04/\"/></soap:Body>" + "</soap:Envelope>";
    private RestQueryFilter restQueryFilter;
    private XmlUtils xmlUtils;
    private WSSecurityHandler wsSecurityHandler;
    private SoapRequestFactory soapRequestFactory;
    private SoapEnvelopeUtils soapEnvelopeUtils;
    private QueryParameterUtils queryParameterUtils;
    private AwsQuerySecurityHandler awsQuerySecurityHandler;
    private Chain chain;

    @Before
    public void before() throws Exception {
        this.restQueryFilter = new RestQueryFilter();
        this.restQueryFilter.setHttpUtils(new HttpUtils());

        this.chain = mock(Chain.class);

        this.xmlUtils = mock(XmlUtils.class);
        this.restQueryFilter.setXmlUtils(this.xmlUtils);

        this.wsSecurityHandler = mock(WSSecurityHandler.class);
        this.restQueryFilter.setWSSecurityHandler(wsSecurityHandler);

        this.soapRequestFactory = mock(SoapRequestFactory.class);
        this.restQueryFilter.setSoapRequestFactory(this.soapRequestFactory);

        this.soapEnvelopeUtils = mock(SoapEnvelopeUtils.class);
        this.restQueryFilter.setSoapEnvelopeUtils(this.soapEnvelopeUtils);

        this.queryParameterUtils = mock(QueryParameterUtils.class);
        this.restQueryFilter.setQueryParameterUtils(this.queryParameterUtils);

        this.awsQuerySecurityHandler = mock(AwsQuerySecurityHandler.class);
        this.restQueryFilter.setAwsQuerySecurityHandler(this.awsQuerySecurityHandler);
    }

    static class MockHttpExchange extends HttpExchange {

        private InputStream is;
        private OutputStream os;
        private HttpContext mockHttpContext;
        private int status;
        private Headers mockRequestHeaders;
        private Headers mockResponseHeaders;
        private String queryString;

        public MockHttpExchange(InputStream is, String queryString) {
            this.is = is;
            this.queryString = queryString;
            this.os = new ByteArrayOutputStream();
            this.mockHttpContext = mock(HttpContext.class);
            when(mockHttpContext.getPath()).thenReturn("/");
            this.mockRequestHeaders = mock(Headers.class);
            List<String> hosts = new ArrayList<String>();
            hosts.add("localhost:8773");
            when(mockRequestHeaders.get("host")).thenReturn(hosts);
            this.mockResponseHeaders = mock(Headers.class);
        }

        @Override
        public void close() {
            // TODO Auto-generated method stub
        }

        @Override
        public Object getAttribute(String arg0) {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public HttpContext getHttpContext() {
            return mockHttpContext;
        }

        @Override
        public InetSocketAddress getLocalAddress() {
            InetSocketAddress socket = new InetSocketAddress("localhost", 8773);
            return socket;
        }

        @Override
        public HttpPrincipal getPrincipal() {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public String getProtocol() {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public InetSocketAddress getRemoteAddress() {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public InputStream getRequestBody() {
            return is;
        }

        @Override
        public Headers getRequestHeaders() {
            return mockRequestHeaders;
        }

        @Override
        public String getRequestMethod() {
            return "POST";
        }

        @Override
        public URI getRequestURI() {
            if (queryString.length() > 0)
                return URI.create("/?" + queryString);
            return URI.create("/");
        }

        @Override
        public OutputStream getResponseBody() {
            return os;
        }

        @Override
        public int getResponseCode() {
            return status;
        }

        @Override
        public Headers getResponseHeaders() {
            return mockResponseHeaders;
        }

        @Override
        public void sendResponseHeaders(int arg0, long arg1) throws IOException {
            this.status = arg0;
        }

        @Override
        public void setAttribute(String arg0, Object arg1) {
            // TODO Auto-generated method stub
        }

        @Override
        public void setStreams(InputStream arg0, OutputStream arg1) {
            if (null != arg0)
                this.is = arg0;
            if (null != arg1)
                this.os = arg1;
        }

        public String getResponseBodyAsString() {
            return new String(((ByteArrayOutputStream) this.os).toByteArray());
        }
    }

    @Test
    public void testDescription() {
        assertEquals(RestQueryFilter.class.getName(), this.restQueryFilter.description());
    }

    @Test
    public void testDoServiceSOAP() throws Exception {
        // setup
        final String expectedResult = "<SOAP-ENV:Envelope xmlns:SOAP-ENV=\"http://schemas.xmlsoap.org/soap/envelope/\"><SOAP-ENV:Header/><SOAP-ENV:Body><result xmlns=\"\"/></SOAP-ENV:Body></SOAP-ENV:Envelope>";
        InputStream is = new ByteArrayInputStream(ENVELOPE.getBytes());
        MockHttpExchange httpExchange = new MockHttpExchange(is, "");

        XmlObject doc = XmlObject.Factory.parse(ENVELOPE);
        when(this.xmlUtils.parseInputString(isA(String.class))).thenReturn(doc);

        String userid = "FredBlogs";

        when(this.wsSecurityHandler.processEnvelope(isA(Document.class))).thenReturn(userid);

        doAnswer(new Answer<Object>() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                MockHttpExchange mockHttpExchange = (MockHttpExchange) invocation.getArguments()[0];
                mockHttpExchange.sendResponseHeaders(200, expectedResult.length());
                mockHttpExchange.getResponseBody().write(expectedResult.getBytes());
                return null;
            }
        }).when(this.chain).doFilter(httpExchange);

        // act
        this.restQueryFilter.doFilter(httpExchange, chain);

        // assert
        assertEquals(expectedResult, httpExchange.getResponseBodyAsString());
        assertEquals(HttpServletResponse.SC_OK, httpExchange.getResponseCode());
        verify(this.chain).doFilter(httpExchange);
    }

    @Test
    public void testDoServiceSOAPThrowable() throws Exception {
        // setup
        InputStream is = new ByteArrayInputStream(ENVELOPE.getBytes());
        MockHttpExchange httpExchange = new MockHttpExchange(is, "");

        XmlObject doc = XmlObject.Factory.parse(ENVELOPE);
        when(this.xmlUtils.parseInputString(isA(String.class))).thenReturn(doc);

        String userid = "FredBlogs";

        when(this.wsSecurityHandler.processEnvelope(isA(Document.class))).thenReturn(userid);

        final String message = "shit happens";

        doAnswer(new Answer<Object>() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                throw new Throwable(message);
            }
        }).when(this.chain).doFilter(httpExchange);

        // act
        this.restQueryFilter.doFilter(httpExchange, chain);

        // assert
        String response = httpExchange.getResponseBodyAsString();
        assertTrue(response.contains("http://schemas.xmlsoap.org/soap/envelope/"));
        assertTrue(response.contains("<faultstring>" + message + "</faultstring>"));
        assertTrue(response.contains("<faultcode>InternalError</faultcode>"));
        assertEquals(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, httpExchange.getResponseCode());
        verify(this.chain).doFilter(httpExchange);
    }

    @Test
    public void testDoServiceSOAPBadAuth() throws Exception {
        // setup
        InputStream is = new ByteArrayInputStream(ENVELOPE.getBytes());
        MockHttpExchange httpExchange = new MockHttpExchange(is, "");

        XmlObject doc = XmlObject.Factory.parse(ENVELOPE);
        when(this.xmlUtils.parseInputString(isA(String.class))).thenReturn(doc);

        String message = "user not found";
        when(this.wsSecurityHandler.processEnvelope(isA(Document.class))).thenThrow(new WSSecurityHandlerException(message));

        // act
        this.restQueryFilter.doFilter(httpExchange, chain);

        // assert
        String response = httpExchange.getResponseBodyAsString();
        assertTrue(response.contains("http://schemas.xmlsoap.org/soap/envelope/"));
        assertTrue(response.contains("<faultstring>" + message + "</faultstring>"));
        assertTrue(response.contains("<faultcode>AuthFailure</faultcode>"));
        assertEquals(HttpServletResponse.SC_BAD_REQUEST, httpExchange.getResponseCode());
        verify(this.chain, never()).doFilter(httpExchange);
    }

    @Test
    public void testDoServiceSOAPBadVersion() throws Exception {
        // setup
        String invalidVersion = "2010-13-13";
        String envelope = ENVELOPE.replace("2009-04-04", invalidVersion);
        InputStream is = new ByteArrayInputStream(envelope.getBytes());
        MockHttpExchange httpExchange = new MockHttpExchange(is, "");

        XmlObject doc = XmlObject.Factory.parse(envelope);
        when(this.xmlUtils.parseInputString(isA(String.class))).thenReturn(doc);

        // act
        this.restQueryFilter.doFilter(httpExchange, chain);

        // assert
        String response = httpExchange.getResponseBodyAsString();
        assertTrue(response.contains("http://schemas.xmlsoap.org/soap/envelope/"));
        assertTrue(response.contains("<faultstring>unsupported AWS version</faultstring>"));
        assertTrue(response.contains("<faultcode>SOAP-ENV:Client</faultcode>"));
        assertEquals(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, httpExchange.getResponseCode());
        verify(this.chain, never()).doFilter(httpExchange);
    }

    @Test
    public void testDoServiceSOAPBadXmlRequest() throws Exception {
        // setup
        InputStream is = new ByteArrayInputStream(ENVELOPE.getBytes());
        MockHttpExchange httpExchange = new MockHttpExchange(is, "");

        String message = "bad xml";
        // when(this.xmlUtils.parseInputStream(isA(InputStream.class))).thenThrow(new XmlFormattingException(message,
        // new RuntimeException()));
        when(this.xmlUtils.parseInputString(isA(String.class))).thenThrow(new XmlFormattingException(message, new RuntimeException()));

        // act
        this.restQueryFilter.doFilter(httpExchange, chain);

        // assert
        String response = httpExchange.getResponseBodyAsString();
        assertTrue(response.contains("http://schemas.xmlsoap.org/soap/envelope/"));
        assertTrue(response.contains("<faultstring>" + message + "</faultstring>"));
        assertTrue(response.contains("<faultcode>Invalid_XML_request</faultcode>"));
        assertEquals(HttpServletResponse.SC_BAD_REQUEST, httpExchange.getResponseCode());
        verify(this.chain, never()).doFilter(httpExchange);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testDoServiceREST() throws Exception {
        // setup
        InputStream mockInputStream = mock(InputStream.class);
        when(mockInputStream.read((byte[]) any(), anyInt(), anyInt())).thenReturn(-1);
        MockHttpExchange httpExchange = new MockHttpExchange(mockInputStream, "Action=Test");

        String soap = "<SOAP-ENV:Envelope xmlns:SOAP-ENV=\"http://schemas.xmlsoap.org/soap/envelope/\"><SOAP-ENV:Header/><SOAP-ENV:Body><Payload xmlns=\"test.com\"/></SOAP-ENV:Body></SOAP-ENV:Envelope>";
        when(this.soapRequestFactory.getSoap(isA(Map.class))).thenReturn(soap);

        byte[] bytePayload = "<results/>".getBytes();
        when(this.soapEnvelopeUtils.removeEnvelope(isA(byte[].class))).thenReturn(bytePayload);

        // act
        this.restQueryFilter.doFilter(httpExchange, chain);

        // assert
        verify(this.wsSecurityHandler, never()).processEnvelope(isA(Document.class));
        assertEquals(new String(bytePayload), httpExchange.getResponseBodyAsString());
        assertEquals(HttpServletResponse.SC_OK, httpExchange.getResponseCode());
        verify(this.chain).doFilter(isA(HttpExchange.class));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testDoServiceRESTThrowable() throws Exception {
        // setup
        InputStream mockInputStream = mock(InputStream.class);
        when(mockInputStream.read((byte[]) any(), anyInt(), anyInt())).thenReturn(-1);
        MockHttpExchange httpExchange = new MockHttpExchange(mockInputStream, "Action=Test");

        String soap = "<SOAP-ENV:Envelope xmlns:SOAP-ENV=\"http://schemas.xmlsoap.org/soap/envelope/\"><SOAP-ENV:Header/><SOAP-ENV:Body><Payload xmlns=\"test.com\"/></SOAP-ENV:Body></SOAP-ENV:Envelope>";
        when(this.soapRequestFactory.getSoap(isA(Map.class))).thenReturn(soap);

        final String message = "shit happens";

        doAnswer(new Answer() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                throw new Throwable(message);
            }
        }).when(this.chain).doFilter(isA(HttpExchange.class));

        // act
        this.restQueryFilter.doFilter(httpExchange, chain);

        // assert
        String response = httpExchange.getResponseBodyAsString();
        assertTrue(response.contains("<Code>InternalError</Code>"));
        assertTrue(response.contains("<Message>service error - " + message + "</Message>"));

        verify(this.wsSecurityHandler, never()).processEnvelope(isA(Document.class));
        assertEquals(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, httpExchange.getResponseCode());
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testDoServiceRESTBadAuth() throws Exception {
        // setup
        InputStream mockInputStream = mock(InputStream.class);
        when(mockInputStream.read((byte[]) any(), anyInt(), anyInt())).thenReturn(-1);
        MockHttpExchange httpExchange = new MockHttpExchange(mockInputStream, "Action=Test");

        String soap = "<SOAP-ENV:Envelope xmlns:SOAP-ENV=\"http://schemas.xmlsoap.org/soap/envelope/\"><SOAP-ENV:Header/><SOAP-ENV:Body><Payload xmlns=\"test.com\"/></SOAP-ENV:Body></SOAP-ENV:Envelope>";
        when(this.soapRequestFactory.getSoap(isA(Map.class))).thenReturn(soap);

        byte[] bytePayload = "<results/>".getBytes();
        when(this.soapEnvelopeUtils.removeEnvelope(isA(byte[].class))).thenReturn(bytePayload);

        String message = "User not found";
        when(this.awsQuerySecurityHandler.validate(isA(Map.class), isA(String.class), isA(String.class), isA(String.class))).thenThrow(new WSSecurityHandlerException(message));

        // act
        this.restQueryFilter.doFilter(httpExchange, chain);

        // assert
        verify(this.wsSecurityHandler, never()).processEnvelope(isA(Document.class));
        assertEquals(HttpServletResponse.SC_BAD_REQUEST, httpExchange.getResponseCode());
        assertTrue(httpExchange.getResponseBodyAsString().contains("<Code>AuthFailure</Code>"));
        assertTrue(httpExchange.getResponseBodyAsString().contains("<Message>" + message + "</Message>"));
        verify(this.chain, never()).doFilter(isA(HttpExchange.class));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testDoServiceRESTSoapFault() throws IOException {
        // setup
        String message = "Shit Happens";
        String code = "OMG";
        InputStream mockInputStream = mock(InputStream.class);
        when(mockInputStream.read((byte[]) any(), anyInt(), anyInt())).thenReturn(-1);
        MockHttpExchange httpExchange = new MockHttpExchange(mockInputStream, "Action=Test");
        String soap = "<SOAP-ENV:Envelope xmlns:SOAP-ENV=\"http://schemas.xmlsoap.org/soap/envelope/\"><SOAP-ENV:Header/><SOAP-ENV:Body><Payload xmlns=\"test.com\"/></SOAP-ENV:Body></SOAP-ENV:Envelope>";
        when(this.soapRequestFactory.getSoap(isA(Map.class))).thenReturn(soap);

        byte[] bytePayload = ("<SOAP-ENV:Fault xmlns:SOAP-ENV=\"http://schemas.xmlsoap.org/soap/envelope/\"><faultcode>" + code + "</faultcode><faultstring>" + message + "</faultstring><detail><aws:RequestId xmlns:aws=\"http://webservices.amazon.com/AWSFault/2005-15-09\">ad56d51c-b1df-4b15-95ca-9f71c2c65eea</aws:RequestId></detail></SOAP-ENV:Fault>")
                .getBytes();
        when(this.soapEnvelopeUtils.removeEnvelope(isA(byte[].class))).thenReturn(bytePayload);
        when(this.soapEnvelopeUtils.isSoapFault(isA(byte[].class))).thenReturn(true);
        when(this.soapEnvelopeUtils.getFaultCode(isA(byte[].class))).thenReturn(code);
        when(this.soapEnvelopeUtils.getFaultString(isA(byte[].class))).thenReturn(message);

        // act
        this.restQueryFilter.doFilter(httpExchange, chain);

        // assert
        assertEquals(HttpServletResponse.SC_BAD_REQUEST, httpExchange.getResponseCode());
        assertTrue(httpExchange.getResponseBodyAsString().contains("<Code>" + code + "</Code>"));
        assertTrue(httpExchange.getResponseBodyAsString().contains("<Message>" + message + "</Message>"));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testDoServiceRESTRequestParameterEmptyValue() throws Exception {
        // setup
        InputStream mockInputStream = mock(InputStream.class);
        when(mockInputStream.read((byte[]) any(), anyInt(), anyInt())).thenReturn(-1);
        MockHttpExchange httpExchange = new MockHttpExchange(mockInputStream, "Action=Test&Address=&Name=Fred");

        String soap = "<SOAP-ENV:Envelope xmlns:SOAP-ENV=\"http://schemas.xmlsoap.org/soap/envelope/\"><SOAP-ENV:Header/><SOAP-ENV:Body><Payload xmlns=\"test.com\"/></SOAP-ENV:Body></SOAP-ENV:Envelope>";
        when(this.soapRequestFactory.getSoap(isA(Map.class))).thenReturn(soap);

        byte[] bytePayload = "<results/>".getBytes();
        when(this.soapEnvelopeUtils.removeEnvelope(isA(byte[].class))).thenReturn(bytePayload);

        // act
        this.restQueryFilter.doFilter(httpExchange, chain);

        // assert
        verify(this.wsSecurityHandler, never()).processEnvelope(isA(Document.class));
        assertEquals(new String(bytePayload), httpExchange.getResponseBodyAsString());
        assertEquals(HttpServletResponse.SC_OK, httpExchange.getResponseCode());
        verify(this.chain).doFilter(isA(HttpExchange.class));
    }

    @Ignore("test excluded because of comment out code in RestQueryFilter - not sure it's needed")
    @SuppressWarnings("unchecked")
    @Test
    public void testThatStatusCodeGreaterThan399ResultsInSoapFault() throws IOException {
        // setup
        MockHttpExchange httpExchange = new MockHttpExchange(mock(InputStream.class), "Action=Test&Address=&Name=Fred");

        String soap = "<SOAP-ENV:Envelope xmlns:SOAP-ENV=\"http://schemas.xmlsoap.org/soap/envelope/\"><SOAP-ENV:Header/><SOAP-ENV:Body><Payload xmlns=\"test.com\"/></SOAP-ENV:Body></SOAP-ENV:Envelope>";
        when(this.soapRequestFactory.getSoap(isA(Map.class))).thenReturn(soap);

        doAnswer(new Answer<Object>() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                HttpExchangeImpl httpExchangeImpl = (HttpExchangeImpl) invocation.getArguments()[0];
                httpExchangeImpl.sendResponseHeaders(404, 0);
                return null;
            }
        }).when(this.chain).doFilter(isA(HttpExchangeImpl.class));

        // act
        this.restQueryFilter.doFilter(httpExchange, chain);

        // assert
        assertEquals(400, httpExchange.getResponseCode());
        assertTrue(httpExchange.getResponseBodyAsString().contains("UnknownParameter"));
        assertTrue(httpExchange.getResponseBodyAsString().contains("service error - 404"));
    }
}
