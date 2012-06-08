package com.ragstorooks.testrr.cli;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.List;
import java.util.Map;
import java.util.Vector;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.log4j.MDC;

class StreamGobbler implements Runnable {
    protected static final Log LOG = LogFactory.getLog(StreamGobbler.class);
    private InputStream inputStream;
    private Map<String, Object> mdcMap;
    private CountDownLatch streamReadLatch = new CountDownLatch(1);
	private List<String> lines = new Vector<String>();

    StreamGobbler(InputStream aInputStream, Map<String, Object> mdcMap) {
        this.inputStream = aInputStream;
        this.mdcMap = mdcMap;
    }

    public void run() {
        try {
            setupMdc();
            InputStreamReader isr = new InputStreamReader(inputStream);
            BufferedReader br = new BufferedReader(isr);
            String line = null;
            while ((line = br.readLine()) != null) {
                processLine(line);
            }
        } catch (Throwable t) {
            LOG.error(t.getMessage(), t);
        } finally {
            streamReadLatch.countDown();
            try {
				inputStream.close();
			} catch (IOException e) {
	            LOG.error("Error closing input stream from external process", e);
			}
        }
    }

    private void setupMdc() {
        if (mdcMap != null) {
            for (String key : mdcMap.keySet())
                MDC.put(key, mdcMap.get(key));
        }
    }

    protected void processLine(String line) {
        LOG.debug(line);
		lines.add(line);
   }
	
	public List<String> getLines() {
		try {
			if (!streamReadLatch.await(10, TimeUnit.SECONDS))
				LOG.error("Output stream was not read in 10 seconds");
		} catch (InterruptedException e) {
			LOG.error(e.getMessage(), e);
		}
		return lines;
	}
}
