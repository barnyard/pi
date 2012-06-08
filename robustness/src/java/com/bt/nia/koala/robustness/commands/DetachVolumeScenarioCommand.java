package com.bt.nia.koala.robustness.commands;

import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.bt.nia.koala.robustness.parsers.DetachVolumeOutputParser;
import com.ragstorooks.testrr.ScenarioListener;

public class DetachVolumeScenarioCommand extends Ec2ScenarioCommandBase {
	private static final Log log = LogFactory.getLog(DetachVolumeScenarioCommand.class);

	public DetachVolumeScenarioCommand(String scenarioId, ScenarioListener scenarioListener, AtomicBoolean scenarioCompleted, Executor executor, Map<String, Object> params) {
		super(scenarioId, scenarioListener, scenarioCompleted, executor, params);
	}

	@Override
	public void execute(Map<String, Object> params) throws Throwable {
		String volumeId = params.get("volumeId").toString();
		log.info("Attempting to detach volume " + volumeId);

		List<String> output = executeEc2Command("ec2-detach-volume", new String[] { volumeId });
		DetachVolumeOutputParser detachVolumeOutputParser = new DetachVolumeOutputParser();
		String detachedVolumeId = detachVolumeOutputParser.parse(output)[0];
		if (!volumeId.equals(detachedVolumeId))
			throw new RuntimeException(String.format("Expected vol id %s to be detached, but got %s", volumeId, detachedVolumeId));
	}
}