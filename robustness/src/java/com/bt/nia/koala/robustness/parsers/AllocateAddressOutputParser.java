package com.bt.nia.koala.robustness.parsers;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.bt.nia.koala.robustness.commands.UnexpectedOutputException;

public class AllocateAddressOutputParser extends OutputParserBase {
	private static final String ALLOCATE_LINE_REGEX = "^ADDRESS\\s+(\\S+)\\s*$";
	
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
			Pattern p = Pattern.compile(ALLOCATE_LINE_REGEX);
			Matcher m = p.matcher(line);
			
			String address = null;
			while (m.find()) {
				address = m.group(1);
			}
			
			if (address == null)
				throw new UnexpectedOutputException(String.format("Unexpected allocate line: %s", line));
			
			return address;
		} else 
			throw new UnexpectedOutputException("Expected 1 line");
	}
}
