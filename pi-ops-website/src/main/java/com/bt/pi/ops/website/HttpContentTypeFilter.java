/* (c) British Telecommunications plc, 2010, All Rights Reserved */
package com.bt.pi.ops.website;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.UriBuilder;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.sun.jersey.spi.container.ContainerRequest;
import com.sun.jersey.spi.container.ContainerRequestFilter;

public class HttpContentTypeFilter implements ContainerRequestFilter {
	private static final String DOT = ".";
	private static final Log LOG = LogFactory.getLog(HttpContentTypeFilter.class);
	private static final Map<String, String> MAP = new HashMap<String, String>();
	
	public HttpContentTypeFilter(){
		MAP.put("xml", MediaType.APPLICATION_XML);
		MAP.put("json", MediaType.APPLICATION_JSON);
		MAP.put("html", MediaType.TEXT_HTML);
	}
	
	@Override
	public ContainerRequest filter(ContainerRequest request) {		
		String path = request.getPath();
		String extension = path.substring(path.lastIndexOf(DOT)+1);
		if(MAP.containsKey(extension)){
			LOG.debug("overriding Accept type to " + MAP.get(extension));
			List<String> acceptType = Arrays.asList(new String[] {MAP.get(extension)});
			request.getRequestHeaders().put("accept", acceptType);
			String newPath = path.replaceFirst(DOT+extension+"$", "");
			request.setUris(request.getBaseUri(), UriBuilder.fromUri(request.getRequestUri()).replacePath(newPath).build());
		}
		
		return request;
	}
}
