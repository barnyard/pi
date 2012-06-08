package com.bt.nia.koala.robustness.parsers;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.bt.nia.koala.robustness.commands.UnexpectedOutputException;

public class AddDeleteGroupOutputParser extends OutputParserBase {
	private static final String GROUP_LINE_REGEX = "^GROUP\\s+(\\S+)\\s*.*$";

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
			Pattern p = Pattern.compile(GROUP_LINE_REGEX);
			Matcher m = p.matcher(line);

			String group = null;
			while (m.find()) {
				group = m.group(1).trim();
			}

			if (group == null)
				throw new UnexpectedOutputException(String.format("Unexpected add/delete group line: %s", line));

			return group;
		} else
			throw new UnexpectedOutputException("Expected 1 line");
	}
}
