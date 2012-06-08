package com.bt.pi.ops.website;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.junit.Test;

import com.sun.jersey.api.NotFoundException;

public class NotFoundExceptionMapperTest {
	@Test
	public void shouldReturnConflictResponse() throws Exception {
		// setup
		NotFoundExceptionMapper mapper = new NotFoundExceptionMapper();

		// act
		Response response = mapper.toResponse(new NotFoundException());

		// assert
		assertThat(response.getStatus(), equalTo(Status.NOT_FOUND.getStatusCode()));
	}
}
