package com.bt.nia.koala.robustness.parsers;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.bt.nia.koala.robustness.commands.UnexpectedOutputException;

public abstract class OutputParserBase implements OutputParser {
	public String[] parse(List<String> lines) throws UnexpectedOutputException {
		List<String> results = new ArrayList<String>();
		
		String[] linesStringArray = new String[lines.size()];
		linesStringArray = lines.toArray(linesStringArray);
		
		int minNumLines = getMinNumLines();
		int maxNumLines = getMaxNumLines();
		if (lines.size() < minNumLines || lines.size() > maxNumLines)
			throw new UnexpectedOutputException(String.format("%s expected between %d and %d output lines, but got %s", getClass().getSimpleName(), minNumLines, maxNumLines, Arrays.toString(linesStringArray)));
		
		for (int i = 0; i < lines.size(); i++) {
			String result = parseLine(i, lines.get(i));
			if (result != null)
				results.add(result);
		}
		
		String[] resultsArray = new String[results.size()];		
		return results.toArray(resultsArray);
	}

	protected abstract int getMinNumLines();
	protected abstract int getMaxNumLines();
	protected abstract String parseLine(int lineNumber, String line); 
}