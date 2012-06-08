package com.bt.nia.koala.robustness.commands.s3;

import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jets3t.service.model.S3Bucket;

import com.bt.nia.koala.robustness.commands.ScenarioRunDetails;

public class S3DeleteObjectsFromBucketScenarioCommand extends S3BaseScenarioCommand {

	private static final Log LOG = LogFactory.getLog(S3DeleteObjectsFromBucketScenarioCommand.class);

	public S3DeleteObjectsFromBucketScenarioCommand(ScenarioRunDetails runDetails) {
		super(runDetails);
	}

	@SuppressWarnings("unchecked")
	@Override
	protected void execute(Map<String, Object> params) throws Throwable {
		super.execute(params);
		S3Bucket bucket = new S3Bucket((String) params.get("bucketName"));
		List<String> keys = (List<String>) params.get("objectKeys");
		if (keys != null) {
			for (String objectKey : keys) {
				LOG.debug("Deleting object " + objectKey + " from bucket " + bucket.getName());
				getWalrusService().deleteObject(bucket, objectKey);
			}
		}
	}
}
