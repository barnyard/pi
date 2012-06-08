package com.bt.pi.api.utils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;

@SuppressWarnings("rawtypes")
public class QueryParameterUtilsTest {
    private Map<String, String> map;
    private QueryParameterUtils queryParameterUtils;

    @Before
    public void setUp() throws Exception {
        this.queryParameterUtils = new QueryParameterUtils();
        this.map = new HashMap<String, String>();
    }

    @Test
    public void testSanitiseParameters() {
        // setup
        map.put("Action", "RunInstances");

        // act
        Map<String, Object> result = queryParameterUtils.sanitiseParameters(map);

        // assert
        assertEquals(map, result);
    }

    @Test
    public void testSanitiseParametersDuplicatesVersionToInteger() {
        // setup
        map.put("Action", "RunInstances");
        map.put("Version", "2009-04-04");

        // act
        Map<String, Object> result = queryParameterUtils.sanitiseParameters(map);

        // assert
        assertEquals(3, result.size());
        assertEquals(20090404, result.get("VersionInteger"));
    }

    @Test
    public void testSanitiseParametersForRunInstancesSecurityGroupdot1() {
        // setup
        map.put("Action", "RunInstances");
        map.put("SecurityGroup.1", "default");

        // act
        Map<String, Object> result = queryParameterUtils.sanitiseParameters(map);

        // assert
        assertEquals("RunInstances", result.get("Action"));
        assertEquals(2, result.size());
        assertNotNull(result.get("SecurityGroup"));
        assertTrue(result.get("SecurityGroup") instanceof List);
        assertEquals(1, ((List) result.get("SecurityGroup")).size());
        assertEquals("default", ((List) result.get("SecurityGroup")).get(0));
    }

    @Test
    public void testSanitiseParametersForRunInstancesSecurityGroupdot1and2() {
        // setup
        map.put("Action", "RunInstances");
        map.put("SecurityGroup.2", "root");
        map.put("SecurityGroup.1", "default");

        // act
        Map<String, Object> result = queryParameterUtils.sanitiseParameters(map);

        // assert
        assertEquals("RunInstances", result.get("Action"));
        assertEquals(2, result.size());
        assertNotNull(result.get("SecurityGroup"));
        assertTrue(result.get("SecurityGroup") instanceof List);
        assertEquals(2, ((List) result.get("SecurityGroup")).size());
        assertEquals("default", ((List) result.get("SecurityGroup")).get(0));
        assertEquals("root", ((List) result.get("SecurityGroup")).get(1));
    }

    @Test
    public void testSanitiseParametersForRunInstancesPlacementDotAvailabilityZone() {
        // setup
        map.put("Action", "RunInstances");
        map.put("Placement.AvailabilityZone", "zone");

        // act
        Map<String, Object> result = queryParameterUtils.sanitiseParameters(map);

        // assert
        assertEquals("RunInstances", result.get("Action"));
        assertEquals(2, result.size());
        assertNotNull(result.get("Placement"));
        assertTrue(result.get("Placement") instanceof Map);
        assertEquals(1, ((Map) result.get("Placement")).size());
        assertEquals("zone", ((Map) result.get("Placement")).get("AvailabilityZone"));
    }

    @Test
    public void testSanitiseParametersForRunInstancesBlockDeviceMappingDot1() {
        // setup
        map.put("Action", "RunInstances");
        map.put("BlockDeviceMapping.1.VirtualName", "v1");
        map.put("BlockDeviceMapping.1.DeviceName", "d1");

        // act
        Map<String, Object> result = queryParameterUtils.sanitiseParameters(map);

        // assert
        assertEquals("RunInstances", result.get("Action"));
        assertEquals(2, result.size());
        Object o = result.get("BlockDeviceMapping");
        assertNotNull(o);
        assertTrue(o instanceof List);
        List list = (List) o;
        assertEquals(1, list.size());
        Object m = list.get(0);
        assertTrue(m instanceof Map);
        Map map = (Map) m;
        assertEquals(2, map.size());
        assertEquals("v1", map.get("VirtualName"));
        assertEquals("d1", map.get("DeviceName"));
    }

