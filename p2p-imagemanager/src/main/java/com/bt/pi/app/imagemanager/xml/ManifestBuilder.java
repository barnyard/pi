/* (c) British Telecommunications plc, 2010, All Rights Reserved */
package com.bt.pi.app.imagemanager.xml;

import java.io.File;
import java.util.List;

import javax.annotation.Resource;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.stereotype.Component;
import org.w3c.dom.Document;

@Component
public class ManifestBuilder {
    private static final String BUILD_S = "build(%s)";
    private static final Log LOG = LogFactory.getLog(ManifestBuilder.class);
    private XMLParser parser;
    private XPathEvaluator evaluator;

    public ManifestBuilder() {
        this.parser = null;
        this.evaluator = null;
    }

    public Manifest build(String xml) {
        LOG.debug(String.format(BUILD_S, xml));
        Document document = this.parser.parse(xml);

        return evaluate(document);
    }

    private Manifest evaluate(Document document) {
        String encryptedKey = this.evaluator.getValue("//ec2_encrypted_key", document);
        String encryptedIV = this.evaluator.getValue("//ec2_encrypted_iv", document);
        String signature = this.evaluator.getValue("//signature", document);
        String machineConfiguration = this.evaluator.getXMLFragment("//machine_configuration", document);
        String image = this.evaluator.getXMLFragment("//image", document);
        List<String> partFilenames = this.evaluator.getValues("//image/parts/part/filename", document);
        String arch = this.evaluator.getValue("//machine_configuration/architecture", document);
        String kernelId = this.evaluator.getValue("//machine_configuration/kernel_id", document);
        String ramdiskId = this.evaluator.getValue("//machine_configuration/ramdisk_id", document);

        return new Manifest(encryptedKey, encryptedIV, signature, machineConfiguration, image, partFilenames, arch, kernelId, ramdiskId);
    }

    public Manifest build(File file) {
        LOG.debug(String.format(BUILD_S, file.toString()));
        Document document = this.parser.parse(file);

        return evaluate(document);
    }

    @Resource
    public void setParser(XMLParser aParser) {
        this.parser = aParser;
    }

    @Resource
    public void setEvaluator(XPathEvaluator anEvaluator) {
        this.evaluator = anEvaluator;
    }
}
