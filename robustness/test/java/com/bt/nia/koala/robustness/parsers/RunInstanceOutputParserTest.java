package com.bt.nia.koala.robustness.parsers;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.List;

import org.junit.Before;
import org.junit.Test;

import com.bt.nia.koala.robustness.commands.UnexpectedOutputException;

public class RunInstanceOutputParserTest {
	private RunInstanceOutputParser runInstanceOutputParser;
	private List<String> outputLines;
	private String reservationOutput;
	private String pendingOutput;
	private String piReservationOutput;
	private String piPendingOutput;

	@Before
	public void before() {
		runInstanceOutputParser = new RunInstanceOutputParser();

		outputLines = new ArrayList<String>();

		reservationOutput = "RESERVATION     r-3C980796      rapajiu rapajiu-default";
		pendingOutput = "INSTANCE        i-53B30A0C      emi-F63E1186    0.0.0.0 0.0.0.0 pending u-keypair       0               m1.small       2009-02-02T17:51:46+0000         eki-925A138B";

		piReservationOutput = "RESERVATION     r-707AD3FE      david6  default";
		piPendingOutput = "INSTANCE        i-60C5FF8D      pmi-DDDDDDDD	 				pending         0               m1.small        2010-03-22T11:01:01+0000";
	}

	@Test(expected = UnexpectedOutputException.class)
	public void shouldFailIfIncorrectOrder() {
		// setup
		outputLines.add(pendingOutput);
		outputLines.add(reservationOutput);

		// act
		runInstanceOutputParser.parse(outputLines);
	}

	@Test(expected = UnexpectedOutputException.class)
	public void shouldFailIfNotTwoLines() {
		// setup
		outputLines.add(reservationOutput);

		// act
		runInstanceOutputParser.parse(outputLines);
	}

	@Test(expected = UnexpectedOutputException.class)
	public void shouldFailIfGarbage() {
		// setup
		outputLines.add("jwe w0-fjsd s0j");

		// act
		runInstanceOutputParser.parse(outputLines);
	}

	@Test
	public void shouldDetectPendingAndReturnInstanceId() {
		// setup
		outputLines.add(reservationOutput);
		outputLines.add(pendingOutput);

		// act
		String[] res = runInstanceOutputParser.parse(outputLines);

		// assert
		assertEquals(1, res.length);
		assertEquals("i-53B30A0C", res[0]);
	}

	@Test
	public void shouldDetectPendingAndReturnInstanceIdForPiOutput() {
		// setup
		outputLines.add(piReservationOutput);
		outputLines.add(piPendingOutput);

		// act
		String[] res = runInstanceOutputParser.parse(outputLines);

		// assert
		assertEquals(1, res.length);
		assertEquals("i-60C5FF8D", res[0]);
	}
}
