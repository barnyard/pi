package com.bt.nia.koala.robustness.commands;

import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicBoolean;

import com.ragstorooks.testrr.ScenarioListener;

public class DeleteGroupScenarioCommand extends AddDeleteGroupScenarioCommand {
	public DeleteGroupScenarioCommand(String scenarioId, ScenarioListener scenarioListener, AtomicBoolean scenarioCompleted, Executor executor, Map<String, Object> params) {
		super(scenarioId, scenarioListener, scenarioCompleted, executor, params);
	}

	@Override
	public void execute(Map<String, Object> params) throws Throwable {
		String securityGroup = params.get("securityGroup").toString();
		doExecute(params, "ec2delgrp", new String[] { securityGroup });
	}
}
