package com.bt.nia.koala.robustness.parsers;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.bt.nia.koala.robustness.commands.UnexpectedOutputException;

public class DeleteSnapshotOutputParser extends OutputParserBase {
	private static final String DELETE_SNAPSHOT_LINE_REGEX = "^SNAPSHOT\\s+(\\S+)\\s*$";

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
			Pattern p = Pattern.compile(DELETE_SNAPSHOT_LINE_REGEX);
			Matcher m = p.matcher(line);

			String snapshotId = null;
			while (m.find()) {
				snapshotId = m.group(1);
			}

			if (snapshotId == null)
				throw new UnexpectedOutputException(String.format("Unexpected delete snapshot line: %s", line));

			return snapshotId;
		} else
			throw new UnexpectedOutputException("Expected 1 line");
	}
}