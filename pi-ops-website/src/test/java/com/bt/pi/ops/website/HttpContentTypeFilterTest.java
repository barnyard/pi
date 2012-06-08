/* (c) British Telecommunications plc, 2010, All Rights Reserved */
package com.bt.pi.ops.website;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.net.URI;
import java.net.URISyntaxException;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;

import org.junit.Before;
import org.junit.Test;

import com.sun.jersey.core.util.MultivaluedMapImpl;
import com.sun.jersey.spi.container.ContainerRequest;

public class HttpContentTypeFilterTest {
	private static final String TEXT_PLAIN = "text/plain";
	private static final String ACCEPT = "accept";
	private HttpContentTypeFilter filter;
	private ContainerRequest request;
	private MultivaluedMap<String, String> headers;
	private URI uri;
	
	@Before
	public void doBefore() throws URISyntaxException{
		filter = new HttpContentTypeFilter();
		
		headers = new MultivaluedMapImpl();
		uri = new URI("http://localhost:8888/foo");
		request = mock(ContainerRequest.class);
		
		when(request.getRequestHeaders()).thenReturn(headers);
		when(request.getBaseUri()).thenReturn(uri);
		when(request.getRequestUri()).thenReturn(uri);
		
		headers.add(ACCEPT, TEXT_PLAIN);
	}
	
	@Test
	public void filterShouldDoNothingIfNoExtension(){
		//setup
		when(request.getPath()).thenReturn("users");
		
		//act
		filter.filter(request);
		
		//assert		
		assertEquals(TEXT_PLAIN, headers.getFirst(ACCEPT));
	}
	
	@Test
	public void filterShouldDoNothingIfExtensionUnknown(){
		//setup
		when(request.getPath()).thenReturn("users.blah");
		
		//act
		filter.filter(request);
		
		//assert		
		assertEquals(TEXT_PLAIN, headers.getFirst(ACCEPT));
	}
	
	@Test
	public void filterShouldChangeAcceptIfExtensionJson(){
		//setup
		when(request.getPath()).thenReturn("users.json");
		
		//act
		filter.filter(request);
		
		//assert		
		assertEquals(MediaType.APPLICATION_JSON, headers.getFirst(ACCEPT));
	}
	
	@Test
	public void filterShouldChangeAcceptIfExtensionHtml(){
		//setup
		when(request.getPath()).thenReturn("users.html");
		
		//act
		filter.filter(request);
		
		//assert		
		assertEquals(MediaType.TEXT_HTML, headers.getFirst(ACCEPT));
	}
	
	@Test
	public void filterShouldChangeAcceptIfExtensionXml(){
		//setup
		when(request.getPath()).thenReturn("users.xml");
		
		//act
		filter.filter(request);
		
		//assert		
		assertEquals(MediaType.APPLICATION_XML, headers.getFirst(ACCEPT));
	}
	
	
	
	
}
