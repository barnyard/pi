package com.bt.nia.koala.robustness.commands;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.commons.lang.NotImplementedException;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.ragstorooks.testrr.ScenarioListener;

public class DeleteBundleScenarioCommand extends Ec2ScenarioCommandBase {
	private static final Log log = LogFactory.getLog(DeleteBundleScenarioCommand.class);

	private String bucketName;
	private String prefix;
	private boolean kernel;
	private boolean logErrors = true;

	public DeleteBundleScenarioCommand(ScenarioRunDetails runDetails, ImageDetails imageDetails) {
		this(runDetails.getScenarioId(), runDetails.getScenarioListener(), runDetails.getScenarioCompleted(), runDetails.getExecutor(), runDetails.getParams(), imageDetails.getImageBucket(),
				imageDetails.getImageFileName());
	}

	public DeleteBundleScenarioCommand(String scenarioId, ScenarioListener scenarioListener, AtomicBoolean scenarioCompleted, ScheduledExecutorService executor, Map<String, Object> params,
			String bucketName, String prefix, boolean kernel, boolean logErrors) {
		super(scenarioId, scenarioListener, scenarioCompleted, executor, params);
		this.bucketName = bucketName;
		this.prefix = prefix;
		this.kernel = kernel;
		this.logErrors = logErrors;
	}

	public DeleteBundleScenarioCommand(String scenarioId, ScenarioListener scenarioListener, AtomicBoolean scenarioCompleted, ScheduledExecutorService executor, Map<String, Object> params,
			String bucketName, String prefix) {
		this(scenarioId, scenarioListener, scenarioCompleted, executor, params, bucketName, prefix, false, true);
	}

	@Override
	protected void execute(Map<String, Object> params) throws Throwable {
		List<String> args = new ArrayList<String>();
		args.add("-b");
		args.add(this.bucketName);
		args.add("--prefix");
		args.add(this.prefix);
		args.add("-y");
		args.add("--clear");
		args.add("--url");
		String s3Url = (String) params.get("S3_URL");
		args.add(s3Url);

		if (this.kernel) {
			throw new NotImplementedException();
		} else {
			args.add("-a");
			args.add((String) params.get("EC2_ACCESS_KEY"));
			args.add("-s");
			args.add((String) params.get("EC2_SECRET_KEY"));
		}
		args.add("--debug");

		log.info(String.format("Trying to delete bundle %s %s from %s", prefix, bucketName, s3Url));
		executeEc2AmiToolCommand("ec2-delete-bundle", args.toArray(new String[] {}), this.logErrors);
	}
}
