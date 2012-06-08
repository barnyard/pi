package com.bt.nia.koala.robustness.commands.s3;

import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jets3t.service.model.S3Bucket;

import com.bt.nia.koala.robustness.commands.ScenarioRunDetails;

public class S3DeleteObjectFromBucketScenarioCommand extends S3BaseScenarioCommand {

	private static final Log LOG = LogFactory.getLog(S3DeleteObjectFromBucketScenarioCommand.class);

	public S3DeleteObjectFromBucketScenarioCommand(ScenarioRunDetails runDetails) {
		super(runDetails);
	}

	@Override
	protected void execute(Map<String, Object> params) throws Throwable {
		super.execute(params);
		LOG.debug("Deleting object from bucket");
		S3Bucket bucket = new S3Bucket((String) params.get("bucketName"));
		String objectKey = (String) params.get("objectKey");
		getWalrusService().deleteObject(bucket, objectKey);
	}
}
