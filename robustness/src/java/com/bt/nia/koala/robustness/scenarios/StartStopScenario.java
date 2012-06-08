/* (c) British Telecommunications plc, 2010, All Rights Reserved */
package com.bt.nia.koala.robustness.scenarios;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.Queue;
import java.util.UUID;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.bt.nia.koala.robustness.PircData;
import com.bt.nia.koala.robustness.commands.AddGroupScenarioCommand;
import com.bt.nia.koala.robustness.commands.AttachVolumeScenarioCommand;
import com.bt.nia.koala.robustness.commands.BundleImageScenarioCommand;
import com.bt.nia.koala.robustness.commands.CheckImageStatusScenarioCommand;
import com.bt.nia.koala.robustness.commands.CheckInstanceStatusScenarioCommand;
import com.bt.nia.koala.robustness.commands.CheckSnapshotStatusScenarioCommand;
import com.bt.nia.koala.robustness.commands.CheckVolumeStatusScenarioCommand;
import com.bt.nia.koala.robustness.commands.CreateSnapshotScenarioCommand;
import com.bt.nia.koala.robustness.commands.CreateVolumeScenarioCommand;
import com.bt.nia.koala.robustness.commands.DeleteBundleScenarioCommand;
import com.bt.nia.koala.robustness.commands.DeleteGroupScenarioCommand;
import com.bt.nia.koala.robustness.commands.DeleteSnapshotScenarioCommand;
import com.bt.nia.koala.robustness.commands.DeleteVolumeScenarioCommand;
import com.bt.nia.koala.robustness.commands.DeregisterImagesScenarioCommand;
import com.bt.nia.koala.robustness.commands.DeregisterKernelRamdiskScenarioCommand;
import com.bt.nia.koala.robustness.commands.DetachVolumeScenarioCommand;
import com.bt.nia.koala.robustness.commands.ImageDetails;
import com.bt.nia.koala.robustness.commands.RegisterBundleScenarioCommand;
import com.bt.nia.koala.robustness.commands.RegisterKernelRamdiskScenarioCommand;
import com.bt.nia.koala.robustness.commands.RunInstanceScenarioCommand;
import com.bt.nia.koala.robustness.commands.ScenarioRunDetails;
import com.bt.nia.koala.robustness.commands.TerminateInstanceScenarioCommand;
import com.bt.nia.koala.robustness.commands.UploadBundleScenarioCommand;
import com.bt.nia.koala.robustness.commands.WaitScenarioCommand;
import com.bt.nia.koala.robustness.commands.ImageDetails.ImageType;
import com.bt.nia.koala.robustness.commands.RegisterKernelRamdiskScenarioCommand.RegistrationType;
import com.ragstorooks.testrr.ScenarioCommand;
import com.ragstorooks.testrr.ScenarioCommanderBase;

public abstract class StartStopScenario extends ScenarioCommanderBase {
	private static final Log log = LogFactory.getLog(StartStopScenario.class);

	private final static String DEFAULT_IMAGE_PATH = "etc/images/ttylinux/";

	public StartStopScenario(ScheduledExecutorService executor) {
		super(executor);
	}

	protected ImageDetails getDefaultKernelImageDetails() {
		return new ImageDetails("robustness" + generateRandomString(), "vmlinuz-2.6.16.33-xen", getKernelImagePath(), ImageType.KERNEL);
	}

	protected ImageDetails getDefaultMachineImageDetails() {
		return new ImageDetails("robustness" + generateRandomString(), "ttylinux.img", getMachineImagePath(), ImageType.KERNEL);
	}

	private String getKernelImagePath() {
		return DEFAULT_IMAGE_PATH;
	}

	private String getMachineImagePath() {
		return DEFAULT_IMAGE_PATH;
	}

	protected void setupCerts(PircData pircData, Map<String, Object> params) {
		params.put("S3_URL", pircData.getS3Url());
		params.put("EC2_URL", pircData.getEc2Url());
		params.put("EC2_PRIVATE_KEY", pircData.getEc2PrivateKey());
		params.put("EC2_CERT", pircData.getEc2Cert());
		params.put("EC2_ACCESS_KEY", pircData.getEc2AccessKey());
		params.put("EC2_SECRET_KEY", pircData.getEc2SecretKey());
		params.put("PI_CERT", pircData.getPiCert());
	}

	protected void setup(final String scenarioId, Map<String, Object> params, String imageId, String kernelId, String ramdiskId) {
		String securityGroup = scenarioId + "_" + System.currentTimeMillis();

		log.info(String.format("Setting up imageId:%s, kernelId:%s, ramdiskId:%s", imageId, kernelId, ramdiskId));

		params.put("imageId", imageId);
		params.put("kernelId", kernelId);
		params.put("ramdiskId", ramdiskId);
		params.put("securityGroup", securityGroup);
	}

