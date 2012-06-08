package com.bt.nia.koala.robustness.commands;

import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.bt.nia.koala.robustness.PircData;
import com.bt.nia.koala.robustness.parsers.KoalaPortalOutputParser;
import com.ragstorooks.testrr.ScenarioListener;

public class CheckKoalaPortalScenarioCommand extends Ec2ScenarioCommandBase {
	private static final Log LOG = LogFactory.getLog(CheckKoalaPortalScenarioCommand.class);
	private final PircData pircData;

	public CheckKoalaPortalScenarioCommand(String scenarioId, ScenarioListener scenarioListener, AtomicBoolean scenarioCompleted, Executor executor, Map<String, Object> params, PircData thePircData) {
		super(scenarioId, scenarioListener, scenarioCompleted, executor, params);
		pircData = thePircData;
	}

	@Override
	protected void execute(Map<String, Object> params) throws Throwable {
		String httpProxyHost = System.getenv("HTTP_PROXY_HOST");
		String httpProxyPort = System.getenv("HTTP_PROXY_PORT");

		String proxyParam = (httpProxyHost != null && httpProxyPort != null) ? "-xhttp://" + httpProxyHost + ":" + httpProxyPort : "";
		String url = pircData.getEc2Url();
		String timeoutParamName = "--max-time";
		String timeoutParamValue = "150";

		String[] cmdParams = null;
		if (proxyParam != null && proxyParam.length() > 0) {
			LOG.info(String.format("using proxy %s to check %s", proxyParam, url));
			cmdParams = new String[] { proxyParam, timeoutParamName, timeoutParamValue, "--insecure", "-s", url };
		} else {
			LOG.info(String.format("Not using proxy to check %s", url));
			cmdParams = new String[] { timeoutParamName, timeoutParamValue, "--insecure", "-s", url };
		}
		List<String> output = executeCommand("curl", cmdParams);
		KoalaPortalOutputParser koalaPortalOutputParser = new KoalaPortalOutputParser();
		koalaPortalOutputParser.parse(output);
	}
}
