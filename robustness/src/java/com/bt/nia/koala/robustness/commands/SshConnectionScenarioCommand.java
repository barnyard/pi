package com.bt.nia.koala.robustness.commands;

import java.io.ByteArrayOutputStream;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicBoolean;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.rev6.scf.SshCommand;
import org.rev6.scf.SshConnection;
import org.rev6.scf.SshException;

import com.ragstorooks.testrr.ScenarioListener;

public class SshConnectionScenarioCommand extends Ec2ScenarioCommandBase {

	private static final Log LOG = LogFactory.getLog(SshConnectionScenarioCommand.class);

	public SshConnectionScenarioCommand(String scenarioId, ScenarioListener scenarioListener, AtomicBoolean scenarioCompleted, Executor executor, Map<String, Object> params) {
		super(scenarioId, scenarioListener, scenarioCompleted, executor, params);
	}
	
	@Override
	protected void execute(Map<String, Object> params) throws Throwable {
		String ipAddress = params.get("ipAddress").toString();
		String username = params.get("sshUsername").toString();
		String password = params.get("sshPassword").toString();
		String command = params.get("sshCommand").toString();
		SshConnection ssh = new SshConnection(ipAddress , username, password);
		try {
			ssh.connect();
			LOG.info("connected via ssh to " + ipAddress);
			ByteArrayOutputStream gobbler = new ByteArrayOutputStream();
			SshCommand cmd = new SshCommand(command, gobbler);
			ssh.executeTask(cmd);
			LOG.info(String.format("output of '%s': %s", command, gobbler.toString()));
			params.put("sshOutput", gobbler.toString());
		} catch (SshException e) {
			LOG.error(e);
			throw new RuntimeException("Expected to connect to host " + ipAddress);
		} finally {
		   if (ssh != null)
	        ssh.disconnect();
		}
	}
	
}
