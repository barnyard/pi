/* (c) British Telecommunications plc, 2010, All Rights Reserved */
/* (c) British Telecommunications plc, 2010, All Rights Reserved */
package com.bt.pi.ops.website;

import static org.junit.Assert.assertNotNull;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.io.OutputStream;
import java.io.Writer;

import org.junit.Before;
import org.junit.Test;

import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;

public class FreemarkerTemplateProcessorTest {

	private static final String TEMPLATE_NAME = "foo";
	private FreemarkerTemplateProcessor processor;
	private Configuration config;
	private Template template;
	private Object model;
	private OutputStream outStream;
	
	@Before
	public void doBefore() throws IOException{
		processor = new FreemarkerTemplateProcessor();
		
		config = mock(Configuration.class);
		template = mock(Template.class);
		model = mock(Object.class);
		outStream = mock(OutputStream.class);
		
		when(config.getTemplate(any(String.class))).thenReturn(template);
		
		processor.setConfiguration(config);
	}
	
	@Test
	public void writeToShouldProcessTheTemplate() throws IOException, TemplateException{
		//act 
		processor.writeTo(TEMPLATE_NAME, model, outStream);

		//assert
		verify(template).process(any(Object.class), any(Writer.class));
	}
	
	@Test
	public void resolveShouldFindTheTemplate(){
		//act
		String result = processor.resolve(TEMPLATE_NAME);
		
		//assert
		assertNotNull(result);
	}
}


