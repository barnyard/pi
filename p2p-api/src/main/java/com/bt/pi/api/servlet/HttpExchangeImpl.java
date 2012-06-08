/* (c) British Telecommunications plc, 2009, All Rights Reserved */
package com.bt.pi.api.servlet;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URI;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpContext;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpPrincipal;

/**
 * A wrapper for HttpExchange to allow us to subvert the streams for REST processing
 */
public class HttpExchangeImpl extends HttpExchange {
    private static final Log LOG = LogFactory.getLog(HttpExchangeImpl.class);
    private ByteArrayOutputStream os = new ByteArrayOutputStream();
    private HttpExchange httpExchange;
    private byte[] buffer;
    private int statusCode = -1;

    public HttpExchangeImpl(HttpExchange aHttpExchange, byte[] bs) {
        this.httpExchange = aHttpExchange;
        this.buffer = bs;
    }

    public byte[] getBytes() {
        return this.os.toByteArray();
    }

    @Override
    public OutputStream getResponseBody() {
        return os;
    }

    @Override
    public void close() {
        try {
            this.os.close();
            this.httpExchange.getRequestBody().close();
        } catch (IOException e) {
            LOG.warn("Error closing streams", e);
        }
    }

    @Override
    public Object getAttribute(String arg0) {
        return this.httpExchange.getAttribute(arg0);
    }

    @Override
    public HttpContext getHttpContext() {
        return this.httpExchange.getHttpContext();
    }

    @Override
    public InetSocketAddress getLocalAddress() {
        return this.httpExchange.getLocalAddress();
    }

    @Override
    public HttpPrincipal getPrincipal() {
        return this.httpExchange.getPrincipal();
    }

    @Override
    public String getProtocol() {
        return this.httpExchange.getProtocol();
    }

    @Override
    public InetSocketAddress getRemoteAddress() {
        return this.httpExchange.getRemoteAddress();
    }

    @Override
    public InputStream getRequestBody() {
        return new ByteArrayInputStream(buffer);
    }

    @Override
    public Headers getRequestHeaders() {
        return this.httpExchange.getRequestHeaders();
    }

    @Override
    public String getRequestMethod() {
        return "POST";
    }

    @Override
    public URI getRequestURI() {
        return this.httpExchange.getRequestURI();
    }

    @Override
    public int getResponseCode() {
        if (this.statusCode < 0)
            return this.httpExchange.getResponseCode();
        return this.statusCode;
    }

    @Override
    public Headers getResponseHeaders() {
        return this.httpExchange.getResponseHeaders();
    }

    @Override
    public void sendResponseHeaders(int arg0, long arg1) throws IOException {
        this.statusCode = arg0;
    }

    @Override
    public void setAttribute(String arg0, Object arg1) {
        this.httpExchange.setAttribute(arg0, arg1);
    }

    @Override
    public void setStreams(InputStream arg0, OutputStream arg1) {
        this.httpExchange.setStreams(arg0, arg1);
    }
}
