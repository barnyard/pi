/* (c) British Telecommunications plc, 2009, All Rights Reserved */
package com.bt.pi.api.utils;

import java.io.ByteArrayInputStream;
import java.io.IOException;

import javax.annotation.Resource;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.stereotype.Component;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

/**
 * some utilities for dealing with SOAP messages
 */
@Component
public class SoapEnvelopeUtils {
	private static final String ERROR_PARSING_XML = "Error parsing XML";
	private static final Log LOG = LogFactory.getLog(SoapEnvelopeUtils.class);
	private static final String BODY = "Body";
	private static final String SOAP_NS = "http://schemas.xmlsoap.org/soap/envelope/";
	private static final String REMOVE_ENVELOPE_ERROR_MESSAGE = "error stripping SOAP envelope from xml";
	private XmlUtils xmlUtils;
	private DocumentBuilderFactory factory;
	private DocumentBuilder builder;

	public SoapEnvelopeUtils() {
		this.xmlUtils = null;
		factory = DocumentBuilderFactory.newInstance();
		factory.setNamespaceAware(true);
	}

	@Resource
	public void setXmlUtils(XmlUtils aXmlUtils) {
		this.xmlUtils = aXmlUtils;
	}

	public byte[] removeEnvelope(byte[] bytes) {
		LOG.debug(String.format("removeEnvelope(%s)", new String(bytes)));
		try {
			Element envelopeElement = readBytes(bytes);
			Node body = envelopeElement.getElementsByTagNameNS(SOAP_NS, BODY).item(0);
			NodeList childNodes = body.getChildNodes();
			for (int i = 0; i < childNodes.getLength(); i++){
				Node item = childNodes.item(i);
				if (item.getNodeType() == Node.ELEMENT_NODE){
					return this.xmlUtils.xmlToString(item).getBytes();
				}
			}
			return new byte[0];
		} catch (XmlFormattingException e) {
			LOG.error(REMOVE_ENVELOPE_ERROR_MESSAGE, e);
			throw new XmlFormattingException(REMOVE_ENVELOPE_ERROR_MESSAGE, e);
		}
	}

	public boolean isSoapFault(byte[] bytes) {
		try {
			Element rootElement = readBytes(bytes);
			if (! "Fault".equals(rootElement.getLocalName()))
				return false;
			return SOAP_NS.equals(rootElement.getNamespaceURI());
		} catch (XmlFormattingException e) {
			LOG.warn(ERROR_PARSING_XML, e);
			return false;
		}
	}

	public String getFaultString(byte[] xml) {
		return getFaultElement(xml, "faultstring");
	}

	public String getFaultCode(byte[] xml) {
		return getFaultElement(xml, "faultcode");
	}
	
	private String getFaultElement(byte[] xml, String name) {
		if (! isSoapFault(xml))
			return null;
		try {
			Element faultElement = readBytes(xml);
			NodeList elementsByTagName = faultElement.getElementsByTagName(name);
			if (elementsByTagName.getLength() < 1)
				return null;
			Element item = (Element) elementsByTagName.item(0);
			return item.getTextContent();
		} catch (XmlFormattingException e) {
			LOG.warn(ERROR_PARSING_XML, e);
			return null;
		}
	}
	
	private Element readBytes(byte[] xml) {
		try {
			builder = factory.newDocumentBuilder();
			Document document = builder.parse(new ByteArrayInputStream(xml));
			return document.getDocumentElement();
		} catch (ParserConfigurationException e) {
			throw new XmlFormattingException(ERROR_PARSING_XML, e);
		} catch (SAXException e) {
			throw new XmlFormattingException(ERROR_PARSING_XML, e);
		} catch (IOException e) {
			throw new XmlFormattingException(ERROR_PARSING_XML, e);
		}
	}
}
