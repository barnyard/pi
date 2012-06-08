package com.bt.nia.koala.robustness.parsers;

import java.util.regex.Matcher;

public class DescribeInstancePrivateIpAddressParser extends DescribeInstanceOutputParser {
	@Override
	protected String getValue(Matcher m) {
		return m.group(2);
	}
}
