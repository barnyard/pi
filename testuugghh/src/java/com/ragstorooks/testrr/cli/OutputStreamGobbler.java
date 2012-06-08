package com.ragstorooks.testrr.cli;

import java.io.InputStream;
import java.util.Map;

public class OutputStreamGobbler extends StreamGobbler {

	OutputStreamGobbler(InputStream inputStream, Map<String, Object> mdcMap) {
		super(inputStream, mdcMap);
	}
}
