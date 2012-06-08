package com.bt.pi.api.utils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import com.sun.net.httpserver.HttpContext;
import com.sun.net.httpserver.HttpExchange;

@RunWith(MockitoJUnitRunner.class)
public class HttpUtilsTest {
    private HttpUtils httpUtils = new HttpUtils();
    @Mock
    private HttpExchange httpExchange;
    @Mock
    private HttpContext httpContext;
    private String path = "path/";

    @Before
    public void before() {
        when(httpExchange.getHttpContext()).thenReturn(httpContext);
        when(httpContext.getPath()).thenReturn(path);
    }

    @Test
    public void testReadPayload() throws IOException {
        // setup
        String payload = "payload";
        InputStream is = new ByteArrayInputStream(payload.getBytes());
        when(httpExchange.getRequestBody()).thenReturn(is);

        // act
        String result = httpUtils.readPayload(httpExchange);

        // assert
        assertEquals(payload, result);
    }

    @Test
    public void testReadRequestParametersFromQuery() throws URISyntaxException {
        // setup
        String payload = "";
        String queryString = "a=a&d&c=&&b=b";
        URI uri = new URI(path + "?" + queryString);
        when(httpExchange.getRequestURI()).thenReturn(uri);

        // act
        Map<String, String> result = httpUtils.readRequestParameters(httpExchange, payload);

        // assert
        assertEquals(4, result.size());
        assertEquals("a", result.get("a"));
        assertEquals("", result.get("c"));
        assertEquals("b", result.get("b"));
        assertNull(result.get("d"));
    }

    @Test
    public void testReadRequestParametersNoQuery() throws URISyntaxException {
        // setup
        String payload = "";
        URI uri = new URI(path);
        when(httpExchange.getRequestURI()).thenReturn(uri);

        // act
        Map<String, String> result = httpUtils.readRequestParameters(httpExchange, payload);

        // assert
        assertEquals(0, result.size());
    }

    @Test
    public void testReadRequestParametersNoQueryWithParameterPayload() throws URISyntaxException {
        // setup
        String payload = "a=a&d&c=&&b=b";
        URI uri = new URI(path);
        when(httpExchange.getRequestURI()).thenReturn(uri);

        // act
        Map<String, String> result = httpUtils.readRequestParameters(httpExchange, payload);

        // assert
        assertEquals(4, result.size());
        assertEquals("a", result.get("a"));
        assertEquals("", result.get("c"));
        assertEquals("b", result.get("b"));
        assertNull(result.get("d"));
    }

    @Test
    public void testReadRequestParametersNoQueryXmlPayload() throws URISyntaxException {
        // setup
        String payload = "<xml/>";
        URI uri = new URI(path);
        when(httpExchange.getRequestURI()).thenReturn(uri);

        // act
        Map<String, String> result = httpUtils.readRequestParameters(httpExchange, payload);

        // assert
        assertEquals(0, result.size());
    }

    @Test
    public void testReadRequestParametersQueryAndParameterPayload() throws URISyntaxException {
        // setup
        String payload = "a=a&d&c=&&b=b";
        String queryString = "f=f";
        URI uri = new URI(path + "?" + queryString);
        when(httpExchange.getRequestURI()).thenReturn(uri);

        // act
        Map<String, String> result = httpUtils.readRequestParameters(httpExchange, payload);

        // assert
        assertEquals(5, result.size());
        assertEquals("a", result.get("a"));
        assertEquals("", result.get("c"));
        assertEquals("b", result.get("b"));
        assertNull(result.get("d"));
        assertEquals("f", result.get("f"));
    }
}
