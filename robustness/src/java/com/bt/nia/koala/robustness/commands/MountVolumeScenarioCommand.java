package com.bt.nia.koala.robustness.commands;

import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.ragstorooks.testrr.CliScenarioCommandBase;
import com.ragstorooks.testrr.ScenarioListener;

public class MountVolumeScenarioCommand extends CliScenarioCommandBase {
	private static final Log log = LogFactory.getLog(MountVolumeScenarioCommand.class);

	public MountVolumeScenarioCommand(String arg0, ScenarioListener arg1, AtomicBoolean arg2, Executor arg3, Map<String, Object> arg4) {
		super(arg0, arg1, arg2, arg3, arg4);
	}

	@Override
	public void execute(Map<String, Object> params) throws Throwable {
		String publicIpAddress = (String) params.get("ipAddress");
		String username = (String) params.get("curlUsername");
		String password = (String) params.get("curlPassword");
		String cmd = "/sbin/mkfs.ext3 /dev/sdb";

		log.info(String.format("Attempting to mount the attached volume on instance with address %s", publicIpAddress));
		List<String> result = executeCommand("curl", new String[] { "-k", "-u", String.format("%s:%s", username, password), String.format("https://%s/foo/%s", publicIpAddress, cmd) });
	}

}
