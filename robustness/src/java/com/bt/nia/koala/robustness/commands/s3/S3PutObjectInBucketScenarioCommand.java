package com.bt.nia.koala.robustness.commands.s3;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jets3t.service.model.S3Object;

import com.bt.nia.koala.robustness.commands.Ec2ScenarioCommandBase;
import com.bt.nia.koala.robustness.commands.ScenarioRunDetails;

public class S3PutObjectInBucketScenarioCommand extends S3BaseScenarioCommand {
	private static final Log LOG = LogFactory.getLog(S3PutObjectInBucketScenarioCommand.class);
	private ScenarioRunDetails runDetails;
	private String objectValue;

	public S3PutObjectInBucketScenarioCommand(ScenarioRunDetails runDetails, String objectSize) {
		super(runDetails);
		this.runDetails = runDetails;
		this.objectValue = objectSize;
	}

	@Override
	protected void execute(Map<String, Object> params) throws Throwable {
		super.execute(params);
		String objectKey = UUID.randomUUID().toString().replace("-", "");
		String bucketName = (String) params.get("bucketName");
		LOG.debug("Putting object of size " + objectValue + " into bucket " + bucketName + " with key " + objectKey);
		final PisssTestObject pisssTestObject = new PisssTestObject(objectValue);
		params.put("objectValue", objectValue);
		params.put("testObject", pisssTestObject);
		addObjectKeyToParams(params, objectKey);
		S3Object s3Obj = new S3Object(objectValue);
		s3Obj.setKey(objectKey);
		s3Obj.setDataInputStream(pisssTestObject.getInputStream());
		s3Obj.setContentLength(pisssTestObject.getSizeInBytes());
		getWalrusService().putObject(bucketName, s3Obj);
	}

	@Override
	protected Ec2ScenarioCommandBase getCompensationCommand() {
		return new S3DeleteBucketScenarioCommand(runDetails);
	}

	@SuppressWarnings("unchecked")
	private void addObjectKeyToParams(Map<String, Object> params, String objectKey) {
		params.put("objectKey", objectKey);
		List<String> objectKeys = (List<String>) params.get("objectKeys");
		if (objectKeys == null)
			objectKeys = new ArrayList<String>();
		objectKeys.add(objectKey);
		params.put("objectKeys", objectKeys);
	}
}
