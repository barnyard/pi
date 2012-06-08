package com.ragstorooks.testrr.cli;

import java.io.InputStream;
import java.util.Map;

public class ErrorStreamGobbler extends StreamGobbler {
	private boolean logErrors = true;

	ErrorStreamGobbler(InputStream inputStream, Map<String, Object> mdcMap) {
		this(inputStream, true, mdcMap);
	}

	ErrorStreamGobbler(InputStream inputStream, boolean logErrors, Map<String, Object> mdcMap) {
		super(inputStream, mdcMap);
		this.logErrors = logErrors;
	}

	@Override
	protected void processLine(String line) {
		if (this.logErrors)
			LOG.error(line);
		super.processLine(line);
	}
}
