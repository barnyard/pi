package com.bt.pi.api.utils;

import com.bt.pi.core.id.PId;

public interface UrlFactory {

    String getUri(String s);

    PId getIdFromUrl(String url);
}
