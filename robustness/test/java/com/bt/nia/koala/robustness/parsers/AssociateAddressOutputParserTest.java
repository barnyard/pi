package com.bt.nia.koala.robustness.parsers;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.List;

import org.junit.Before;
import org.junit.Test;

import com.bt.nia.koala.robustness.commands.UnexpectedOutputException;
import com.bt.nia.koala.robustness.parsers.AssociateAddressOutputParser;


public class AssociateAddressOutputParserTest {
	private AssociateAddressOutputParser associateAddressOutputParser;
	private List<String> outputLines;
	private String associateAddressOutput;
	
	@Before
	public void before() {
		associateAddressOutputParser = new AssociateAddressOutputParser();
		
		outputLines = new ArrayList<String>();
		
		associateAddressOutput = "ADDRESS 205.248.174.131 i-4168092E ";		
	}
	
	@Test(expected=UnexpectedOutputException.class)
	public void shouldFailIfNotOneLine() {
		// act
		associateAddressOutputParser.parse(outputLines);
	}
	
	@Test(expected=UnexpectedOutputException.class)
	public void shouldFailIfGarbage() {
		// setup
		outputLines.add("jwe w0-fjsd s0j");
		
		// act
		associateAddressOutputParser.parse(outputLines);
	}
	
	@Test
	public void shouldDetectPendingAndReturnInstanceId() {
		// setup
		outputLines.add(associateAddressOutput);
		
		// act
		String[] res = associateAddressOutputParser.parse(outputLines);
		
		// assert
		assertEquals(1, res.length);
		assertEquals("205.248.174.131", res[0]);
	}
}
