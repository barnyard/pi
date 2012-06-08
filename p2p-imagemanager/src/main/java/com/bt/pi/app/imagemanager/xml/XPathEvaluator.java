/* (c) British Telecommunications plc, 2010, All Rights Reserved */
package com.bt.pi.app.imagemanager.xml;

import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;

import javax.xml.transform.OutputKeys;
import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.xml.dtm.ref.DTMNodeList;
import org.springframework.stereotype.Component;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

@Component
public class XPathEvaluator {
    private static final String COULD_NOT_EVALUATE_XPATH_EXPRESSION_MESSAGE = "Could not evaluate %s in xml document";
    private static final Log LOG = LogFactory.getLog(XPathEvaluator.class);

    public XPathEvaluator() {
    }

    public String getValue(String name, Document docRoot) {
        LOG.debug(String.format("getValue(%s, %s)", name, docRoot));
        try {
            XPath xpath = XPathFactory.newInstance().newXPath();
            return (String) xpath.evaluate(name, docRoot, XPathConstants.STRING);
        } catch (XPathExpressionException xpee) {
            throw new ParseException(String.format(COULD_NOT_EVALUATE_XPATH_EXPRESSION_MESSAGE, name), xpee);
        }
    }

    public List<String> getValues(String name, Document docRoot) {
        LOG.debug(String.format("getValues(%s, %s)", name, docRoot));
        try {
            XPath xpath = XPathFactory.newInstance().newXPath();
            DTMNodeList nodes = (DTMNodeList) xpath.evaluate(name, docRoot, XPathConstants.NODESET);
            List<String> values = new ArrayList<String>();
            for (int i = 0; i < nodes.getLength(); ++i) {
                values.add(nodes.item(i).getFirstChild().getNodeValue());
            }
            return values;
        } catch (XPathExpressionException xpee) {
            throw new ParseException(String.format(COULD_NOT_EVALUATE_XPATH_EXPRESSION_MESSAGE, name), xpee);
        }
    }

    public String getXMLFragment(String name, Document docRoot) {
        LOG.debug(String.format("getXMLFragment(%s, %s)", name, docRoot));
        try {
            XPath xpath = XPathFactory.newInstance().newXPath();
            Node node = (Node) xpath.evaluate(name, docRoot, XPathConstants.NODE);
            Source source = new DOMSource(node);
            StringWriter writer = new StringWriter();
            Result result = new StreamResult(writer);

            Transformer transformer = TransformerFactory.newInstance().newTransformer();
            transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
            transformer.transform(source, result);

            return writer.toString().replaceAll("\"", "'");
        } catch (XPathExpressionException xpee) {
            throw new ParseException(String.format(COULD_NOT_EVALUATE_XPATH_EXPRESSION_MESSAGE, name), xpee);
        } catch (TransformerException te) {
            throw new ParseException(String.format("Could not convert node %s to String", name), te);
        }
    }
}
