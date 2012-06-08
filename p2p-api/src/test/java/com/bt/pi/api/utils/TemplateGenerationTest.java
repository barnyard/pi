package com.bt.pi.api.utils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;

import com.amazonaws.ec2.doc.x20081201.RunInstancesDocument;
import com.bt.pi.api.utils.QueryParameterUtils;
import com.bt.pi.api.utils.SoapEnvelopeUtils;
import com.bt.pi.api.utils.SoapRequestFactory;
import com.bt.pi.api.utils.XmlMappingException;
import com.bt.pi.api.utils.XmlUtils;

import freemarker.template.Configuration;

public class TemplateGenerationTest {

	private String PLACEMENT_AVAILABILTY_ZONE = "Placement.AvailabilityZone";
	private String SECURITY_GROUP_1 = "SecurityGroup.1";
	private String USER_DATA = "UserData";
	
	private SoapRequestFactory soapRequestFactory;
	private Map<String, Object> parameters;
	private Configuration configuration;
	private SoapEnvelopeUtils soapEnvelopeUtils;
	private XmlUtils xmlUtils;
	private Map<String, String> params;
	
	@Before
	public void before(){
		soapRequestFactory = new SoapRequestFactory();
		parameters = new HashMap<String, Object>();
		configuration = new Configuration();
		configuration.setClassForTemplateLoading(getClass(), "/templates");
		soapRequestFactory.setConfiguration(configuration);
		soapEnvelopeUtils = new SoapEnvelopeUtils();
		xmlUtils = new XmlUtils();
		soapEnvelopeUtils.setXmlUtils(xmlUtils);
		
		params = new HashMap<String, String>();
		params.put("Action", "RunInstances");
		params.put("Version", "2008-12-01");
		params.put("MinCount", "1");
		params.put("MaxCount", "2");
		params.put("ImageId", "emi-123");
		params.put("KeyName", "key-123");
		params.put(SECURITY_GROUP_1, "sec-123");
		params.put("AdditionalInfo", "info-123");
		params.put(USER_DATA, "user-123");
		params.put("AddressingType", "add-123");
		params.put("InstanceType", "large");
		params.put(PLACEMENT_AVAILABILTY_ZONE, "zone");
		params.put("KernelId", "k-123");
		params.put("RamdiskId", "r-123");
		params.put("BlockDeviceMapping.1.Virtual", "v-123");
		params.put("BlockDeviceMapping.1.Device", "d-123");
	}
	
	@Test (expected=XmlMappingException.class)
	public void testNullValueGeneration(){
		// setup
		List<String> instances = new ArrayList<String>();
		instances.add("i-123");
		parameters.put("Action", "TerminateInstances");
		parameters.put("InstanceId", instances);
		parameters.put("Version", null);
		// act
		soapRequestFactory.getSoap(parameters);
	}

	@Test
	public void testTerminateInstancesGeneration(){
		// setup
		List<String> instances = new ArrayList<String>();
		instances.add("i-123");
		parameters.put("Action", "TerminateInstances");
		parameters.put("InstanceId", instances);
		parameters.put("Version", "2008-12-01");
		// act
		String result = soapRequestFactory.getSoap(parameters);
		// assert
		String expected = "<soap:Envelope xmlns:soap=\"http://schemas.xmlsoap.org/soap/envelope/\">" + 
			"<soap:Body><TerminateInstances xmlns=\"http://ec2.amazonaws.com/doc/2008-12-01/\">" + 
			"<instancesSet><item><instanceId>i-123</instanceId></item></instancesSet>" + 
			"</TerminateInstances></soap:Body></soap:Envelope>";
		assertEquals(expected, formatResult(result));
	}
	
	
	@Test
	public void testTerminateInstancesMultipleGeneration(){
		// setup
		List<String> instances = new ArrayList<String>();
		instances.add("i-123");
		instances.add("i-456");
		parameters.put("Action", "TerminateInstances");
		parameters.put("InstanceId", instances);
		parameters.put("Version", "2008-12-01");
		// act
		String result = soapRequestFactory.getSoap(parameters);
		// assert
		String expected = "<soap:Envelope xmlns:soap=\"http://schemas.xmlsoap.org/soap/envelope/\">" + 
			"<soap:Body><TerminateInstances xmlns=\"http://ec2.amazonaws.com/doc/2008-12-01/\">" + 
			"<instancesSet><item><instanceId>i-123</instanceId></item><item><instanceId>i-456" + 
			"</instanceId></item></instancesSet>" + 
			"</TerminateInstances></soap:Body></soap:Envelope>";
		assertEquals(expected, formatResult(result));		
	}
	
