package com.bt.nia.koala.robustness.parsers;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.bt.nia.koala.robustness.commands.UnexpectedOutputException;

public class RunInstanceOutputParser extends OutputParserBase {
	private static final String RESERVATION_LINE_REGEX = "^RESERVATION\\s+r-\\w+\\s+\\S+\\s+\\S+";
	private static final String PENDING_LINE_REGEX = "^INSTANCE\\s+(i-\\w+)\\s+[ep]mi-\\S+\\s+.+\\s+pending\\s+.*?$";

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
			Pattern p = Pattern.compile(PENDING_LINE_REGEX);
			Matcher m = p.matcher(line);

			String instanceId = null;
			while (m.find()) {
				instanceId = m.group(1);
			}

			if (instanceId == null)
				throw new UnexpectedOutputException(String.format("Unexpected pending line: %s", line));

			return instanceId;
		} else
			throw new UnexpectedOutputException("Expected 2 lines");
	}
}
