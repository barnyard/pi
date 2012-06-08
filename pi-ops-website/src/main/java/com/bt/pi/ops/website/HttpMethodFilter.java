/* (c) British Telecommunications plc, 2009, All Rights Reserved */
package com.bt.pi.ops.website;

import java.util.Locale;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.sun.jersey.spi.container.ContainerRequest;
import com.sun.jersey.spi.container.ContainerRequestFilter;

/**
 * Clients may override the HTTP method by setting either the
 * X-HTTP-Method-Override header or the _method form or query parameter in a
 * POST request. If both the X-HTTP-Method-Override header and _method parameter
 * are present in the request then the X-HTTP-Method-Override header will be
 * used.
 * 
 * Inspired by
 * https://jersey.dev.java.net/nonav/apidocs/1.1.0-ea/jersey/com/sun/
 * jersey/api/container/filter/PostReplaceFilter.html
 */
public class HttpMethodFilter implements ContainerRequestFilter {
	/** The name of the form or query parameter that overrides the HTTP method. */
	public static final String METHOD = "_method";

	private static final Log LOG = LogFactory.getLog(HttpMethodFilter.class);
	private static final String HEADER = "X-HTTP-Method-Override";

	public HttpMethodFilter(){}
	
	@Override
	public ContainerRequest filter(ContainerRequest request) {
		if (request.getMethod().equalsIgnoreCase("POST") 
				&& (!override(request.getHeaderValue(HEADER), request))
				&& (!override(request.getFormParameters().getFirst(METHOD), request))) {
					override(request.getQueryParameters().getFirst(METHOD),	request);
					}

		return request;
	}

	private boolean override(String method, ContainerRequest request) {
		if (null != method && !method.isEmpty()) {
			LOG.debug(String.format("changing request method from %s to %s", request.getMethod(), method.toUpperCase(Locale.getDefault())));
			request.setMethod(method.toUpperCase(Locale.getDefault()));
			return true;
		}

		return false;
	}
}
