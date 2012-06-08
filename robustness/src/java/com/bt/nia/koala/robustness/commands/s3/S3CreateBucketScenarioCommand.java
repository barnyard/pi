package com.bt.nia.koala.robustness.commands.s3;

import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.bt.nia.koala.robustness.commands.Ec2ScenarioCommandBase;
import com.bt.nia.koala.robustness.commands.ScenarioRunDetails;

public class S3CreateBucketScenarioCommand extends S3BaseScenarioCommand {
	private static final Log LOG = LogFactory.getLog(S3CreateBucketScenarioCommand.class);
	private ScenarioRunDetails runDetails;

	public S3CreateBucketScenarioCommand(ScenarioRunDetails runDetails) {
		super(runDetails);
		this.runDetails = runDetails;
	}

	@Override
	protected void execute(Map<String, Object> params) throws Throwable {
		super.execute(params);
		final String bucketName = getBucketName();
		LOG.debug("Creating bucket " + bucketName);
		getWalrusService().getOrCreateBucket(bucketName);
		params.put("bucketName", bucketName);
	}

	@Override
	protected Ec2ScenarioCommandBase getCompensationCommand() {
		return new S3DeleteBucketScenarioCommand(runDetails);
	}
}
