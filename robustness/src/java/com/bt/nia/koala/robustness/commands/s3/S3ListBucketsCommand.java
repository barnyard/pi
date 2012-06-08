package com.bt.nia.koala.robustness.commands.s3;

import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jets3t.service.model.S3Bucket;

import com.bt.nia.koala.robustness.commands.ScenarioRunDetails;

public class S3ListBucketsCommand extends S3BaseScenarioCommand {
	private static final Log LOG = LogFactory.getLog(S3ListBucketsCommand.class);

	public S3ListBucketsCommand(ScenarioRunDetails runDetails) {
		super(runDetails);
	}

	@Override
	public void execute(Map<String, Object> params) throws Throwable {
		super.execute(params);
		S3Bucket[] myBuckets = getWalrusService().listAllBuckets();
		LOG.debug(String.format("Got %d bucket(s)", myBuckets.length));
		if (myBuckets.length > 0) {
			StringBuilder sb = new StringBuilder("Found buckets: ");
			for (S3Bucket b : myBuckets)
				sb.append(b.getName()).append(" ");
			LOG.debug(sb.toString());
		}
	}

}