package com.bt.nia.koala.robustness.commands;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.ragstorooks.testrr.ScenarioListener;

public class UploadBundleScenarioCommand extends Ec2ScenarioCommandBase {
	private static final Log log = LogFactory.getLog(UploadBundleScenarioCommand.class);

	private String manifestFilename;
	private String bucketName;
	private String deleteBundlePrefix;

	public UploadBundleScenarioCommand(String scenarioId, ScenarioListener scenarioListener, AtomicBoolean scenarioCompleted, ScheduledExecutorService executor, Map<String, Object> params,
			String manifestFilename, String bucketName, String deleteBundlePrefix) {
		super(scenarioId, scenarioListener, scenarioCompleted, executor, params);
		this.manifestFilename = manifestFilename;
		this.bucketName = bucketName;
		this.deleteBundlePrefix = deleteBundlePrefix;

	}

	@Override
	protected void execute(Map<String, Object> params) throws Throwable {
		log.debug(String.format("Uploading bundle: %s on bucket: %s and deleteBundlePrefix: %s", this.manifestFilename, this.bucketName, this.deleteBundlePrefix));

		List<String> args = new ArrayList<String>();
		args.add("-b");
		args.add(this.bucketName);
		args.add("-m");
		args.add(this.manifestFilename);
		args.add("--url");
		args.add((String) params.get("S3_URL"));

		args.add("-a");
		args.add((String) params.get("EC2_ACCESS_KEY"));
		args.add("-s");
		args.add((String) params.get("EC2_SECRET_KEY"));

		args.add("--debug");
		List<String> output = executeEc2AmiToolCommand("ec2-upload-bundle", args.toArray(new String[] {}));

		log.debug("upload command has run :" + output);
		String target = "Bundle upload completed";
		for (String line : output)
			if (line.contains(target))
				return;

		throw new RuntimeException("ec2-upload-bundle failed: " + Arrays.toString(output.toArray()));
	}

	@Override
	protected Ec2ScenarioCommandBase getCompensationCommand() {
		return new DeleteBundleScenarioCommand(getScenarioId(), null, null, (ScheduledExecutorService) getExecutor(), getParams(), this.bucketName, this.deleteBundlePrefix);
	}
}
