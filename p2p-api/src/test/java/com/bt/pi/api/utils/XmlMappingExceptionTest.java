package com.bt.pi.api.utils;

import static org.junit.Assert.*;

import org.junit.Test;

import com.bt.pi.api.utils.XmlMappingException;

public class XmlMappingExceptionTest {

	@Test
	public void testXmlFormattingException() {
		assertNotNull(new XmlMappingException("blah", new RuntimeException("shit happens")));
	}
}
