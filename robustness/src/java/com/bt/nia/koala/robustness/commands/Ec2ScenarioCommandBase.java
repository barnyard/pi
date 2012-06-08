package com.bt.nia.koala.robustness.commands;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.util.StringUtils;

import com.ragstorooks.testrr.CliScenarioCommandBase;
import com.ragstorooks.testrr.ScenarioListener;

public abstract class Ec2ScenarioCommandBase extends CliScenarioCommandBase {
	// protected static final String ADMIN_EC2_CERT =
	// System.getenv("ADMIN_EC2_CERT");
	// protected static final String ADMIN_EC2_PRIVATE_KEY =
	// System.getenv("ADMIN_EC2_PRIVATE_KEY");
	// protected static final String ADMIN_EUCALYPTUS_CERT =
	// System.getenv("ADMIN_EUCALYPTUS_CERT");
	// protected static final String EC2_ACCESS_KEY =
	// System.getenv("EC2_ACCESS_KEY");
	// protected static final String EC2_SECRET_KEY =
	// System.getenv("EC2_SECRET_KEY");
	// protected static final String ADMIN_EC2_ACCESS_KEY =
	// System.getenv("ADMIN_EC2_ACCESS_KEY");
	// protected static final String ADMIN_EC2_SECRET_KEY =
	// System.getenv("ADMIN_EC2_SECRET_KEY");
	// protected static final String S3_URL = System.getenv("S3_URL");

	protected static final String HTTP_PROXY_HOST = System.getenv("HTTP_PROXY_HOST");
	protected static final String HTTP_PROXY_PORT = System.getenv("HTTP_PROXY_PORT");

	private static final Log log = LogFactory.getLog(Ec2ScenarioCommandBase.class);
	private static final String EC2_HOME = System.getenv("EC2_HOME");
	private static final String COMMAND_SUFFIX = System.getenv("COMMAND_SUFFIX") == null ? "" : System.getenv("COMMAND_SUFFIX");
	private final Map<String, Object> commandParams;

	public Ec2ScenarioCommandBase(ScenarioRunDetails runDetails) {
		this(runDetails.getScenarioId(), runDetails.getScenarioListener(), runDetails.getScenarioCompleted(), runDetails.getExecutor(), runDetails.getParams());
	}

	public Ec2ScenarioCommandBase(String scenarioId, ScenarioListener scenarioListener, AtomicBoolean scenarioCompleted, Executor executor, Map<String, Object> params) {
		super(scenarioId, scenarioListener, scenarioCompleted, executor, params);
		commandParams = params;
	}

	@SuppressWarnings("unchecked")
	@Override
	protected void cleanup(Map<String, Object> params) throws Throwable {
		List<Ec2ScenarioCommandBase> compensationCommands = (List<Ec2ScenarioCommandBase>) params.get("compensationCommands");
		for (int i = compensationCommands.size() - 1; i >= 0; i--) {
			Ec2ScenarioCommandBase sc = compensationCommands.get(i);
			try {
				log.debug(String.format("Executing COMPENSATION command %s for scenario %s", sc.getClass().getSimpleName(), getScenarioId()));
				sc.execute(getParams());
			} catch (Throwable t) {
				log.warn("Error executing compensation command " + sc.getClass().getSimpleName(), t);
			}
		}
	}

	protected List<String> executeEc2Command(String scriptName, String[] params) throws Throwable {
		return executeEc2Command(scriptName, params, true);
	}

	protected List<String> executeEc2Command(String scriptName, String[] params, boolean failOnError) throws Throwable {
		String httpProxy = getProxy();

		String commandLineArguments = String.format("--url %s --private-key %s --cert %s", commandParams.get("EC2_URL"), commandParams.get("EC2_PRIVATE_KEY"), commandParams.get("EC2_CERT"));

		String path = "/bin/sh";
		String[] newParams = new String[2];
		newParams[0] = "-c";
		newParams[1] = httpProxy + EC2_HOME + "/bin/" + scriptName + " " + COMMAND_SUFFIX + " " + commandLineArguments + " " + StringUtils.arrayToDelimitedString(params, " ");

		try {
			return execute(path, newParams, failOnError);
		} catch (Throwable ex) {
			if (null != ex.getMessage() && (ex.getMessage().contains("Looks like you are going to timeout") || ex.getMessage().contains("Read timeout. Please try again later"))) {
				return execute(path, newParams, failOnError);
			} else {
				throw ex;
			}
		}
	}

	private String getProxy() {
		String httpProxy = "unset http_proxy;";

		if (HTTP_PROXY_HOST != null && HTTP_PROXY_PORT != null) {
			httpProxy = String.format("export http_proxy=%s:%s;", HTTP_PROXY_HOST, HTTP_PROXY_PORT);
		}
		return httpProxy;
	}

	protected List<String> executeEc2AmiToolCommand(String scriptName, String[] params) throws Throwable {
		return executeEc2AmiToolCommand(scriptName, params, true);
	}

	protected List<String> executeEc2AmiToolCommand(String scriptName, String[] params, boolean failOnError) throws Throwable {
		String httpProxy = getProxy();

		String path = "/bin/sh";
		String[] newParams = new String[2];
		newParams[0] = "-c";
		newParams[1] = httpProxy + scriptName + " " + StringUtils.arrayToDelimitedString(params, " ");

		return execute(path, newParams, failOnError);
	}

	private List<String> execute(String path, String[] params, boolean failOnError) throws Throwable {
		log.debug(String.format("Running command: %s with params: %s", path, Arrays.toString(params)));
		return executeCommand(path, params, failOnError);
	}

}
