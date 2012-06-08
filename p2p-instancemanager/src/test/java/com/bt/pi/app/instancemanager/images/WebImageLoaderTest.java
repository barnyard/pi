package com.bt.pi.app.instancemanager.images;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpException;
import org.apache.commons.httpclient.methods.GetMethod;
import org.junit.Before;
import org.junit.Test;

import com.bt.pi.app.instancemanager.images.WebImageLoader;

public class WebImageLoaderTest {
    private WebImageLoader webImageLoader;
    private HttpClient httpClient;
    private GetMethod getMethod;
    private InputStream inputStream;
    private OutputStream outputStream;
    private String fromPath;
    private String piCacheDirectory;
    private String imageUri;

    @Before
    public void setUp() throws Exception {

        imageUri = "http://localhost";

        fromPath = "image";
        piCacheDirectory = "/tmp";

        httpClient = mock(HttpClient.class);
        getMethod = mock(GetMethod.class);
        when(getMethod.getResponseBodyAsStream()).thenReturn(inputStream);

        webImageLoader = new WebImageLoader() {
            @Override
            protected GetMethod getMethod(String imagePath) {
                assertThat(imagePath, equalTo(imageUri + "/" + fromPath));
                return getMethod;
            }

            @Override
            protected OutputStream getOutputStream(String directory, String filename) throws FileNotFoundException {
                assertThat(directory, equalTo(piCacheDirectory));
                assertThat(filename, equalTo("image"));
                return outputStream;
            }

            @Override
            protected void copyStreamToFile(InputStream iStream, OutputStream oStream) throws IOException {
                assertThat(iStream, equalTo(inputStream));
                assertThat(oStream, equalTo(outputStream));
            }
        };

        webImageLoader.setImagePath(imageUri);
        webImageLoader.setHttpClient(httpClient);
    }

    @Test
    public void shouldSaveImageInPiCacheDirectory() throws Exception {
        // setup

        // act
        String savedImagePath = webImageLoader.saveImage(fromPath, piCacheDirectory);

        // assert
        verify(httpClient).executeMethod(getMethod);
        assertThat(savedImagePath, equalTo("image"));
    }

    @Test(expected = RuntimeException.class)
    public void shouldWrapIOExceptionAsRuntimeException() throws Exception {
        // setup
        Throwable exception = new IOException();
        when(httpClient.executeMethod(getMethod)).thenThrow(exception);

        // act & assert
        try {
            webImageLoader.saveImage(fromPath, piCacheDirectory);
        } catch (Exception e) {
            assertThat(e.getCause(), equalTo(exception));
            throw e;
        }
    }

    @Test(expected = RuntimeException.class)
    public void shouldWrapHttpExceptionAsRuntimeException() throws Exception {
        // setup
        Throwable exception = new HttpException();
        when(httpClient.executeMethod(getMethod)).thenThrow(exception);

        // act & assert
        try {
            webImageLoader.saveImage(fromPath, piCacheDirectory);
        } catch (Exception e) {
            assertThat(e.getCause(), equalTo(exception));
            throw e;
        }
    }

    @Test
    public void testGetMethod() throws Exception {
        // setup
        webImageLoader = new WebImageLoader();

        // act
        GetMethod method = webImageLoader.getMethod(fromPath);

        // assert
        assertThat(method.getURI().toString(), equalTo(fromPath));
    }
}
