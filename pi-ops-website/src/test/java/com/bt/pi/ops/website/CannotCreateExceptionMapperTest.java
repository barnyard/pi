package com.bt.pi.ops.website;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.junit.Test;

import com.bt.pi.ops.website.exception.CannotCreateException;

public class CannotCreateExceptionMapperTest {
	@Test
	public void shouldReturnConflictResponse() throws Exception {
		// setup
		CannotCreateExceptionMapper mapper = new CannotCreateExceptionMapper();

		// act
		Response response = mapper.toResponse(new CannotCreateException(""));

		// assert
		assertThat(response.getStatus(), equalTo(Status.CONFLICT.getStatusCode()));
	}
}
