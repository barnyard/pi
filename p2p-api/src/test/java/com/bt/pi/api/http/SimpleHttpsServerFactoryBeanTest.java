package com.bt.pi.api.http;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.isA;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.EOFException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.security.KeyStore;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLParameters;
import javax.net.ssl.TrustManagerFactory;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.springframework.core.io.Resource;

import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpsConfigurator;
import com.sun.net.httpserver.HttpsParameters;
import com.sun.net.httpserver.HttpsServer;

@RunWith(PowerMockRunner.class)
@PrepareForTest( { SimpleHttpsServerFactoryBean.class, KeyStore.class, KeyManagerFactory.class, SSLContext.class, HttpsServer.class, TrustManagerFactory.class })
@PowerMockIgnore( { "org.apache.commons.logging.*", "org.apache.log4j.*" })
public class SimpleHttpsServerFactoryBeanTest {
    private InetSocketAddress inetSocketAddress;
    private HttpsServer httpsServer;
    private SSLContext sslContext;
    private KeyStore keyStore;
    private SimpleHttpsServerFactoryBean simpleHttpsServerFactoryBean;
    private KeyManagerFactory keyManagerFactory;
    private TrustManagerFactory trustManagerFactory;

    @Before
    public void setup() throws Exception {
        inetSocketAddress = mock(InetSocketAddress.class);
        httpsServer = PowerMockito.mock(HttpsServer.class);
        sslContext = PowerMockito.mock(SSLContext.class);
        keyManagerFactory = PowerMockito.mock(KeyManagerFactory.class);
        keyStore = PowerMockito.mock(KeyStore.class);
        trustManagerFactory = PowerMockito.mock(TrustManagerFactory.class);

        PowerMockito.mockStatic(KeyManagerFactory.class);
        PowerMockito.mockStatic(KeyStore.class);
        PowerMockito.mockStatic(SSLContext.class);
        PowerMockito.mockStatic(HttpsServer.class);
        PowerMockito.mockStatic(TrustManagerFactory.class);

        when(KeyManagerFactory.getInstance("SunX509")).thenReturn(keyManagerFactory);
        when(KeyStore.getInstance("JKS")).thenReturn(keyStore);
        when(SSLContext.getInstance("TLS")).thenReturn(sslContext);
        when(HttpsServer.create(inetSocketAddress, -1)).thenReturn(httpsServer);
        when(TrustManagerFactory.getInstance("SunX509")).thenReturn(trustManagerFactory);

        simpleHttpsServerFactoryBean = new SimpleHttpsServerFactoryBean();

        InputStream inputStream = mock(InputStream.class);
        Resource keyStoreLocation = mock(Resource.class);
        when(keyStoreLocation.getInputStream()).thenReturn(inputStream);
        simpleHttpsServerFactoryBean.setKeyStoreLocation(keyStoreLocation);
    }

    @Test
    public void shouldSetHttpsParametersOnEveryRequest() throws Exception {
        // setup
        final SSLParameters defaultSslParameters = mock(SSLParameters.class);
        final String[] cipherSuites = new String[0];
        final String[] protocols = new String[0];
        SSLEngine sslEngine = mock(SSLEngine.class);
        when(sslEngine.getEnabledCipherSuites()).thenReturn(cipherSuites);
        when(sslEngine.getEnabledProtocols()).thenReturn(protocols);

        when(sslContext.getDefaultSSLParameters()).thenReturn(defaultSslParameters);
        when(sslContext.createSSLEngine()).thenReturn(sslEngine);

        doAnswer(new Answer<Object>() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {

                HttpsParameters httpsParameters = mock(HttpsParameters.class);

                HttpsConfigurator httpsConfigurator = (HttpsConfigurator) invocation.getArguments()[0];
                httpsConfigurator.configure(httpsParameters);

                verify(httpsParameters).setSSLParameters(defaultSslParameters);
                verify(httpsParameters).setNeedClientAuth(false);
                verify(httpsParameters).setWantClientAuth(false);
                verify(httpsParameters).setCipherSuites(cipherSuites);
                verify(httpsParameters).setProtocols(protocols);

                return null;
            }
        }).when(httpsServer).setHttpsConfigurator(isA(HttpsConfigurator.class));

        // act
        HttpServer result = simpleHttpsServerFactoryBean.getInitializedServer(inetSocketAddress);

        // assert
        verify(httpsServer).setHttpsConfigurator(isA(HttpsConfigurator.class));
        assertEquals(httpsServer, result);
    }

    @Test
    public void shouldReturnNullWhileCreatingHttpsServerIfKeyStoreIsEmpty() throws Exception {
        // setup
        PowerMockito.doThrow(new EOFException()).when(keyStore).load(isA(InputStream.class), isA(char[].class));

        // act
        HttpServer result = simpleHttpsServerFactoryBean.getInitializedServer(inetSocketAddress);

        // assert
        assertNull(result);
    }

    @Test
    public void shouldReturnHttpsServerOnGetObject() throws Exception {
        // setup
        when(HttpsServer.create(isA(InetSocketAddress.class), eq(-1))).thenReturn(httpsServer);
        simpleHttpsServerFactoryBean.afterPropertiesSet();

        // act
        HttpsServer result = (HttpsServer) simpleHttpsServerFactoryBean.getObject();

        // assert
        assertThat(result, equalTo(httpsServer));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void shouldReturnHttpsServerOnGetObjectType() throws Exception {
        // setup
        when(HttpsServer.create(isA(InetSocketAddress.class), eq(-1))).thenReturn(httpsServer);
        simpleHttpsServerFactoryBean.afterPropertiesSet();

        // act
        Class result = simpleHttpsServerFactoryBean.getObjectType();

        // assert
        assertEquals(HttpsServer.class, result.getSuperclass());
    }
}
