package com.bt.nia.koala.robustness.parsers;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.List;

import org.junit.Before;
import org.junit.Test;

import com.bt.nia.koala.robustness.commands.UnexpectedOutputException;

public class DescribeInstanceOutputParserTest {
	protected DescribeInstanceOutputParser describeInstanceOutputParser;
	protected List<String> outputLines;
	protected String reservationOutput;
	protected String instanceOutput;
	private String piReservationOutput;
	private String piInstanceOutput;

	@Before
	public void before() {
		describeInstanceOutputParser = new DescribeInstanceOutputParser();

		outputLines = new ArrayList<String>();

		reservationOutput = "RESERVATION     r-50940996      rapajiu default";
		instanceOutput = "INSTANCE        i-4BF108C2      emi-F63E1186    1.2.3.4 172.16.1.2      running u-keypair       0               m1.small2009-02-03T10:44:21+0000                eki-925A138B";

		piReservationOutput = "RESERVATION     r-5ADEE11D      robustness      default";
		piInstanceOutput = "INSTANCE        i-40700A5C      pmi-ADABAAAD    10.19.6.26      172.31.0.52     running         0               m1.small        2010-03-22T12:22:02+0000                pki-ADABAAAD    pri-ADABAAAD    linux";
	}

	@Test(expected = UnexpectedOutputException.class)
	public void shouldFailIfIncorrectOrder() {
		// setup
		outputLines.add(instanceOutput);
		outputLines.add(reservationOutput);

		// act
		describeInstanceOutputParser.parse(outputLines);
	}

	@Test(expected = UnexpectedOutputException.class)
	public void shouldFailIfNotTwoLines() {
		// setup
		outputLines.add(reservationOutput);

		// act
		describeInstanceOutputParser.parse(outputLines);
	}

	@Test(expected = UnexpectedOutputException.class)
	public void shouldFailIfGarbage() {
		// setup
		outputLines.add("jwe w0-fjsd s0j");

		// act
		describeInstanceOutputParser.parse(outputLines);
	}

	@Test
	public void shouldDetectPendingAndReturnInstanceId() {
		// setup
		outputLines.add(reservationOutput);
		outputLines.add(instanceOutput);

		// act
		String[] res = describeInstanceOutputParser.parse(outputLines);

		// assert
		assertEquals(1, res.length);
		assertEquals("running", res[0]);
	}

	@Test
	public void shouldDetectPendingAndReturnInstanceIdForPiOutput() {
		// setup
		outputLines.add(piReservationOutput);
		outputLines.add(piInstanceOutput);

		// act
		String[] res = describeInstanceOutputParser.parse(outputLines);

		// assert
		assertEquals(1, res.length);
		assertEquals("running", res[0]);
	}

	@Test
	public void shouldReturnStatusIfPublicIpMissing() throws Exception {
		// setup
		piInstanceOutput = "INSTANCE	i-000HKcIr	pmi-55c6b996		10.19.169.2	terminated		0		m1.small	2011-11-29T09:32:08+0000	cube1-1a	pki-7f611cbd		linux";
		outputLines.add(piReservationOutput);
		outputLines.add(piInstanceOutput);

		// act
		String[] res = describeInstanceOutputParser.parse(outputLines);

		// assert
		assertEquals("terminated", res[0]);
	}
}
