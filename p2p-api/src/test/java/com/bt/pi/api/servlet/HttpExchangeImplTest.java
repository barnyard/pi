package com.bt.pi.api.servlet;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URI;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Matchers;

import com.bt.pi.api.servlet.HttpExchangeImpl;
import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpContext;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpPrincipal;

public class HttpExchangeImplTest {
    private HttpExchangeImpl httpExchangeImpl;
    private byte[] bs;
    private HttpExchange httpExchange;

    @Before
    public void setUp() throws Exception {
        this.bs = "some data".getBytes();
        this.httpExchange = mock(HttpExchange.class);
        this.httpExchangeImpl = new HttpExchangeImpl(httpExchange, bs);
    }

    @Test
    public void testClose() throws IOException {
        // setup
        InputStream is = mock(InputStream.class);
        when(this.httpExchange.getRequestBody()).thenReturn(is);

        // act
        this.httpExchangeImpl.close();

        // assert
        verify(is).close();
    }

    @Test
    public void testSendResponseHeaders() throws IOException {
        // setup

        // act
        this.httpExchangeImpl.sendResponseHeaders(1, 2);

        // assert
        verify(this.httpExchange, never()).sendResponseHeaders(Matchers.anyInt(), Matchers.anyLong());
    }

    @Test
    public void testGetResponseCode() {
        // setup
        int expectedResult = 200;
        when(this.httpExchange.getResponseCode()).thenReturn(expectedResult);

        // act
        int result = this.httpExchangeImpl.getResponseCode();

        // assert
        assertEquals(expectedResult, result);
    }

    @Test
    public void testGetBytes() throws IOException {
        // setup
        byte[] byteArray = "fred".getBytes();
        this.httpExchangeImpl.getResponseBody().write(byteArray);

        // act
        byte[] result = this.httpExchangeImpl.getBytes();

        // assert
        assertByteArrayEquals(byteArray, result);
    }

    private void assertByteArrayEquals(byte[] byteArray, byte[] result) {
        assertEquals(byteArray.length, result.length);
        for (int i = 0; i < byteArray.length; i++) {
            assertEquals(byteArray[i], result[i]);
        }
    }

    @Test
    public void testGetResponseBody() throws IOException {
        // setup
        byte[] byteArray = "fred".getBytes();
        this.httpExchangeImpl.getResponseBody().write(byteArray);

        // act
        OutputStream result = this.httpExchangeImpl.getResponseBody();

        // assert
        assertTrue(result instanceof ByteArrayOutputStream);
        ByteArrayOutputStream bos = (ByteArrayOutputStream) result;
        assertByteArrayEquals(byteArray, bos.toByteArray());
    }

    @Test
    public void testGetAttributeString() {
        // setup
        String name = "name";
        Object expectedResult = "result";
        when(this.httpExchange.getAttribute(name)).thenReturn(expectedResult);

        // act
        Object result = this.httpExchangeImpl.getAttribute(name);

        // assert
        assertEquals(expectedResult, result);
    }

    @Test
    public void testGetHttpContext() {
        // setup
        HttpContext expectedResult = mock(HttpContext.class);
        when(this.httpExchange.getHttpContext()).thenReturn(expectedResult);

        // act
        HttpContext result = this.httpExchangeImpl.getHttpContext();

        // assert
        assertEquals(expectedResult, result);
    }

    @Test
    public void testGetLocalAddress() {
        // setup
        InetSocketAddress expectedResult = mock(InetSocketAddress.class);
        when(this.httpExchange.getLocalAddress()).thenReturn(expectedResult);

        // act
        InetSocketAddress result = this.httpExchangeImpl.getLocalAddress();

        // assert
        assertEquals(expectedResult, result);
    }

    @Test
    public void testGetPrincipal() {
        // setup
        HttpPrincipal expectedResult = mock(HttpPrincipal.class);
        when(this.httpExchange.getPrincipal()).thenReturn(expectedResult);

        // act
        HttpPrincipal result = this.httpExchangeImpl.getPrincipal();

        // assert
        assertEquals(expectedResult, result);
    }

    @Test
    public void testGetProtocol() {
        // setup
        String expectedResult = "http";
        when(this.httpExchange.getProtocol()).thenReturn(expectedResult);

        // act
        String result = this.httpExchangeImpl.getProtocol();

        // assert
        assertEquals(expectedResult, result);
    }

    @Test
    public void testGetRemoteAddress() {
        // setup
        InetSocketAddress expectedResult = mock(InetSocketAddress.class);
        when(this.httpExchange.getRemoteAddress()).thenReturn(expectedResult);

        // act
        InetSocketAddress result = this.httpExchangeImpl.getRemoteAddress();

        // assert
        assertEquals(expectedResult, result);
    }

    @Test
    public void testGetRequestBody() throws IOException {
        // setup

        // act
        InputStream result = this.httpExchangeImpl.getRequestBody();

        // assert
        assertTrue(result instanceof ByteArrayInputStream);
        ByteArrayInputStream bais = (ByteArrayInputStream) result;
        byte[] buffer = new byte[this.bs.length];
        assertEquals(this.bs.length, bais.read(buffer));
        assertByteArrayEquals(this.bs, buffer);
    }

    @Test
    public void testGetRequestHeaders() {
        // setup
        Headers expectedResult = mock(Headers.class);
        when(this.httpExchange.getRequestHeaders()).thenReturn(expectedResult);

        // act
        Headers result = this.httpExchangeImpl.getRequestHeaders();

        // assert
        assertEquals(expectedResult, result);
    }

    @Test
    public void testGetRequestMethod() {
        // setup
        String expectedResult = "POST";

        // act
        String result = this.httpExchangeImpl.getRequestMethod();

        // assert
        assertEquals(expectedResult, result);
    }

    @Test
    public void testGetRequestURI() {
        // setup
        URI expectedResult = URI.create("/");
        when(this.httpExchange.getRequestURI()).thenReturn(expectedResult);

        // act
        URI result = this.httpExchangeImpl.getRequestURI();

        // assert
        assertEquals(expectedResult, result);
    }

    @Test
    public void testGetResponseHeaders() {
        // setup
        Headers expectedResult = mock(Headers.class);
        when(this.httpExchange.getResponseHeaders()).thenReturn(expectedResult);

        // act
        Headers result = this.httpExchangeImpl.getResponseHeaders();

        // assert
        assertEquals(expectedResult, result);
    }

    @Test
    public void testSetAttributeStringObject() {
        // setup
        String name = "name";
        Object value = new Object();

        // act
        this.httpExchangeImpl.setAttribute(name, value);

        // assert
        verify(this.httpExchange).setAttribute(name, value);
    }

    @Test
    public void testSetStreamsInputStreamOutputStream() {
        // setup
        InputStream is = mock(InputStream.class);
        OutputStream os = mock(OutputStream.class);

        // act
        this.httpExchangeImpl.setStreams(is, os);

        // assert
        verify(this.httpExchange).setStreams(is, os);
    }

    @Test
    public void testThatSendResponseHeaderStatusCodeIsCached() throws IOException {
        // setup
        long length = 0;
        int code = 404;

        // act
        this.httpExchangeImpl.sendResponseHeaders(code, length);
        int result = this.httpExchangeImpl.getResponseCode();

        // assert
        assertEquals(code, result);
    }
}
