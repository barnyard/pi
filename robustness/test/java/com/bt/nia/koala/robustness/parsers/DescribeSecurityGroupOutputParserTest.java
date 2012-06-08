package com.bt.nia.koala.robustness.parsers;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.List;

import org.junit.Before;
import org.junit.Test;

public class DescribeSecurityGroupOutputParserTest {
	protected DescribeSecurityGroupOutputParser describeSecurityGroupOutputParser;
	protected List<String> outputLines;
	protected String groupNameOutput;
	protected String networkRuleOutput;

	@Before
	public void before() {
		describeSecurityGroupOutputParser = new DescribeSecurityGroupOutputParser();

		outputLines = new ArrayList<String>();

		groupNameOutput = "GROUP	nauman	testGroup	test";
		networkRuleOutput = "PERMISSION	nauman	testGroup	ALLOWS	TCP	22	22	FROM	CIDR	0.0.0.0/0";
	}

	@Test
	public void shouldFailIfIncorrectOrder() {
		// setup
		outputLines.add(groupNameOutput);

		// act
		String[] res = describeSecurityGroupOutputParser.parse(outputLines);

		// assert
		assertEquals(0, res.length);
	}

	@Test
	public void shouldGetSecurityGroupFromNetworkRule() {
		// setup
		outputLines.add(groupNameOutput);
		outputLines.add(networkRuleOutput);

		// act
		String[] res = describeSecurityGroupOutputParser.parse(outputLines);

		// assert
		assertEquals(1, res.length);
		assertEquals("testGroup", res[0]);
	}
}
