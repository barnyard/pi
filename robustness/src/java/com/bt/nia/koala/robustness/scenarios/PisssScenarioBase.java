package com.bt.nia.koala.robustness.scenarios;

import java.util.Map;
import java.util.concurrent.ScheduledExecutorService;

import com.bt.nia.koala.robustness.PircData;
import com.ragstorooks.testrr.ScenarioCommanderBase;

public abstract class PisssScenarioBase extends ScenarioCommanderBase {
	protected final PircData pircData;

	public PisssScenarioBase(ScheduledExecutorService executor, PircData aPircData) {
		super(executor);
		this.pircData = aPircData;
	}

	protected void setupCerts(PircData pircData, Map<String, Object> params) {
		params.put("S3_URL", pircData.getS3Url());
		params.put("EC2_URL", pircData.getEc2Url());
		params.put("EC2_PRIVATE_KEY", pircData.getEc2PrivateKey());
		params.put("EC2_CERT", pircData.getEc2Cert());
		params.put("EC2_ACCESS_KEY", pircData.getEc2AccessKey());
		params.put("EC2_SECRET_KEY", pircData.getEc2SecretKey());
		params.put("PI_CERT", pircData.getPiCert());
	}
}