    @Test
    public void testSanitiseParametersForRunInstancesBlockDeviceMappingDot1And2() {
        // setup
        map.put("Action", "RunInstances");
        map.put("BlockDeviceMapping.1.VirtualName", "v1");
        map.put("BlockDeviceMapping.1.DeviceName", "d1");
        map.put("BlockDeviceMapping.2.VirtualName", "v2");
        map.put("BlockDeviceMapping.2.DeviceName", "d2");

        // act
        Map<String, Object> result = queryParameterUtils.sanitiseParameters(map);

        // assert
        assertEquals("RunInstances", result.get("Action"));
        assertEquals(2, result.size());
        Object o = result.get("BlockDeviceMapping");
        assertNotNull(o);
        assertTrue(o instanceof List);
        List list = (List) o;
        assertEquals(2, list.size());

        Object m1 = list.get(0);
        assertTrue(m1 instanceof Map);
        Map map1 = (Map) m1;
        assertEquals(2, map1.size());
        assertEquals("v1", map1.get("VirtualName"));
        assertEquals("d1", map1.get("DeviceName"));

        Object m2 = list.get(1);
        assertTrue(m2 instanceof Map);
        Map map2 = (Map) m2;
        assertEquals(2, map2.size());
        assertEquals("v2", map2.get("VirtualName"));
        assertEquals("d2", map2.get("DeviceName"));
    }

    @Test
    public void testSanitiseParametersForRunInstancesBlockDeviceMappingDot1And2And109() {
        // setup
        map.put("Action", "RunInstances");
        map.put("BlockDeviceMapping.1.VirtualName", "v1");
        map.put("BlockDeviceMapping.1.DeviceName", "d1");
        map.put("BlockDeviceMapping.2.VirtualName", "v2");
        map.put("BlockDeviceMapping.2.DeviceName", "d2");
        map.put("BlockDeviceMapping.109.DeviceName", "d109");
        map.put("BlockDeviceMapping.109.VirtualName", "v109");

        // act
        Map<String, Object> result = queryParameterUtils.sanitiseParameters(map);

        // assert
        assertEquals("RunInstances", result.get("Action"));
        assertEquals(2, result.size());
        Object o = result.get("BlockDeviceMapping");
        assertNotNull(o);
        assertTrue(o instanceof List);
        List list = (List) o;
        assertEquals(3, list.size());

        Object m1 = list.get(0);
        assertTrue(m1 instanceof Map);
        Map map1 = (Map) m1;
        assertEquals(2, map1.size());
        assertEquals("v1", map1.get("VirtualName"));
        assertEquals("d1", map1.get("DeviceName"));

        Object m2 = list.get(1);
        assertTrue(m2 instanceof Map);
        Map map2 = (Map) m2;
        assertEquals(2, map2.size());
        assertEquals("v2", map2.get("VirtualName"));
        assertEquals("d2", map2.get("DeviceName"));

        Object m3 = list.get(2);
        assertTrue(m3 instanceof Map);
        Map map3 = (Map) m3;
        assertEquals(2, map3.size());
        assertEquals("v109", map3.get("VirtualName"));
        assertEquals("d109", map3.get("DeviceName"));
    }

    @Test
    public void testSanitiseParametersInstanceIdDot1() {
        // setup
        map.put("Action", "DescribeInstances");
        map.put("InstanceId.1", "i-111");

        // act
        Map<String, Object> result = queryParameterUtils.sanitiseParameters(map);

        // assert
        assertEquals("DescribeInstances", result.get("Action"));
        assertEquals(2, result.size());
        Object o = result.get("InstanceId");
        assertNotNull(o);
        assertTrue(o instanceof List);
        List list = (List) o;
        assertEquals(1, list.size());
        assertEquals("i-111", list.get(0));
    }

