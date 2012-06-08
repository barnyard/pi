package com.bt.nia.koala.robustness.commands;

import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.bt.nia.koala.robustness.parsers.DeleteVolumeOutputParser;
import com.ragstorooks.testrr.ScenarioListener;

public class DeleteVolumeScenarioCommand extends Ec2ScenarioCommandBase {
	private static final Log log = LogFactory.getLog(DeleteVolumeScenarioCommand.class);

	public DeleteVolumeScenarioCommand(String scenarioId, ScenarioListener scenarioListener, AtomicBoolean scenarioCompleted, Executor executor, Map<String, Object> params) {
		super(scenarioId, scenarioListener, scenarioCompleted, executor, params);
	}

	@Override
	public void execute(Map<String, Object> params) throws Throwable {
		String volumeId = params.get("volumeId").toString();
		log.info("Trying to delete volume " + volumeId);

		List<String> output = executeEc2Command("ec2-delete-volume", new String[] { volumeId });
		DeleteVolumeOutputParser deleteVolumeOutputParser = new DeleteVolumeOutputParser();
		String deletedVolumeId = deleteVolumeOutputParser.parse(output)[0];
		if (!volumeId.equals(deletedVolumeId))
			throw new RuntimeException(String.format("Expected vol id %s to be deleted, but got %s", volumeId, deletedVolumeId));
	}
}