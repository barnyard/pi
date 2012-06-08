package com.bt.nia.koala.robustness.parsers;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.List;

import org.junit.Before;
import org.junit.Test;

import com.bt.nia.koala.robustness.commands.UnexpectedOutputException;
import com.bt.nia.koala.robustness.parsers.TerminateInstanceOutputParser;


public class TerminateInstanceOutputParserTest {
	private TerminateInstanceOutputParser terminateInstanceOutputParser;
	private List<String> outputLines;
	private String terminatingOutput;
	
	@Before
	public void before() {
		terminateInstanceOutputParser = new TerminateInstanceOutputParser();
		
		outputLines = new ArrayList<String>();
		
		terminatingOutput = "INSTANCE        i-46E60833      running shutting-down";
	}
	
	
	@Test(expected=UnexpectedOutputException.class)
	public void shouldFailIfNotOneLine() {
		// setup
		outputLines.add(terminatingOutput);
		outputLines.add(terminatingOutput);
		
		// act
		terminateInstanceOutputParser.parse(outputLines);
	}
	
	@Test(expected=UnexpectedOutputException.class)
	public void shouldFailIfGarbage() {
		// setup
		outputLines.add("jwe w0-fjsd s0j");
		
		// act
		terminateInstanceOutputParser.parse(outputLines);
	}
	
	@Test
	public void shouldDetectPendingAndReturnInstanceId() {
		// setup
		outputLines.add(terminatingOutput);
		
		// act
		String[] res = terminateInstanceOutputParser.parse(outputLines);
		
		// assert
		assertEquals(1, res.length);
		assertEquals("i-46E60833", res[0]);
	}
}
