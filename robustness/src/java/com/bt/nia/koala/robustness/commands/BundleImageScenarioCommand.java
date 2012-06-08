package com.bt.nia.koala.robustness.commands;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.bt.nia.koala.robustness.commands.ImageDetails.ImageType;
import com.ragstorooks.testrr.ScenarioListener;

public class BundleImageScenarioCommand extends Ec2ScenarioCommandBase {
	private static final Log LOG = LogFactory.getLog(BundleImageScenarioCommand.class);

	private String imageFilePath;
	private ImageType imageType;
	private String destinationPath;
	private String kernelParamsKey;
	private String ramdiskParamsKey;

	private static Object lock = new Object();

	public BundleImageScenarioCommand(String scenarioId, ScenarioListener scenarioListener, AtomicBoolean scenarioCompleted, ScheduledExecutorService executor, Map<String, Object> params,
			String imageFilePath, ImageType imageType, String destinationPath, String kernelParamsKey, String ramdiskParamsKey) {
		super(scenarioId, scenarioListener, scenarioCompleted, executor, params);
		this.imageFilePath = imageFilePath;
		this.imageType = imageType;
		this.destinationPath = destinationPath;
		this.kernelParamsKey = kernelParamsKey;
		this.ramdiskParamsKey = ramdiskParamsKey;
	}

	@Override
	protected void execute(Map<String, Object> params) throws Throwable {
		LOG.info(String.format("Bundling image: %s with type: %s to %s", this.imageFilePath, imageType.toString(), destinationPath));

		List<String> args = new ArrayList<String>();
		args.add("--debug");
		args.add("-i");
		args.add(this.imageFilePath);
		args.add("-r");
		args.add("x86_64");
		args.add("-d");
		args.add(this.destinationPath);
		if (imageType == ImageType.MACHINE) {
			String kernelId = (String) params.get(kernelParamsKey);
			if (null != kernelId) {
				args.add("--kernel");
				args.add(kernelId);
			}
			String ramdiskId = (String) params.get(ramdiskParamsKey);
			if (null != ramdiskId) {
				args.add("--ramdisk");
				args.add(ramdiskId);
			}
		}

		args.add("-c");
		args.add((String) params.get("EC2_CERT"));

		args.add("-k");
		args.add((String) params.get("EC2_PRIVATE_KEY"));

		args.add("--ec2cert");
		args.add((String) params.get("PI_CERT"));

		args.add("-u");
		args.add("000000000000");
		synchronized (lock) {
			List<String> output = executeEc2AmiToolCommand("ec2-bundle-image", args.toArray(new String[] {}));

			String target = "ec2-bundle-image complete";
			for (String line : output)
				if (line.contains(target))
					return;
			throw new RuntimeException("ec2-bundle-image failed: " + Arrays.toString(output.toArray()));
		}
	}
}
