package com.bt.pi.api.utils;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import org.junit.Before;
import org.junit.Test;

import com.bt.pi.app.common.id.PiIdBuilder;

public class ImageUrlFactoryTest {

    private ImageUrlFactory imageUrlFactory;
    private PiIdBuilder idBuilder;

    @Before
    public void before() {
        idBuilder = mock(PiIdBuilder.class);
        imageUrlFactory = new ImageUrlFactory(idBuilder);
    }

    @Test
    public void testImageFactoryUrl() {
        // setup
        String url = "url";

        // act
        imageUrlFactory.getIdFromUrl(url);

        // verify
        verify(idBuilder).getPId(url);

    }

}
