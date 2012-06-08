package com.bt.nia.koala.robustness.scenarios;

import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.bt.nia.koala.robustness.PircData;
import com.bt.nia.koala.robustness.commands.AllocateAddressScenarioCommand;
import com.bt.nia.koala.robustness.commands.AssociateAddressScenarioCommand;
import com.bt.nia.koala.robustness.commands.CheckInstanceAssociatedAddressScenarioCommand;
import com.bt.nia.koala.robustness.commands.DisassociateAddressScenarioCommand;
import com.bt.nia.koala.robustness.commands.ImageDetails;
import com.bt.nia.koala.robustness.commands.ReleaseAddressScenarioCommand;
import com.bt.nia.koala.robustness.commands.ScenarioRunDetails;
import com.ragstorooks.testrr.ScenarioCommand;

public class InstanceStartStopScenarioWithAddressing extends StartStopScenario {
	private static final Log log = LogFactory.getLog(InstanceStartStopScenarioWithAddressing.class);
	private final PircData pircData;

	public InstanceStartStopScenarioWithAddressing(ScheduledExecutorService executor, PircData aPircData) {
		super(executor);
		this.pircData = aPircData;
	}

	@Override
	protected void addCommands(final String scenarioId, final AtomicBoolean scenarioCompleted, Map<String, Object> params, Queue<ScenarioCommand> commandQueue) {
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
		// prepare instances
		addBundleUploadAndRegisterKernelAndImage(runDetails, kernelDetails, imageDetails, ramdiskParamsKey, kernelParamsKey, usernameParamsKey);
		addStartCommands(scenarioId, scenarioCompleted, params, commandQueue, null, "imageId", kernelParamsKey, ramdiskParamsKey);

		// allocate address
		commandQueue.add(new AllocateAddressScenarioCommand(scenarioId, getScenarioListener(), scenarioCompleted, getExecutor(), params));

		// associate address
		commandQueue.add(new AssociateAddressScenarioCommand(scenarioId, getScenarioListener(), scenarioCompleted, getExecutor(), params));

		// check instance ip address
		commandQueue.add(new CheckInstanceAssociatedAddressScenarioCommand(scenarioId, getScenarioListener(), scenarioCompleted, getExecutor(), params));

		// disassociate address
		commandQueue.add(new DisassociateAddressScenarioCommand(scenarioId, getScenarioListener(), scenarioCompleted, getExecutor(), params));

		// de-allocate address
		commandQueue.add(new ReleaseAddressScenarioCommand(scenarioId, getScenarioListener(), scenarioCompleted, getExecutor(), params));

		// stop the instance and tear everything down
		addStopCommands(scenarioId, scenarioCompleted, params, commandQueue);
		addDeregisterAndDeleteImageAndKernel(runDetails, imageDetails, kernelDetails, kernelParamsKey, usernameParamsKey);
	}
}
