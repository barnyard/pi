package com.bt.nia.koala.robustness.parsers;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.bt.nia.koala.robustness.commands.UnexpectedOutputException;

public class DescribeSecurityGroupOutputParser extends OutputParserBase {
	private static final String GROUP_NAME_REGEX = "^GROUP\\s(\\S*)\\s(\\S*)\\s(.*)$";
	private static final String NETWORK_RULE_REGEX = "^PERMISSION\\s\\S*\\s(\\S*).*$";

	protected Pattern p = Pattern.compile(NETWORK_RULE_REGEX);

	@Override
	protected int getMaxNumLines() {
		return 2;
	}

	@Override
	protected int getMinNumLines() {
		return 1;
	}

	@Override
	protected String parseLine(int lineNumber, String line) {
		if (lineNumber == 0) {
			// group name
			boolean groupNameRegex = line.matches(GROUP_NAME_REGEX);
			if (!groupNameRegex)
				throw new UnexpectedOutputException(String.format("Unexpected reservation line: %s", line));
			return null;
		} else if (lineNumber == 1) {
			// network rule
			Matcher m = p.matcher(line);
			String securityGroupName = null;
			while (m.find()) {
				securityGroupName = getValue(m);
			}

			return securityGroupName;
		} else
			return null;

	}

	protected String getValue(Matcher m) {
		return m.group(1);
	}

}
