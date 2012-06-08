package com.bt.nia.koala.robustness.commands;

import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.ragstorooks.testrr.ScenarioCommandBase;
import com.ragstorooks.testrr.ScenarioListener;

public class WaitScenarioCommand extends ScenarioCommandBase {

	private static final String SLEEP = "sleep";
	private static final Log LOG = LogFactory.getLog(WaitScenarioCommand.class);
	private long sleep = -1;

	public WaitScenarioCommand(String arg0, ScenarioListener arg1, AtomicBoolean arg2, Executor arg3, Map<String, Object> arg4) {
		super(arg0, arg1, arg2, arg3, arg4);
	}

	public WaitScenarioCommand(String arg0, ScenarioListener arg1, AtomicBoolean arg2, Executor arg3, Map<String, Object> arg4, long sleep) {
		this(arg0, arg1, arg2, arg3, arg4);
		this.sleep = sleep;
	}

	@Override
	protected void cleanup(Map<String, Object> arg0) throws Throwable {

	}

	@Override
	protected void execute(Map<String, Object> params) throws Throwable {
		if (sleep == -1) {
			if (params.containsKey(SLEEP)) {
				sleep = Long.parseLong((String) params.get(SLEEP));
			} else {
				sleep = 0;
			}
		}
		LOG.info(String.format("Going to sleep for %s milliseconds", Long.toString(sleep)));
		Thread.sleep(sleep);
	}

	@Override
	protected ScenarioCommandBase getCompensationCommand() {
		return null;
	}

	public long getDelayMillis() {
		return 0;
	}

}
