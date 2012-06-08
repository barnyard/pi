package com.bt.nia.koala.robustness.commands;

import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.bt.nia.koala.robustness.parsers.AttachVolumeOutputParser;
import com.ragstorooks.testrr.ScenarioListener;

public class AttachVolumeScenarioCommand extends Ec2ScenarioCommandBase {
	private static final Log log = LogFactory.getLog(AttachVolumeScenarioCommand.class);

	public AttachVolumeScenarioCommand(String scenarioId, ScenarioListener scenarioListener, AtomicBoolean scenarioCompleted, Executor executor, Map<String, Object> params) {
		super(scenarioId, scenarioListener, scenarioCompleted, executor, params);
	}

	@Override
	public void execute(Map<String, Object> params) throws Throwable {
		String volumeId = params.get("volumeId").toString();
		String instanceId = params.get("instanceId").toString();
		log.info(String.format("Attaching volume %s to instance %s", volumeId, instanceId));

		List<String> output = executeEc2Command("ec2-attach-volume", new String[] { volumeId, "-i" + instanceId, "-d/dev/sdb" });
		AttachVolumeOutputParser attachVolumeOutputParser = new AttachVolumeOutputParser();
		String attachedVolumeId = attachVolumeOutputParser.parse(output)[0];
		if (!volumeId.equals(attachedVolumeId))
			throw new RuntimeException(String.format("Expected vol id %s to be attached, but got %s", volumeId, attachedVolumeId));
	}

	@Override
	protected Ec2ScenarioCommandBase getCompensationCommand() {
		return new DetachVolumeScenarioCommand(getScenarioId(), null, null, getExecutor(), getParams());
	}
}