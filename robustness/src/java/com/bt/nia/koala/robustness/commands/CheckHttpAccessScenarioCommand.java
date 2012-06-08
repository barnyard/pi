package com.bt.nia.koala.robustness.commands;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.ragstorooks.testrr.ScenarioListener;

public class CheckHttpAccessScenarioCommand extends Ec2ScenarioCommandBase {

	private static final Log LOG = LogFactory.getLog(CheckHttpAccessScenarioCommand.class);

	public CheckHttpAccessScenarioCommand(String scenarioId, ScenarioListener scenarioListener, AtomicBoolean scenarioCompleted, Executor executor, Map<String, Object> params) {
		super(scenarioId, scenarioListener, scenarioCompleted, executor, params);
	}

	@Override
	protected void execute(Map<String, Object> params) throws Throwable {
		String uri = "http://" + params.get("ipAddress").toString() + "/?" + new Random().nextLong();
		String timeoutParamName = "--max-time";
		String timeoutParamValue = "60";
		System.out.println("KOALA -----" + uri);
		String[] cmdParams = null;

		String proxyList = System.getenv("HTTP_PROXY_LIST");
		String[] proxies = (proxyList != null) ? proxyList.split(" ") : new String[0];
		if (proxies.length == 0) {
			cmdParams = new String[] { "-x", "", "--connect-timeout", "120", timeoutParamName, timeoutParamValue, "--insecure", "-s", uri };
			pingServer(cmdParams, params.get("findMe").toString());
		} else {
			int i = 0;
			boolean succeeded = false;
			int j = 0;
			while (j < 5 && !succeeded) {
				while (i < proxies.length && !succeeded) {
					String proxyParam = proxies[i];
					try {
						cmdParams = new String[] { "-x", proxyParam, timeoutParamName, timeoutParamValue, "--insecure", "-s", uri };
						pingServer(cmdParams, params.get("findMe").toString());
						succeeded = true;
					} catch (UnexpectedOutputException ex) {
						if (i == proxies.length - 1)
							throw ex;
					}
					i++;
				}
				j++;
				Thread.sleep(5000);
			}
		}
	}

	private void pingServer(String[] cmdParams, String findMe) throws Throwable {
		LOG.debug(String.format("pingServer(%s, %s)", Arrays.asList(cmdParams), findMe));
		List<String> ping;
		try {
			ping = executeCommand("curl", cmdParams);
		} catch (RuntimeException ex) {
			if (ex.getMessage().contains("Exit status was 28"))
				throw new UnexpectedOutputException("cURL timeout");
			else if (ex.getMessage().contains("Exit status was 7"))
				throw new UnexpectedOutputException("cURL could not connect to host");
			else if (ex.getMessage().contains("Exit status was "))
				throw new UnexpectedOutputException("cURL returned an unknown error. " + ex.getMessage(), ex);
			else
				throw ex;
		}
		StringBuilder builder = new StringBuilder();
		for (int i = 0; i < ping.size(); i++) {
			builder.append(ping.get(i));
		}
		if (builder.toString().toLowerCase().contains(findMe.toLowerCase())) {
			LOG.info("Found Apache page");
		} else if (builder.toString().contains("BT Web scanner")) {
			throw new UnexpectedOutputException("Received BT proxy page");
		} else {
			throw new UnexpectedOutputException("Did not find " + findMe + " on the page");
		}
	}
}
