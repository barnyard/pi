package com.bt.pi.ops.website;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

import org.springframework.stereotype.Component;

import com.sun.jersey.api.NotFoundException;

@Provider
@Component
public class NotFoundExceptionMapper implements ExceptionMapper<NotFoundException> {
	public NotFoundExceptionMapper() {
	}

	@Override
	public Response toResponse(NotFoundException e) {
		return Response.status(Status.NOT_FOUND).entity(e.getMessage()).type("text/plain").build();
	}
}
