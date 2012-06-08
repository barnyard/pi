package com.bt.nia.koala.robustness.parsers;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.bt.nia.koala.robustness.commands.UnexpectedOutputException;

public class DescribeSnapshotOutputParser extends OutputParserBase {
	private static String SNAPSHOT_LINE_REGEX = "^SNAPSHOT\\s+\\S+\\s+\\S+\\s+(\\S+)\\s+\\S+\\s\\S*$";

	@Override
	protected int getMinNumLines() {
		return 1;
	}

	@Override
	protected int getMaxNumLines() {
		return 2;
	}

	@Override
	protected String parseLine(int lineNumber, String line) {
		if (lineNumber == 0) {
			Pattern p = Pattern.compile(SNAPSHOT_LINE_REGEX);
			Matcher m = p.matcher(line);

			String status = null;
			while (m.find()) {
				status = m.group(1);
			}

			if (status == null)
				throw new UnexpectedOutputException(String.format("Unexpected snapshot status line: %s", line));

			return status;
		} else
			throw new UnexpectedOutputException("Expected 1 line");
	}
}
