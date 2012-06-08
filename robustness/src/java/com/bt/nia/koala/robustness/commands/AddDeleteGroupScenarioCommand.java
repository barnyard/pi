/* (c) British Telecommunications plc, 2010, All Rights Reserved */
package com.bt.nia.koala.robustness.commands;

import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.bt.nia.koala.robustness.parsers.AddDeleteGroupOutputParser;
import com.ragstorooks.testrr.ScenarioListener;

public abstract class AddDeleteGroupScenarioCommand extends Ec2ScenarioCommandBase {

	private static final Log log = LogFactory.getLog(AddDeleteGroupScenarioCommand.class);

	public AddDeleteGroupScenarioCommand(String scenarioId, ScenarioListener scenarioListener, AtomicBoolean scenarioCompleted, Executor executor, Map<String, Object> params) {
		super(scenarioId, scenarioListener, scenarioCompleted, executor, params);
	}

	protected void doExecute(Map<String, Object> params, String command, String[] commandArgs) throws Throwable {
		String securityGroup = params.get("securityGroup").toString();
		log.info(String.format("Trying to perform %s with group %s", command, securityGroup));

		List<String> output = executeEc2Command(command, commandArgs);
		AddDeleteGroupOutputParser outputParser = new AddDeleteGroupOutputParser();
		String groupId = outputParser.parse(output)[0];
		if (!groupId.equals(securityGroup))
			throw new RuntimeException(String.format("Expected group '%s' but got '%s' when doing %s", securityGroup, groupId, command));
	}

}