/* (c) British Telecommunications plc, 2009, All Rights Reserved */
package com.bt.pi.app.instancemanager.images;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpException;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.io.IOUtils;
import org.springframework.stereotype.Component;

import com.bt.pi.core.conf.Property;

@Component
public class WebImageLoader implements ImageLoader {
    private HttpClient httpClient;
    private String imagePath;

    public WebImageLoader() {
        httpClient = new HttpClient();
    }

    @Property(key = "image.path", defaultValue = "var/images")
    public void setImagePath(String value) {
        this.imagePath = value;
    }

    @Override
    public String saveImage(String imageId, String piCacheDirectory) {
        String imageUrl = String.format("%s/%s", this.imagePath, imageId);

        try {
            GetMethod getMethod = getMethod(imageUrl);
            httpClient.executeMethod(getMethod);
            InputStream inputStream = getMethod.getResponseBodyAsStream();
            OutputStream outputStream = getOutputStream(piCacheDirectory, imageId);
            copyStreamToFile(inputStream, outputStream);
            return imageId;
        } catch (HttpException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    protected void setHttpClient(HttpClient aHttpClient) {
        httpClient = aHttpClient;
    }

    protected GetMethod getMethod(String path) {
        return new GetMethod(path);
    }

    protected OutputStream getOutputStream(String piCacheDirectory, String filename) throws FileNotFoundException {
        return new FileOutputStream(new File(String.format("%s%s%s", piCacheDirectory, File.pathSeparator, filename)));
    }

    protected void copyStreamToFile(InputStream inputStream, OutputStream outputStream) throws IOException {
        IOUtils.copy(inputStream, outputStream);
    }
}
