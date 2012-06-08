package com.bt.pi.api.utils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.isA;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import com.bt.pi.api.utils.SoapRequestFactory;
import com.bt.pi.api.utils.XmlMappingException;


import freemarker.core.Environment;
import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;

public class SoapRequestFactoryTest {

	private SoapRequestFactory soapRequestFactory;
	private Map<String, Object> parameters;
	private Configuration configuration;

	@Before
	public void setUp() throws Exception {
		this.soapRequestFactory = new SoapRequestFactory();
		this.configuration = mock(Configuration.class);
		this.soapRequestFactory.setConfiguration(configuration);
		this.parameters = new HashMap<String, Object>();
	}

	@Test(expected = XmlMappingException.class)
	public void testGetSoapNoAction() {
		// setup

		// act
		try {
			this.soapRequestFactory.getSoap(parameters);
			fail();
		} catch (XmlMappingException e) {
			// assert
			assertEquals("Action null not supported", e.getMessage());
			throw e;
		}
	}

	@Test(expected = XmlMappingException.class)
	public void testGetSoapBogusAction() throws IOException {
		// setup
		when(this.configuration.getTemplate("bogus.ftl")).thenThrow(new IOException("file not found"));
		this.parameters.put("Action", "bogus");

		// act
		try {
			this.soapRequestFactory.getSoap(parameters);
			fail();
		} catch (XmlMappingException e) {
			// assert
			assertEquals("Action bogus not supported", e.getMessage());
			throw e;
		}
	}

	@Test
	public void testGetSoapGoodAction() throws IOException, TemplateException {
		// setup
		Template template = mock(Template.class);
		when(this.configuration.getTemplate("RunInstances.ftl")).thenReturn(template);
		final String soap = "<Soap/>";
		doAnswer(new Answer<Object>() {
			@Override
			public Object answer(InvocationOnMock invocation) throws Throwable {
				StringWriter sw = (StringWriter) invocation.getArguments()[1];
				sw.append(soap);
				return null;
			}
		}).when(template).process(isA(Map.class), isA(StringWriter.class));

		this.parameters.put("Action", "RunInstances");

		// act
		String result = this.soapRequestFactory.getSoap(parameters);

		// assert
		assertEquals(soap, result);
	}

	@Test(expected = XmlMappingException.class)
	public void testGetSoapTemplateFailsIOException() throws IOException, TemplateException {
		// setup
		Template template = mock(Template.class);
		when(this.configuration.getTemplate("RunInstances.ftl")).thenReturn(template);
		String message = "oh dear";
		doThrow(new IOException(message)).when(template).process(isA(Map.class), isA(StringWriter.class));

		this.parameters.put("Action", "RunInstances");

		// act
		try {
			this.soapRequestFactory.getSoap(parameters);
			fail();
		} catch (XmlMappingException e) {
			// assert
			assertEquals("Error processing request: " + message, e.getMessage());
			throw e;
		}
	}

	@Test(expected = XmlMappingException.class)
	public void testGetSoapTemplateFailsTemplateException() throws IOException, TemplateException {
		// setup
		Template template = mock(Template.class);
		when(this.configuration.getTemplate("RunInstances.ftl")).thenReturn(template);
		String message = "oh dear";
		Environment env = template.createProcessingEnvironment(parameters, new StringWriter());
		doThrow(new TemplateException(message, env)).when(template).process(isA(Map.class), isA(StringWriter.class));

		this.parameters.put("Action", "RunInstances");

		// act
		try {
			this.soapRequestFactory.getSoap(parameters);
			fail();
		} catch (XmlMappingException e) {
			// assert
			assertEquals("Error processing request: " + message, e.getMessage());
			throw e;
		}
	}
}
