package com.bt.nia.koala.robustness.parsers;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.bt.nia.koala.robustness.commands.UnexpectedOutputException;

public class TerminateInstanceOutputParser extends OutputParserBase {
	private static final String TERMINATING_LINE_REGEX = "^INSTANCE\\s+(i-\\w+)\\s+(running|pending)\\s(shutting-down|terminated)$";

	@Override
	protected int getMinNumLines() {
		return 1;
	}

	@Override
	protected int getMaxNumLines() {
		return 1;
	}

	@Override
	protected String parseLine(int lineNumber, String line) {
		if (lineNumber == 0) {
			Pattern p = Pattern.compile(TERMINATING_LINE_REGEX);
			Matcher m = p.matcher(line);

			String instanceId = null;
			while (m.find()) {
				instanceId = m.group(1);
			}

			if (instanceId == null)
				throw new UnexpectedOutputException(String.format("Unexpected terminating line: %s", line));

			return instanceId;
		} else
			throw new UnexpectedOutputException("Expected 1 line");
	}
}