	protected void addStartCommands(final String scenarioId, final AtomicBoolean scenarioCompleted, Map<String, Object> params, Queue<ScenarioCommand> commandQueue, String securityGroupParamsKey,
			String imageIdParamsKey, String kernelIdParamsKey, String ramdiskIdParamsKey) {
		commandQueue.add(new AddGroupScenarioCommand(scenarioId, getScenarioListener(), scenarioCompleted, getExecutor(), params));
		commandQueue.add(new RunInstanceScenarioCommand(scenarioId, getScenarioListener(), scenarioCompleted, getExecutor(), params, securityGroupParamsKey, imageIdParamsKey, kernelIdParamsKey,
				ramdiskIdParamsKey));
		commandQueue.add(new CheckInstanceStatusScenarioCommand(scenarioId, getScenarioListener(), scenarioCompleted, getExecutor(), params, "running", new String[] { "terminated", "shutting-down" },
				"pending"));
	}

	protected void addStopCommands(final String scenarioId, final AtomicBoolean scenarioCompleted, Map<String, Object> params, Queue<ScenarioCommand> commandQueue) {
		commandQueue.add(new TerminateInstanceScenarioCommand(scenarioId, getScenarioListener(), scenarioCompleted, getExecutor(), params));
		commandQueue.add(new CheckInstanceStatusScenarioCommand(scenarioId, getScenarioListener(), scenarioCompleted, getExecutor(), params, "terminated", new String[0], "pending"));
		// add wait of 10 seconds to avoid race condition of instance deleted
		// but not yet removed from security group
		String sleep = System.getenv("ROBUSTNESS_SLEEP");
		addWaitCommand(scenarioId, scenarioCompleted, params, commandQueue, sleep);
		commandQueue.add(new DeleteGroupScenarioCommand(scenarioId, getScenarioListener(), scenarioCompleted, getExecutor(), params));
	}

	protected void addWaitCommand(final String scenarioId, final AtomicBoolean scenarioCompleted, Map<String, Object> params, Queue<ScenarioCommand> commandQueue) {
		String sleep = System.getenv("ROBUSTNESS_SLEEP");
		if (sleep != null) {
			addWaitCommand(scenarioId, scenarioCompleted, params, commandQueue, sleep);
		}
	}

	protected void addWaitCommand(final String scenarioId, final AtomicBoolean scenarioCompleted, Map<String, Object> params, Queue<ScenarioCommand> commandQueue, String period) {
		params.put("sleep", period);
		commandQueue.add(new WaitScenarioCommand(scenarioId, getScenarioListener(), scenarioCompleted, getExecutor(), params));
	}

	protected void addDeleteVolumeCommands(String scenarioId, AtomicBoolean scenarioCompleted, Map<String, Object> params, Queue<ScenarioCommand> commandQueue) {
		commandQueue.add(new DeleteVolumeScenarioCommand(scenarioId, getScenarioListener(), scenarioCompleted, getExecutor(), params));
		commandQueue.add(new CheckVolumeStatusScenarioCommand(scenarioId, getScenarioListener(), scenarioCompleted, getExecutor(), params, "deleted", true));
	}

	protected void addDetachVolumeCommands(String scenarioId, AtomicBoolean scenarioCompleted, Map<String, Object> params, Queue<ScenarioCommand> commandQueue) {
		commandQueue.add(new DetachVolumeScenarioCommand(scenarioId, getScenarioListener(), scenarioCompleted, getExecutor(), params));
		commandQueue.add(new CheckVolumeStatusScenarioCommand(scenarioId, getScenarioListener(), scenarioCompleted, getExecutor(), params, "available"));
	}

	protected void addAttachVolumeCommands(String scenarioId, AtomicBoolean scenarioCompleted, Map<String, Object> params, Queue<ScenarioCommand> commandQueue) {
		commandQueue.add(new AttachVolumeScenarioCommand(scenarioId, getScenarioListener(), scenarioCompleted, getExecutor(), params));
		commandQueue.add(new CheckVolumeStatusScenarioCommand(scenarioId, getScenarioListener(), scenarioCompleted, getExecutor(), params, "in-use", "deleted"));
	}

	protected void addCreateVolumeCommands(String scenarioId, AtomicBoolean scenarioCompleted, Map<String, Object> params, Queue<ScenarioCommand> commandQueue) {
		commandQueue.add(new CreateVolumeScenarioCommand(scenarioId, getScenarioListener(), scenarioCompleted, getExecutor(), params));
		commandQueue.add(new CheckVolumeStatusScenarioCommand(scenarioId, getScenarioListener(), scenarioCompleted, getExecutor(), params, "available", "deleted"));
	}

	protected void addCreateSnapshotCommands(String scenarioId, AtomicBoolean scenarioCompleted, Map<String, Object> params, Queue<ScenarioCommand> commandQueue) {
		commandQueue.add(new CreateSnapshotScenarioCommand(scenarioId, getScenarioListener(), scenarioCompleted, getExecutor(), params));
		commandQueue.add(new CheckSnapshotStatusScenarioCommand(scenarioId, getScenarioListener(), scenarioCompleted, getExecutor(), params, "complete", "deleted"));
	}

