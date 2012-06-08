package com.bt.pi.api.utils;

import com.bt.pi.app.common.entities.Image;
import com.bt.pi.app.common.id.PiIdBuilder;
import com.bt.pi.core.id.PId;

public class ImageUrlFactory implements UrlFactory {

    private PiIdBuilder piIdBuilder;

    public ImageUrlFactory(PiIdBuilder idBuilder) {
        piIdBuilder = idBuilder;
    }

    @Override
    public String getUri(String s) {
        return Image.getUrl(s);
    }

    @Override
    public PId getIdFromUrl(String url) {
        return piIdBuilder.getPId(url);
    }
}