	@Test
	public void testDescribeInstancesSingleInstanceGeneration(){
		// setup
		List<String> instances = new ArrayList<String>();
		instances.add("i-123");
		parameters.put("Action", "DescribeInstances");
		parameters.put("InstanceId", instances);
		parameters.put("Version", "2008-12-01");
		// act
		String result = soapRequestFactory.getSoap(parameters);
		// assert
		String expected = "<soap:Envelope xmlns:soap=\"http://schemas.xmlsoap.org/soap/envelope/\">" + 
			"<soap:Body><DescribeInstances xmlns=\"http://ec2.amazonaws.com/doc/2008-12-01/\">" + 
			"<instancesSet><item><instanceId>i-123</instanceId></item></instancesSet>" +
			"</DescribeInstances></soap:Body></soap:Envelope>";
		assertEquals(expected, formatResult(result));
	}

	@Test
	public void testDescribeInstancesMultipleInstanceGeneration(){
		// setup
		List<String> instances = new ArrayList<String>();
		instances.add("i-123");
		instances.add("i-456");
		parameters.put("Action", "DescribeInstances");
		parameters.put("InstanceId", instances);
		parameters.put("Version", "2008-12-01");
		// act
		String result = soapRequestFactory.getSoap(parameters);
		// assert
		String expected = "<soap:Envelope xmlns:soap=\"http://schemas.xmlsoap.org/soap/envelope/\">" + 
			"<soap:Body><DescribeInstances xmlns=\"http://ec2.amazonaws.com/doc/2008-12-01/\">" + 
			"<instancesSet><item><instanceId>i-123</instanceId></item><item><instanceId>i-456</instanceId></item></instancesSet>" +
			"</DescribeInstances></soap:Body></soap:Envelope>";
		assertEquals(expected, formatResult(result));
	}
	
	@Test
	public void testDescribeInstancesAllInstancesGeneration(){
		// setup
		parameters.put("Action", "DescribeInstances");
		parameters.put("Version", "2008-12-01");
		parameters.put("ImageId", "emi-123");
		// act
		String result = soapRequestFactory.getSoap(parameters);
		// assert
		String expected = "<soap:Envelope xmlns:soap=\"http://schemas.xmlsoap.org/soap/envelope/\">" + 
			"<soap:Body><DescribeInstances xmlns=\"http://ec2.amazonaws.com/doc/2008-12-01/\">" + 
			"</DescribeInstances></soap:Body></soap:Envelope>";
		assertEquals(expected, formatResult(result));
	}
	
	@Test
	public void test20081201RunInstances() throws Exception {
		
		parameters = new QueryParameterUtils().sanitiseParameters(params);

		// act
		String result = soapRequestFactory.getSoap(parameters);
		// assert
		result = new String(soapEnvelopeUtils.removeEnvelope(result.getBytes()));
		RunInstancesDocument runInstancesDocument = RunInstancesDocument.Factory.parse(result);
		mandatoryAsserts(runInstancesDocument);
		assertRunInstance(runInstancesDocument);
		assertEquals("zone", runInstancesDocument.getRunInstances().getPlacement().getAvailabilityZone());
		assertEquals("sec-123", runInstancesDocument.getRunInstances().getGroupSet().getItemArray(0).getGroupId());

	}
	
