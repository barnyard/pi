package com.bt.nia.koala.robustness.parsers;

import java.util.Iterator;
import java.util.List;

import com.bt.nia.koala.robustness.commands.UnexpectedOutputException;

public class KoalaPortalOutputParser extends OutputParserBase {
	private static final String KOALA_PORTAL_TITLE_REGEX = "^.*?<title>BT Cloud</title>.*?$";

	@Override
	protected int getMinNumLines() {
		throw new RuntimeException("Not used");
	}

	@Override
	protected int getMaxNumLines() {
		throw new RuntimeException("Not used");
	}

	@Override
	public String[] parse(List<String> lines) throws UnexpectedOutputException {
		Iterator<String> iter = lines.iterator();
		StringBuilder output = new StringBuilder();
		while (iter.hasNext()) {
			String line = iter.next();
			if (line.matches(KOALA_PORTAL_TITLE_REGEX))
				return null;
			else if (line.contains("HTML><HEAD><TITLE>Directory:"))
				throw new UnexpectedOutputException("Directory listing found on home page");
			else
				output.append(line + "\n");
		}

		throw new UnexpectedOutputException(String.format("No title in koala portal output:\n%s", output.toString()));
	}

	@Override
	protected String parseLine(int lineNumber, String line) {
		throw new RuntimeException("Not used");
	}
}
