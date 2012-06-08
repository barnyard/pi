package com.bt.nia.koala.robustness.commands.s3;

import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jets3t.service.S3ServiceException;

import com.bt.nia.koala.robustness.commands.ScenarioRunDetails;

public class S3DeleteBucketScenarioCommand extends S3BaseScenarioCommand {

	private static final Log LOG = LogFactory.getLog(S3DeleteBucketScenarioCommand.class);

	public S3DeleteBucketScenarioCommand(ScenarioRunDetails runDetails) {
		super(runDetails);
	}

	@Override
	protected void execute(Map<String, Object> params) throws Throwable {
		super.execute(params);
		String bucketName = (String) params.get("bucketName");
		LOG.debug("Deleting bucket " + bucketName);
		try {
			getWalrusService().deleteBucket(bucketName);
		} catch (S3ServiceException ex) {
			if (ex.getS3ErrorCode().contains("204")) {
				LOG.info("204 status code received, carrying on");
			} else if (ex.getS3ErrorCode().contains("404")) {
				LOG.info("404 status code received, carrying on");
			} else {
				throw ex;
			}
		}
	}
}
