/* (c) British Telecommunications plc, 2010, All Rights Reserved */
package com.bt.pi.app.imagemanager.xml;

import java.io.File;
import java.io.IOException;
import java.io.StringReader;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.commons.io.FileUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.stereotype.Component;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

@Component
public class XMLParser {
    private static final Log LOG = LogFactory.getLog(XMLParser.class);

    public XMLParser() {
    }

    public Document parse(File file) {
        LOG.debug(String.format("parse(%s)", file));
        if (null == file)
            throw new IllegalArgumentException("file cannot be null");
        try {
            return parse(FileUtils.readFileToString(file));
        } catch (IOException ioe) {
            throw new ParseException(String.format("XML file %s not found", file.getPath()), ioe);
        }
    }

    public Document parse(String xml) {
        DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder docBuilder;
        try {
            docBuilder = docFactory.newDocumentBuilder();
            return docBuilder.parse(new InputSource(new StringReader(xml)));
        } catch (ParserConfigurationException pce) {
            throw new ParseException("XML parser incorrectly configured", pce);
        } catch (SAXException saxe) {
            throw new ParseException(String.format("Invalid XML  %s", xml), saxe);
        } catch (IOException ioe) {
            throw new ParseException(String.format("unable to read XML %s", xml), ioe);
        }
    }
}
