package com.bt.nia.koala.robustness.scenarios;

import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.atomic.AtomicBoolean;

import com.bt.nia.koala.robustness.PircData;
import com.bt.nia.koala.robustness.commands.ImageDetails;
import com.bt.nia.koala.robustness.commands.ScenarioRunDetails;
import com.ragstorooks.testrr.ScenarioCommand;

public class InstanceStartStopScenarioWithBundlingAddressAndVolumes extends StartStopScenario {
	private final PircData pircData;

	public InstanceStartStopScenarioWithBundlingAddressAndVolumes(ScheduledExecutorService executor, PircData pircData) {
		super(executor);
		this.pircData = pircData;
	}

	@Override
	protected void addCommands(String scenarioId, AtomicBoolean scenarioCompleted, Map<String, Object> params, Queue<ScenarioCommand> commandQueue) {
		setup(scenarioId, params, null, null, null);

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

		// create and attach volumes
		addCreateVolumeCommands(scenarioId, scenarioCompleted, params, commandQueue);
		addAttachVolumeCommands(scenarioId, scenarioCompleted, params, commandQueue);

		addWaitCommand(scenarioId, scenarioCompleted, params, commandQueue, "15000");

		addDetachVolumeCommands(scenarioId, scenarioCompleted, params, commandQueue);

		addDeleteVolumeCommands(scenarioId, scenarioCompleted, params, commandQueue);

		// tear down instances
		addStopCommands(scenarioId, scenarioCompleted, params, commandQueue);
		addDeregisterAndDeleteImageAndKernel(runDetails, imageDetails, kernelDetails, kernelParamsKey, usernameParamsKey);
	}

}
