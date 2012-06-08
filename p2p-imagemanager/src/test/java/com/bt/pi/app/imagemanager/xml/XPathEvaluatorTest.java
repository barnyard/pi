package com.bt.pi.app.imagemanager.xml;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.net.URL;

import org.junit.Before;
import org.junit.Test;
import org.w3c.dom.Document;


public class XPathEvaluatorTest {

	private static final String INVALID_XPATH_EXPRESSION = "???";
	private XPathEvaluator xpathEvaluator;

	@Before
	public void setUp() {
		xpathEvaluator = new XPathEvaluator();
	}

	@Test
	public void getValueShouldReturnTheStringValueOfEvaluatingValidXPathExpressionAgainstDocument() {
		assertEquals("2007-10-10", xpathEvaluator.getValue("/manifest/version", getDocument()));
	}

	@Test(expected = ParseException.class)
	public void getValueShouldThrowAParseExceptionWhenGivenAnXPathExpressionThatCannotBeEvaluated() {
		xpathEvaluator.getValue(INVALID_XPATH_EXPRESSION, getDocument());
	}

	@Test
	public void getValuesShouldReturnTheValuesFromEvaluatingValidXPathExpressionAgainstDocument() {
		assertTrue(xpathEvaluator.getValues("/manifest/image/parts/part", getDocument()).size() == 2);
	}
	
	@Test(expected = ParseException.class)
	public void getValuesShouldThrowAParseExceptionWhenGivenAnXPathExpressionThatCannotBeEvaluated() {
		xpathEvaluator.getValues(INVALID_XPATH_EXPRESSION, getDocument());
	}

	@Test
	public void getXMLFragmentShouldReturnTheXmlStringFromEvaluatingValidXPathExpression() {
		assertEquals("<machine_configuration><architecture>i386</architecture></machine_configuration>", xpathEvaluator.getXMLFragment("//machine_configuration", getDocument()));
	}

	@Test(expected = ParseException.class)
	public void getXMLFramentShouldThrowParseExceptionWhenGivenAnXPathExpressionThatCannotBeEvaluated() {
		xpathEvaluator.getXMLFragment(INVALID_XPATH_EXPRESSION, getDocument());
	}

	private Document getDocument() {
		URL fileUrl = Thread.currentThread().getContextClassLoader().getResource("valid.xml");
		File file = new File(fileUrl.getPath());
		XMLParser parser = new XMLParser();
		return parser.parse(file);
	}

}
