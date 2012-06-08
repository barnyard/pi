package com.bt.pi.app.common.entities;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import java.util.HashMap;
import java.util.Map;

import org.junit.Test;

import com.bt.pi.core.parser.KoalaJsonParser;

public class InstanceTypesTest {
    private InstanceTypes instanceTypes = new InstanceTypes();;
    private KoalaJsonParser koalaJsonParser = new KoalaJsonParser();

    @Test
    public void testDefaults() throws Exception {
        // assert
        assertThat(instanceTypes.getType(), equalTo(InstanceTypes.class.getSimpleName()));
        assertThat(instanceTypes.getUrl(), equalTo("instancetypes:all"));
    }

    @Test
    public void testMapSetterGetter() throws Exception {
        // setup
        Map<String, InstanceTypeConfiguration> map = new HashMap<String, InstanceTypeConfiguration>();

        // act
        instanceTypes.setInstanceTypes(map);

        // assert
        assertThat(instanceTypes.getInstanceTypes(), equalTo(map));
    }

    @Test
    public void testAddAndGetInstanceTypeConfiguration() throws Exception {
        // setup
        InstanceTypeConfiguration instanceTypeConfiguration1 = new InstanceTypeConfiguration("test1", 1, 2, 3);
        InstanceTypeConfiguration instanceTypeConfiguration2 = new InstanceTypeConfiguration("test2", 11, 21, 31);

        // act
        instanceTypes.addInstanceType(instanceTypeConfiguration1);
        instanceTypes.addInstanceType(instanceTypeConfiguration2);

        // assert
        assertThat(instanceTypes.getInstanceTypeConfiguration("test1"), equalTo(instanceTypeConfiguration1));
        assertThat(instanceTypes.getInstanceTypeConfiguration("test2"), equalTo(instanceTypeConfiguration2));
        assertNull(instanceTypes.getInstanceTypeConfiguration("test3"));
    }

    @Test
    public void shouldReturnNullForInstanceTypeConfigurationIfInstanceTypeIsNull() {
        assertNull(instanceTypes.getInstanceTypeConfiguration(null));
    }

    @Test
    public void testSerialisation() {
        // setup
        Map<String, InstanceTypeConfiguration> map = new HashMap<String, InstanceTypeConfiguration>();
        map.put("type1", new InstanceTypeConfiguration("type1", 3, 1024, 10));
        instanceTypes.setInstanceTypes(map);

        // act
        String json = koalaJsonParser.getJson(instanceTypes);
        InstanceTypes result = (InstanceTypes) koalaJsonParser.getObject(json, InstanceTypes.class);

        // assert
        assertEquals(instanceTypes.getInstanceTypes(), result.getInstanceTypes());
    }

    @Test
    public void oldFormatShouldNotBeDeprecated() {
        // setup
        String json = "{ \"type\" : \"InstanceTypes\",   \"url\" : \"instancetypes:all\",  \"instanceTypes\" : {" + "\"type1\" : {" + "\"instanceType\" : \"type1\"," + "\"numCores\" : 3," + "\"memorySizeInMB\" : 1024," + "\"diskSizeInGB\" : 10"
                + "}}," + "\"uriScheme\" : \"instancetypes\"," + "\"version\" : 0}";

        // act
        InstanceTypes result = (InstanceTypes) koalaJsonParser.getObject(json, InstanceTypes.class);

        // assert
        assertFalse(result.getInstanceTypes().get("type1").isDeprecated());
    }

    @Test
    public void testDeprecatedFromJson() {
        // setup
        String json = "{ \"type\" : \"InstanceTypes\",   \"url\" : \"instancetypes:all\",  \"instanceTypes\" : {" + "\"type1\" : {" + "\"instanceType\" : \"type1\"," + "\"numCores\" : 3," + "\"memorySizeInMB\" : 1024,"
                + "\"diskSizeInGB\" : 10, \"deprecated\" : true" + "}}," + "\"uriScheme\" : \"instancetypes\"," + "\"version\" : 0}";

        // act
        InstanceTypes result = (InstanceTypes) koalaJsonParser.getObject(json, InstanceTypes.class);

        // assert
        assertTrue(result.getInstanceTypes().get("type1").isDeprecated());
    }
}
