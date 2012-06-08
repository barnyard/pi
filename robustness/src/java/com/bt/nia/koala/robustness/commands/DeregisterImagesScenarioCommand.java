package com.bt.nia.koala.robustness.commands;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.ragstorooks.testrr.ScenarioListener;

public class DeregisterImagesScenarioCommand extends Ec2ScenarioCommandBase {
	private static final Log log = LogFactory.getLog(DeregisterImagesScenarioCommand.class);

	private List<String> paths;

	public DeregisterImagesScenarioCommand(ScenarioRunDetails runDetails, ImageDetails imageDetails) {
		super(runDetails);
		paths = Arrays.asList(new String[] { imageDetails.getManifestLocationOnServer() });
	}

	public DeregisterImagesScenarioCommand(String scenarioId, ScenarioListener scenarioListener, AtomicBoolean scenarioCompleted, ScheduledExecutorService executor, Map<String, Object> params,
			String[] identifiers) {
		super(scenarioId, scenarioListener, scenarioCompleted, executor, params);
		this.paths = Arrays.asList(identifiers);
	}

	@Override
	protected void execute(Map<String, Object> params) throws Throwable {
		List<String> output = executeEc2Command("ec2-describe-images", new String[] {});
		String target = "IMAGE";
		String imageId = null;
		for (String line : output) {
			if (line.contains(target)) {
				String path = line.split("\t")[2];
				if (this.paths.contains(path)) {
					imageId = line.split("\t")[1];
					deregisterImage(imageId);
				}
			}
		}
	}

	private void deregisterImage(String imageId) throws Throwable {
		log.info("Trying to deregister image " + imageId);
		executeEc2Command("ec2-deregister", new String[] { imageId }, false);
	}
}
