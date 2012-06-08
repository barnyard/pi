package com.bt.nia.koala.robustness.commands;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.bt.nia.koala.robustness.parsers.RunInstanceOutputParser;
import com.ragstorooks.testrr.ScenarioListener;

public class RunInstanceScenarioCommand extends Ec2ScenarioCommandBase {
	private static final String RAMDISK_ID = "ramdiskId";
	private static final String KERNEL_ID = "kernelId";
	private static final String IMAGE_ID = "imageId";
	private static final String SECURITY_GROUP = "securityGroup";
	private static final Log log = LogFactory.getLog(RunInstanceScenarioCommand.class);
	private final String imageIdParamsKey;
	private final String kernelIdParamsKey;
	private final String ramdiskIdParamsKey;
	private final String securityGroupParamsKey;

	public RunInstanceScenarioCommand(String scenarioId, ScenarioListener scenarioListener, AtomicBoolean scenarioCompleted, Executor executor, Map<String, Object> params) {
		this(scenarioId, scenarioListener, scenarioCompleted, executor, params, SECURITY_GROUP);
	}

	public RunInstanceScenarioCommand(String scenarioId, ScenarioListener scenarioListener, AtomicBoolean scenarioCompleted, Executor executor, Map<String, Object> params, String securityGroupKey) {
		this(scenarioId, scenarioListener, scenarioCompleted, executor, params, securityGroupKey, IMAGE_ID);
	}

	public RunInstanceScenarioCommand(String scenarioId, ScenarioListener scenarioListener, AtomicBoolean scenarioCompleted, Executor executor, Map<String, Object> params, String securityGroupKey,
			String imageIdKey) {
		this(scenarioId, scenarioListener, scenarioCompleted, executor, params, securityGroupKey, imageIdKey, KERNEL_ID);
	}

	public RunInstanceScenarioCommand(String scenarioId, ScenarioListener scenarioListener, AtomicBoolean scenarioCompleted, Executor executor, Map<String, Object> params, String securityGroupKey,
			String imageIdKey, String kernelIdKey) {
		this(scenarioId, scenarioListener, scenarioCompleted, executor, params, securityGroupKey, imageIdKey, kernelIdKey, RAMDISK_ID);
	}

	public RunInstanceScenarioCommand(String scenarioId, ScenarioListener scenarioListener, AtomicBoolean scenarioCompleted, Executor executor, Map<String, Object> params, String securityGroupKey,
			String imageIdKey, String kernelIdKey, String ramdiskIdKey) {
		super(scenarioId, scenarioListener, scenarioCompleted, executor, params);
		securityGroupParamsKey = (securityGroupKey == null) ? SECURITY_GROUP : securityGroupKey;
		imageIdParamsKey = (imageIdKey == null) ? IMAGE_ID : imageIdKey;
		kernelIdParamsKey = (kernelIdKey == null) ? KERNEL_ID : kernelIdKey;
		ramdiskIdParamsKey = (ramdiskIdKey == null) ? RAMDISK_ID : ramdiskIdKey;
	}

	@Override
	public void execute(Map<String, Object> params) throws Throwable {
		String imageId = (String) params.get(imageIdParamsKey);
		String kernelId = (String) params.get(kernelIdParamsKey);
		String ramdiskId = (String) params.get(ramdiskIdParamsKey);
		String securityGroup = (String) params.get(securityGroupParamsKey);

		log.info(String.format("Trying to run an instance of image %s with kernel %s and ramdisk %s", imageId, kernelId, ramdiskId));

		List<String> output = executeEc2Command("ec2run", getEc2RunArgs(imageId, kernelId, ramdiskId, securityGroup));
		RunInstanceOutputParser runInstanceOutputParser = new RunInstanceOutputParser();
		String instanceId = runInstanceOutputParser.parse(output)[0];

		log.info("Started instance with ID " + instanceId);
		params.put("instanceId", instanceId);
	}

	private String[] getEc2RunArgs(String imageId, String kernelId, String ramdiskId, String securityGroup) {
		List<String> ec2RunArgs = new ArrayList<String>();
		ec2RunArgs.add(imageId);
		ec2RunArgs.add("--kernel");
		ec2RunArgs.add(kernelId);
		if (ramdiskId != null) {
			ec2RunArgs.add("--ramdisk");
			ec2RunArgs.add(ramdiskId);
		}
		if (securityGroup != null) {
			ec2RunArgs.add("-g");
			ec2RunArgs.add(securityGroup);
		}
		String[] args = new String[ec2RunArgs.size()];
		return ec2RunArgs.toArray(args);
	}

	@Override
	protected Ec2ScenarioCommandBase getCompensationCommand() {
		return new TerminateInstanceScenarioCommand(getScenarioId(), null, null, getExecutor(), getParams());
	}
}
