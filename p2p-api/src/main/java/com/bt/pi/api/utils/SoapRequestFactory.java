/* (c) British Telecommunications plc, 2009, All Rights Reserved */
package com.bt.pi.api.utils;

import java.io.IOException;
import java.io.StringWriter;
import java.util.Map;

import javax.annotation.Resource;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.stereotype.Component;


import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;

/**
 * wrapper for FreeMarker template processing
 */
@Component
public class SoapRequestFactory {
	private static final String ACTION_NOT_SUPPORTED = "Action %s not supported";
	private static final String ERROR_PROCESSING_REQUEST_S = "Error processing request: %s";
	private static final Log LOG = LogFactory.getLog(SoapRequestFactory.class);
	private Configuration configuration;
	
	public SoapRequestFactory(){
		this.configuration = null;
	}
	
	public String getSoap(Map<String, Object> parameters) {
		LOG.debug(String.format("getSoap(%s)", parameters));
		String action = (String) parameters.get("Action");
		if (null == action)
			throw new XmlMappingException("Action null not supported");
		
		Template template = getTemplate(action);
		
		try {
			return render(template, parameters);
		} catch (IOException e) {
			LOG.error(String.format(ERROR_PROCESSING_REQUEST_S, e.getMessage()), e);
			throw new XmlMappingException(String.format(ERROR_PROCESSING_REQUEST_S, e.getMessage()), e);
		} catch (TemplateException e) {
			LOG.error(String.format(ERROR_PROCESSING_REQUEST_S, e.getMessage()), e);
			throw new XmlMappingException(String.format(ERROR_PROCESSING_REQUEST_S, e.getMessage()), e);
		}
	}
	
	private Template getTemplate(String action){
		Template template;
		try {
			template = this.configuration.getTemplate(String.format("%s.ftl", action));
		} catch (IOException ex) {
			try {
				template = configuration.getTemplate(String.format("templates/%s.ftl", action));
			} catch (IOException e){
				LOG.error(e.getMessage());
				throw new XmlMappingException(String.format(ACTION_NOT_SUPPORTED, action), e);
			}
		}
		if (null == template)
			throw new XmlMappingException(String.format(ACTION_NOT_SUPPORTED, action));
		return template;
	}
	
	private String render(Template template, Map<String, Object> model) throws TemplateException, IOException {
		StringWriter stringWriter = new StringWriter();
		template.process(model, stringWriter);
		return stringWriter.toString();
	}
	
	@Resource
	public void setConfiguration(Configuration cfg) {
		this.configuration = cfg;
	}
}
