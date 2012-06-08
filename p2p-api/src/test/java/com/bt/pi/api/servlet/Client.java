/* (c) British Telecommunications plc, 2009, All Rights Reserved */
package com.bt.pi.api.servlet;

import java.io.IOException;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpException;
import org.apache.commons.httpclient.methods.PostMethod;

public class Client {
	public static void main(String[] args) throws HttpException, IOException {
		HttpClient httpClient = new HttpClient();
		PostMethod postMethod = new PostMethod("http://localhost:8773/?Action=RunInstances&ImageId=ami-60a54009&MaxCount=3&MinCount=1&Placement.AvailabilityZone=ueast-1a&Monitoring.Enabled=true&AuthParams");
		
		int result = httpClient.executeMethod(postMethod);
		System.out.println(result);
		System.out.println(postMethod.getResponseBodyAsString());
	}
}