	@Test
	public void test20081201RunInstancesNoUserData() throws Exception{
		// setup
		params.remove(USER_DATA);
		parameters = new QueryParameterUtils().sanitiseParameters(params);

		// act
		String result = soapRequestFactory.getSoap(parameters);

		// assert
		result = new String(soapEnvelopeUtils.removeEnvelope(result.getBytes()));
		RunInstancesDocument runInstancesDocument = RunInstancesDocument.Factory.parse(result);
		assertFalse(runInstancesDocument.getRunInstances().isSetUserData());
	}

	@Test
	public void test20081201RunInstancesNoPlacementZone() throws Exception{
		// setup
		params.remove(PLACEMENT_AVAILABILTY_ZONE);
		parameters = new QueryParameterUtils().sanitiseParameters(params);

		// act
		String result = soapRequestFactory.getSoap(parameters);

		// assert
		result = new String(soapEnvelopeUtils.removeEnvelope(result.getBytes()));
		RunInstancesDocument runInstancesDocument = RunInstancesDocument.Factory.parse(result);
		assertFalse(runInstancesDocument.getRunInstances().isSetPlacement());
	}
	
	@Test
	public void test20081201RunInstancesNoSecurityGroups() throws Exception{
		// setup
		params.remove(SECURITY_GROUP_1);
		parameters = new QueryParameterUtils().sanitiseParameters(params);

		// act
		String result = soapRequestFactory.getSoap(parameters);

		// assert
		result = new String(soapEnvelopeUtils.removeEnvelope(result.getBytes()));
		RunInstancesDocument runInstancesDocument = RunInstancesDocument.Factory.parse(result);
		assertNull(runInstancesDocument.getRunInstances().getGroupSet());
	}
	
	@Test
	public void test20081201RunInstancesNoBlockDevices() throws Exception{
		// setup
		params.remove("BlockDeviceMapping.1.Virtual");
		params.remove("BlockDeviceMapping.1.Device");
		parameters = new QueryParameterUtils().sanitiseParameters(params);

		// act
		String result = soapRequestFactory.getSoap(parameters);

		// assert
		result = new String(soapEnvelopeUtils.removeEnvelope(result.getBytes()));
		RunInstancesDocument runInstancesDocument = RunInstancesDocument.Factory.parse(result);
		assertFalse(runInstancesDocument.getRunInstances().isSetBlockDeviceMapping());
	}
	
	@Test
	public void test20081201RunInstancesOnlyBlockDeviceMappingVirtual() throws Exception{
		// setup
		params.remove("BlockDeviceMapping.1.Device");
		parameters = new QueryParameterUtils().sanitiseParameters(params);

		// act
		String result = soapRequestFactory.getSoap(parameters);

		// assert
		result = new String(soapEnvelopeUtils.removeEnvelope(result.getBytes()));
		RunInstancesDocument runInstancesDocument = RunInstancesDocument.Factory.parse(result);
		assertEquals("v-123", runInstancesDocument.getRunInstances().getBlockDeviceMapping().getItemArray(0).getVirtualName());
		assertNull(runInstancesDocument.getRunInstances().getBlockDeviceMapping().getItemArray(0).getDeviceName());
	}
	
	@Test
	public void test20081201RunInstancesOnlyBlockDeviceMappingDevice() throws Exception{
		// setup
		params.remove("BlockDeviceMapping.1.Virtual");
		parameters = new QueryParameterUtils().sanitiseParameters(params);

		// act
		String result = soapRequestFactory.getSoap(parameters);

		// assert
		result = new String(soapEnvelopeUtils.removeEnvelope(result.getBytes()));
		RunInstancesDocument runInstancesDocument = RunInstancesDocument.Factory.parse(result);
		assertEquals("d-123", runInstancesDocument.getRunInstances().getBlockDeviceMapping().getItemArray(0).getDeviceName());
		assertNull(runInstancesDocument.getRunInstances().getBlockDeviceMapping().getItemArray(0).getVirtualName());
	}
	
