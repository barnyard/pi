/* (c) British Telecommunications plc, 2009, All Rights Reserved */
package com.bt.pi.api.utils;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.net.URI;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.io.IOUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.stereotype.Component;

import com.sun.net.httpserver.HttpExchange;

@Component
public class HttpUtils {
    private static final Log LOG = LogFactory.getLog(HttpUtils.class);

    public HttpUtils() {
    }

    public String readPayload(HttpExchange httpExchange) throws IOException {
        InputStream inputStream = httpExchange.getRequestBody();
        StringWriter sw = new StringWriter();
        IOUtils.copy(inputStream, sw);
        return sw.toString();
    }

    public Map<String, String> readRequestParameters(HttpExchange arg0, String payload) {
        String path = arg0.getHttpContext().getPath();
        LOG.debug("path: " + path);

        URI requestURI = arg0.getRequestURI();
        LOG.debug("request URI: " + requestURI.toString());

        String stripped = requestURI.toString().substring(path.length());
        LOG.debug(stripped);

        String queryString = "";
        if (stripped.startsWith("?"))
            queryString = stripped.substring(1);

        LOG.debug("queryString: " + queryString);

        Map<String, String> result = readRequestParameters(queryString);
        if (!payload.startsWith("<"))
            result.putAll(readRequestParameters(payload));
        return result;
    }

    private Map<String, String> readRequestParameters(String request) {
        Map<String, String> result = new HashMap<String, String>();
        String[] split = request.split("&");
        LOG.debug(Arrays.toString(split));
        for (String pair : split) {
            LOG.debug(String.format("pair: %s", pair));
            if (pair.length() < 1)
                continue;
            int equalsIndex = pair.indexOf("=");
            if (equalsIndex > -1) {
                String value = pair.substring(equalsIndex + 1);
                result.put(pair.substring(0, equalsIndex), value);
            } else {
                result.put(pair, null);
            }
        }
        return result;
    }
}
