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

public class InstanceStartStopScenarioWithBundling extends StartStopScenario {
	private final static Log LOG = LogFactory.getLog(InstanceStartStopScenarioWithBundling.class);

	private final PircData pircData;

	public InstanceStartStopScenarioWithBundling(ScheduledExecutorService executor, PircData aPircData) {
		super(executor);
		this.pircData = aPircData;
	}

	@Override
	protected void addCommands(String scenarioId, AtomicBoolean scenarioCompleted, Map<String, Object> params, Queue<ScenarioCommand> commandQueue) {
		// setup
		setupCerts(pircData, params);
		setup(scenarioId, params, null, null, null);

		String ramdiskParamsKey = generateRandomString();
		String kernelParamsKey = generateRandomString();
		String usernameParamsKey = generateRandomString();

		params.put(usernameParamsKey, pircData.getEc2UserId());
		ScenarioRunDetails runDetails = new ScenarioRunDetails(scenarioId, scenarioCompleted, getExecutor(), params, commandQueue, getScenarioListener());

		ImageDetails kernelDetails = getDefaultKernelImageDetails();
		ImageDetails imageDetails = getDefaultMachineImageDetails();

		// run
		addBundleUploadAndRegisterKernelAndImage(runDetails, kernelDetails, imageDetails, ramdiskParamsKey, kernelParamsKey, usernameParamsKey);

		addStartCommands(scenarioId, scenarioCompleted, params, commandQueue, null, "imageId", kernelParamsKey, ramdiskParamsKey);
		addStopCommands(scenarioId, scenarioCompleted, params, commandQueue);

		addDeregisterAndDeleteImageAndKernel(runDetails, imageDetails, kernelDetails, kernelParamsKey, usernameParamsKey);
	}
}
