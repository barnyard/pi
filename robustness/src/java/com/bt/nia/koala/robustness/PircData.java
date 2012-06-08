package com.bt.nia.koala.robustness;

public class PircData {

	private String s3Url;
	private String ec2Url;
	private String ec2PrivateKey;
	private String ec2Cert;
	private String ec2AccessKey;
	private String ec2SecretKey;
	private String ec2UserId;
	private String piCert;
	private String keyDir;

	public PircData(String theKeyDir) {
		keyDir = theKeyDir;
	}

	public String getKeyDir() {
		return keyDir;
	}

	public void setKeyDir(String theKeyDir) {
		keyDir = theKeyDir;
	}

	public String getS3Url() {
		return s3Url;
	}

	public void setS3Url(String s3Url) {
		this.s3Url = s3Url;
	}

	public String getEc2Url() {
		return ec2Url;
	}

	public void setEc2Url(String ec2Url) {
		this.ec2Url = ec2Url;
	}

	public String getEc2PrivateKey() {
		return ec2PrivateKey;
	}

	public void setEc2PrivateKey(String ec2PrivateKey) {
		this.ec2PrivateKey = String.format("%s/%s", getKeyDir(), ec2PrivateKey);
	}

	public String getEc2Cert() {
		return ec2Cert;
	}

	public void setEc2Cert(String ec2Cert) {
		this.ec2Cert = String.format("%s/%s", getKeyDir(), ec2Cert);
	}

	public String getEc2AccessKey() {
		return ec2AccessKey;
	}

	public void setEc2AccessKey(String ec2AccessKey) {
		this.ec2AccessKey = ec2AccessKey;
	}

	public String getEc2SecretKey() {
		return ec2SecretKey;
	}

	public void setEc2SecretKey(String ec2SecretKey) {
		this.ec2SecretKey = ec2SecretKey;
	}

	public String getEc2UserId() {
		return ec2UserId;
	}

	public void setEc2UserId(String ec2UserId) {
		this.ec2UserId = ec2UserId;
	}

	public void setPiCert(String piCert) {
		this.piCert = String.format("%s/%s", getKeyDir(), piCert);
	}

	public String getPiCert() {
		return piCert;
	}

	@Override
	public String toString() {
		return "PircData [ec2AccessKey=" + ec2AccessKey + ", ec2Cert=" + ec2Cert + ", ec2PrivateKey=" + ec2PrivateKey + ", ec2SecretKey=" + ec2SecretKey + ", ec2Url=" + ec2Url + ", ec2UserId="
				+ ec2UserId + ", piCert=" + piCert + ", s3Url=" + s3Url + "]";
	}

}
