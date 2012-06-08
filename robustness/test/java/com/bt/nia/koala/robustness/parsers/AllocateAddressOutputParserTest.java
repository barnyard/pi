package com.bt.nia.koala.robustness.parsers;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.List;

import org.junit.Before;
import org.junit.Test;

import com.bt.nia.koala.robustness.commands.UnexpectedOutputException;

public class AllocateAddressOutputParserTest {
	private AllocateAddressOutputParser allocateAddressOutputParser;
	private List<String> outputLines;
	private String allocateAddressOutput;

	@Before
	public void before() {
		allocateAddressOutputParser = new AllocateAddressOutputParser();

		outputLines = new ArrayList<String>();

		allocateAddressOutput = "ADDRESS   	205.248.174.131 ";
	}

	@Test(expected = UnexpectedOutputException.class)
	public void shouldFailIfNotOneLine() {
		// act
		allocateAddressOutputParser.parse(outputLines);
	}

	@Test(expected = UnexpectedOutputException.class)
	public void shouldFailIfGarbage() {
		// setup
		outputLines.add("jwe w0-fjsd s0j");

		// act
		allocateAddressOutputParser.parse(outputLines);
	}

	@Test
	public void shouldDetectPendingAndReturnInstanceId() {
		// setup
		outputLines.add(allocateAddressOutput);

		// act
		String[] res = allocateAddressOutputParser.parse(outputLines);

		// assert
		assertEquals(1, res.length);
		assertEquals("205.248.174.131", res[0]);
	}

	@Test
	public void shouldDetectAddress() {
		// setup
		outputLines.add("ADDRESS 10.19.8.23");

		// act
		String[] res = allocateAddressOutputParser.parse(outputLines);

		// assert
		assertEquals(1, res.length);
		assertEquals("10.19.8.23", res[0]);
	}
}
