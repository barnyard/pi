package com.bt.pi.api.utils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Before;
import org.junit.Test;

import com.bt.pi.api.utils.SoapEnvelopeUtils;
import com.bt.pi.api.utils.XmlUtils;

public class SoapEnvelopeUtilsTest {
	private static final String SOAP_NS = "http://schemas.xmlsoap.org/soap/envelope/";
	private static final String XML_PREFIX = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>";
	private SoapEnvelopeUtils soapEnvelopeUtils;
	private XmlUtils xmlUtils;
	private String payload = "<ns1:stuff xmlns:ns1=\"fred.com\"><a>a</a></ns1:stuff>";

	@Before
	public void before() {
		this.soapEnvelopeUtils = new SoapEnvelopeUtils();
		this.xmlUtils = new XmlUtils();
		this.soapEnvelopeUtils.setXmlUtils(this.xmlUtils);
	}

	@Test
	public void testRemoveEnvelope() throws Exception {
		// setup
		String request = "<soap:Envelope xmlns:soap=\"" + SOAP_NS + "\">" + "<soap:Body>" + payload + "</soap:Body>" + "</soap:Envelope>";

		// act
		byte[] result = this.soapEnvelopeUtils.removeEnvelope(request.getBytes());

		// assert
		assertEquals(XML_PREFIX + payload, new String(result));
	}

	@Test
	public void testRemoveEnvelopeFormatted() throws Exception {
		// setup
		String request = "<?xml version=\"1.0\" encoding=\"UTF-8\"?><soap:Envelope xmlns:soap=\"http://schemas.xmlsoap.org/soap/envelope/\"><soap:Body>" + "  \t  \n   " + payload + "\n"
				+ "</soap:Body>" + "</soap:Envelope>";

		// act
		byte[] result = this.soapEnvelopeUtils.removeEnvelope(request.getBytes());

		// assert
		assertEquals(XML_PREFIX + payload, new String(result));
	}

	@Test
	public void removeFromFault() {
		// setup
		String soap = "<SOAP-ENV:Envelope xmlns:SOAP-ENV=\"http://schemas.xmlsoap.org/soap/envelope/\" xmlns:aws=\"http://webservices.amazon.com/AWSFault/2005-15-09\">" + "<SOAP-ENV:Body>"
				+ "<SOAP-ENV:Fault>" + "<faultcode>aws:Client.IncompatibleTokens</faultcode>"
				+ "<faultstring>The transaction could not be completed because the tokens have incompatible payment instructions: Assertion Failed for Recipient</faultstring>" + "<detail>"
				+ "<aws:RequestId xmlns:aws=\"http://webservices.amazon.com/AWSFault/2005-15-09\">ad56d51c-b1df-4b15-95ca-9f71c2c65eea</aws:RequestId>" + "</detail>" + "</SOAP-ENV:Fault>"
				+ "</SOAP-ENV:Body>" + "</SOAP-ENV:Envelope>";
		byte[] fault = soap.getBytes();

		// act
		byte[] result = this.soapEnvelopeUtils.removeEnvelope(fault);

		assertEquals(
				XML_PREFIX
						+ "<SOAP-ENV:Fault xmlns:SOAP-ENV=\"http://schemas.xmlsoap.org/soap/envelope/\"><faultcode>aws:Client.IncompatibleTokens</faultcode><faultstring>The transaction could not be completed because the tokens have incompatible payment instructions: Assertion Failed for Recipient</faultstring><detail><aws:RequestId xmlns:aws=\"http://webservices.amazon.com/AWSFault/2005-15-09\">ad56d51c-b1df-4b15-95ca-9f71c2c65eea</aws:RequestId></detail></SOAP-ENV:Fault>",
				new String(result));
	}

	@Test
	public void testIsSoapFaultTrue() {
		// setup
		byte[] xml = "<SOAP-ENV:Fault xmlns:SOAP-ENV=\"http://schemas.xmlsoap.org/soap/envelope/\"><faultcode>aws:Client.IncompatibleTokens</faultcode><faultstring>The transaction could not be completed because the tokens have incompatible payment instructions: Assertion Failed for Recipient</faultstring><detail><aws:RequestId xmlns:aws=\"http://webservices.amazon.com/AWSFault/2005-15-09\">ad56d51c-b1df-4b15-95ca-9f71c2c65eea</aws:RequestId></detail></SOAP-ENV:Fault>"
				.getBytes();

		// act
		boolean result = this.soapEnvelopeUtils.isSoapFault(xml);

		// assert
		assertTrue(result);
	}

	@Test
	public void testIsSoapFaultWrongNamespace() {
		// setup
		byte[] xml = "<SOAP-ENV:Fault xmlns:SOAP-ENV=\"http://bogus/\"></SOAP-ENV:Fault>".getBytes();

		// act
		boolean result = this.soapEnvelopeUtils.isSoapFault(xml);

		// assert
		assertFalse(result);
	}

	@Test
	public void testIsSoapFaultWrongName() {
		// setup
		byte[] xml = "<SOAP-ENV:Bogus xmlns:SOAP-ENV=\"http://schemas.xmlsoap.org/soap/envelope/\"></SOAP-ENV:Bogus>".getBytes();

		// act
		boolean result = this.soapEnvelopeUtils.isSoapFault(xml);

		// assert
		assertFalse(result);
	}

	@Test
	public void testIsSoapFaultBadXml() {
		// setup
		byte[] xml = "<Bogus>".getBytes();

		// act
		boolean result = this.soapEnvelopeUtils.isSoapFault(xml);

		// assert
		assertFalse(result);
	}
	
	@Test
	public void testGetFaultCode() {
		// setup
		String code = "code";
		String message = "message";
		byte[] xml = ("<SOAP-ENV:Fault xmlns:SOAP-ENV=\"http://schemas.xmlsoap.org/soap/envelope/\"><faultcode>" + code + "</faultcode><faultstring>" + message + "</faultstring></SOAP-ENV:Fault>")
				.getBytes();

		// act
		String result = this.soapEnvelopeUtils.getFaultCode(xml);

		// assert
		assertEquals(code, result);
	}
	
	@Test
	public void testGetFaultString() {
		// setup
		String code = "code";
		String message = "message";
		byte[] xml = ("<SOAP-ENV:Fault xmlns:SOAP-ENV=\"http://schemas.xmlsoap.org/soap/envelope/\"><faultcode>" + code + "</faultcode><faultstring>" + message + "</faultstring></SOAP-ENV:Fault>")
				.getBytes();

		// act
		String result = this.soapEnvelopeUtils.getFaultString(xml);

		// assert
		assertEquals(message, result);
	}
}
