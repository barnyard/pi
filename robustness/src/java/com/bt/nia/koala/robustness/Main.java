package com.bt.nia.koala.robustness;

import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ScheduledThreadPoolExecutor;

import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import com.bt.nia.koala.robustness.scenarios.InstanceStartStopScenarioWithAccess;
import com.bt.nia.koala.robustness.scenarios.InstanceStartStopScenarioWithAddressing;
import com.bt.nia.koala.robustness.scenarios.InstanceStartStopScenarioWithBundling;
import com.bt.nia.koala.robustness.scenarios.PisssBasicScenario;
import com.bt.nia.koala.robustness.scenarios.SnapshotScenario;
import com.bt.nia.koala.robustness.scenarios.VolumeScenario;
import com.ragstorooks.testrr.Runner;
import com.ragstorooks.testrr.ScenarioBase;
import com.ragstorooks.testrr.ScenarioResult;

public class Main {

	private static final String ROBUSTNESS_USER = "robustnessbuild";

	public static void main(String[] args) throws IOException {
		ApplicationContext applicationContext = new ClassPathXmlApplicationContext("classpath:applicationContext.xml");

		Runner runner = (Runner) applicationContext.getBean("scenarioRunner");
		ScheduledThreadPoolExecutor scheduledThreadPoolExecutor = (ScheduledThreadPoolExecutor) applicationContext.getBean("scheduledThreadPoolExecutor");

		System.setProperty("javax.net.ssl.trustStore", "etc/ssl_keystore");
		System.setProperty("javax.net.ssl.trustStorePassword", "badgermonkeyfish");

		// create user, get zip file, extract
		OpsWebsiteAccessor.deleteUserIfExist(ROBUSTNESS_USER);
		PircData pircData = OpsWebsiteAccessor.createUser(ROBUSTNESS_USER);

		System.out.println(pircData);

		Map<ScenarioBase, Integer> scenarioWeightings = new HashMap<ScenarioBase, Integer>();

		scenarioWeightings.put(new InstanceStartStopScenarioWithAddressing(scheduledThreadPoolExecutor, pircData), 1);
		scenarioWeightings.put(new InstanceStartStopScenarioWithAccess(scheduledThreadPoolExecutor, pircData), 1);
		scenarioWeightings.put(new InstanceStartStopScenarioWithBundling(scheduledThreadPoolExecutor, pircData), 1);
		if ("yes".equals(System.getenv("RUN_SNAPSHOT")))
			scenarioWeightings.put(new SnapshotScenario(scheduledThreadPoolExecutor, pircData), 1);
		else
			scenarioWeightings.put(new VolumeScenario(scheduledThreadPoolExecutor, pircData), 1);
		scenarioWeightings.put(new PisssBasicScenario(scheduledThreadPoolExecutor, pircData), 1);

		runner.setNumberOfRuns(scenarioWeightings.size());
		runner.setScenarioWeightings(scenarioWeightings);
		runner.run();

		Map<String, ScenarioResult> failures = runner.getScenarioFailures();
		Iterator<?> iterator = failures.keySet().iterator();
		while (iterator.hasNext()) {
			String scenarioId = (String) iterator.next();
			ScenarioResult result = failures.get(scenarioId);
			System.out.println(String.format("Scenario %s(%s) failed because: %s", result.getScenarioType().getSimpleName(), scenarioId, result.getMessage()));
		}
		System.out.println(String.format("Total time taken to run scenarios: %d milliseconds", runner.getTotalRunTimeMilliSeconds()));
		System.out.println(String.format("Success rate overall: %f percent", runner.getSuccessRate()));

		SecurityManager sm = System.getSecurityManager();
		System.setSecurityManager(null);

		try {
			scheduledThreadPoolExecutor.shutdownNow();
		} finally {
			System.setSecurityManager(sm);
		}

		if (failures.size() > 0)
			System.exit(1);
		else
			System.exit(0);
	}
}
