/* (c) British Telecommunications plc, 2010, All Rights Reserved */
package com.bt.nia.koala.robustness.commands.s3;

import java.io.IOException;
import java.io.InputStream;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class PisssTestObject {
	private long sizeBytes;
	private byte[] digestBytes;

	public PisssTestObject(String size) {
		sizeBytes = convertToBytes(size);
		digestBytes = calculateMD5(new SizedInputStream(sizeBytes));
	}

	public byte[] getDigest() {
		return digestBytes;
	}

	public long getSizeInBytes() {
		return sizeBytes;
	}

	public InputStream getInputStream() {
		return new SizedInputStream(sizeBytes);
	}

	public byte[] calculateMD5(InputStream is) {
		MessageDigest digest;
		try {
			digest = MessageDigest.getInstance("MD5");
			is = new DigestInputStream(is, digest);
			while (is.read() != -1) {
			}
			return digest.digest();
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return new byte[0];
	}

	private long convertToBytes(String value) {
		String modifier = value.substring(value.length() - 1, value.length());
		long number = Long.parseLong(value.substring(0, value.length() - 1));
		if (modifier.equalsIgnoreCase("B")) {
			return number;
		} else if (modifier.equalsIgnoreCase("K")) {
			return number * 1024;
		} else if (modifier.equalsIgnoreCase("M")) {
			return number * 1024 * 1024;
		} else if (modifier.equalsIgnoreCase("G")) {
			return number * 1024 * 1024 * 1024;
		}

		return 0;
	}
}
