/* (c) British Telecommunications plc, 2010, All Rights Reserved */
package com.bt.nia.koala.robustness;

import java.io.File;
import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.ws.rs.core.MediaType;

import org.apache.commons.httpclient.Credentials;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpException;
import org.apache.commons.httpclient.UsernamePasswordCredentials;
import org.apache.commons.httpclient.auth.AuthScope;
import org.apache.commons.httpclient.methods.DeleteMethod;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.methods.StringRequestEntity;
import org.apache.commons.io.FileUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class OpsWebsiteAccessor {
	private static final Log LOG = LogFactory.getLog(OpsWebsiteAccessor.class);

	private static final String ROBUSTNESS_OPS_USERNAME = "ops";
	private static final String ROBUSTNESS_OPS_PASSWORD = "b@7ny@rd";
	private static final String USERS_URI = "%s/users";

	private static String getOpsWebsiteUrl() {
		return System.getenv("OPS_URL");
	}

	@SuppressWarnings("unchecked")
	public static PircData createUser(String username) throws IOException {
		// create a temporary directory to store the certs etc.
		final File temp = File.createTempFile("tmp", Long.toString(System.nanoTime()));
		temp.delete();
		temp.mkdir();
		String keyDir = temp.getAbsolutePath();

		HttpClient httpClient = logInClient();

		PostMethod postUser = new PostMethod(String.format(USERS_URI, getOpsWebsiteUrl()));
		String put_post_content = "username=" + username + "&realname=Robustness%20User&email=robustness@cloud21cn.com&enabled=true&maxInstances=30&maxCores=30";
		postUser.setRequestEntity(new StringRequestEntity(put_post_content, MediaType.APPLICATION_FORM_URLENCODED, "UTF-8"));
		int postUserRet = httpClient.executeMethod(postUser);
		if (postUserRet != 201)
			throw new RuntimeException("Created User Returned " + postUserRet);

		String postCertsUrl = String.format(getUserUri(username) + "/certificate", getOpsWebsiteUrl());
		PostMethod postUserCert = new PostMethod(postCertsUrl);
		int postUserCertRet = httpClient.executeMethod(postUserCert);
		if (postUserCertRet != 200)
			throw new RuntimeException("Getting User Cert returned " + postUserCertRet);

		String piCertFile = String.format("%s/pi-robustness-build-cert.zip", keyDir);

		LOG.debug("Saving cert in " + piCertFile);

		FileUtils.writeByteArrayToFile(new File(piCertFile), postUserCert.getResponseBody());

		ZipUtils.unzipFile(piCertFile, keyDir);

		java.util.List<String> pircContents = FileUtils.readLines(new File(keyDir + "/pirc"));

		return getPircData(pircContents, keyDir);
	}

	private static PircData getPircData(java.util.List<String> pircContents, String keyDir) {
		PircData pircData = new PircData(keyDir);

		for (String line : pircContents) {
			if (line.contains("EC2_PRIVATE_KEY=")) {
				pircData.setEc2PrivateKey(match(".*EC2_PRIVATE_KEY=\\$.*\\/(.*)$", line));
				continue;
			} else if (line.contains("EC2_CERT=")) {
				pircData.setEc2Cert(match(".*EC2_CERT=\\$.*\\/(.*)$", line));
				continue;
			} else if (line.contains("PI_CERT=")) {
				pircData.setPiCert(match(".*PI_CERT=.*\\/(.*)$", line));
				continue;
			} else if (line.contains("EC2_ACCESS_KEY=")) {
				pircData.setEc2AccessKey(match(".*EC2_ACCESS_KEY=\\'(.*)\\'", line));
				continue;
			} else if (line.contains("EC2_SECRET_KEY=")) {
				pircData.setEc2SecretKey(match(".*EC2_SECRET_KEY=\\'(.*)\\'", line));
				continue;
			} else if (line.contains("S3_URL=")) {
				pircData.setS3Url(match("^.*S3_URL=(.*)$", line));
				continue;
			} else if (line.contains("EC2_URL=")) {
				pircData.setEc2Url(match("^.*EC2_URL=(.*)$", line));
				continue;
			} else if (line.contains("EC2_USER_ID=")) {
				pircData.setEc2UserId(match("^.*EC2_USER_ID='(.*)'$", line));
				continue;
			}
		}
		return pircData;
	}

	private static String match(String regex, String line) {
		Pattern pattern = Pattern.compile(regex);
		Matcher matcher = pattern.matcher(line);
		if (matcher.matches()) {
			return matcher.group(1);
		}

		return null;
	}

	public static void deleteUserIfExist(String username) throws IOException {
		// send a delete request for the pi-robustness user
		GetMethod getUser = new GetMethod(String.format(getUserUri(username), getOpsWebsiteUrl()));
		getUser.setRequestHeader("accept", MediaType.APPLICATION_JSON);

		HttpClient httpClient = logInClient();

		int getReturnCode = httpClient.executeMethod(getUser);

		if (getReturnCode == 200) {
			// user exists, so delete it
			LOG.debug(String.format("Deleting user " + username, getOpsWebsiteUrl()));
			DeleteMethod deleteUser = new DeleteMethod(String.format(getUserUri(username), getOpsWebsiteUrl()));
			httpClient.executeMethod(deleteUser);
		} else if (getReturnCode == 404) {
			LOG.debug("User " + username + " already deleted.");
		}
	}

	public static String registerKernel(String username, String manifestLocation) throws HttpException, IOException {
		HttpClient httpClient = logInClient();

		PostMethod postKernel = new PostMethod(getKernelsUri(username));
		postKernel.addParameter("image_manifest_location", manifestLocation);
		int returnCode = httpClient.executeMethod(postKernel);
		String response = new String(postKernel.getResponseBody());

		if (returnCode != 200)
			throw new RuntimeException("Kernel registration failed.\n" + response);
		String kernelId = match(".*:\"(.*)\".*", response);
		LOG.debug("Kernel registered with ID " + kernelId);
		return kernelId;
	}

	public static String registerRamdisk(String username, String manifestLocation) throws HttpException, IOException {
		HttpClient httpClient = logInClient();

		PostMethod postRamdisk = new PostMethod(getRamdisksUri(username));
		postRamdisk.addParameter("image_manifest_location", manifestLocation);
		int returnCode = httpClient.executeMethod(postRamdisk);
		String response = new String(postRamdisk.getResponseBody());

		if (returnCode != 200)
			throw new RuntimeException("Ramdisk registration failed.\n" + response);

		return match(".*:\"(.*)\".*", response);
	}

	public static void deleteRamdisk(String username, String ramdiskId) throws HttpException, IOException {
		HttpClient httpClient = logInClient();

		DeleteMethod deleteRamdisk = new DeleteMethod(getRamdisksUri(username) + "/" + ramdiskId);

		int returnCode = httpClient.executeMethod(deleteRamdisk);

		if (returnCode != 200)
			throw new RuntimeException("Ramdisk deletion failed.\n" + new String(deleteRamdisk.getResponseBody()));

	}

	public static void deleteKernel(String username, String kernelId) throws HttpException, IOException {
		HttpClient httpClient = logInClient();

		DeleteMethod deleteKernel = new DeleteMethod(getKernelsUri(username) + "/" + kernelId);

		int returnCode = httpClient.executeMethod(deleteKernel);

		if (returnCode != 200)
			throw new RuntimeException("Kernel deletion failed.\n" + new String(deleteKernel.getResponseBody()));

	}

	private static String getUserUri(String username) {
		return String.format(USERS_URI + "/%s", getOpsWebsiteUrl(), username);
	}

	private static String getKernelsUri(String username) {
		return getUserUri(username) + "/kernels";
	}

	private static String getRamdisksUri(String username) {
		return getUserUri(username) + "/ramdisks";
	}

	private static HttpClient logInClient() {
		HttpClient cli = new HttpClient();
		cli.getParams().setAuthenticationPreemptive(true);
		Credentials credentials = new UsernamePasswordCredentials(ROBUSTNESS_OPS_USERNAME, ROBUSTNESS_OPS_PASSWORD);
		cli.getState().setCredentials(AuthScope.ANY, credentials);
		return cli;
	}
}
