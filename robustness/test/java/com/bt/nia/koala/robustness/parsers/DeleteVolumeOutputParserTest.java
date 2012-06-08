package com.bt.nia.koala.robustness.parsers;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.List;

import org.junit.Before;
import org.junit.Test;

import com.bt.nia.koala.robustness.commands.UnexpectedOutputException;


public class DeleteVolumeOutputParserTest {
	private DeleteVolumeOutputParser deleteVolumeOutputParser;
	private List<String> outputLines;
	private String deleteVolumeOutput;
	
	@Before
	public void before() {
		deleteVolumeOutputParser = new DeleteVolumeOutputParser();
		
		outputLines = new ArrayList<String>();
		
		deleteVolumeOutput = "VOLUME  vol-2DCF0465";		
	}
	
	@Test(expected=UnexpectedOutputException.class)
	public void shouldFailIfNotOneLine() {
		// act
		deleteVolumeOutputParser.parse(outputLines);
	}
	
	@Test(expected=UnexpectedOutputException.class)
	public void shouldFailIfGarbage() {
		// setup
		outputLines.add("jwe w0-fjsd s0j");
		
		// act
		deleteVolumeOutputParser.parse(outputLines);
	}
	
	@Test
	public void shouldDetectPendingAndReturnInstanceId() {
		// setup
		outputLines.add(deleteVolumeOutput);
		
		// act
		String[] res = deleteVolumeOutputParser.parse(outputLines);
		
		// assert
		assertEquals(1, res.length);
		assertEquals("vol-2DCF0465", res[0]);
	}
}
