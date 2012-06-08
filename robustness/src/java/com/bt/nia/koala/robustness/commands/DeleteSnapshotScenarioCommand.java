package com.bt.nia.koala.robustness.commands;

import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.bt.nia.koala.robustness.parsers.DeleteSnapshotOutputParser;
import com.ragstorooks.testrr.ScenarioListener;

public class DeleteSnapshotScenarioCommand extends Ec2ScenarioCommandBase {
	private static final Log log = LogFactory.getLog(DeleteSnapshotScenarioCommand.class);

	public DeleteSnapshotScenarioCommand(String scenarioId, ScenarioListener scenarioListener, AtomicBoolean scenarioCompleted, Executor executor, Map<String, Object> params) {
		super(scenarioId, scenarioListener, scenarioCompleted, executor, params);
	}

	@Override
	public void execute(Map<String, Object> params) throws Throwable {
		String snapshotId = params.get("snapshotId").toString();
		log.info("Trying to delete snapshot" + snapshotId);

		List<String> output = executeEc2Command("ec2-delete-snapshot", new String[] { snapshotId });
		DeleteSnapshotOutputParser deleteSnapshotOutputParser = new DeleteSnapshotOutputParser();
		String deletedSnapshotId = deleteSnapshotOutputParser.parse(output)[0];
		if (!snapshotId.equals(deletedSnapshotId))
			throw new RuntimeException(String.format("Expected snapshot id %s to be deleted, but got %s", snapshotId, deletedSnapshotId));
	}
}