/* (c) British Telecommunications plc, 2010, All Rights Reserved */
package com.bt.pi.ops.website;

import static org.junit.Assert.assertEquals;

import javax.ws.rs.core.Response;

import org.junit.Test;

public class IllegalArgumentExceptionMapperTest {

	@Test
	public void toResponseShouldConvertTo400() {
		// setup
		IllegalArgumentExceptionMapper iaem = new IllegalArgumentExceptionMapper();

		// act
		Response resp = iaem.toResponse(new IllegalArgumentException());

		// assert
		assertEquals(400, resp.getStatus());
	}

}
