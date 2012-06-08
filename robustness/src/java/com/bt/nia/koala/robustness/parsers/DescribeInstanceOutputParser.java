package com.bt.nia.koala.robustness.parsers;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.bt.nia.koala.robustness.commands.UnexpectedOutputException;

public class DescribeInstanceOutputParser extends OutputParserBase {
	private static final String RESERVATION_LINE_REGEX = "^RESERVATION\\s+r-\\w+\\s+\\S+\\s+\\S+$";
	private static final String INSTANCE_LINE_REGEX = "^INSTANCE\\s+i-\\w+\\s+[ep]mi-\\S+\\s+(\\S+)\\s+(\\b\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\b+)\\s+(\\w+)\\s+.*?$";
	private static final String INSTANCE_LINE_REGEX_WITHOUT_PUBLIC_IP = "^INSTANCE\\s+i-\\w+\\s+[ep]mi-\\S+\\s+(\\S+)\\s+(\\w+)\\s+.*?$";

	protected Pattern p = Pattern.compile(INSTANCE_LINE_REGEX);
	private Pattern patternWithoutPublicIp = Pattern.compile(INSTANCE_LINE_REGEX_WITHOUT_PUBLIC_IP);

	@Override
	protected int getMinNumLines() {
		return 2;
	}

	@Override
	protected int getMaxNumLines() {
		return 2;
	}

	@Override
	protected String parseLine(int lineNumber, String line) {
		if (lineNumber == 0) {
			boolean reservationLineMatches = line.matches(RESERVATION_LINE_REGEX);
			if (!reservationLineMatches)
				throw new UnexpectedOutputException(String.format("Unexpected reservation line: %s", line));
			return null;
		} else if (lineNumber == 1) {
			Matcher m = p.matcher(line);
			String status = null;
			while (m.find()) {
				status = getValue(m);
			}

			if (status == null && line.matches(INSTANCE_LINE_REGEX_WITHOUT_PUBLIC_IP)) {
				m = patternWithoutPublicIp.matcher(line);
				while (m.find())
					status = m.group(2);
			}

			if (status == null) {
				if (line.matches("^INSTANCE\\s+i-\\w+\\s+[ep]mi-\\S+\\s+\\s+([a-z]\\w+)\\s+.*?$"))
					throw new UnexpectedOutputException(String.format("No IP addresses were returned in %s", line));
				throw new UnexpectedOutputException(String.format("Unexpected status line: %s", line));
			}

			return status;
		} else
			throw new UnexpectedOutputException("Expected 2 lines");
	}

	protected String getValue(Matcher m) {
		return m.group(3);
	}
}
