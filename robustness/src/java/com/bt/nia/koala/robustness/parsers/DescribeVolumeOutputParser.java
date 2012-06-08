package com.bt.nia.koala.robustness.parsers;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.bt.nia.koala.robustness.commands.UnexpectedOutputException;

public class DescribeVolumeOutputParser extends OutputParserBase {
	private static final String ATTACHMENT_LINE_REGEX = "^ATTACHMENT\\s+\\S+\\s+\\S+\\s+(\\S+)\\s+[\\Qattacheding\\E]*?\\s+\\S+\\s*$";
	private static String VOLUME_LINE_REGEX = "^VOLUME\\s+\\S+\\s+\\S+\\s+(\\S+)\\s+\\S+\\s*$";

	public DescribeVolumeOutputParser(String availabilityZone) {
		this(availabilityZone, false);
	}

	public DescribeVolumeOutputParser(String availabilityZone, boolean isFromSnapshot) {
		super();
		if (isFromSnapshot)
			VOLUME_LINE_REGEX = String.format("^VOLUME\\s+\\S+\\s+\\S+\\s+\\S+\\s+[\\Q%s\\E]*?\\s+(\\S+)\\s+\\S+\\s*$", availabilityZone);
		else
			VOLUME_LINE_REGEX = String.format("^VOLUME\\s+\\S+\\s+\\S+\\s+[\\Q%s\\E]*?\\s+(\\S+)\\s+\\S+\\s*$", availabilityZone);
	}

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
			Pattern p = Pattern.compile(VOLUME_LINE_REGEX);
			Matcher m = p.matcher(line);

			String status = null;
			while (m.find()) {
				status = m.group(1);
			}

			if (status == null)
				throw new UnexpectedOutputException(String.format("Unexpected vol status line: %s", line));

			return status;
		} else if (lineNumber == 1) {
			Pattern p = Pattern.compile(ATTACHMENT_LINE_REGEX);
			Matcher m = p.matcher(line);

			String dev = null;
			while (m.find()) {
				dev = m.group(1);
			}

			if (dev == null)
				throw new UnexpectedOutputException(String.format("Unexpected desc vol attachment status line: %s", line));

			return dev;
		} else
			throw new UnexpectedOutputException("Expected 1 line");
	}
}
