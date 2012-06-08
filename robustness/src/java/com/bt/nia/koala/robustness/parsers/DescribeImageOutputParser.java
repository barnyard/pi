package com.bt.nia.koala.robustness.parsers;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.bt.nia.koala.robustness.commands.UnexpectedOutputException;

public class DescribeImageOutputParser extends OutputParserBase {
	// String IMAGE_LINE_REGEX =
	// "^IMAGE\\s+pmi-\\w+\\s+.+/ttylinux.img.manifest.xml\\s+\\S+\\s+(\\w+)\\s+private\\s+x86_64\\s+MACHINE\\s+(pki-\\w+)?\\s+linux$";

	String IMAGE_LINE_REGEX = "^IMAGE\\s+p[mkr]i-.*(AVAILABLE|PENDING|FAILED)";

	protected Pattern p = Pattern.compile(IMAGE_LINE_REGEX);;

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
		Matcher m = p.matcher(line);
		String status = null;
		while (m.find()) {
			status = getValue(m);
		}

		if (status == null)
			throw new UnexpectedOutputException(String.format("Unexpected status line: %s", line));

		return status;
	}

	protected String getValue(Matcher m) {
		return m.group(1);
	}
}
