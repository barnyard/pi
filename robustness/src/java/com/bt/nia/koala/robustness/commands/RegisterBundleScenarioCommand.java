package com.bt.nia.koala.robustness.commands;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class RegisterBundleScenarioCommand extends Ec2ScenarioCommandBase {
	private static final Log log = LogFactory.getLog(RegisterBundleScenarioCommand.class);

	private String name;

	public RegisterBundleScenarioCommand(ScenarioRunDetails runDetails, String manifestLocation) {
		super(runDetails.getScenarioId(), runDetails.getScenarioListener(), runDetails.getScenarioCompleted(), runDetails.getExecutor(), runDetails.getParams());
		this.name = manifestLocation;
	}

	@Override
	protected void execute(Map<String, Object> params) throws Throwable {
		log.debug(String.format("Registering image with manifest: %s", this.name));

		List<String> args = new ArrayList<String>();

		args.add(this.name);

		log.info("Trying to register image " + name);

		// this is a dirty, nasty hacky thing...
		// added to enforce a chance for gluster to propogate its data
		Thread.sleep(10000);

		List<String> output = executeEc2Command("ec2-register", args.toArray(new String[] {}));

		String target = "IMAGE";
		String imageId = null;
		for (String line : output) {
			if (line.contains(target)) {
				imageId = line.split("\t")[1];
				if (imageId.startsWith("pmi-"))
					getParams().put("imageId", imageId);
				if (imageId.startsWith("pki-"))
					getParams().put("kernelId", imageId);
			}
		}
		if (null == imageId)
			throw new RuntimeException("ec2-register failed: " + Arrays.toString(output.toArray()));

		output = executeEc2Command("ec2-describe-images", new String[] {});
		for (String line : output)
			if (line.contains(imageId)) {
				log.debug(String.format("*** Image id: %s", imageId));
				log.debug(params);
				return;
			}

		throw new RuntimeException("ec2-register failed: ec2-describe-images does not contain the image id" + Arrays.toString(output.toArray()));
	}
}
