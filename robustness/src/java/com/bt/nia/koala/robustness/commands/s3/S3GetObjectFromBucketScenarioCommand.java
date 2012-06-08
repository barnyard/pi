package com.bt.nia.koala.robustness.commands.s3;

import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jets3t.service.model.S3Bucket;
import org.jets3t.service.model.S3Object;

import com.bt.nia.koala.robustness.commands.ScenarioRunDetails;

public class S3GetObjectFromBucketScenarioCommand extends S3BaseScenarioCommand {

	private static final Log LOG = LogFactory.getLog(S3GetObjectFromBucketScenarioCommand.class);

	public S3GetObjectFromBucketScenarioCommand(ScenarioRunDetails runDetails) {
		super(runDetails);
	}

	@Override
	protected void execute(Map<String, Object> params) throws Throwable {
		super.execute(params);
		S3Bucket bucket = new S3Bucket((String) params.get("bucketName"));
		String objectKey = (String) params.get("objectKey");
		LOG.debug("Getting object " + (String) params.get("bucketName") + "/" + objectKey);
		S3Object result = getWalrusService().getObject(bucket, objectKey);
		PisssTestObject testObject = (PisssTestObject) params.get("testObject");
		byte[] resultDigest = testObject.calculateMD5(result.getDataInputStream());
		if (!new String(resultDigest).equals(new String(testObject.getDigest()))) {
			throw new RuntimeException("Uploaded and downloaded files have different digests");
		}
	}
}
