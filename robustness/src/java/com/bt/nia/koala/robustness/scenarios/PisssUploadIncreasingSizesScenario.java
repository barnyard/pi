package com.bt.nia.koala.robustness.scenarios;

import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.atomic.AtomicBoolean;

import com.bt.nia.koala.robustness.PircData;
import com.bt.nia.koala.robustness.commands.ScenarioRunDetails;
import com.bt.nia.koala.robustness.commands.s3.S3CreateBucketScenarioCommand;
import com.bt.nia.koala.robustness.commands.s3.S3DeleteBucketScenarioCommand;
import com.bt.nia.koala.robustness.commands.s3.S3DeleteObjectFromBucketScenarioCommand;
import com.bt.nia.koala.robustness.commands.s3.S3GetObjectFromBucketScenarioCommand;
import com.bt.nia.koala.robustness.commands.s3.S3ListBucketsCommand;
import com.bt.nia.koala.robustness.commands.s3.S3PutObjectInBucketScenarioCommand;
import com.ragstorooks.testrr.ScenarioCommand;

public class PisssUploadIncreasingSizesScenario extends PisssScenarioBase {
	private static final String[] SIZES = { "1K", "500K", "1M", "2M", "5M" };

	public PisssUploadIncreasingSizesScenario(ScheduledExecutorService executor, PircData aPircData) {
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

		// run
		for (String size : SIZES) {
			commandQueue.add(new S3PutObjectInBucketScenarioCommand(runDetails, size));
			commandQueue.add(new S3GetObjectFromBucketScenarioCommand(runDetails));
			commandQueue.add(new S3DeleteObjectFromBucketScenarioCommand(runDetails));
		}

		// clean up
		commandQueue.add(new S3DeleteBucketScenarioCommand(runDetails));
		commandQueue.add(new S3ListBucketsCommand(runDetails));
	}
}