	@Test
	public void test20090404RunInstances() throws Exception {
		
		params.remove("Version");
		params.put("Version", "2009-04-04");
		params.put("Monitoring.Enabled", "true");
		parameters = new QueryParameterUtils().sanitiseParameters(params);

		// act
		String result = soapRequestFactory.getSoap(parameters);

		// assert
		result = new String(soapEnvelopeUtils.removeEnvelope(result.getBytes()));
		com.amazonaws.ec2.doc.x20090404.RunInstancesDocument runInstancesDocument = com.amazonaws.ec2.doc.x20090404.RunInstancesDocument.Factory.parse(result);
		mandatoryAsserts(runInstancesDocument);
		assertRunInstance(runInstancesDocument);
		assertEquals(true, runInstancesDocument.getRunInstances().getMonitoring().getEnabled());
	}

	@Test
	public void testAuthorizeSecurityGroupIngressNoSourceSecurityGroupName() {
		// setup
		params.put("Action", "AuthorizeSecurityGroupIngress");
		params.put("Version", "2008-12-01");
		params.put("GroupName", "default");
		params.put("IpProtocol", "tcp");
		params.put("FromPort", "80");
		params.put("ToPort", "90");
		params.put("SourceSecurityGroupOwnerId", "fred");
		parameters = new QueryParameterUtils().sanitiseParameters(params);
		
		// act
		String result = soapRequestFactory.getSoap(parameters);
		
		// assert
		result = new String(soapEnvelopeUtils.removeEnvelope(result.getBytes()));
		result = formatResult(result); 
		assertTrue(result.contains("<groups></groups>"));
		assertTrue(result.contains("<ipRanges></ipRanges>"));
	}
	
	@Test
	public void testAuthorizeSecurityGroupIngressNoSourceSecurityGroupOwnerId() {
		// setup
		params.put("Action", "AuthorizeSecurityGroupIngress");
		params.put("Version", "2008-12-01");
		params.put("GroupName", "default");
		params.put("IpProtocol", "tcp");
		params.put("FromPort", "80");
		params.put("ToPort", "90");
		params.put("SourceSecurityGroupName", "web");
		parameters = new QueryParameterUtils().sanitiseParameters(params);
		
		// act
		String result = soapRequestFactory.getSoap(parameters);
		
		// assert
		result = new String(soapEnvelopeUtils.removeEnvelope(result.getBytes()));
		result = formatResult(result); 
		assertTrue(result.contains("<groups></groups>"));
		assertTrue(result.contains("<ipRanges></ipRanges>"));
	}
	
	@Test
	public void testAuthorizeSecurityGroupIngressWithBothSourceSecurityGroupOwnerIdAndSourceSecurityGroupName() {
		// setup
		params.put("Action", "AuthorizeSecurityGroupIngress");
		params.put("Version", "2008-12-01");
		params.put("GroupName", "default");
		params.put("IpProtocol", "tcp");
		params.put("FromPort", "80");
		params.put("ToPort", "90");
		params.put("SourceSecurityGroupName", "web");
		params.put("SourceSecurityGroupOwnerId", "fred");
		parameters = new QueryParameterUtils().sanitiseParameters(params);
		
		// act
		String result = soapRequestFactory.getSoap(parameters);
		
		// assert
		result = new String(soapEnvelopeUtils.removeEnvelope(result.getBytes()));
		result = formatResult(result); 
		assertTrue(result.contains("<groups><item><userId>fred</userId><groupName>web</groupName></item></groups>"));
		assertTrue(result.contains("<ipRanges></ipRanges>"));
	}

