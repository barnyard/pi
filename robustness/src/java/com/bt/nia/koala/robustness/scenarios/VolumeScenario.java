package com.bt.nia.koala.robustness.scenarios;

import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.bt.nia.koala.robustness.PircData;
import com.bt.nia.koala.robustness.commands.ImageDetails;
import com.bt.nia.koala.robustness.commands.ScenarioRunDetails;
import com.ragstorooks.testrr.ScenarioCommand;

public class VolumeScenario extends StartStopScenario {
	private static final Log log = LogFactory.getLog(VolumeScenario.class);
	private final PircData pircData;

	public VolumeScenario(ScheduledExecutorService executor, PircData aPircData) {
		super(executor);
		this.pircData = aPircData;
	}

	@Override
	protected void addCommands(String scenarioId, AtomicBoolean scenarioCompleted, Map<String, Object> params, Queue<ScenarioCommand> commandQueue) {
		setup(scenarioId, params, null, null, null);
		setupCerts(pircData, params);

		String ramdiskParamsKey = generateRandomString();
		String kernelParamsKey = generateRandomString();
		String usernameParamsKey = generateRandomString();

		params.put(usernameParamsKey, pircData.getEc2UserId());
		ScenarioRunDetails runDetails = new ScenarioRunDetails(scenarioId, scenarioCompleted, getExecutor(), params, commandQueue, getScenarioListener());

		ImageDetails kernelDetails = getDefaultKernelImageDetails();
		ImageDetails imageDetails = getDefaultMachineImageDetails();
		// run
		// prepare instances
		addBundleUploadAndRegisterKernelAndImage(runDetails, kernelDetails, imageDetails, ramdiskParamsKey, kernelParamsKey, usernameParamsKey);
		addStartCommands(scenarioId, scenarioCompleted, params, commandQueue, null, "imageId", kernelParamsKey, ramdiskParamsKey);

		// do the volume attachment testing
		addCreateVolumeCommands(scenarioId, scenarioCompleted, params, commandQueue);
		addAttachVolumeCommands(scenarioId, scenarioCompleted, params, commandQueue);
		// let's have some fun
		// params.put("curlUsername", "mrmagoo");
		// params.put("curlPassword", "17cheesecakemonsterswith4pies");
		// commandQueue.add(new MountVolumeScenarioCommand(scenarioId,
		// getScenarioListener(), scenarioCompleted, getExecutor(), params));
		// enough fun for now
		addDetachVolumeCommands(scenarioId, scenarioCompleted, params, commandQueue);
		addDeleteVolumeCommands(scenarioId, scenarioCompleted, params, commandQueue);

		// tear it all down
		addStopCommands(scenarioId, scenarioCompleted, params, commandQueue);
		addDeregisterAndDeleteImageAndKernel(runDetails, imageDetails, kernelDetails, kernelParamsKey, usernameParamsKey);
	}
}
