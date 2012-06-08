package com.bt.nia.koala.robustness.parsers;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.List;

import org.junit.Before;
import org.junit.Test;

import com.bt.nia.koala.robustness.commands.UnexpectedOutputException;


public class AttachVolumeOutputParserTest {
	private AttachVolumeOutputParser attachVolumeOutputParser;
	private List<String> outputLines;
	private String attachVolumeOutput;
	
	@Before
	public void before() {
		attachVolumeOutputParser = new AttachVolumeOutputParser();
		
		outputLines = new ArrayList<String>();
		
		attachVolumeOutput = "ATTACHMENT      vol-2DCF0465    i-4D930980      /dev/sdb        attaching       2009-05-15T16:24:51+0000";		
	}
	
	@Test(expected=UnexpectedOutputException.class)
	public void shouldFailIfNotOneLine() {
		// act
		attachVolumeOutputParser.parse(outputLines);
	}
	
	@Test(expected=UnexpectedOutputException.class)
	public void shouldFailIfGarbage() {
		// setup
		outputLines.add("jwe w0-fjsd s0j");
		
		// act
		attachVolumeOutputParser.parse(outputLines);
	}
	
	@Test
	public void shouldDetectPendingAndReturnInstanceId() {
		// setup
		outputLines.add(attachVolumeOutput);
		
		// act
		String[] res = attachVolumeOutputParser.parse(outputLines);
		
		// assert
		assertEquals(1, res.length);
		assertEquals("vol-2DCF0465", res[0]);
	}
}
