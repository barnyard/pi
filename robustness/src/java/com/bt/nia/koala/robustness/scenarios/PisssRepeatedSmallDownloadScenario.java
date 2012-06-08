package com.bt.nia.koala.robustness.scenarios;

import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.bt.nia.koala.robustness.PircData;
import com.bt.nia.koala.robustness.commands.ScenarioRunDetails;
import com.bt.nia.koala.robustness.commands.s3.S3CreateBucketScenarioCommand;
import com.bt.nia.koala.robustness.commands.s3.S3DeleteBucketScenarioCommand;
import com.bt.nia.koala.robustness.commands.s3.S3DeleteObjectFromBucketScenarioCommand;
import com.bt.nia.koala.robustness.commands.s3.S3GetObjectFromBucketScenarioCommand;
import com.bt.nia.koala.robustness.commands.s3.S3ListBucketsCommand;
import com.bt.nia.koala.robustness.commands.s3.S3PutObjectInBucketScenarioCommand;
import com.ragstorooks.testrr.ScenarioCommand;

public class PisssRepeatedSmallDownloadScenario extends PisssScenarioBase {
	private static final Log LOG = LogFactory.getLog(PisssRepeatedSmallDownloadScenario.class);
	private static final int NUMBER_OF_GETS = Integer.parseInt(System.getProperty("number_gets", "50"));

	public PisssRepeatedSmallDownloadScenario(ScheduledExecutorService executor, PircData aPircData) {
		super(executor, aPircData);
	}

	@Override
	protected void addCommands(final String scenarioId, final AtomicBoolean scenarioCompleted, Map<String, Object> params, Queue<ScenarioCommand> commandQueue) {
		setupCerts(pircData, params);

		ScenarioRunDetails runDetails = new ScenarioRunDetails(scenarioId, scenarioCompleted, getExecutor(), params, commandQueue, getScenarioListener());

		// setup
		commandQueue.add(new S3ListBucketsCommand(runDetails));
		commandQueue.add(new S3CreateBucketScenarioCommand(runDetails));
		commandQueue.add(new S3ListBucketsCommand(runDetails));
		commandQueue.add(new S3PutObjectInBucketScenarioCommand(runDetails, "50K"));

		LOG.debug("Number of gets:" + NUMBER_OF_GETS);

		// run
		for (int i = 0; i < NUMBER_OF_GETS; i++)
			commandQueue.add(new S3GetObjectFromBucketScenarioCommand(runDetails));

		// clean up
		commandQueue.add(new S3DeleteObjectFromBucketScenarioCommand(runDetails));
		commandQueue.add(new S3DeleteBucketScenarioCommand(runDetails));
		commandQueue.add(new S3ListBucketsCommand(runDetails));
	}
}
