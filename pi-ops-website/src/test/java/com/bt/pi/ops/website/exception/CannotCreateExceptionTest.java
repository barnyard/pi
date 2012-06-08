/* (c) British Telecommunications plc, 2010, All Rights Reserved */
package com.bt.pi.ops.website.exception;

import org.junit.Test;

import com.bt.pi.ops.website.exception.CannotCreateException;

public class CannotCreateExceptionTest {
	@Test
	public void canInstantiateCannotCreateException(){
		//act
		new CannotCreateException("");
	}
}
