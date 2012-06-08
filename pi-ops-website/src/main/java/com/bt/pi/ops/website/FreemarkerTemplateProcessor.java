/* (c) British Telecommunications plc, 2010, All Rights Reserved */
package com.bt.pi.ops.website;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.HashMap;
import java.util.Map;

import javax.annotation.Resource;
import javax.ws.rs.ext.Provider;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.stereotype.Component;

import com.sun.jersey.spi.template.TemplateProcessor;

import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;

/*
 * shamelessly borrowed class from the interweb
 */
@Component
@Provider
public class FreemarkerTemplateProcessor implements TemplateProcessor {
	private static final Log LOG = LogFactory.getLog(FreemarkerTemplateProcessor.class);
	private Configuration configuration;

	public FreemarkerTemplateProcessor() {
		configuration = null;
	}

	@Resource
	public void setConfiguration(Configuration config) {
		configuration = config;
	}

	@Override
	public String resolve(String theTemplateName) {
		String templateName = theTemplateName;
		LOG.debug("Trying to find template named " + templateName);
		templateName = templateName.substring(templateName.lastIndexOf('/') + 1);
		String template = null;
		String[] paths = new String[] { templateName, String.format("%s.ftl", templateName), String.format("templates/%s", templateName), String.format("templates/%s.ftl", templateName), };
		for (String path : paths) {
			if (null == template) {
				try {
					if (null != this.configuration.getTemplate(path)) {
						template = path;
					}
				} catch (IOException ex) {
					LOG.trace("Template not found at path " + path + ". Trying another");
				}
			}
		}
		if (null != template)
			LOG.debug("Found template named " + template);
		return template;
	}

	@SuppressWarnings("unchecked")
	@Override
	public void writeTo(String templatePath, Object model, OutputStream stream) throws IOException {
		LOG.debug("Generating view with template " + templatePath);

		Template template = this.configuration.getTemplate(templatePath);
		Map<String, Object> vars;
		if (model instanceof Map) {
			vars = new HashMap<String, Object>((Map) model);
		} else {
			vars = new HashMap<String, Object>(1);
			vars.put("it", model);
		}

		try {
			template.process(vars, new OutputStreamWriter(stream));
		} catch (TemplateException e) {
			LOG.warn("Exception with template found", e);
		}
	}
}
