package com.bt.nia.koala.robustness.commands;

import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicBoolean;

import com.ragstorooks.testrr.ScenarioListener;

public class AddGroupScenarioCommand extends AddDeleteGroupScenarioCommand {
	public AddGroupScenarioCommand(String scenarioId, ScenarioListener scenarioListener, AtomicBoolean scenarioCompleted, Executor executor, Map<String, Object> params) {
		super(scenarioId, scenarioListener, scenarioCompleted, executor, params);
	}

	@Override
	public void execute(Map<String, Object> params) throws Throwable {
		String securityGroup = params.get("securityGroup").toString();
		doExecute(params, "ec2addgrp", new String[] { securityGroup, "-d", "\"robustness group\"" });
	}

	@Override
	protected Ec2ScenarioCommandBase getCompensationCommand() {
		return new DeleteGroupScenarioCommand(getScenarioId(), null, null, getExecutor(), getParams());
	}
}
