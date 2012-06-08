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

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.security.KeyStore;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.TrustManagerFactory;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.core.io.Resource;

import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpsConfigurator;
import com.sun.net.httpserver.HttpsParameters;
import com.sun.net.httpserver.HttpsServer;

/**
 * Extension to the SimpleHttpServerFactoryBean that creates a HTTPS server instead of a HTTP server, based on the HTTPS
 * server that is included in Sun's JRE 1.6. Exposes the resulting {@link com.sun.net.httpserver.HttpsServer} object.
 * 
 * @author Martti von Hertzen
 * 
 */
public class SimpleHttpsServerFactoryBean extends SimpleHttpServerFactoryBean {
    private static final Log LOG = LogFactory.getLog(SimpleHttpsServerFactoryBean.class);

    private static final int FOUR_FOUR_THREE = 443;
    private static final String SUN_X509 = "SunX509";

    private char[] password = new char[0];
    private Resource keyStoreLocation;
    private String keyManagerAlgorithm = SUN_X509;
    private String trustManagerAlgorithm = SUN_X509;
    private String sslContextProtocol = "TLS";
    private String keyStoreType = "JKS";

    /**
     * Default constructor, sets the default port to 443.
     */
    public SimpleHttpsServerFactoryBean() {
        setPort(FOUR_FOUR_THREE);
    }

    /**
     * Sets the {@link KeyManagerFactory}'s algorithm. Default is SunX509.
     * 
     * @param aKeyManagerAlgorithm
     */
    public void setKeyManagerAlgorithm(String aKeyManagerAlgorithm) {
        this.keyManagerAlgorithm = aKeyManagerAlgorithm;
    }

    /**
     * Sets the {@link TrustManagerFactory}'s algorithm. Default is SunX509.
     * 
     * @param aTrustManagerAlgorithm
     */
    public void setTrustManagerAlgorithm(String aTrustManagerAlgorithm) {
        this.trustManagerAlgorithm = aTrustManagerAlgorithm;
    }

    /**
     * Set the {@link SSLContext}. Default is TLS.
     * 
     * @param aSslContextProtocol
     */
    public void setSslContextProtocol(String aSslContextProtocol) {
        this.sslContextProtocol = aSslContextProtocol;
    }

    /**
     * Set the {@link KeyStore}'s type. Default is JKS.
     * 
     * @param aKeyStoreType
     */
    public void setKeyStoreType(String aKeyStoreType) {
        this.keyStoreType = aKeyStoreType;
    }

    /**
     * Specify the location for the key store (file containing the SSL certificate). See <a
     * href="http://java.sun.com/javase/6/docs/technotes/tools/windows/keytool.html">
     * http://java.sun.com/javase/6/docs/technotes/tools/windows/keytool.html</a>
     * 
     * @param aKeyStoreLocation
     */
    public void setKeyStoreLocation(Resource aKeyStoreLocation) {
        this.keyStoreLocation = aKeyStoreLocation;
    }

    /**
     * Specify the password for the {@link KeyStore} and the {@link KeyManagerFactory}.
     */
    public void setPassword(String aPassword) {
        this.password = aPassword.toCharArray();
    }

    protected HttpServer getInitializedServer(InetSocketAddress address) throws IOException {
        HttpsServer server = HttpsServer.create(address, getBacklog());
        try {
            SSLContext sslContext = SSLContext.getInstance(sslContextProtocol);

            KeyStore ks = KeyStore.getInstance(keyStoreType);
            InputStream is = keyStoreLocation.getInputStream();
            try {
                ks.load(is, password);
            } catch (EOFException e) {
                LOG.warn(String.format("Unable to load certificate store %s. This may be possible because https isn't enabled with a valid certificate", keyStoreLocation));
                return null;
            }

            KeyManagerFactory kmf = KeyManagerFactory.getInstance(keyManagerAlgorithm);
            kmf.init(ks, password);

            TrustManagerFactory tmf = TrustManagerFactory.getInstance(trustManagerAlgorithm);
            tmf.init(ks);

            sslContext.init(kmf.getKeyManagers(), tmf.getTrustManagers(), null);

            final SSLEngine m_engine = sslContext.createSSLEngine();

            server.setHttpsConfigurator(new HttpsConfigurator(sslContext) {
                public void configure(HttpsParameters params) {
                    params.setSSLParameters(getSSLContext().getDefaultSSLParameters());
                    params.setNeedClientAuth(false);
                    params.setWantClientAuth(false);
                    params.setCipherSuites(m_engine.getEnabledCipherSuites());
                    params.setProtocols(m_engine.getEnabledProtocols());
                }
            });
        } catch (Throwable e) {
            throw new IOException("initializing HttpsServer failed due to exception", e);
        }
        return server;
    }
}