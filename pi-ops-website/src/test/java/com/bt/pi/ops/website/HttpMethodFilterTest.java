/* (c) British Telecommunications plc, 2010, All Rights Reserved */
package com.bt.pi.ops.website;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.Before;
import org.junit.Test;

import com.sun.jersey.api.representation.Form;
import com.sun.jersey.spi.container.ContainerRequest;

public class HttpMethodFilterTest {

	private static final String NEW_METHOD = "NEW_METHOD";
	private ContainerRequest request;
	private HttpMethodFilter methodFilter;
	private Form form;
	
	@Before
	public void doBefore(){
		methodFilter = new HttpMethodFilter();
		
		request = mock(ContainerRequest.class);
		form = new Form();
		
		when(request.getMethod()).thenReturn("post");
		when(request.getHeaderValue(any(String.class))).thenReturn(null);
		when(request.getFormParameters()).thenReturn(form);
	}
	
	@Test
	public void filterShouldDoNothingIfNotPOST(){
		//setup
		when(request.getMethod()).thenReturn("get");
		
		//act 
		methodFilter.filter(request);
		
		//assert
		verify(request, never()).setMethod(any(String.class));
	}
	
	@Test
	public void filterShouldChangeIf_methodPassedIn(){
		//setup
		form.add("_method", NEW_METHOD);
		
		//act 
		methodFilter.filter(request);
		
		//assert
		verify(request).setMethod(NEW_METHOD);
	}
}
