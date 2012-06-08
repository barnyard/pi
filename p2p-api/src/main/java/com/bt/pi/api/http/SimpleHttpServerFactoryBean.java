/*
 * Copyright 2002-2008 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.bt.pi.api.http;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.core.task.TaskExecutor;
import org.springframework.core.task.support.ConcurrentExecutorAdapter;

import com.sun.net.httpserver.Authenticator;
import com.sun.net.httpserver.Filter;
import com.sun.net.httpserver.HttpContext;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

/**
 * {@link org.springframework.beans.factory.FactoryBean} that creates a simple HTTP server, based on the HTTP server
 * that is included in Sun's JRE 1.6. Starts the HTTP server on initialization and stops it on destruction. Exposes the
 * resulting {@link com.sun.net.httpserver.HttpServer} object.
 * 
 * <p>
 * Allows for registering {@link com.sun.net.httpserver.HttpHandler HttpHandlers} for specific {@link #setContexts
 * context paths}. Alternatively, register such context-specific handlers programmatically on the
 * {@link com.sun.net.httpserver.HttpServer} itself.
 * 
 * @author Juergen Hoeller
 * @author Arjen Poutsma
 * @since 2.5.1
 * @see #setPort
 * @see #setContexts
 */
public class SimpleHttpServerFactoryBean implements FactoryBean, InitializingBean, DisposableBean {
    private static final Log LOG = LogFactory.getLog(SimpleHttpServerFactoryBean.class);
    private static final int EIGHT_ZERO_EIGHT_ZERO = 8080;

    private int port = EIGHT_ZERO_EIGHT_ZERO;
    private String hostname;
    private int backlog = -1;
    private int shutdownDelay;
    private Executor executor;
    private Map<String, HttpHandler> contexts;
    private List<Filter> filters;
    private Authenticator authenticator;
    private HttpServer server;

    public SimpleHttpServerFactoryBean() {
        executor = null;
        contexts = null;
        filters = null;
        authenticator = null;
        server = null;
    }

    /**
     * Specify the HTTP server's port. Default is 8080.
     */
    public void setPort(int aPort) {
        this.port = aPort;
    }

    /**
     * Specify the HTTP server's hostname to bind to. Default is localhost; can be overridden with a specific network
     * address to bind to.
     */
    public void setHostname(String aHostname) {
        this.hostname = aHostname;
    }

    /**
     * Specify the HTTP server's TCP backlog. Default is -1, indicating the system's default value.
     */
    public void setBacklog(int aBacklog) {
        this.backlog = aBacklog;
    }

    int getBacklog() {
        return this.backlog;
    }

    /**
     * Specify the number of seconds to wait until HTTP exchanges have completed when shutting down the HTTP server.
     * Default is 0.
     */
    public void setShutdownDelay(int aShutdownDelay) {
        this.shutdownDelay = aShutdownDelay;
    }

    /**
     * Set the JDK concurrent executor to use for dispatching incoming requests.
     * 
     * @see com.sun.net.httpserver.HttpServer#setExecutor
     */
    public void setExecutor(Executor anExecutor) {
        this.executor = anExecutor;
    }

    /**
     * Set the Spring TaskExecutor to use for dispatching incoming requests.
     * 
     * @see com.sun.net.httpserver.HttpServer#setExecutor
     */
    public void setTaskExecutor(TaskExecutor anExecutor) {
        this.executor = new ConcurrentExecutorAdapter(anExecutor);
    }

    /**
     * Register {@link com.sun.net.httpserver.HttpHandler HttpHandlers} for specific context paths.
     * 
     * @param aContexts
     *            a Map with context paths as keys and HttpHandler objects as values
     * @see org.springframework.remoting.httpinvoker.SimpleHttpInvokerServiceExporter
     * @see org.springframework.remoting.caucho.SimpleHessianServiceExporter
     * @see org.springframework.remoting.caucho.SimpleBurlapServiceExporter
     */
    public void setContexts(Map<String, HttpHandler> aContexts) {
        this.contexts = aContexts;
    }

    /**
     * Register common {@link com.sun.net.httpserver.Filter Filters} to be applied to all locally registered
     * {@link #setContexts contexts}.
     */
    public void setFilters(List<Filter> aFilters) {
        this.filters = aFilters;
    }

    /**
     * Register a common {@link com.sun.net.httpserver.Authenticator} to be applied to all locally registered
     * {@link #setContexts contexts}.
     */
    public void setAuthenticator(Authenticator anAuthenticator) {
        this.authenticator = anAuthenticator;
    }

    public void afterPropertiesSet() throws IOException {
        InetSocketAddress address = this.hostname != null ? new InetSocketAddress(this.hostname, this.port) : new InetSocketAddress(this.port);
        this.server = getInitializedServer(address);
        if (server == null) {
            LOG.warn("Unable to create server");
            return;
        }

        if (this.executor != null) {
            this.server.setExecutor(this.executor);
        }
        if (this.contexts != null) {
            for (String key : this.contexts.keySet()) {
                HttpContext httpContext = this.server.createContext(key, this.contexts.get(key));
                if (this.filters != null) {
                    httpContext.getFilters().addAll(this.filters);
                }
                if (this.authenticator != null)
                    httpContext.setAuthenticator(this.authenticator);
            }
        }
        if (LOG.isInfoEnabled()) {
            LOG.info("Starting HttpServer at address " + address);
        }
        this.server.start();
    }

    protected HttpServer getInitializedServer(InetSocketAddress address) throws IOException {
        return HttpServer.create(address, this.backlog);
    }

    public Object getObject() {
        return this.server;
    }

    public Class getObjectType() {
        return this.server != null ? this.server.getClass() : HttpServer.class;
    }

    public boolean isSingleton() {
        return true;
    }

    public void destroy() {
        LOG.info("Stopping HttpServer");
        this.server.stop(this.shutdownDelay);
    }
}