package com.bt.nia.koala.robustness.parsers;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class DescribeInstanceIpAddressParserTest extends DescribeInstanceOutputParserTest {
	@Test
	public void shouldDetectIpAddress() {
		// setup
		describeInstanceOutputParser = new DescribeInstancePublicIpAddressParser();
		outputLines.add(reservationOutput);
		outputLines.add(instanceOutput);

		// act
		String[] res = describeInstanceOutputParser.parse(outputLines);

		// assert
		assertEquals(1, res.length);
		assertEquals("1.2.3.4", res[0]);
	}
}
