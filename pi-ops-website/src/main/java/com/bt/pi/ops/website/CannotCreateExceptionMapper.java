package com.bt.pi.ops.website;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

import org.springframework.stereotype.Component;

import com.bt.pi.ops.website.exception.CannotCreateException;

@Provider
@Component
public class CannotCreateExceptionMapper implements ExceptionMapper<CannotCreateException> {
	public CannotCreateExceptionMapper() {
	}

	@Override
	public Response toResponse(CannotCreateException e) {
		return Response.status(Status.CONFLICT).entity(e.getMessage()).type("text/plain").build();
	}
}
