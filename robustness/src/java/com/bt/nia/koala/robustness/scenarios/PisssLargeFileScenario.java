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

public class PisssLargeFileScenario extends PisssScenarioBase {
	private static final String FILE_SIZE = "10M";

	public PisssLargeFileScenario(ScheduledExecutorService executor, PircData aPircData) {
		super(executor, aPircData);
	}

	@Override
	protected void addCommands(final String scenarioId, final AtomicBoolean scenarioCompleted, Map<String, Object> params, Queue<ScenarioCommand> commandQueue) {
		setupCerts(pircData, params);

		ScenarioRunDetails runDetails = new ScenarioRunDetails(scenarioId, scenarioCompleted, getExecutor(), params, commandQueue, getScenarioListener());

		commandQueue.add(new S3ListBucketsCommand(runDetails));
		commandQueue.add(new S3CreateBucketScenarioCommand(runDetails));
		commandQueue.add(new S3ListBucketsCommand(runDetails));
		commandQueue.add(new S3PutObjectInBucketScenarioCommand(runDetails, FILE_SIZE));
		commandQueue.add(new S3GetObjectFromBucketScenarioCommand(runDetails));
		commandQueue.add(new S3DeleteObjectFromBucketScenarioCommand(runDetails));
		commandQueue.add(new S3DeleteBucketScenarioCommand(runDetails));
		commandQueue.add(new S3ListBucketsCommand(runDetails));
	}
}
