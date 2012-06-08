package com.bt.nia.koala.robustness.commands;

import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.bt.nia.koala.robustness.parsers.CreateSnapshotOutputParser;
import com.ragstorooks.testrr.ScenarioListener;

public class CreateSnapshotScenarioCommand extends Ec2ScenarioCommandBase {
	private static final Log LOG = LogFactory.getLog(CreateSnapshotScenarioCommand.class);

	public CreateSnapshotScenarioCommand(String scenarioId, ScenarioListener scenarioListener, AtomicBoolean scenarioCompleted, Executor executor, Map<String, Object> params) {
		super(scenarioId, scenarioListener, scenarioCompleted, executor, params);
	}

	@Override
	public void execute(Map<String, Object> params) throws Throwable {
		String volumeId = params.get("volumeId").toString();
		LOG.info(String.format("Creating a snapshot from %s", volumeId));

		List<String> output = executeEc2Command("ec2-create-snapshot", new String[] { volumeId });
		CreateSnapshotOutputParser createSnapshotOutputParser = new CreateSnapshotOutputParser();
		String snapshotId = createSnapshotOutputParser.parse(output)[0];
		LOG.debug("Created a snapshot with id " + snapshotId);
		params.put("snapshotId", snapshotId);
	}

	@Override
	protected Ec2ScenarioCommandBase getCompensationCommand() {
		return new DeleteSnapshotScenarioCommand(getScenarioId(), null, null, getExecutor(), getParams());
	}
}