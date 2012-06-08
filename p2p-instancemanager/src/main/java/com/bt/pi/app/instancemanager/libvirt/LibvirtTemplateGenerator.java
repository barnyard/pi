package com.bt.pi.app.instancemanager.libvirt;

import java.util.Map;

import javax.annotation.Resource;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.stereotype.Component;

import com.bt.pi.core.util.template.TemplateHelper;

@Component
public class LibvirtTemplateGenerator {
    private static final Log LOG = LogFactory.getLog(LibvirtTemplateGenerator.class);
    private TemplateHelper templateHelper;

    public LibvirtTemplateGenerator() {
        templateHelper = null;
    }

    public String buildXml(Map<String, Object> model) {
        LOG.debug(String.format("buildXml(%s)", model));
        return templateHelper.generate("libvirt.xml.ftl", model);
    }

    @Resource
    public void setTemplateHelper(TemplateHelper aTemplateHelper) {
        this.templateHelper = aTemplateHelper;
    }
}
