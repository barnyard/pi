/* (c) British Telecommunications plc, 2009, All Rights Reserved */
package com.bt.pi.api.utils;

import java.io.StringWriter;

import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.xmlbeans.XmlException;
import org.apache.xmlbeans.XmlObject;
import org.springframework.stereotype.Component;
import org.w3c.dom.Node;

/**
 * some utilities for converting xml object types
 */
@Component
public class XmlUtils {
    private static final Log LOG = LogFactory.getLog(XmlUtils.class);
    private static final String FORMAT_ERROR_MESSAGE = "error formatting xml string";
    private static final String PARSE_ERROR_MESSAGE = "error parsing xml string";

    public XmlUtils() {
    }

    public String xmlToString(Node node) {
        try {
            Source source = new DOMSource(node);
            StringWriter stringWriter = new StringWriter();
            Result result = new StreamResult(stringWriter);
            TransformerFactory factory = TransformerFactory.newInstance();
            Transformer transformer = factory.newTransformer();
            transformer.transform(source, result);
            return stringWriter.getBuffer().toString();
        } catch (TransformerConfigurationException e) {
            LOG.error(FORMAT_ERROR_MESSAGE, e);
            throw new XmlFormattingException(FORMAT_ERROR_MESSAGE, e);
        } catch (TransformerException e) {
            LOG.error(FORMAT_ERROR_MESSAGE, e);
            throw new XmlFormattingException(FORMAT_ERROR_MESSAGE, e);
        }
    }

    public XmlObject parseInputString(String string) {
        LOG.debug(String.format("parseInputString(%s)", string));
        try {
            return XmlObject.Factory.parse(string);
        } catch (XmlException e) {
            LOG.warn(PARSE_ERROR_MESSAGE, e);
            throw new XmlFormattingException(PARSE_ERROR_MESSAGE, e);
        }
    }
}