    @Test
    public void testSanitiseParametersInstanceIdDot1And2And3() {
        // setup
        map.put("Action", "DescribeInstances");
        map.put("InstanceId.1", "i-111");
        map.put("InstanceId.3", "i-333");
        map.put("InstanceId.2", "i-222");

        // act
        Map<String, Object> result = queryParameterUtils.sanitiseParameters(map);

        // assert
        assertEquals("DescribeInstances", result.get("Action"));
        assertEquals(2, result.size());
        Object o = result.get("InstanceId");
        assertNotNull(o);
        assertTrue(o instanceof List);
        List list = (List) o;
        assertEquals(3, list.size());
        assertEquals("i-111", list.get(0));
        assertEquals("i-222", list.get(1));
        assertEquals("i-333", list.get(2));
    }

    @Test
    public void testSanitiseParametersRegionDot1And2And3() {
        // setup
        map.put("Action", "DescribeRegions");
        map.put("Region.1", "r-111");
        map.put("Region.3", "r-333");
        map.put("Region.2", "r-222");

        // act
        Map<String, Object> result = queryParameterUtils.sanitiseParameters(map);

        // assert
        assertEquals("DescribeRegions", result.get("Action"));
        assertEquals(2, result.size());
        Object o = result.get("Region");
        assertNotNull(o);
        assertTrue(o instanceof List);
        List list = (List) o;
        assertEquals(3, list.size());
        assertEquals("r-111", list.get(0));
        assertEquals("r-222", list.get(1));
        assertEquals("r-333", list.get(2));
    }

    @Test
    public void testSanitiseParametersInstanceIdBadIndex() {
        // setup
        map.put("Action", "DescribeInstances");
        map.put("InstanceId.1", "i-111");
        map.put("InstanceId.3", "i-333");
        map.put("InstanceId.Bogus", "i-222");

        // act
        Map<String, Object> result = queryParameterUtils.sanitiseParameters(map);

        // assert
        assertEquals("DescribeInstances", result.get("Action"));
        assertEquals(3, result.size());
        Object o = result.get("InstanceId");
        assertNotNull(o);
        assertTrue(o instanceof List);
        List list = (List) o;
        assertEquals(2, list.size());
        assertEquals("i-111", list.get(0));
        assertEquals("i-333", list.get(1));

        assertEquals("i-222", result.get("InstanceId.Bogus"));
    }

    @Test
    public void testSanitiseParametersInstanceIdMoreThan9() {
        // setup
        map.put("Action", "DescribeInstances");
        map.put("InstanceId.1", "i-111");
        map.put("InstanceId.10", "i-000");
        map.put("InstanceId.3", "i-333");
        map.put("InstanceId.2", "i-222");
        map.put("InstanceId.4", "i-444");
        map.put("InstanceId.5", "i-555");
        map.put("InstanceId.6", "i-666");
        map.put("InstanceId.7", "i-777");
        map.put("InstanceId.8", "i-888");
        map.put("InstanceId.9", "i-999");

        // act
        Map<String, Object> result = queryParameterUtils.sanitiseParameters(map);

        // assert
        assertEquals("DescribeInstances", result.get("Action"));
        assertEquals(2, result.size());
        Object o = result.get("InstanceId");
        assertNotNull(o);
        assertTrue(o instanceof List);
        List list = (List) o;
        assertEquals(10, list.size());
        assertEquals("i-111", list.get(0));
        assertEquals("i-222", list.get(1));
        assertEquals("i-333", list.get(2));
        assertEquals("i-444", list.get(3));
        assertEquals("i-555", list.get(4));
        assertEquals("i-666", list.get(5));
        assertEquals("i-777", list.get(6));
        assertEquals("i-888", list.get(7));
        assertEquals("i-999", list.get(8));
        assertEquals("i-000", list.get(9));
    }

    @SuppressWarnings("rawtypes")
    @Test
    public void testSanitiseParametersMonitoringEnabled() {
        // setup
        map.put("Action", "DescribeInstances");
        map.put("Monitoring.Enabled", "true");

        // act
        Map<String, Object> result = queryParameterUtils.sanitiseParameters(map);

        // assert
        assertEquals("DescribeInstances", result.get("Action"));
        assertEquals(2, result.size());
        Object o = result.get("Monitoring");
        assertNotNull(o);
        assertTrue(o instanceof Map);
        Map map = (Map) o;
        assertEquals(1, map.size());
        assertEquals("true", map.get("Enabled"));
    }

