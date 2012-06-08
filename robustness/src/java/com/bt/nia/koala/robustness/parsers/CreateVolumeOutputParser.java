package com.bt.nia.koala.robustness.parsers;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.bt.nia.koala.robustness.commands.UnexpectedOutputException;

public class CreateVolumeOutputParser extends OutputParserBase {
	private static String CREATE_VOL_LINE_REGEX = "^VOLUME\\s+(\\S+)\\s+\\S+\\s+creating\\s+\\S+\\s*$";

	public CreateVolumeOutputParser(String availabilityZone) {
		this(availabilityZone, false);
	}

	public CreateVolumeOutputParser(String availabilityZone, boolean isFromSnapshot) {
		super();
		if (isFromSnapshot)
			CREATE_VOL_LINE_REGEX = String.format("^VOLUME\\s+(\\S+)\\s+\\S+\\s+\\S+\\s+[\\Q%s\\E]*?\\s+creating\\s+\\S+\\s*$", availabilityZone);
		else
			CREATE_VOL_LINE_REGEX = String.format("^VOLUME\\s+(\\S+)\\s+\\S+\\s+[\\Q%s\\E]*?\\s+creating\\s+\\S+\\s*$", availabilityZone);
	}

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
			Pattern p = Pattern.compile(CREATE_VOL_LINE_REGEX);
			Matcher m = p.matcher(line);

			String address = null;
			while (m.find()) {
				address = m.group(1);
			}

			if (address == null)
				throw new UnexpectedOutputException(String.format("Unexpected create vol line: %s, does not match regex %s", line, CREATE_VOL_LINE_REGEX));

			return address;
		} else
			throw new UnexpectedOutputException("Expected 1 line");
	}
}