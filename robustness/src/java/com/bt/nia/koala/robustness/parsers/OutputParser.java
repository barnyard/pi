package com.bt.nia.koala.robustness.parsers;

import java.util.List;

import com.bt.nia.koala.robustness.commands.UnexpectedOutputException;

public interface OutputParser {
	String[] parse(List<String> lines) throws UnexpectedOutputException;
}