    @Test
    public void testQueryParametersAreUrlDecoded() throws IOException {
        // setup
        map.put("Action", "DescribeInstances");
        map.put("Monitoring.Enabled", "true");
        map.put("DeviceId", "%2Fdev%2Fsdb");

        // act
        Map<String, Object> result = queryParameterUtils.sanitiseParameters(map);

        // assert
        assertEquals("/dev/sdb", result.get("DeviceId"));
    }

    @Test
    public void testSanitiseParametersVolumes() throws Exception {
        // setup
        map.put("Action", "DescribeVolumes");
        map.put("VolumeId.1", "i-111");
        map.put("VolumeId.10", "i-000");
        map.put("VolumeId.3", "i-333");
        map.put("VolumeId.2", "i-222");
        map.put("VolumeId.4", "i-444");
        map.put("VolumeId.5", "i-555");
        map.put("VolumeId.6", "i-666");
        map.put("VolumeId.7", "i-777");
        map.put("VolumeId.8", "i-888");
        map.put("VolumeId.9", "i-999");
        // act
        Map<String, Object> result = queryParameterUtils.sanitiseParameters(map);
        // assert
        assertEquals("DescribeVolumes", result.get("Action"));
        assertEquals(2, result.size());
        Object o = result.get("VolumeId");
        assertNotNull(o);
        assertTrue(o instanceof List);
        List list = (List) o;
        assertEquals(10, list.size());
        assertEquals("i-111", list.get(0));
        assertEquals("i-222", list.get(1));
        assertEquals("i-333", list.get(2));
        assertEquals("i-444", list.get(3));
        assertEquals("i-555", list.get(4));
        assertEquals("i-666", list.get(5));
        assertEquals("i-777", list.get(6));
        assertEquals("i-888", list.get(7));
        assertEquals("i-999", list.get(8));
        assertEquals("i-000", list.get(9));
    }

    @Test
    public void testSanitiseParametersPublicIps() throws Exception {
        // setup
        map.put("Action", "DescribeAddresses");
        map.put("PublicIp.1", "1.1.1.1");
        map.put("PublicIp.10", "10.10.10.10");
        map.put("PublicIp.3", "3.3.3.3");
        map.put("PublicIp.2", "2.2.2.2");
        map.put("PublicIp.4", "4.4.4.4");
        map.put("PublicIp.5", "5.5.5.5");
        map.put("PublicIp.6", "6.6.6.6");
        map.put("PublicIp.7", "7.7.7.7");
        map.put("PublicIp.8", "8.8.8.8");
        map.put("PublicIp.9", "9.9.9.9");

        // act
        Map<String, Object> result = queryParameterUtils.sanitiseParameters(map);

        // assert
        assertEquals("DescribeAddresses", result.get("Action"));
        assertEquals(2, result.size());
        Object o = result.get("PublicIp");
        assertNotNull(o);
        assertTrue(o instanceof List);
        List list = (List) o;
        assertEquals(10, list.size());
        assertEquals("1.1.1.1", list.get(0));
        assertEquals("2.2.2.2", list.get(1));
        assertEquals("3.3.3.3", list.get(2));
        assertEquals("4.4.4.4", list.get(3));
        assertEquals("5.5.5.5", list.get(4));
        assertEquals("6.6.6.6", list.get(5));
        assertEquals("7.7.7.7", list.get(6));
        assertEquals("8.8.8.8", list.get(7));
        assertEquals("9.9.9.9", list.get(8));
        assertEquals("10.10.10.10", list.get(9));
    }