	protected void addRestoreFromSnapshotCommands(String scenarioId, AtomicBoolean scenarioCompleted, Map<String, Object> params, Queue<ScenarioCommand> commandQueue) {
		commandQueue.add(new CreateVolumeScenarioCommand(scenarioId, getScenarioListener(), scenarioCompleted, getExecutor(), params));
		commandQueue.add(new CheckVolumeStatusScenarioCommand(scenarioId, getScenarioListener(), scenarioCompleted, getExecutor(), params, "available", "deleted"));
	}

	protected void addDeleteSnapshotCommands(String scenarioId, AtomicBoolean scenarioCompleted, Map<String, Object> params, Queue<ScenarioCommand> commandQueue) {
		commandQueue.add(new DeleteSnapshotScenarioCommand(scenarioId, getScenarioListener(), scenarioCompleted, getExecutor(), params));
	}

	protected void addBundleAndUploadImageCommands(ScenarioRunDetails runDetails, ImageDetails imageDetails, String kernelParamsKey, String ramdiskParamsKey) {
		imageDetails.setBundledDirectory(generateTempDir());

		runDetails.getCommandQueue().add(
				new BundleImageScenarioCommand(runDetails.getScenarioId(), getScenarioListener(), runDetails.getScenarioCompleted(), runDetails.getExecutor(), runDetails.getParams(), imageDetails
						.getLocalImagePath(), imageDetails.getImageType(), imageDetails.getBundledDirectory(), kernelParamsKey, ramdiskParamsKey));
		runDetails.getCommandQueue().add(
				new UploadBundleScenarioCommand(runDetails.getScenarioId(), getScenarioListener(), runDetails.getScenarioCompleted(), runDetails.getExecutor(), runDetails.getParams(), imageDetails
						.getLocalManifestPath(), imageDetails.getImageBucket(), imageDetails.getImageFileName()));
	}

	protected void addRegisterKernelRamdiskCommand(ScenarioRunDetails runDetails, String manifestLocationKey, String usernameParamsKey, String imageIdKey, RegistrationType registrationType) {
		runDetails.getCommandQueue().add(new RegisterKernelRamdiskScenarioCommand(runDetails, manifestLocationKey, usernameParamsKey, imageIdKey, registrationType));
	}

	protected void addBundleUploadAndRegisterKernelOrRamdiskCommands(ScenarioRunDetails runDetails, ImageDetails imageDetails, String ramdiskParamsKey, String kernelParamsKey,
			String usernameParamsKey, String imagePath) {
		String manifestLocationKey = generateRandomString();
		runDetails.getParams().put(manifestLocationKey, imageDetails.getManifestLocationOnServer());

		addBundleAndUploadImageCommands(runDetails, imageDetails, kernelParamsKey, ramdiskParamsKey);
		addRegisterKernelRamdiskCommand(runDetails, manifestLocationKey, usernameParamsKey, kernelParamsKey, RegistrationType.valueOf(imageDetails.getImageType()));
		runDetails.getCommandQueue().add(new CheckImageStatusScenarioCommand(kernelParamsKey, runDetails, "AVAILABLE"));
	}

	protected void addBundleUploadAndRegisterKernelAndImage(ScenarioRunDetails runDetails, ImageDetails kernelDetails, ImageDetails imageDetails, String ramdiskParamsKey, String kernelParamsKey,
			String usernameParamsKey) {
		addBundleUploadAndRegisterKernelOrRamdiskCommands(runDetails, kernelDetails, ramdiskParamsKey, kernelParamsKey, usernameParamsKey, kernelDetails.getImageDirectory());
		addBundleAndUploadImageCommands(runDetails, imageDetails, kernelParamsKey, ramdiskParamsKey);

		runDetails.getCommandQueue().add(new RegisterBundleScenarioCommand(runDetails, imageDetails.getManifestLocationOnServer()));
		runDetails.getCommandQueue().add(new CheckImageStatusScenarioCommand(runDetails, "AVAILABLE"));
	}

	protected static String generateRandomString() {
		return UUID.randomUUID().toString();
	}

	private String generateTempDir() {
		try {
			File temp = File.createTempFile("img-", ".tmp");
			temp.delete();
			temp.mkdir();
			log.debug("Created temporary file " + temp.getAbsolutePath());
			return temp.getAbsolutePath();
		} catch (IOException e) {
			throw new RuntimeException("Unable to create temporary directory for bundling.", e);
		}
	}

	protected void addDeregisterAndDeleteImageAndKernel(ScenarioRunDetails runDetails, ImageDetails imageDetails, ImageDetails kernelDetails, String kernelParamsKey, String usernameParamsKey) {
		runDetails.getCommandQueue().add(new DeregisterImagesScenarioCommand(runDetails, imageDetails));
		runDetails.getCommandQueue().add(new DeleteBundleScenarioCommand(runDetails, imageDetails));
		runDetails.getCommandQueue().add(new DeregisterKernelRamdiskScenarioCommand(runDetails, usernameParamsKey, kernelParamsKey, RegistrationType.KERNEL));
		runDetails.getCommandQueue().add(new DeleteBundleScenarioCommand(runDetails, kernelDetails));
	}

}
