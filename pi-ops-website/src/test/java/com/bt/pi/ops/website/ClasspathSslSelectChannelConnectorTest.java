/* (c) British Telecommunications plc, 2010, All Rights Reserved */
package com.bt.pi.ops.website;

import org.junit.Test;

public class ClasspathSslSelectChannelConnectorTest {
	@Test
	public void shouldNotExplodeWithExceptions() {
		(new ClasspathSslSelectChannelConnector()).setKeystore("classpathSSCCTestFile");
	}
}