    @Test
    public void testSanitiseParametersZoneNames() throws Exception {
        // setup
        map.put("Action", "DescribeAvailabilityZones");
        map.put("ZoneName.1", "Zone1");
        map.put("ZoneName.0", "Zone0");
        map.put("ZoneName.10", "Zone10");
        map.put("ZoneName.345", "Zone345");
        map.put("ZoneName.9999", "Zone9999");

        // act
        Map<String, Object> result = queryParameterUtils.sanitiseParameters(map);

        // assert
        assertEquals("DescribeAvailabilityZones", result.get("Action"));
        assertEquals(2, result.size());
        Object o = result.get("ZoneName");
        assertNotNull(o);
        assertTrue(o instanceof List);
        List list = (List) o;
        assertEquals(5, list.size());
        assertEquals("Zone0", list.get(0));
        assertEquals("Zone1", list.get(1));
        assertEquals("Zone10", list.get(2));
        assertEquals("Zone345", list.get(3));
        assertEquals("Zone9999", list.get(4));
    }

    @Test
    public void testSanitiseParametersForDescribeImages() throws Exception {
        // setup
        map.put("Action", "DescribeImages");
        map.put("ExecutableBy.1", "Fred");
        map.put("ImageId.0", "kmi-111");
        map.put("ExecutableBy.2", "John");
        map.put("ImageId.2", "kmi-222");
        map.put("Owner.5", "FredOwner");
        map.put("Owner.45", "JohnOwner");

        // act
        Map<String, Object> result = queryParameterUtils.sanitiseParameters(map);

        // assert
        assertEquals(4, result.size());
        assertEquals("DescribeImages", result.get("Action"));

        Object imageIds = result.get("ImageId");
        assertNotNull(imageIds);
        assertTrue(imageIds instanceof List);
        List imageIdsList = (List) imageIds;
        assertEquals(2, imageIdsList.size());
        assertEquals("kmi-111", imageIdsList.get(0));
        assertEquals("kmi-222", imageIdsList.get(1));

        Object executableBys = result.get("ExecutableBy");
        assertNotNull(executableBys);
        assertTrue(executableBys instanceof List);
        List executableBysList = (List) executableBys;
        assertEquals(2, executableBysList.size());
        assertEquals("Fred", executableBysList.get(0));
        assertEquals("John", executableBysList.get(1));

        Object owners = result.get("Owner");
        assertNotNull(owners);
        assertTrue(owners instanceof List);
        List ownersList = (List) owners;
        assertEquals(2, ownersList.size());
        assertEquals("FredOwner", ownersList.get(0));
        assertEquals("JohnOwner", ownersList.get(1));
    }

    @Test
    public void testSanitiseParametersKeyNameSingle() throws Exception {
        // setup
        map.put("Action", "CreateKeyPair");
        map.put("KeyName", "testKey");

        // act
        Map<String, Object> result = queryParameterUtils.sanitiseParameters(map);

        // assert
        assertEquals("CreateKeyPair", result.get("Action"));
        assertEquals(2, result.size());
        assertEquals("testKey", result.get("KeyName"));
    }

    @Test
    public void testSanitiseParametersKeyNameMultiple() throws Exception {
        // setup
        map.put("Action", "CreateKeyPair");
        map.put("KeyName.1", "testKey1");
        map.put("KeyName.100", "testKey100");

        // act
        Map<String, Object> result = queryParameterUtils.sanitiseParameters(map);

        // assert
        assertEquals("CreateKeyPair", result.get("Action"));
        assertEquals(2, result.size());
        Object o = result.get("KeyName");
        assertTrue(o instanceof List);
        List list = (List) o;
        assertEquals(2, list.size());
        assertEquals("testKey1", list.get(0));
        assertEquals("testKey100", list.get(1));
    }

    @Test
    public void testGroupNameArray() throws Exception {
        // setup
        map.put("Action", "DescribeSecurityGroups");
        map.put("GroupName.1", "name1");
        map.put("GroupName.21", "name21");
        map.put("GroupName.100", "name100");

        // act
        Map<String, Object> result = queryParameterUtils.sanitiseParameters(map);

        // assert
        assertEquals("DescribeSecurityGroups", result.get("Action"));
        assertEquals(2, result.size());
        Object o = result.get("GroupName");
        assertTrue(o instanceof List);
        List list = (List) o;
        assertEquals(3, list.size());
        assertEquals("name1", list.get(0));
        assertEquals("name21", list.get(1));
        assertEquals("name100", list.get(2));
    }
}
