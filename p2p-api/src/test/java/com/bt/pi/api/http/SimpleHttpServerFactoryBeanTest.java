package com.bt.pi.api.http;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;

import org.junit.Before;
import org.junit.Test;

import com.sun.net.httpserver.Authenticator;
import com.sun.net.httpserver.Filter;
import com.sun.net.httpserver.HttpContext;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

public class SimpleHttpServerFactoryBeanTest {
    private HttpServer httpServer;
    private SimpleHttpServerFactoryBean simpleHttpServerFactoryBean;

    @Before
    public void setup() {
        httpServer = mock(HttpServer.class);

        simpleHttpServerFactoryBean = new SimpleHttpServerFactoryBean() {
            @Override
            protected HttpServer getInitializedServer(InetSocketAddress address) throws IOException {
                return httpServer;
            }
        };
    }

    @Test
    public void shouldNotThrowAnyExceptionsIfUnableToInitializeServer() throws Exception {
        // setup
        simpleHttpServerFactoryBean = new SimpleHttpServerFactoryBean() {
            @Override
            protected HttpServer getInitializedServer(InetSocketAddress address) throws IOException {
                return null;
            }
        };

        // act
        try {
            simpleHttpServerFactoryBean.afterPropertiesSet();
        } catch (Throwable t) {
            fail("Unexpected exception: " + t.getMessage());
        }
    }

    @Test
    public void shouldStartHttpServer() throws Exception {
        // act
        simpleHttpServerFactoryBean.afterPropertiesSet();

        // assert
        verify(httpServer).start();
    }

    @Test
    public void shouldSetFiltersAndAuthenticatorsOnHttpContext() throws Exception {
        // setup
        Map<String, HttpHandler> contexts = new HashMap<String, HttpHandler>();
        HttpHandler httpHandler = mock(HttpHandler.class);
        contexts.put("test1", httpHandler);

        Filter filter = mock(Filter.class);
        List<Filter> filters = new ArrayList<Filter>();
        filters.add(filter);

        Authenticator authenticator = mock(Authenticator.class);

        simpleHttpServerFactoryBean.setContexts(contexts);
        simpleHttpServerFactoryBean.setFilters(filters);
        simpleHttpServerFactoryBean.setAuthenticator(authenticator);

        List<Filter> httpContextFilters = new ArrayList<Filter>();

        HttpContext httpContext = mock(HttpContext.class);
        when(httpContext.getFilters()).thenReturn(httpContextFilters);
        when(httpServer.createContext("test1", httpHandler)).thenReturn(httpContext);

        // act
        simpleHttpServerFactoryBean.afterPropertiesSet();

        // assert
        verify(httpContext).setAuthenticator(authenticator);
        assertThat(httpContextFilters.contains(filter), is(true));
    }

    @Test
    public void shouldSetExecutor() throws Exception {
        Executor executor = mock(Executor.class);
        simpleHttpServerFactoryBean.setExecutor(executor);

        // act
        simpleHttpServerFactoryBean.afterPropertiesSet();

        // assert
        verify(httpServer).setExecutor(executor);
    }

    @Test
    public void shouldReturnHttpServerOnGetObject() throws Exception {
        // setup
        simpleHttpServerFactoryBean.afterPropertiesSet();

        // act
        HttpServer result = (HttpServer) simpleHttpServerFactoryBean.getObject();

        // assert
        assertThat(result, equalTo(httpServer));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void shouldReturnHttpServerOnGetObjectType() throws Exception {
        // act
        Class result = simpleHttpServerFactoryBean.getObjectType();

        // assert
        assertEquals(HttpServer.class, result);
    }

    @Test
    public void shouldReturnTrueForIsSingleton() throws Exception {
        // act
        boolean result = simpleHttpServerFactoryBean.isSingleton();

        // assert
        assertThat(result, is(true));
    }
}
