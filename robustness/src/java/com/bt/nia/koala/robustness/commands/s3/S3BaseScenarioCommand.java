package com.bt.nia.koala.robustness.commands.s3;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Map;
import java.util.UUID;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jets3t.service.Constants;
import org.jets3t.service.Jets3tProperties;
import org.jets3t.service.S3Service;
import org.jets3t.service.impl.rest.httpclient.RestS3Service;
import org.jets3t.service.security.AWSCredentials;

import com.bt.nia.koala.robustness.commands.Ec2ScenarioCommandBase;
import com.bt.nia.koala.robustness.commands.ScenarioRunDetails;

public class S3BaseScenarioCommand extends Ec2ScenarioCommandBase {

	private static final Log LOG = LogFactory.getLog(S3BaseScenarioCommand.class);
	private String httpProxyHost;
	private String httpProxyPort;
	private String s3Url;
	private String s3Hostname;
	private S3Service walrusService;
	private static final String BUCKET_NAME = "footestbucket";

	public S3BaseScenarioCommand(ScenarioRunDetails runDetails) {
		super(runDetails);
	}

	@Override
	protected void execute(Map<String, Object> params) throws Throwable {
		httpProxyHost = System.getenv("HTTP_PROXY_HOST");
		httpProxyPort = System.getenv("HTTP_PROXY_PORT");
		String accessKey = (String) params.get("EC2_ACCESS_KEY");
		String secretKey = (String) params.get("EC2_SECRET_KEY");

		s3Url = (String) params.get("S3_URL");
		s3Hostname = s3Url.substring(s3Url.indexOf("://") + 3, s3Url.indexOf(":", 8));

		String jets3tPropertiesFile = "src/properties/jets3t.properties";
		int i = 0;
		boolean success = false;
		while (!success) {
			try {
				Jets3tProperties.getInstance(Constants.JETS3T_PROPERTIES_FILENAME).loadAndReplaceProperties(new FileInputStream(jets3tPropertiesFile), "jets3t.properties in Cockpit's home folder ");
				success = true;
			} catch (IOException e) {
				if (i < 5) {
					i++;
					Thread.sleep(1000);
					LOG.warn("Unable to load jets3t.properties file on attempt " + (i + 1) + ". Will retry...");
				} else {
					throw new RuntimeException("Unable to load jets3t.properties file: " + jets3tPropertiesFile, e);
				}
			}
		}

		Jets3tProperties.getInstance(Constants.JETS3T_PROPERTIES_FILENAME).setProperty("s3service.s3-endpoint", s3Hostname);
		if (httpProxyHost != null && httpProxyPort != null) {
			Jets3tProperties.getInstance(Constants.JETS3T_PROPERTIES_FILENAME).setProperty("httpclient.proxy-host", httpProxyHost);
			Jets3tProperties.getInstance(Constants.JETS3T_PROPERTIES_FILENAME).setProperty("httpclient.proxy-port", httpProxyPort);
		}

		AWSCredentials awsCredentials = new AWSCredentials(accessKey, secretKey);
		walrusService = new RestS3Service(awsCredentials);
	}

	public S3Service getWalrusService() {
		return walrusService;
	}

	protected String getBucketName() {
		return "pisssrobustness" + UUID.randomUUID().toString().replace("-", "");
	}

}
