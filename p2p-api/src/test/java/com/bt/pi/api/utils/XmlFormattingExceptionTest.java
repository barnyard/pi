package com.bt.pi.api.utils;

import static org.junit.Assert.*;

import org.junit.Test;

import com.bt.pi.api.utils.XmlFormattingException;

public class XmlFormattingExceptionTest {

	@Test
	public void testXmlFormattingException() {
		assertNotNull(new XmlFormattingException("blah", new RuntimeException("shit happens")));
	}
}
