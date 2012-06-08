package com.bt.nia.koala.robustness.scenarios;

import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.bt.nia.koala.robustness.PircData;
import com.bt.nia.koala.robustness.commands.AuthoriseAccessScenarioCommand;
import com.bt.nia.koala.robustness.commands.CheckHttpAccessScenarioCommand;
import com.bt.nia.koala.robustness.commands.CheckSecurityGroupStatusScenarioCommand;
import com.bt.nia.koala.robustness.commands.ImageDetails;
import com.bt.nia.koala.robustness.commands.RevokeAccessScenarioCommand;
import com.bt.nia.koala.robustness.commands.ScenarioRunDetails;
import com.ragstorooks.testrr.ScenarioCommand;

public class InstanceStartStopScenarioWithAccess extends StartStopScenario {
	private static final Log log = LogFactory.getLog(InstanceStartStopScenarioWithAccess.class);
	private final PircData pircData;

	public InstanceStartStopScenarioWithAccess(ScheduledExecutorService executor, PircData aPircData) {
		super(executor);
		this.pircData = aPircData;
	}

	@Override
	protected void addCommands(String scenarioId, AtomicBoolean scenarioCompleted, Map<String, Object> params, Queue<ScenarioCommand> commandQueue) {
		setupCerts(pircData, params);
		setup(scenarioId, params, null, null, null);

		String ramdiskParamsKey = generateRandomString();
		String kernelParamsKey = generateRandomString();
		String usernameParamsKey = generateRandomString();

		params.put(usernameParamsKey, pircData.getEc2UserId());
		ScenarioRunDetails runDetails = new ScenarioRunDetails(scenarioId, scenarioCompleted, getExecutor(), params, commandQueue, getScenarioListener());

		ImageDetails kernelDetails = getDefaultKernelImageDetails();
		ImageDetails imageDetails = getDefaultMachineImageDetails();
		imageDetails.setImageDirectory(getImageDirectory());

		// run
		// prepare instances
		addBundleUploadAndRegisterKernelAndImage(runDetails, kernelDetails, imageDetails, ramdiskParamsKey, kernelParamsKey, usernameParamsKey);
		addStartCommands(scenarioId, scenarioCompleted, params, commandQueue, null, "imageId", kernelParamsKey, ramdiskParamsKey);

		String findMe = null;
		if (findMe == null)
			findMe = "<title>pi test image</title>";
		params.put("findMe", findMe);

		// curl
		commandQueue.add(new AuthoriseAccessScenarioCommand(scenarioId, getScenarioListener(), scenarioCompleted, getExecutor(), params));

		commandQueue.add(new CheckSecurityGroupStatusScenarioCommand(scenarioId, getScenarioListener(), scenarioCompleted, getExecutor(), params, params.get("securityGroup").toString()));

		addWaitCommand(scenarioId, scenarioCompleted, params, commandQueue);

		commandQueue.add(new CheckHttpAccessScenarioCommand(scenarioId, getScenarioListener(), scenarioCompleted, getExecutor(), params));
		commandQueue.add(new RevokeAccessScenarioCommand(scenarioId, getScenarioListener(), scenarioCompleted, getExecutor(), params));

		addStopCommands(scenarioId, scenarioCompleted, params, commandQueue);
		addDeregisterAndDeleteImageAndKernel(runDetails, imageDetails, kernelDetails, kernelParamsKey, usernameParamsKey);
	}

	private String getImageDirectory() {
		return "etc/images/tinyhttp/";
	}
}
