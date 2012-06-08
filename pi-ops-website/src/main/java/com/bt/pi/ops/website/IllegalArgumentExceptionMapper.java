/* (c) British Telecommunications plc, 2010, All Rights Reserved */
package com.bt.pi.ops.website;

import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

import org.springframework.stereotype.Component;

@Provider
@Component
public class IllegalArgumentExceptionMapper implements ExceptionMapper<IllegalArgumentException> {
	private static final int HTTP_BAD_REQUEST = 400;

	public IllegalArgumentExceptionMapper() {
	}

	public Response toResponse(IllegalArgumentException ex) {
		return Response.status(HTTP_BAD_REQUEST).entity(ex.getMessage()).type("text/plain").build();
	}
}