	@Test
	public void testAuthorizeSecurityGroupIngressWithCidrIp() {
		// setup
		params.put("Action", "AuthorizeSecurityGroupIngress");
		params.put("Version", "2008-12-01");
		params.put("GroupName", "default");
		params.put("IpProtocol", "tcp");
		params.put("FromPort", "80");
		params.put("ToPort", "90");
		params.put("SourceSecurityGroupName", "web");
		params.put("CidrIp", "1.2.3.4");
		parameters = new QueryParameterUtils().sanitiseParameters(params);
		
		// act
		String result = soapRequestFactory.getSoap(parameters);
		
		// assert
		result = new String(soapEnvelopeUtils.removeEnvelope(result.getBytes()));
		result = formatResult(result); 
		assertTrue(result.contains("<groups></groups>"));
		assertTrue(result.contains("<ipRanges><item><cidrIp>1.2.3.4</cidrIp></item></ipRanges>"));
	}

	private String formatResult(String result) {
		return result.replaceAll(">[\\s]+<", "><").trim();
	}

	private void assertRunInstance(com.amazonaws.ec2.doc.x20081201.RunInstancesDocument runInstancesDocument) {
		assertEquals("key-123", runInstancesDocument.getRunInstances().getKeyName());
		assertEquals("info-123", runInstancesDocument.getRunInstances().getAdditionalInfo());
		assertEquals("user-123", runInstancesDocument.getRunInstances().getUserData().getData());
		assertEquals("add-123", runInstancesDocument.getRunInstances().getAddressingType());
		assertEquals("large", runInstancesDocument.getRunInstances().getInstanceType());
		assertEquals("k-123", runInstancesDocument.getRunInstances().getKernelId());
		assertEquals("r-123", runInstancesDocument.getRunInstances().getRamdiskId());
		assertEquals("v-123", runInstancesDocument.getRunInstances().getBlockDeviceMapping().getItemArray(0).getVirtualName());
		assertEquals("d-123", runInstancesDocument.getRunInstances().getBlockDeviceMapping().getItemArray(0).getDeviceName());
	}
	
	private void assertRunInstance(com.amazonaws.ec2.doc.x20090404.RunInstancesDocument runInstancesDocument) {
		assertEquals("key-123", runInstancesDocument.getRunInstances().getKeyName());
		assertEquals("sec-123", runInstancesDocument.getRunInstances().getGroupSet().getItemArray(0).getGroupId());
		assertEquals("info-123", runInstancesDocument.getRunInstances().getAdditionalInfo());
		assertEquals("user-123", runInstancesDocument.getRunInstances().getUserData().getData());
		assertEquals("add-123", runInstancesDocument.getRunInstances().getAddressingType());
		assertEquals("large", runInstancesDocument.getRunInstances().getInstanceType());
		assertEquals("zone", runInstancesDocument.getRunInstances().getPlacement().getAvailabilityZone());
		assertEquals("k-123", runInstancesDocument.getRunInstances().getKernelId());
		assertEquals("r-123", runInstancesDocument.getRunInstances().getRamdiskId());
		assertEquals("v-123", runInstancesDocument.getRunInstances().getBlockDeviceMapping().getItemArray(0).getVirtualName());
		assertEquals("d-123", runInstancesDocument.getRunInstances().getBlockDeviceMapping().getItemArray(0).getDeviceName());
	}	
    
	private void mandatoryAsserts(com.amazonaws.ec2.doc.x20081201.RunInstancesDocument runInstancesDocument) {
		assertEquals(1, runInstancesDocument.getRunInstances().getMinCount());
		assertEquals(2, runInstancesDocument.getRunInstances().getMaxCount());
		assertEquals("emi-123", runInstancesDocument.getRunInstances().getImageId());
	}

	private void mandatoryAsserts(com.amazonaws.ec2.doc.x20090404.RunInstancesDocument runInstancesDocument) {
		assertEquals(1, runInstancesDocument.getRunInstances().getMinCount());
		assertEquals(2, runInstancesDocument.getRunInstances().getMaxCount());
		assertEquals("emi-123", runInstancesDocument.getRunInstances().getImageId());
	}
}
