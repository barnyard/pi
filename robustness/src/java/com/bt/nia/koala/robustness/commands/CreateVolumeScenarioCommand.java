package com.bt.nia.koala.robustness.commands;

import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.bt.nia.koala.robustness.parsers.CreateVolumeOutputParser;
import com.ragstorooks.testrr.ScenarioListener;

public class CreateVolumeScenarioCommand extends Ec2ScenarioCommandBase {
	private static final Log LOG = LogFactory.getLog(CreateVolumeScenarioCommand.class);

	public CreateVolumeScenarioCommand(String scenarioId, ScenarioListener scenarioListener, AtomicBoolean scenarioCompleted, Executor executor, Map<String, Object> params) {
		super(scenarioId, scenarioListener, scenarioCompleted, executor, params);
	}

	@Override
	public void execute(Map<String, Object> params) throws Throwable {
		String availabilityZone = System.getenv("KOALA_AVAILABILITY_ZONE");
		String size = "1";
		LOG.info(String.format("Trying to create a %sGB volume in the %s zone", size, availabilityZone));

		String[] arguments = new String[] { "-s" + size, "-z" + availabilityZone };
		if (params.get("snapshotId") != null)
			arguments = new String[] { "--snapshot " + params.get("snapshotId").toString(), "-z" + availabilityZone };
		List<String> output = executeEc2Command("ec2-create-volume", arguments);

		CreateVolumeOutputParser createVolumeOutputParser = params.get("snapshotId") != null ? new CreateVolumeOutputParser(availabilityZone, true) : new CreateVolumeOutputParser(availabilityZone);
		String volumeId = createVolumeOutputParser.parse(output)[0];

		LOG.debug("Created a volume with id" + volumeId);
		params.put("volumeId", volumeId);
		params.put("isFromSnapshot", params.get("snapshotId") != null);
	}

	@Override
	protected Ec2ScenarioCommandBase getCompensationCommand() {
		return new DeleteVolumeScenarioCommand(getScenarioId(), null, null, getExecutor(), getParams());
	}
}