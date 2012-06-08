package com.bt.pi.api.utils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.ByteArrayInputStream;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.apache.xmlbeans.XmlObject;
import org.junit.Before;
import org.junit.Test;
import org.w3c.dom.Document;

public class XmlUtilsTest {
    private static final String SOAP_NS = "http://schemas.xmlsoap.org/soap/envelope/";
    private static final String XML_PREFIX = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>";
    private static final String ENV = XML_PREFIX + "<soap:Envelope xmlns:soap=\"" + SOAP_NS + "\">" + "<soap:Body/>" + "</soap:Envelope>";
    private XmlUtils xmlUtils;

    @Before
    public void before() {
        this.xmlUtils = new XmlUtils();
    }

    @Test
    public void testXmlUtils() {
        assertNotNull(this.xmlUtils);
    }

    @Test
    public void testXmlToString() throws Exception {
        // setup
        String xml = ENV;
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);

        DocumentBuilder builder = factory.newDocumentBuilder();
        Document node = builder.parse(new ByteArrayInputStream(xml.getBytes()));

        // act
        String result = this.xmlUtils.xmlToString(node);

        // assert
        assertEquals(xml, result);
    }

    @Test
    public void testParseInputSoapRequest() throws Exception {
        // setup
        String xml = ENV;

        // act
        XmlObject result = this.xmlUtils.parseInputString(xml);

        // assert
        assertEquals(xml, XML_PREFIX + result.xmlText());
    }
}
